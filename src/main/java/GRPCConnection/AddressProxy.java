package GRPCConnection;

import org.AutoHealerAndClusterSearch.AutoHealerAndClusterSearch.Application;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class AddressProxy implements Watcher
{
    private static final String address = "192.168.184.10:2181";
    private static final int SESSION_TIMEOUT = 3000; //dead client

    private static final String COORDINATOR_ZNODE_PATH = "/coordinator_node";
    private ZooKeeper zooKeeper;

    private String IP;

    private final Logger logger = LoggerFactory.getLogger(AddressProxy.class);

    public static AddressProxy start() throws IOException, InterruptedException {
        AddressProxy addressProxy = new AddressProxy();
        ZooKeeper zooKeeper = addressProxy.connectToZookeeper();
        //addressProxy.run();
        //addressProxy.close();
        return addressProxy;
    }

    private synchronized void updateAddress() throws InterruptedException, KeeperException
    {
        List<String> temp = zooKeeper.getChildren(COORDINATOR_ZNODE_PATH , this);
        if (temp.isEmpty())  return;

        String node = temp.get(0);
        node = COORDINATOR_ZNODE_PATH + "/" + node;
        Stat stat = zooKeeper.exists(node , false);
        IP = new String(zooKeeper.getData(node , false , stat));
        IP = IP.split(":")[0];
    }

    public String getAddress() throws InterruptedException, KeeperException
    {
        if (IP==null)
        {
            updateAddress();
        }
        return IP;
    }
    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType())
        {
            case NodeChildrenChanged ->
            {
                try {
                    updateAddress();
                } catch (InterruptedException | KeeperException e)
                {
                    logger.error("Error Updating Coordinator address in Proxy");
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public ZooKeeper connectToZookeeper() throws IOException
    {
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
}
