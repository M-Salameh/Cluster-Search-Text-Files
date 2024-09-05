package WebSide;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class WebClient {

    private HttpClient client;

    //static final Logger logger = LoggerFactory.getLogger(WebClient.class);
    public WebClient()
    {
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    }


    public JsonObject sendQuery(String url, String query) throws ExecutionException, InterruptedException
    {
        /*JsonParser jsonParser = new JsonParser();
        JsonObject queryPayload = jsonParser.parse(query).getAsJsonObject();*/

        JsonReader jsonReader = new JsonReader(new StringReader(query));
        jsonReader.setLenient(true);

        CompletableFuture<JsonObject> response = new CompletableFuture<>();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(query))
                .build();

        response = client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    JsonObject jsonObject = null;
                    try {
                        jsonObject = JsonParser.parseString(body).getAsJsonObject();
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                    return jsonObject;
                });

        return response.join();
    }


    /* send task (post http request) asynchronously */
    /*public String sendQuery(String url, byte[] queryPayload) throws ExecutionException, InterruptedException {
        CompletableFuture<String> response = new CompletableFuture<>();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(queryPayload))
                .build();

        response = client.sendAsync
                        (httpRequest ,
                                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).
                thenApply(HttpResponse::body);

        String tem = response.join();
        return tem;
    }*/

}
