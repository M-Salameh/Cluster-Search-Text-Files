package WebSide;

import com.google.gson.JsonObject;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class ClientApplication
{
    private static final String SearchEngine = "http://localhost:5566/search";

    public static void main(String[] args) throws ExecutionException, InterruptedException
    {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Search Query : ");
        String query = scanner.nextLine();
        WebClient webClient = new WebClient();
        JsonObject ans = webClient.sendQuery(SearchEngine , query);
        String[] kk = ans.toString().split(",");

        for (String k : kk)
        {
            System.out.println(k);
        }
    }
}
