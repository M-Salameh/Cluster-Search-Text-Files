package GRPCConnection.GRPCServer;

import io.grpc.stub.StreamObserver;
import org.AutoHealerAndClusterSearch.AutoHealerAndClusterSearch.Coordinator;
import org.AutoHealerAndClusterSearch.SearchReply;
import org.AutoHealerAndClusterSearch.SearchRequest;
import org.AutoHealerAndClusterSearch.SearchServiceGrpc;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * the server of GRPC is the coordinator
 * it has threads embedded , we must configure the Coordinator only yo
 * handle requests sent from here , no need to act like a Server !!
 * */

@GRpcService
public class GRPCServer extends SearchServiceGrpc.SearchServiceImplBase
{
    @Override
    public void search(SearchRequest request, StreamObserver<SearchReply> responseObserver)
    {
        //logger.info("Request Has Arrived , Query is :" + request.getQuery());

        List<String> ans = new ArrayList<>();

        String x = request.getQuery();
        ans.add("replying to : " + x);

        try {
            ans = Coordinator.search(x);
        } catch (IOException | InterruptedException | KeeperException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        SearchReply reply = SearchReply.newBuilder().addAllFiles(ans).build();

        responseObserver.onNext(reply);

        responseObserver.onCompleted();
    }
}
