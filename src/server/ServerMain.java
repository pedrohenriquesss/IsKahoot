package server;

import java.net.*;
import java.util.*;
import game.*;

public class ServerMain {

    private static final int PORT = 8080;
    private static final ActiveGames actives = new ActiveGames();

    public static void main(String[] args) throws Exception {
        try (ServerSocket ss = new ServerSocket(PORT)) {
            System.out.println("Servidor à escuta na porta " + PORT);
            new Thread(ServerMain::tui).start(); // thread para comandos no terminal
            while (true) {
                Socket socket = ss.accept(); // espera por clientes
                new DealWithClient(socket, actives).start(); // trata cada cliente numa thread
            }
        }
    }

    private static void tui() {
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String line = sc.nextLine().trim();
                if (line.isEmpty())
                    continue;

                String[] p = line.split(" ");
                if (p[0].equals("new") && p.length == 4) {
                    try {
                        Quiz quiz = QuizLoader.loadQuiz("resources/quizzes.json");
                        String code = actives.createGame(quiz, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                        if (code != null)
                            System.out.println("Jogo criado: " + code);
                        else
                            System.out.println("Erro ao criar jogo");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Comando desconhecido");
                }
            }
        }
    }
}