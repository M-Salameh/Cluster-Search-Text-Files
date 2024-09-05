package org.AutoHealerAndClusterSearch.AutoHealerAndClusterSearch;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OnElectionAction implements OnElectionCallback
{

    private final Logger logger = LoggerFactory.getLogger(OnElectionAction.class);
    private final ServiceRegistry serviceRegistry;
    private final int port;

    public OnElectionAction(ServiceRegistry serviceRegistry, int port)
    {
        this.serviceRegistry = serviceRegistry;
        this.port = port;
    }

    @Override
    public void onElectedToBeLeader(String IP)
    {
        serviceRegistry.unregisterFromCluster();
        serviceRegistry.registerForUpdates();
        try
        {
            serviceRegistry.registerToCoordinator(IP);
            //start the server
        }
        catch (InterruptedException | KeeperException | IOException e)
        {
            logger.error("Could Not Register to be Coordinator");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onWorker(String IP)
    {
        try
        {
            serviceRegistry.registerToCluster(IP);
        }
        catch (InterruptedException | KeeperException | IOException e)
        {
            logger.error("Could Not Register To Cluster");
            e.printStackTrace();
        }

    }
}
