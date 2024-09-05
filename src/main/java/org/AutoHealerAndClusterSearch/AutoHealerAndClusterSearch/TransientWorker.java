package org.AutoHealerAndClusterSearch.AutoHealerAndClusterSearch;

import org.AutoHealerAndClusterSearch.ObjectExchangeInCluster.SearchQueryRequest;
import org.AutoHealerAndClusterSearch.ObjectExchangeInCluster.SearchQueryResponse;
import org.AutoHealerAndClusterSearch.WordsCountingInFiles.WordsCountingInFiles;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TransientWorker
{
    private static final String ZOOKEEPER_ADDRESS = "192.168.184.10:2181";
    private static final int SESSION_TIMEOUT = 3000;

    private final Logger logger = LoggerFactory.getLogger(TransientWorker.class);

    private static final String WORKERS_ZNODES_PATH = "/workers";

    private int CLUSTER_PORT;

    private ZooKeeper zooKeeper;

    private String filesLocation;
    private String myName = "";
    private String regex = "^";


    public TransientWorker (String SOCKET) throws IOException {
        CLUSTER_PORT =Integer.parseInt(SOCKET.split(":")[1]);
        filesLocation = System.getProperty("user.dir") + "/SearchFiles/";
        System.out.println("FL = " + filesLocation);
        connectToZookeeper();
    }
    public void connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, event -> {
        });
    }


    public void start() throws KeeperException, InterruptedException, IOException
    {
        ServerSocket serverSocket = new ServerSocket(CLUSTER_PORT);
        System.out.println("Server started on port " + CLUSTER_PORT);
        logger.info("Server started on port " + CLUSTER_PORT);

        while (true)
        {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Coordinator connected: " + clientSocket.getInetAddress());
            logger.info("Coordinator connected: " + clientSocket.getInetAddress());

            Thread clientThread = new Thread(() ->
            {
                SearchQueryResponse searchQueryResponse = null;
                try
                {
                    searchQueryResponse = handleCoordinator(clientSocket);
                    sendResponsesToClient(clientSocket, searchQueryResponse);
                }
                catch (IOException | ClassNotFoundException e)
                {
                    throw new RuntimeException(e);
                }

            });

            clientThread.start();
        }
    }

    private void sendResponsesToClient(Socket clientSocket, SearchQueryResponse searchQueryResponseMap) throws IOException
    {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        objectOutputStream.writeObject(searchQueryResponseMap);
        objectOutputStream.close();
    }

    private SearchQueryResponse handleCoordinator(Socket clientSocket) throws IOException, ClassNotFoundException
    {
        SearchQueryRequest searchQueryRequest = extractRequest(clientSocket);

        int numberOfFilesToScan = searchQueryRequest.getNumberOfFilesToScan();
        int filesOffset= searchQueryRequest.getFilesOffset();

        List<String> myPortionOfFiles = getMyFiles(numberOfFilesToScan , filesOffset);

        SearchQueryResponse response = processFiles(searchQueryRequest, myPortionOfFiles);

        return response;
    }

    private SearchQueryRequest extractRequest(Socket clientSocket) throws IOException, ClassNotFoundException
    {
        ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
        SearchQueryRequest searchQueryRequest = (SearchQueryRequest) objectInputStream.readObject();
        return searchQueryRequest;
    }

    private List<String> getMyFiles(int numberOfFilesToScan, int filesOffset)
    {
        List<String> fileNames = new ArrayList<>();

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(Paths.get(filesLocation))) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    fileNames.add(filesLocation+file.getFileName().toString());
                }
            }
        } catch (IOException e)
        {
            logger.error("Can not Access Files");
            e.printStackTrace();
        }
        int startIndex = Math.min(filesOffset, fileNames.size());
        int endIndex = Math.min(filesOffset + numberOfFilesToScan, fileNames.size());
        System.out.println("My Share of Files : " + (endIndex-startIndex));

        return fileNames.subList(startIndex, endIndex);
    }

    private SearchQueryResponse processFiles(SearchQueryRequest query, List<String> myFiles)
    {
        List<String> words = query.getQueryWords();
        SearchQueryResponse searchQueryResponse = new SearchQueryResponse();

        searchQueryResponse.setWordFrequencies
                            (WordsCountingInFiles.countWordsInFiles(words , myFiles));

        return searchQueryResponse;
    }
}

