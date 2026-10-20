package server;

import game.*;
import java.util.*;

// basicamente controla o decorrer do jogo
public class GameHandler extends Thread {

    private final GameState gameState;
    private final ActiveGames actives;
    private final String code;

    public GameHandler(GameState gameState, ActiveGames actives, String code) {
        this.gameState = gameState;
        this.actives = actives;
        this.code = code;
    }

    @Override
    public void run() {
        System.out.println("GameHandler iniciado para o jogo " + code);
        try {
            // percorre as perguntas
            while (gameState.hasMoreQuestions()) {
                Question q = gameState.getCurrentQuestion();
                sendQuestion(q);
                gameState.startRound(30); // inicia uma ronda de 30 segundos
                
                // espera até ao fim da ronda
                synchronized (gameState) {
                    while (gameState.isRoundActive())
                        gameState.wait();
                }
                gameState.updateScores();
                sendScoreboard();
                Thread.sleep(3000);
                gameState.nextQuestion();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // classificação final
            List<Team> leaderboard = gameState.getLeaderboard();
            StringBuilder sb = new StringBuilder("GAME_OVER|");
            for (int i = 0; i < leaderboard.size(); i++) {
                Team t = leaderboard.get(i);
                sb.append(i + 1).append("º lugar: ").append(t.getName()).append(" - ").append(t.getPoints()).append(" pontos");
                if (i < leaderboard.size() - 1)
                    sb.append("|");
            }
            gameState.broadcast(sb.toString());
            actives.removeGame(code);
            System.out.println("Jogo " + code + " terminado");
        }
    }

    // envia a pergunta aos clientes
    private void sendQuestion(Question q) {
        StringBuilder sb = new StringBuilder("QUESTION|");
        sb.append(q.getQuestion()).append("|");
        List<String> options = q.getOptions();
        for (int i = 0; i < options.size(); i++) {
            sb.append(i + 1).append(": ").append(options.get(i));
            if (i < options.size() - 1)
                sb.append("|");
        }
        gameState.broadcast(sb.toString());
    }

    // envia a classificação da ronda
    private void sendScoreboard() {
        List<Team> leaderboard = gameState.getLeaderboard();
        StringBuilder sb = new StringBuilder("SCOREBOARD");
        sb.append("| RESULTADO DA RONDA ");
        for (int i = 0; i < leaderboard.size(); i++) {
            Team t = leaderboard.get(i);
            sb.append("|").append(i + 1).append("º lugar: ").append(t.getName()).append(" - ").append(t.getPoints()).append(" pontos").append(" (+").append(t.getLastRoundPoints()).append(" nesta ronda)");
        }
        gameState.broadcast(sb.toString());
    }
}