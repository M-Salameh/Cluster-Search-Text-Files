package org.AutoHealerAndClusterSearch.AutoHealerAndClusterSearch;

import GRPCConnection.GRPCServiceStart;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher
{
    private static final String WORKERS_ZNODES_PATH = "/workers";
    private static final String PHYSICAL_ZNODES_PATH = "/physical_nodes";

    private static final String COORDINATOR_ZNODE_PATH = "/coordinator_node";
    private final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    private final ZooKeeper zooKeeper;

    private TransientWorker transientWorker = null;
    private String currentZnode = null;
    private static List<String> allServiceAddresses = null;
    
    public ServiceRegistry(ZooKeeper zooKeeper)
    {
        this.zooKeeper = zooKeeper;
        createServiceRegistryZnode();
    }

    private void createServiceRegistryZnode()
    {
        try
        {
            if (zooKeeper.exists(WORKERS_ZNODES_PATH, false) == null) {
                zooKeeper.create(WORKERS_ZNODES_PATH, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            if (zooKeeper.exists(PHYSICAL_ZNODES_PATH, false) == null) {
                zooKeeper.create(PHYSICAL_ZNODES_PATH, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            if (zooKeeper.exists(COORDINATOR_ZNODE_PATH, false) == null) {
                zooKeeper.create(COORDINATOR_ZNODE_PATH, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }
        catch (KeeperException | InterruptedException e)
        {
            logger.error("Could NOT Create Service Registry Znode");
            e.printStackTrace();
        }
    }

    public void registerToCluster(String metadata) throws KeeperException, InterruptedException, IOException {

        if (this.currentZnode != null)
        {
            logger.info("Already registered to service registry");
        }
        else {
            this.currentZnode = zooKeeper.create(PHYSICAL_ZNODES_PATH + "/physical_node_", metadata.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("Registered to service registry");
        }
        transientWorker = new TransientWorker(metadata);
        transientWorker.start();
    }
    public void registerToCoordinator(String metadata) throws InterruptedException, KeeperException, IOException
    {
        this.currentZnode = zooKeeper.create(COORDINATOR_ZNODE_PATH + "/coordinator_", metadata.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        logger.info("Registered to be Coordinator and I am Master !");
        //Coordinator coordinator = new Coordinator();

        GRPCServiceStart.start();
    }
    public void registerForUpdates() {
        try
        {
            masterJob();
        }
        catch (KeeperException | InterruptedException | IOException e)
        {
            logger.error("Could Not Do LEADER JOB !");
            e.printStackTrace();
        }
    }

    public void unregisterFromCluster()
    {
        try
        {
            if (currentZnode != null && zooKeeper.exists(currentZnode, false) != null) {
                zooKeeper.delete(currentZnode, -1);
            }
        }
        catch (KeeperException | InterruptedException e)
        {
            logger.error("Could Not UN-Register From Cluster !!");
            e.printStackTrace();
        }
    }

    private synchronized void masterJob() throws InterruptedException, KeeperException, IOException {
        updateAddresses();
    }

    private void updateAddresses() throws KeeperException, InterruptedException
    {
        List<String> workerZnodes = zooKeeper.getChildren(PHYSICAL_ZNODES_PATH, this);

        List<String> addresses = new ArrayList<>(workerZnodes.size());

        for (String workerZnode : workerZnodes) {
            String workerFullPath = PHYSICAL_ZNODES_PATH + "/" + workerZnode;
            Stat stat = zooKeeper.exists(workerFullPath, false);
            if (stat == null) {
                continue;
            }

            byte[] addressBytes = zooKeeper.getData(workerFullPath, false, stat);
            String address = new String(addressBytes);
            addresses.add(address);
        }

        this.allServiceAddresses = Collections.unmodifiableList(addresses);
        logger.info("The cluster addresses are: " + this.allServiceAddresses);

    }


    public static List<String> getAllServiceAddresses()
    {
        return allServiceAddresses;
    }


    @Override
    public void process(WatchedEvent watchedEvent)
    {
        switch (watchedEvent.getType())
        {
            case NodeChildrenChanged:
            {
                try
                {
                    masterJob();
                }
                catch (InterruptedException | KeeperException | IOException e)
                {
                    logger.error("Could NOT Handle Node Children Changed Event!");
                    throw new RuntimeException(e);
                }
            }

        }
    }


}
