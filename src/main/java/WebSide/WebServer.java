package WebSide;

import GRPCConnection.AddressProxy;
import GRPCConnection.GRPCClient.GRPCClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class WebServer
{
    private final String Search_EndPoint = "/search";
    private GRPCClient grpcClient;
    private AddressProxy addressProxy;
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private final int port;
    private HttpServer server;

    public WebServer(int port) throws IOException, InterruptedException
    {
        this.port = port;
        grpcClient = new GRPCClient();
        addressProxy = AddressProxy.start();
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        int serverPort = 5566;
        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);
        }

        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();

        System.out.println("Server is listening on port " + serverPort);
        logger.info("Server is listening on port " + serverPort);
    }
    public void startServer() {
        try
        {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            logger.info("Server Started at : " + System.nanoTime());
            System.out.println("Server Started");
        }
        catch (IOException e) {
            logger.error("Server failed to Start at : " + System.nanoTime());
            System.out.println("Server failed");
            throw new RuntimeException(e);
        }

        HttpContext searchContext = server.createContext(Search_EndPoint);

        searchContext.setHandler(this::handleSearchRequest);
        HttpContext htmlContext = server.createContext("/");
        htmlContext.setHandler(this::handleHtmlRequest);

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

    }

    private void handleSearchRequest(HttpExchange exchange) throws IOException
    {
        logger.info("Search Request at " + System.nanoTime());
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();

        String query = new String(requestBytes);
        query = extractQuery(query);
        //System.out.println(query);
        List<String> files = new ArrayList<>();
        try
        {
            String o = addressProxy.getAddress();
            files = grpcClient.search(query , o);
        }
        catch (InterruptedException | KeeperException e)
        {
            files.add("Some Thing Went Wrong !!");
        }
        if (files.size() == 0)
        {
            files.add("NO MATCH FOUND");
        }
        sendResponseAsJson(files, exchange);
    }
    private void sendResponseAsJson(List<String> responseList, HttpExchange exchange) throws IOException
    {
        JsonArray jsonArray = new JsonArray();
        for (String file : responseList) {
            jsonArray.add(file);
        }

        JsonObject jsonResponse = new JsonObject();
        jsonResponse.add("files", jsonArray);

        sendResponseJson(jsonResponse.toString(), exchange);
    }

    private void sendResponseJson(String jsonResponse, HttpExchange exchange) throws IOException
    {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(jsonResponse.getBytes());
        outputStream.flush();
        outputStream.close();
        exchange.close();
    }

    private String extractQuery(String query)
    {
        query = query.split(":")[1];
        query = query.replace("\"" , "");
        query = query.replace("}" , "");
        return query;
    }
    private void handleHtmlRequest(HttpExchange exchange) throws IOException {
        String filePath = "index.html";
        File file = new File(filePath);
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, fileBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(fileBytes);
        outputStream.flush();
        outputStream.close();
        exchange.close();
    }

}
