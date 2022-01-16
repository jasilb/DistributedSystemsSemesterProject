package edu.yu.cs.com3800.stage1;


import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

public class ClientImpl implements Client{
    String hostName;
    int hostPort;

    HttpResponse<String> response;
    String body;
    int code=0;
    URL url;
    HttpClient client;
    HttpRequest request;

    public ClientImpl(String hostName, int hostPort) throws MalformedURLException {
        this.hostName= hostName;
        this.hostPort=hostPort;
        url = new URL("http",hostName, hostPort ,"/compileandrun");
        //url = new URL("http://"+hostName+":"+ hostPort+"/compileandrun");

    }

    @Override
    public void sendCompileAndRunRequest(String src) throws IOException {
        if (src==null){
            throw new IOException("scr==null");
        }
         client = HttpClient.newHttpClient();


         //String send = " " +src;
        try {

            request = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .header("Content-Type", "text/x-java-source")
                    .POST(HttpRequest.BodyPublishers.ofString(src))
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (URISyntaxException | InterruptedException e) {
            throw new IOException(e.getMessage());
        }

    }

    @Override
    public Response getResponse() throws IOException {
        if (response==null){
            return null;
        }


        return new Response(response.statusCode(),response.body());

    }






}
