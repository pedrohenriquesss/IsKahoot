package client;

public class ClientMain {

    public static void main(String[] args) throws Exception {

        if (args.length != 5) {
            System.out.println("Uso: java ClientMain <IP> <PORTO> <JOGO> <EQUIPA> <USERNAME>");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String game = args[2];
        String team = args[3];
        String username = args[4];

        ClientConnection conn = new ClientConnection(ip, port); // cria uma ligação ao servidor
        conn.send("JOIN " + game + " " + team + " " + username); // tenta entrar no jogo
        String response = conn.receive();

        if (response.startsWith("ERROR")) {
            System.out.println(response);
            return;
        }
        if (!response.equals("JOINED")) {
            System.out.println("Resposta inesperada");
            return;
        }

        KahootGUI gui = new KahootGUI(conn, username);
        gui.process("JOINED");
        while (true) {
            String msg = conn.receive(); // espera mensagens do servidor
            if (msg == null) break;
            gui.process(msg); // atualiza a interface
        }
        conn.close();
    }
}