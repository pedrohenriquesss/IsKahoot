package server;

import java.io.*;
import java.net.*;

public class DealWithClient extends Thread {
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ActiveGames actives;
    private GameState gameState;
    private String username;
    private volatile boolean connected = true;

    public DealWithClient(Socket socket, ActiveGames actives) throws IOException {
        this.socket = socket;
        this.actives = actives;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() { // thread do cliente
        try {
            String join = in.readLine();
            if (join == null) return;

            String[] parts = join.split(" ");
            if (parts.length < 4 || !"JOIN".equals(parts[0])) {
                out.println("ERROR Formato inválido");
                return;
            }

            String code = parts[1];
            String team = parts[2];
            username = parts[3];

            gameState = actives.getGame(code);
            if (gameState == null) {
                out.println("ERROR Jogo não encontrado");
                return;
            }
            if (!gameState.addPlayer(username, team, this)) {
                out.println("ERROR User já existe ou jogo já começou");
                return;
            }

            out.println("JOINED");

            String msg;
            while (connected && (msg = in.readLine()) != null) {
                if (msg.startsWith("ANSWER ")) {
                    try {
                        int answer = Integer.parseInt(msg.split(" ")[1]); // resposta do jogador à pergunta
                        gameState.submitAnswer(username, answer);
                    } catch (NumberFormatException e) {
                        send("ERROR Resposta inválida");
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Cliente " + username + " desconectado");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // ignora
            }
            connected = false;
        }
    }

    public void send(String msg) {
        if (connected)
            out.println(msg); 
    }
    
}