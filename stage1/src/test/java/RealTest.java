import edu.yu.cs.com3800.stage1.Client;
import edu.yu.cs.com3800.stage1.ClientImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;


import edu.yu.cs.com3800.stage1.SimpleServerImpl;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RealTest {
    static int port = 9000;
    String host = "localhost";
    static SimpleServerImpl server;
    String code1 = "public class code {\n" +
            "     public code() {\n" +
            "     }\n" +
            "     public String run() { \n" +
            "          return \"fake code\";\n" +
            "     }\n" +
            "}";
    String code2 = "public class code2 {\n" +
            "     public code2() {\n" +
            "     }\n" +
            "     public String run() {\n" +
            "          int x =1;\n" +
            "          int y =2;\n" +
            "          return Integer.toString(x+y);\n" +
            "     }\n" +
            "}";
    String code3 = "public class code3 {\n" +
            "     int x =1;\n" +
            "     int y =10;\n" +
            "     public code3() {\n" +
            "     }\n" +
            "     public String run() {\n" +
            "          String nums =\"\";\n" +
            "          for (int i = 0; i < y; i++) {\n" +
            "               nums+=i; \n" +
            "          }\n" +
            "          return nums;\n" +
            "     }\n" +
            "}";
    String brokenCompile = "public class BrokenCompile {\n" +
            "     int x =1;\n" +
            "     int y =10;\n" +
            "     public BrokenCompile() {\n" +
            "     }\n" +
            "     public String run() {\n" +
            "          nums =\"\";\n" +
            "          for (int i = 0; i < y; i++) {\n" +
            "               nums+=i;\n" +
            "          }\n" +
            "          return nums;\n" +
            "     }\n" +
            "}";
    String brokenRun = "public class BrokenRun {\n" +
            "    int x[] =new int[5];\n" +
            "    int y =10;\n" +
            "    public BrokenRun() {\n" +
            "    }\n" +
            "    public String run() {\n" +
            "        String nums =\"\";\n" +
            "        for (int i = 0; i < y; i++) {\n" +
            "            x[i] = y;\n" +
            "        }\n" +
            "        return nums;\n" +
            "    }\n" +
            "}";

    @BeforeClass
    public static void Start() throws IOException {
        System.out.println("LogFile.log");
        server = new SimpleServerImpl(port);
        server.start();
        System.out.println(InetAddress.getLocalHost().getHostAddress());
    }

    @AfterClass
    public static void End() throws IOException {
        server.stop();
    }

    @Test
    public void client1() throws IOException {
        ClientImpl client = new ClientImpl(host, port);
        client.sendCompileAndRunRequest(code1);
        Client.Response response = client.getResponse();

        assertEquals(response.getCode(), 200);
        assertEquals(response.getBody(), "fake code");
        System.out.println("Expected:");
        System.out.println(200);
        System.out.println("actual:");
        System.out.println(response.getCode());
        System.out.println("Expected:");
        System.out.println("fake code");
        System.out.println("actual:");
        System.out.println(response.getBody());
    }

    @Test
    public void clientc2() throws IOException {
        ClientImpl client = new ClientImpl(host, port);
        client.sendCompileAndRunRequest(code2);
        Client.Response response = client.getResponse();
        assertEquals(response.getCode(), 200);
        assertEquals(response.getBody(), "3");
        System.out.println("Expected:");
        System.out.println(200);
        System.out.println("actual:");
        System.out.println(response.getCode());
        System.out.println("Expected:");
        System.out.println("3");
        System.out.println("actual:");
        System.out.println(response.getBody());
    }

    @Test
    public void clientc3() throws IOException {
        ClientImpl client = new ClientImpl(host, port);
        client.sendCompileAndRunRequest(code3);
        Client.Response response = client.getResponse();
        assertEquals(response.getCode(), 200);
        assertEquals(response.getBody(), "0123456789");
        System.out.println("Expected:");
        System.out.println(200);
        System.out.println("actual:");
        System.out.println(response.getCode());
        System.out.println("Expected:");
        System.out.println("0123456789");
        System.out.println("actual:");
        System.out.println(response.getBody());
    }

    @Test
    public void TwoClients1() throws IOException {
        System.out.println("run 2 clients");
        ClientImpl client = new ClientImpl(host, port);
        client.sendCompileAndRunRequest(code1);
        Client.Response response = client.getResponse();
        assertEquals(response.getCode(), 200);
        assertEquals(response.getBody(), "fake code");
        ClientImpl client2 = new ClientImpl(host, port);
        client2.sendCompileAndRunRequest(code1);
        Client.Response response2 = client2.getResponse();
        assertEquals(response2.getCode(), 200);
        assertEquals(response2.getBody(), "fake code");
    }

    @Test
    public void TwoClientsMix1() throws IOException, InterruptedException {
        System.out.println("two clients mixed");
        ClientImpl client = new ClientImpl(host, port);
        client.sendCompileAndRunRequest(code1);
        //Thread.sleep(1000);
        ClientImpl client2 = new ClientImpl(host, port);
        client2.sendCompileAndRunRequest(code1);
        //Thread.sleep(1000);
        Client.Response response = client.getResponse();
        assertEquals(response.getCode(), 200);
        assertEquals(response.getBody(), "fake code");
        //Thread.sleep(1000);
        Client.Response response2 = client2.getResponse();
        assertEquals(response2.getCode(), 200);
        assertEquals(response2.getBody(), "fake code");
    }

    @Test
    public void ClientResponseNull1() throws IOException {
        System.out.println("don't call server");
        ClientImpl client = new ClientImpl(host, port);

        Client.Response response = client.getResponse();
        assertNull(response);
    }

    @Test
    public void TestClient1() throws IOException {
        TestClient client = new TestClient(host, port);
        client.sendCompileAndRunRequest(code1);
        Client.Response response = client.getResponse();
        assertEquals(response.getCode(), 200);
        assertEquals(response.getBody(), "fake code");
    }

    @Test
    public void TestClientAndClientImpl() throws IOException {
        TestClient testClient = new TestClient(host, port);
        ClientImpl clientImpl = new ClientImpl(host, port);
        testClient.sendCompileAndRunRequest(code1);
        clientImpl.sendCompileAndRunRequest(code2);
        Client.Response response = testClient.getResponse();
        assertEquals(response.getCode(), 200);
        assertEquals(response.getBody(), "fake code");
        Client.Response response2 = clientImpl.getResponse();
        assertEquals(response2.getCode(), 200);
        assertEquals(response2.getBody(), "3");
    }

    @Test
    public void clientc4() throws IOException {
        System.out.println("send code with compile error");
        ClientImpl client = new ClientImpl(host, port);
        client.sendCompileAndRunRequest(brokenCompile);
        Client.Response response = client.getResponse();
        assertEquals(response.getCode(), 400);
        System.out.println("expected:");
        System.out.println(400);
        System.out.println("actual:");
        System.out.println(response.getCode());
        System.out.println(response.getBody());
    }

    @Test
    public void clientc5() throws IOException {
        System.out.println("send code with runtime error");
        ClientImpl client = new ClientImpl(host, port);
        client.sendCompileAndRunRequest(brokenRun);
        Client.Response response = client.getResponse();
        assertEquals(response.getCode(), 400);
        System.out.println("expected:");
        System.out.println(400);
        System.out.println("actual:");
        System.out.println(response.getCode());
        System.out.println(response.getBody());
    }

    @Test
    public void sendWrongProperty() throws IOException {
        System.out.println("send code with Content-Type = \"wrong\" ");
        TestClient testClient = new TestClient(host, port);
        testClient.sendWrongProperty(code1);
        Client.Response response = testClient.getResponse();
        assertEquals(response.getCode(), 400);
        assertNull(response.getBody());
        System.out.println("expected:");
        System.out.println(400);
        System.out.println("actual:");
        System.out.println(response.getCode());
        System.out.println("assertNull: "+ response.getBody());
    }
    @Test
    public void sendNullProperty() throws IOException {
        System.out.println("send code with Content-Type = null");
        TestClient testClient = new TestClient(host, port);
        testClient.sendNoProperty(code1);
        Client.Response response = testClient.getResponse();
        assertEquals(response.getCode(), 400);
        assertNull(response.getBody());
        System.out.println("expected:");
        System.out.println(400);
        System.out.println("actual:");
        System.out.println(response.getCode());
        System.out.println("assertNull: "+ response.getBody());
    }

//    @Test
//    public void sendToSomeone() throws IOException {
//        ClientImpl client = new ClientImpl("other person's IP", 9000);
//        System.out.println("client");
//        client.sendCompileAndRunRequest(code1);
//        System.out.println("send");
//        Client.Response response = client.getResponse();
//
//        assertEquals(response.getCode(), 200);
//        assertEquals(response.getBody(), "fake code");
//        System.out.println("Expected:");
//        System.out.println(200);
//        System.out.println("actual:");
//        System.out.println(response.getCode());
//        System.out.println("Expected:");
//        System.out.println("fake code");
//        System.out.println("actual:");
//        System.out.println(response.getBody());
//    }
    @Test(expected = IOException.class)
    public void clientNull() throws IOException {
        System.out.println("src == null");
        ClientImpl client = new ClientImpl(host, port);
        client.sendCompileAndRunRequest(null);

    }




}
















 class TestClient implements Client {
    String hostName;
    int hostPort;

    CompletableFuture<HttpResponse<String>> response;
    String body;
    int code=0;
    URL url;
    HttpClient client;
    HttpRequest request;
    HttpURLConnection httpClient;

    public TestClient(String hostName, int hostPort) throws MalformedURLException {
        this.hostName= hostName;
        this.hostPort=hostPort;
        url = new URL("http",hostName, hostPort ,"/compileandrun");
        System.out.println(url);

    }

    public void sendCompileAndRunRequest(String src) throws IOException {

        httpClient = (HttpURLConnection) url.openConnection();
        httpClient.setDoOutput(true);

        httpClient.setRequestProperty("Content-Type", "text/x-java-source");
        System.out.println(httpClient.getRequestProperties().entrySet());
        //System.out.println(httpClient.getRequestMethod());

        httpClient.getOutputStream().write(src.getBytes(StandardCharsets.UTF_8));
        System.out.println("write");
        System.out.println(httpClient.getRequestMethod());

    }
     public void sendWrongProperty(String src) throws IOException {

         httpClient = (HttpURLConnection) url.openConnection();
         httpClient.setDoOutput(true);

         httpClient.setRequestProperty("Content-Type", "wrong");
         System.out.println(httpClient.getRequestProperties().entrySet());
         //System.out.println(httpClient.getRequestMethod());

         httpClient.getOutputStream().write(src.getBytes(StandardCharsets.UTF_8));
         System.out.println("write");
         System.out.println(httpClient.getRequestMethod());

     }
     public void sendNoProperty(String src) throws IOException {

         httpClient = (HttpURLConnection) url.openConnection();
         httpClient.setDoOutput(true);

         //**httpClient.setRequestProperty("Content-Type", "wrong");**
         System.out.println(httpClient.getRequestProperties().entrySet());
         //System.out.println(httpClient.getRequestMethod());

         httpClient.getOutputStream().write(src.getBytes(StandardCharsets.UTF_8));
         System.out.println("write");
         System.out.println(httpClient.getRequestMethod());

     }
     public void sendNothing(String src) throws IOException {

         httpClient = (HttpURLConnection) url.openConnection();
         httpClient.setDoOutput(true);

         httpClient.setRequestProperty("Content-Type", "text/x-java-source");
         System.out.println(httpClient.getRequestProperties().entrySet());
         System.out.println(httpClient.getRequestMethod());

         //httpClient.getOutputStream().write(src.getBytes(StandardCharsets.UTF_8));
         //System.out.println("write");
         System.out.println(httpClient.getRequestMethod());

     }


    public Response getResponse() throws IOException {
        int responseCode = httpClient.getResponseCode();
        String responseBody= null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(httpClient.getInputStream()));

            StringBuilder response = new StringBuilder();

            while ((responseBody = in.readLine()) != null) {
                response.append(responseBody);
            }
            responseBody = response.toString();
            httpClient.disconnect();
        } catch (IOException e){
            httpClient.disconnect();
            return new Response(responseCode, responseBody);
        }


        if (responseCode == 0) {
            return null;
        } else {
            return new Response(responseCode, responseBody);
        }


    }

}
