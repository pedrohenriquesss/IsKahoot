package server;

import java.util.*;
import game.*;

public class GameState {
    
    private final List<Question> questions;
    private int currentQuestionIndex = 0;

    private final Map<String, DealWithClient> clients = new HashMap<>();
    private final Map<String, String> playerTeam = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();

    private final int maxTeams;
    private final int playersPerTeam;
    private final int totalPlayers;
    private final int questionCount;

    private final ActiveGames actives;
    private final String gameCode;

    private boolean roundActive = false;
    private int roundTimeLeft = 0;

    private ModifiedCountdownLatch individualLatch;
    private final Map<String, TeamBarrier> teamBarriers = new HashMap<>();
    private final Map<String, Integer> currentAnswers = new HashMap<>();
    private final Map<String, Integer> playerBonus = new HashMap<>();

    private boolean gameStarted = false;
    private final Object roundLock = new Object();

    private Thread timeoutThread;
    private Thread timerThread; // atualiza o temporizador

    public GameState(List<Question> questions, int maxTeams, int playersPerTeam, int questionCount, ActiveGames actives, String gameCode) {
        this.questions = questions;
        this.maxTeams = maxTeams;
        this.playersPerTeam = playersPerTeam;
        this.totalPlayers = maxTeams * playersPerTeam;
        this.questionCount = Math.min(questionCount, questions.size());
        this.actives = actives;
        this.gameCode = gameCode;
    }

    public synchronized boolean addPlayer(String username, String teamName, DealWithClient client) {
        if (gameStarted) return false;
        if (playerTeam.containsKey(username)) return false;
        if (!teams.containsKey(teamName) && teams.size() >= maxTeams) return false;

        playerTeam.put(username, teamName);
        clients.put(username, client); // guarda thread do cliente

        teams.putIfAbsent(teamName, new Team(teamName));
        teams.get(teamName).addPlayer(new Player(username)); // adiciona jogador à equipa

        System.out.println("Jogador " + username + " entrou na equipa " + teamName);
        System.out.println("Progresso: " + playerTeam.size() + "/" + totalPlayers);

        if (playerTeam.size() == totalPlayers) { // se já entraram todos, começa o jogo
            gameStarted = true;
            new GameHandler(this, actives, gameCode).start();
        }
        return true;
    }

    public void startRound(int seconds) {
        if (timeoutThread != null && timeoutThread.isAlive()) {
            timeoutThread.interrupt();
        }
        if (timerThread != null && timerThread.isAlive()) {
            timerThread.interrupt();
        }
        
        //limpa o estado da ronda anterior
        synchronized (roundLock) {
            currentAnswers.clear();
            playerBonus.clear();
            teamBarriers.clear();
            roundTimeLeft = seconds;
            roundActive = true;
        }

        Question q = getCurrentQuestion();
        if (q.getType() == QuestionType.INDIVIDUAL) { // barreira para as perguntas individuais
            individualLatch = new ModifiedCountdownLatch(2, 2, seconds * 1000L, totalPlayers);
            timeoutThread = new Thread(() -> {   // espera as respostas
                try {
                    individualLatch.await();
                } catch (InterruptedException e) {
                    return;
                }
                finishRound();
            });
            timeoutThread.start();

        } else {
            for (Team t : getTeamsCopy()) {  // 1 barreira por equipa
                teamBarriers.put(t.getName(), new TeamBarrier(playersPerTeam, seconds * 1000L, null, t.getName()));
            }
            timeoutThread = new Thread(() -> {
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException e) {
                    return;
                }
                finishRound();
            });
            timeoutThread.start();
        }

        timerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                int time;
                synchronized (roundLock) {
                    if (!roundActive) break;
                    time = roundTimeLeft;
                }
                broadcast("TIMER|" + time);
                if (time <= 0) {
                    finishRound();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                synchronized (roundLock) {
                    if (roundActive && roundTimeLeft > 0)
                        roundTimeLeft--;
                }
            }
        });
        timerThread.start();
    }

    private void finishRound() {
        synchronized (roundLock) {
            if (!roundActive) return;
            roundActive = false;
            roundTimeLeft = 0;
            roundLock.notifyAll();
        }
        synchronized (this) {
            this.notifyAll();
        }
    }

    private synchronized List<Team> getTeamsCopy() {
        return new ArrayList<>(teams.values());
    }

    public void submitAnswer(String username, int option) {
        Question q = getCurrentQuestion();
        if (q.getType() == QuestionType.TEAM)
            submitTeamAnswer(username, option);
        else
            submitIndividualAnswer(username, option);
    }

    private void submitIndividualAnswer(String username, int option) {
        boolean shouldFinish = false;
        synchronized (roundLock) {
            if (!roundActive || currentAnswers.containsKey(username)) return;
            int factor = individualLatch.countdown();
            currentAnswers.put(username, option);
            playerBonus.put(username, factor);
            if (currentAnswers.size() == totalPlayers)
                shouldFinish = true;
        }
        if (shouldFinish)
            finishRound();
    }

    private void submitTeamAnswer(String username, int option) {
        String teamName;
        boolean shouldFinish = false;
        synchronized (roundLock) {
            if (!roundActive || currentAnswers.containsKey(username)) return;
            currentAnswers.put(username, option);
            teamName = playerTeam.get(username);
            if (currentAnswers.size() == totalPlayers)
                shouldFinish = true;
        }
        TeamBarrier barrier = teamBarriers.get(teamName);
        if (barrier != null) {
            barrier.addAnswer(username, option);
            try {
                barrier.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (shouldFinish) 
        	finishRound();  
    }
    
    public void updateScores() {
        Question q = getCurrentQuestion();
        if (q == null) return;

        // reinicia os pontos ganhos nesta ronda
        synchronized (this) {
            for (Team t : teams.values())
                t.setLastRoundPoints(0);
        }

        if (q.getType() == QuestionType.TEAM) {
            updateTeamScores(q);
            return;
        }

        for (Map.Entry<String, Integer> entry : getAnswersCopy().entrySet()) {
            String player = entry.getKey();
            int answer = entry.getValue();
            if (!q.isCorrectOption(answer)) continue; // ignora respostas erradas
            String teamName = playerTeam.get(player);
            synchronized (this) {
                Team team = teams.get(teamName);
                if (team == null) continue;
                int factor = playerBonus.getOrDefault(player, 1);
                int points = q.getPoints() * factor;
                team.addPoints(points);
                team.setLastRoundPoints(team.getLastRoundPoints() + points);
            }
        }
    }

    private void updateTeamScores(Question q) {
        synchronized (this) {
            for (Team team : teams.values()) {
                boolean allCorrect = true;
                boolean atLeastOneCorrect = false;
                TeamBarrier barrier = teamBarriers.get(team.getName());
                if (barrier == null) continue;
                Map<String, Integer> teamAnswers = barrier.getAnswers();
                for (Player p : team.getPlayers()) {
                    Integer answers = teamAnswers.get(p.getUsername());
                    if (answers != null && q.isCorrectOption(answers)) {
                        atLeastOneCorrect = true;
                    } else {
                        allCorrect = false;
                    }
                }
                int earned = 0;
                // todos acertaram
                if (allCorrect && team.getPlayers().size() == playersPerTeam) {
                    earned = q.getPoints() * 2;

                // pelo menos um acertou
                } else if (atLeastOneCorrect) {
                    earned = q.getPoints();
                }
                team.addPoints(earned);
                team.setLastRoundPoints(earned);
            }
        }
    }

    private Map<String, Integer> getAnswersCopy() {
        synchronized (roundLock) {
            return new HashMap<>(currentAnswers);
        }
    }

    public synchronized Question getCurrentQuestion() {
        if (currentQuestionIndex >= questions.size()) return null;
        return questions.get(currentQuestionIndex);
    }

    public synchronized void nextQuestion() {
        currentQuestionIndex++;
    }

    public synchronized boolean hasMoreQuestions() {
        return currentQuestionIndex < questionCount;
    }

    public int getRoundTimeLeft() {
        synchronized (roundLock) {
            return roundTimeLeft;
        }
    }

    public boolean isRoundActive() {
        synchronized (roundLock) {
            return roundActive;
        }
    }

    public synchronized List<Team> getLeaderboard() {
        List<Team> list = new ArrayList<>(teams.values());
        list.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
        return list;
    }

    public void broadcast(String msg) {
        List<DealWithClient> copy;
        synchronized (this) {
            copy = new ArrayList<>(clients.values());
        }
        for (DealWithClient c : copy)
            c.send(msg);
    }
    
}