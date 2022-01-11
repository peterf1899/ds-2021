package blockchain.node;


public class Heartbeat implements Runnable {

    private static final int PERIOD = 5000;

    private Node node;
    private boolean running;

    public Heartbeat(Node node) {
        this.node = node;
        this.running = false;
    }

    public void start() {
        // Start new thread for this object when it is not running
        if (!isRunning()) {
            this.running = true;
            new Thread(this).start();
        }
        else
            System.out.println("Heartbeat is already running.");
    }

    public void stop() {
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void run() {
        System.out.println("Heartbeat is started.");

        // Activity of heartbeat
        while (isRunning()) {
            this.node.connectNeighbors();
            try {
                Thread.sleep(PERIOD);
            } catch (InterruptedException ignored) {
            }
        }

        System.out.println("Heartbeat is stopped.");
    }
}
