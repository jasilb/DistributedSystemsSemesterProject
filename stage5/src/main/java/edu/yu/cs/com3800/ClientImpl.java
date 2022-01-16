package edu.yu.cs.com3800;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

class ClientImpl extends Thread implements LoggingServer {
    String hostName;
    int hostPort;
    Logger logger;
    HttpResponse<String> response;
    String body;
    int code = 0;
    URL url;
    HttpClient client;
    HttpRequest request;
    int num;
    String src;

    public ClientImpl(String hostName, int hostPort, int num) throws MalformedURLException {
        this.hostName = hostName;
        this.hostPort = hostPort;
        this.num = num;
        url = new URL("http", hostName, hostPort, "/compileandrun");
        //url = new URL("http://"+hostName+":"+ hostPort+"/compileandrun");
        logger = initializeLogging("client " + hostPort + "_" + num);
        logger.info("start");

    }

    @Override
    public void run() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("sending \n" + src);

        client = HttpClient.newHttpClient();


        //String send = " " +src;
        try {

            request = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .header("Content-Type", "text/x-java-source")
                    .POST(HttpRequest.BodyPublishers.ofString(src))
                    .build();
            System.out.println("send");
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (URISyntaxException e) {
            logger.warning(Util.getStackTrace(e));
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("send");
        System.out.println(response.body().toString());
        return;

    }

    public void sendCompileAndRunRequest(String src) {
        this.src = src;
    }

    public void changeURL(String src) throws MalformedURLException {
        url = new URL("http", hostName, hostPort, "/" + src);

    }



}
