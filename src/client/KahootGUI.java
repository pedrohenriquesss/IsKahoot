package client;

import javax.swing.*;
import java.awt.*;

public class KahootGUI {

    private JFrame frame;
    private JLabel questionLabel;
    private JLabel timerLabel;
    private JTextArea scoreboardArea;
    private JList<String> optionsList;
    private DefaultListModel<String> listModel;
    private JButton answerButton;
    private int selectedOptionIndex = -1;

    public KahootGUI(ClientConnection connection, String username) {
        frame = new JFrame("IsKahoot - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        JPanel centerPanel = new JPanel(new BorderLayout());
        questionLabel = new JLabel("Aguardando pergunta...", SwingConstants.CENTER);

        questionLabel.setFont(new Font("Arial", Font.BOLD, 16));
        centerPanel.add(questionLabel, BorderLayout.NORTH);
        listModel = new DefaultListModel<>();
        optionsList = new JList<>(listModel);
        optionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        optionsList.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // ativa botão quando escolhe resposta
        optionsList.addListSelectionListener(e -> {
            selectedOptionIndex = optionsList.getSelectedIndex();
            answerButton.setEnabled(selectedOptionIndex != -1);
        });

        JScrollPane listScroll = new JScrollPane(optionsList);
        centerPanel.add(listScroll, BorderLayout.CENTER);
        answerButton = new JButton("Responder");
        answerButton.setEnabled(false);

        // envia as resposta ao servidor
        answerButton.addActionListener(e -> {
        	if (selectedOptionIndex != -1) {
        		connection.send("ANSWER " + selectedOptionIndex);
                // nao deixa responder várias vezes
                answerButton.setEnabled(false);
                optionsList.setEnabled(false);
            } else {
                JOptionPane.showMessageDialog(frame, "Selecione uma opção");
            }
        });

        centerPanel.add(answerButton, BorderLayout.SOUTH);
        timerLabel = new JLabel("--", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 24));

        scoreboardArea = new JTextArea();
        scoreboardArea.setEditable(false);
        scoreboardArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scoreScroll = new JScrollPane(scoreboardArea);
        scoreScroll.setPreferredSize(new Dimension(300, 400));

        frame.add(timerLabel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(scoreScroll, BorderLayout.EAST);
        frame.setSize(900, 600);
        frame.setVisible(true);
    }

    // processa as mensagens vindas do servidor
    public void process(String msg) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Cliente recebeu: " + msg);
            // pergunta nova
            if (msg.startsWith("QUESTION|")) {
                String[] parts = msg.split("\\|");
                if (parts.length >= 2) {
                    questionLabel.setText("<html><b>" +  parts[1] + "</b></html>");
                    listModel.clear();
                    // adiciona as opções
                    for (int i = 2; i < parts.length; i++) {
                    	listModel.addElement(parts[i]);
                    }
                    optionsList.setEnabled(true);
                    answerButton.setEnabled(false);
                    selectedOptionIndex = -1;
                    optionsList.clearSelection();
                }
                
            // atualiza o temporizador
            } else if (msg.startsWith("TIMER|")) {
                String time = msg.substring(6);
                timerLabel.setText("Tempo: " + time + " s");

            // atualiza classificação
            } else if (msg.startsWith("SCOREBOARD|")) {
                String board = msg.substring("SCOREBOARD|".length());
                board = board.replace("|", "\n");
                scoreboardArea.setText(board);
                scoreboardArea.setCaretPosition(0);

            // fim do jogo
            } else if (msg.startsWith("GAME_OVER|")) {
                String[] parts = msg.split("\\|");
                StringBuilder sb = new StringBuilder();
                sb.append("Fim do jogo!\n\n");
                for (int i = 1; i < parts.length; i++)
                    sb.append(parts[i]).append("\n");
               
                JOptionPane.showMessageDialog(frame, sb.toString());
                answerButton.setEnabled(false);
                optionsList.setEnabled(false);
            
            } else if (msg.equals("JOINED")) {
                System.out.println("Ligado com sucesso");

            // erro vindo do servidor
            } else if (msg.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(frame, msg, "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}