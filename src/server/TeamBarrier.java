package server;

import java.util.*;
import java.util.concurrent.locks.*;

public class TeamBarrier {
    
	private final int teamMembers;
    private int arrived = 0;
    private boolean released = false;
    private final Runnable barrierAction;
    private final long timeout;
    private final Map<String, Integer> teamAnswers = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public TeamBarrier(int teamMembers, long timeout, Runnable barrierAction, String teamName) {
        this.teamMembers = teamMembers;
        this.timeout = timeout;
        this.barrierAction = barrierAction;
    }

    public void addAnswer(String playerName, int answer) {
        lock.lock();
        try {
            teamAnswers.put(playerName, answer);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Integer> getAnswers() {
        lock.lock();
        try {
            return new HashMap<>(teamAnswers);
        } finally {
            lock.unlock();
        }
    }

    public void await() throws InterruptedException {
        lock.lock();
        try {
            arrived++; // um membro da equipa chegou
            if (released) // se a barreira já foi libertada
            	return;
            if (arrived == teamMembers) {
                released = true;
                if (barrierAction != null) // se já chegaram todos, conta os pontos
                	barrierAction.run();
                condition.signalAll(); // acorda as threads bloqueadas
                return;
            }
            long start = System.currentTimeMillis();
            while (!released) {
                long remaining = timeout - (System.currentTimeMillis() - start);
                if (remaining <= 0) { // se o tempo acabou, conta os pontos
                    released = true;
                    if (barrierAction != null) 
                    	barrierAction.run();
                    condition.signalAll();
                    return;
                }
                condition.await(remaining, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

}