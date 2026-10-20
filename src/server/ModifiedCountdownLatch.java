package server;

public class ModifiedCountdownLatch {
    
	private int count;
    private int bonusFactor;
    private int bonusCount;
    private long waitPeriod;
    private boolean released = false;

    public ModifiedCountdownLatch(int bonusFactor, int bonusCount, long waitPeriod, int count) {
        this.bonusFactor = bonusFactor;
        this.bonusCount = bonusCount;
        this.waitPeriod = waitPeriod;
        this.count = count;
    }

    public synchronized int countdown() {
        if (released || count <= 0) 
        	return 1;
        
        count--;
        int factor = (bonusCount > 0) ? bonusFactor : 1; // se houver bónus para dar, dá, se não, o fator é só 1
        if (bonusCount > 0) 
        	bonusCount--;
        
        if (count == 0) { // -> já todos os jogadores responderam
        	released = true; 
        	notifyAll(); 
        }
        return factor;
    }

    public synchronized void await() throws InterruptedException {
        if (released) return;
        long start = System.currentTimeMillis();
        while (count > 0 && !released) {
            long remaining = waitPeriod - (System.currentTimeMillis() - start);
            if (remaining <= 0) { 
            	released = true; 
            	notifyAll(); 
            	return; 
            }
            wait(remaining);
        }
    }

}