package org.AutoHealerAndClusterSearch.AutoHealerAndClusterSearch;

import org.AutoHealerAndClusterSearch.ObjectExchangeInCluster.FileWordPair;
import org.AutoHealerAndClusterSearch.ObjectExchangeInCluster.SearchQueryRequest;
import org.AutoHealerAndClusterSearch.ObjectExchangeInCluster.SearchQueryResponse;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class Coordinator
{

    private static  String FILES_DIRECTORY = System.getProperty("user.dir") + "/SearchFiles/";;
    private static final Logger logger = LoggerFactory.getLogger(Coordinator.class);

    public static List<String> search(String query) throws IOException, InterruptedException, KeeperException, ExecutionException {
        List<SearchQueryResponse> responses = spreadQuery(query);
        if (responses == null)
        {
            List<String> msg = new ArrayList<>();
            msg.add("No Nodes Are Working , Search Cannot Be Done");
            logger.warn("No Nodes Are Working , Search Cannot Be Done");
            return msg;
        }

        Map<String,Double> filesScore = getFilesScore(responses , query);

        List<String> answer = getFilesInOrder(filesScore);

        //printResponses(answer);

        return answer;
    }

    private static  SearchQueryResponse sendRequestToNode(SearchQueryRequest searchQueryRequest, String ipAddress)
    {
        SearchQueryResponse searchQueryResponse = null;
        String ip = ipAddress.split(":")[0];
        int port = Integer.parseInt(ipAddress.split(":")[1]);
        try (Socket socket = new Socket(ip, port))
        {
          
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(searchQueryRequest);

            // Receive the response
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            searchQueryResponse = (SearchQueryResponse) objectInputStream.readObject();


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return searchQueryResponse;
    }

    private static  void printResponses(List<String> filesAnswer)
    {
        ///GRPC Connection
        if (filesAnswer.size() == 0)
        {
            System.out.println("No Matches Found !!!");
            return;
        }
        for (String file : filesAnswer)
        {
            System.out.println(file);
        }
    }


    private static  void handleClient(String query)
    {
        try
        {
            List<SearchQueryResponse> respons = spreadQuery(query);
            if (respons == null)
            {
                System.out.println("No Nodes Are Working , Search Cannot Be Done");
                logger.warn("No Nodes Are Working , Search Cannot Be Done");
            }

            Map<String,Double> filesScore = getFilesScore(respons , query);
            List<String> answer = getFilesInOrder(filesScore);

            printResponses(answer);

        }
        catch (IOException | InterruptedException | KeeperException e)
        {
            /**
             * retry the responses ???
            * */
            e.printStackTrace();
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }

    }


    /**
     * response from cluster node is map where the keys are words
     * and values are list of pair<fileName,Number Of Appearance of the word in it>
     * we now must calculate the importance of each file for every word we got
     * Each slave will Send the set of words with their frequency percentage in each file
     * and EACH SLAVE HAS A UNIQUE SET OF FILES !!
     * */
    private static  Map<String , Double> getFilesScore(List<SearchQueryResponse> respons , String query)
    {
        if (respons == null) return null;
        if (query.isEmpty()) return null;
        Map<String , Double> wordsIDF = calculateIDF(respons , query);
        Map<String , Double> fileScore = new HashMap<>();

        String[] words= query.split(" ");
        for (String word : words)
        {
            for (SearchQueryResponse response : respons)
            {
                Map<String , Double> temp = response.calcScoreForFilesContainingWord(word , wordsIDF.get(word));
                for(Map.Entry<String , Double> x : temp.entrySet())
                {
                    fileScore.put(x.getKey() , fileScore.getOrDefault(x.getKey() , 0.0) + x.getValue());
                }
            }
        }
        return fileScore;
    }

    private static  List<String> getFilesInOrder(Map<String , Double> filesScore)
    {

        List<String> files = new ArrayList<>();

        if(filesScore == null)
        {
            files.add("No Answer ! -0");
            return files;
        }
        List<Map.Entry<String, Double>> entryList = new ArrayList<>(filesScore.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> entry1, Map.Entry<String, Double> entry2) {
                // Sort in descending order by comparing the values
                return entry2.getValue().compareTo(entry1.getValue());
            }
        });
        for (Map.Entry<String, Double> entry : entryList)
        {
            files.add(entry.getKey() + " : SCORE = "+entry.getValue());
        }
        return files;

    }

    /**
     * Each slave will Send the set of words with their frequency percentage in each file
     * and EACH SLAVE HAS A UNIQUE SET OF FILES !!
     */
    private static  Map<String , Double> calculateIDF(List<SearchQueryResponse> respons , String query)
    {
        Map<String , Double> wordsIDF = new HashMap<>();
        int totalNumberOfFiles = countFilesInDirectory();

        String[] words = query.split(" ");

        for (String word : words)
        {
            int wordFreq = 0;
            for (SearchQueryResponse slaveAns : respons)
            {
                Map<String , List<FileWordPair>> temp = slaveAns.getWordFrequencies();
                wordFreq += temp.getOrDefault(word , new ArrayList<>()).size();
            }
            Double d = wordFreq > 0 ? Math.log(1.0*totalNumberOfFiles/wordFreq) : 0;
            wordsIDF.put(word , d);
        }
        return wordsIDF;
    }
    private static  int countFilesInDirectory()
    {
        try (Stream<Path> files = Files.list(Paths.get(FILES_DIRECTORY)))
        {
            return (int) files.count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * this method broadcasts the query for all nodes in the cluster
     * and tells them what files are every node responsible for !!
     * and try to distribute responsibility as much as possible !!!
     * then receive response from every node
     * response is map here the keys are words
     * and values are list of pair<fileName,Number Of Appearance of the word in it>
     * */
    public static List<SearchQueryResponse> spreadQuery(String query) throws InterruptedException, KeeperException, IOException, ExecutionException
    {
        List<String> physicalZnodesAddresses = ServiceRegistry.getAllServiceAddresses();

        if (physicalZnodesAddresses.isEmpty())
        {
            return null;
        }
        int totalFilesNumber = countFilesInDirectory();

        //System.out.println("Files Number = " + totalFilesNumber);
        int filesNumberforNode = (totalFilesNumber + physicalZnodesAddresses.size()-1)/physicalZnodesAddresses.size();
        int remaining = totalFilesNumber;
        int index = 0;
        int filesOffset=0;

        ExecutorService executorService = Executors.newFixedThreadPool(physicalZnodesAddresses.size());
        List<Callable<SearchQueryResponse>> tasks = new ArrayList<>();

        ///distributing Files for NODES!!!
        while (remaining > 0)
        {

            String ipAddress = physicalZnodesAddresses.get(index);
            SearchQueryRequest searchQueryRequest = new SearchQueryRequest(query , filesNumberforNode , filesOffset);
            tasks.add(() -> sendRequestToNode(searchQueryRequest, ipAddress));
            index = (1 + index) % physicalZnodesAddresses.size();
            filesOffset = (filesOffset + filesNumberforNode) % totalFilesNumber;
            remaining -= filesNumberforNode;
        }

        List<Future<SearchQueryResponse>> futures = executorService.invokeAll(tasks);

        List<SearchQueryResponse> respons = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++)
        {
            SearchQueryResponse searchQueryResponse = futures.get(i).get();
            respons.add(searchQueryResponse);
        }
        executorService.shutdown();

        return respons;
    }

}
