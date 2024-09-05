package GRPCConnection.GRPCClient;



import GRPCConnection.AddressProxy;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.AutoHealerAndClusterSearch.AutoHealerAndClusterSearch.Coordinator;
import org.AutoHealerAndClusterSearch.SearchReply;
import org.AutoHealerAndClusterSearch.SearchRequest;
import org.AutoHealerAndClusterSearch.SearchServiceGrpc;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.checkerframework.checker.units.qual.A;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * the client here will be the web server
 * configure it later
 * */
public class GRPCClient
{
    public List<String> search(String query , String ip) throws InterruptedException, KeeperException
    {

        ManagedChannel channel = ManagedChannelBuilder.
                                    forAddress(ip, 6565).
                                    usePlaintext().build();

        SearchServiceGrpc.SearchServiceBlockingStub stub = SearchServiceGrpc.newBlockingStub(channel);
        SearchRequest request = SearchRequest.newBuilder().setQuery(query).build();
        SearchReply reply = stub.search(request);
        List<String> stringList = reply.getFilesList();
        if(stringList == null || stringList.size()==0 )
        {
            stringList = new ArrayList<>();
            stringList.add("NO MATCH FOUND");
        }
        //SearchRequest request = SearchRequest.newBuilder().setQuery("There is a file to be read be full of kindness").build();
        return stringList;
    }


}
