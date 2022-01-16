import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class ClientImpl extends Thread implements LoggingServer {
    String hostName;
    int hostPort;
    Logger logger;
    HttpResponse<String> response;
    String body;
    int code=0;
    URL url;
    HttpClient client;
    HttpRequest request;
    int num;
    String src;
    public ClientImpl(String hostName, int hostPort, int num  ) throws MalformedURLException {
        this.hostName= hostName;
        this.hostPort=hostPort;
        this.num=num;
        url = new URL("http",hostName, hostPort ,"/compileandrun");
        //url = new URL("http://"+hostName+":"+ hostPort+"/compileandrun");
        logger= initializeLogging("client "+hostPort + "_"+ num);
        logger.info("start");

    }

    @Override
    public void run()  {
        logger.info("sending \n"+ src);

        client = HttpClient.newHttpClient();



        //String send = " " +src;
        try {

            request = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .header("Content-Type", "text/x-java-source")
                    .POST(HttpRequest.BodyPublishers.ofString(src))
                    .build();
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
        System.out.println("send");

    }

    public void sendCompileAndRunRequest( String src){
        this.src =src;
    }
    public void changeURL(String src) throws MalformedURLException {
        url = new URL("http",hostName, hostPort ,"/"+src);

    }
    public Response getResponse() throws InterruptedException {
        logger.info("getting returning message");
        while (response==null){
            Thread.sleep(500);
        }
        logger.info(new Response(response.statusCode(),response.body()).toString());
        System.out.println("Returning"+num);

        return new Response(response.statusCode(),response.body());



    }

    class Response {
        private int code;
        private String body;

        public Response(int code, String body) {
            this.code = code;
            this.body = body;
        }

        public int getCode() {
            return this.code;
        }

        public String getBody() {
            return this.body;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "code=" + code +
                    ", body='" + body + '\'' +
                    '}';
        }
    }




}
