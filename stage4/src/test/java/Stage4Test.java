import edu.yu.cs.com3800.*;
import edu.yu.cs.com3800.stage4.GatewayServer;
import edu.yu.cs.com3800.stage4.ZooKeeperPeerServerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Logger;

import org.junit.*;
public class Stage4Test implements LoggingServer {
    private String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world!\";\n    }\n}\n";
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

     String[] codes = new String[]{code1, code2,code3, brokenCompile,brokenRun};
    static int myPort = 9999;
    private InetSocketAddress myAddress = new InetSocketAddress("localhost", this.myPort);
    private ArrayList<ZooKeeperPeerServer> servers;
    static int port =8000;
    Logger logger;
    @Test
    public void Demo8_1()  throws Exception {
        logger = initializeLogging("demo8_1");
        //step 1: create sender & sending queue
        ArrayList<ClientImpl> clients = new ArrayList<>();
        ClientImpl client = makeClient(1);
        clients.add(client);

        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        int requests =1;
        for (int i = 0; i < requests; i++) {
            String code = this.validClass.replace("world!", "world! from code version " + i);
            clients.get(i).sendCompileAndRunRequest(code);
            client.start();
        }





        //step 4: validate responses from leader
        System.out.println("print");
        printResponses(clients);
        System.out.println("end");
        //step 5: stop servers
        stopServers();


    }
    @Test
    public void Demo8_1Fail()  throws Exception {
        logger = initializeLogging("demo8_1Fail");
        //step 1: create sender & sending queue
        ArrayList<ClientImpl> clients = new ArrayList<>();
        ClientImpl client = makeClient(1);
        clients.add(client);

        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        int requests =1;
        for (int i = 0; i < requests; i++) {

            clients.get(i).sendCompileAndRunRequest(brokenCompile);
            client.start();
        }





        //step 4: validate responses from leader
        System.out.println("print");
        printResponses(clients);
        System.out.println("end");
        //step 5: stop servers
        stopServers();


    }
    @Test
    public void Demo8()  throws Exception {
        logger = initializeLogging("demo10");
        //step 1: create sender & sending queue



        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        ArrayList<ClientImpl> clients = new ArrayList<>();
        int requests =8;
        for (int i = 0; i < requests; i++) {
            ClientImpl client = makeClient(i);
            clients.add(client);
            String code = this.validClass.replace("world!", "world! from code version " + i);
            client.sendCompileAndRunRequest(code);
            client.start();
        }


        //step 4: validate responses from leader
        System.out.println("print");
        printResponses(clients);

        //step 5: stop servers
        stopServers();


    }
    @After
    public void After() throws InterruptedException {
        Thread.sleep(1000);
        myPort+=100;
    }

    private ClientImpl makeClient(int i) throws IOException {
        ClientImpl client = new ClientImpl("localHost", myPort,i);
        return client;
    }

    @Test
    public void Demo10()  throws Exception {
        logger = initializeLogging("demo10");
        //step 1: create sender & sending queue
        ArrayList<ClientImpl> clients = new ArrayList<>();


        //step 2: create servers and return gateway
        GatewayServer gatewayServer= createServers(10);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        int requests =20;

        for (int i = 0; i < requests; i++) {
            ClientImpl client = makeClient(i);
            clients.add(client);
            String code = this.validClass.replace("world!", "world! from code version " + i);
            clients.get(i).sendCompileAndRunRequest(code);
            client.start();
        }


        //step 4: validate responses from leader

        printResponses(clients);

        //step 5: stop servers
        stopServers();

    }
    @Test
    public void DemoAll() throws Exception {
        logger = initializeLogging("demoAll");
        //step 1: create sender & sending queue
        ArrayList<ClientImpl> clients = new ArrayList<>();


        //step 2: create servers
        GatewayServer gatewayServer= createServers(10);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        int requests =20;

        for (int i = 0; i < requests; i++) {
            ClientImpl client = makeClient(i);
            clients.add(client);
            clients.get(i).sendCompileAndRunRequest(codes[i%codes.length]);
            client.start();
        }


        //step 4: validate responses from leader

        printResponses(clients);

        //step 5: stop servers
        stopServers();

    }

    private void printLeaders(GatewayServer gatewayServer) {
        while (gatewayServer.getCurrentLeader()==null){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (ZooKeeperPeerServer server : this.servers) {

            Vote leader = server.getCurrentLeader();
            System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());

        }
        System.out.println("OBSERVER on port " +gatewayServer.getMyPort()+ " has the leader as port: "+ gatewayServer.getCurrentLeader().getPort());

    }

    private void stopServers() {
        for (ZooKeeperPeerServer server : this.servers) {
            server.shutdown();
        }
    }

    private void printResponses(ArrayList<ClientImpl> clients) throws Exception {
        String completeResponse = "";
        for (int i = 0; i < clients.size(); i++) {
            ClientImpl.Response msg = clients.get(i).getResponse();
            logger.info(msg.toString());
            String response = msg.toString();
            completeResponse += "Response #" + i + ":\n" + response + "\n";
        }
        System.out.println(completeResponse);
    }



    private GatewayServer createServers(int x) {
        //create IDs and addresses
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(x);

        for (int i = 1; i <=x*4 ; i+=4) {
            peerIDtoAddress.put((long) i,new InetSocketAddress("localhost",port+i));
        }


        System.out.println(peerIDtoAddress);
        //create servers
        GatewayServer gatewayServer = new GatewayServer(myPort,0,peerIDtoAddress,1);
        peerIDtoAddress.put(0L,new InetSocketAddress("localhost",myPort+2));

        this.servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            if(entry.getKey()==0L){
                continue;
            }
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();

            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map,1);
            map.remove(entry.getKey());
            //System.out.println(server);
            this.servers.add(server);
            new Thread( server, "Server on port " + server.getAddress().getPort()).start();
        }
        new Thread(gatewayServer, "Observer on port " + gatewayServer.getMyPort()).start();
        port+=x*4;
        return gatewayServer;
    }






}
