package server;

import java.util.*;
import game.*;

public class ActiveGames {

    private final Map<String, GameState> actives = new HashMap<>();

    public synchronized String createGame(Quiz quiz, int teams, int playersPerTeam, int questionCount) {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 5).toUpperCase(); // gera código aleatório para o jogo
        } while (actives.containsKey(code));
        GameState gs = new GameState(quiz.getQuestions(), teams, playersPerTeam, questionCount, this, code);
        actives.put(code, gs); // guarda o jogo ativo
        return code;
    }

    public synchronized GameState getGame(String code) {
        return actives.get(code);
    }

    public synchronized void removeGame(String code) {
        actives.remove(code); // remove quando acaba
        System.out.println("Jogo removido: " + code);
    }
}