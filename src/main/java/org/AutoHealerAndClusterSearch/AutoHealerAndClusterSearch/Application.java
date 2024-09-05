package org.AutoHealerAndClusterSearch.AutoHealerAndClusterSearch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;

public class Application implements Watcher
{

    private static final String address = "192.168.184.10:2181";
    private static final int SESSION_TIMEOUT = 3000; //dead client
    //private static final int DEFAULT_PORT = 54321;
    private ZooKeeper zooKeeper;

    private final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {

        /*String IP = args.length == 1 ? args[0] : "127.0.0.1:55451";
        int port = Integer.parseInt(IP.split(":")[1]);*/

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Port !!");

        String IP = "127.0.0.1";
        int port = scanner.nextInt();
        System.out.println("IP is : " + IP );
        System.out.println("Port is : " + port);
        Application application = new Application();
        ZooKeeper zooKeeper = application.connectToZookeeper();

        ServiceRegistry serviceRegistry = new ServiceRegistry(zooKeeper);

        OnElectionAction onElectionAction = new OnElectionAction(serviceRegistry, port);

        LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
        leaderElection.volunteerForLeadership(IP+":"+port);
        leaderElection.reelectLeader();

        application.run();
        application.close();

    }

    public ZooKeeper connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(address, SESSION_TIMEOUT, this);
        return zooKeeper;
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    private void close() throws InterruptedException {
        this.zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    logger.info("Successfully connected to Zookeeper");
                } else if (watchedEvent.getState() == Event.KeeperState.Disconnected) {
                    synchronized (zooKeeper) {
                       logger.warn("Disconnected from Zookeeper");
                        zooKeeper.notifyAll();
                    }
                } else if (watchedEvent.getState() == Event.KeeperState.Closed) {
                    logger.info("Closed Successfully");
                }
                break;
        }
    }
}
