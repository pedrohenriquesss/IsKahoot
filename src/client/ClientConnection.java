package client;

import java.io.*;
import java.net.*;

public class ClientConnection {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientConnection(String host, int port) throws IOException {
        socket = new Socket(host, port); // liga ao servidor
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void send(String msg) {
        out.println(msg); // envia uma linha para o servidor
    }

    public String receive() throws IOException {
        return in.readLine(); // lê uma linha recebida do servidor
    }

    public void close() throws IOException {
        socket.close(); // fecha a ligação
    }
    
}