package game;

import java.util.*;

// representa uma equipa do jogo
public class Team {

    private String name;
    private List<Player> players = new ArrayList<>();
    private int points = 0;
    private int lastRoundPoints = 0;

    public Team(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // adiciona o jogador à equipa
    public void addPlayer(Player p) {
        players.add(p);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getPoints() {
        return points;
    }

    // soma os pontos à equipa
    public void addPoints(int p) {
        points += p;
    }

    public int getLastRoundPoints() {
        return lastRoundPoints;
    }

    public void setLastRoundPoints(int lastRoundPoints) {
        this.lastRoundPoints = lastRoundPoints;
    }
}