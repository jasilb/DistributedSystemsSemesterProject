import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Vote;
import edu.yu.cs.com3800.ZooKeeperPeerServer;
import edu.yu.cs.com3800.stage5.GatewayServer;
import edu.yu.cs.com3800.stage5.ZooKeeperPeerServerImpl;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Stage5Test implements LoggingServer {
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
    private HashMap<Long, ZooKeeperPeerServer> map;
    static int port =8000;
    Logger logger;

    @After
    public void After() throws InterruptedException {
        Thread.sleep(1000);
        myPort+=100;
    }

    private ClientImpl makeClient(int i) throws IOException {
        ClientImpl client = new ClientImpl("localHost", myPort,i);
        return client;
    }


    private void printLeaders(GatewayServer gatewayServer) {


        for (ZooKeeperPeerServer server : this.servers) {

            Vote leader = server.getCurrentLeader();
            System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());

        }
        System.out.println("OBSERVER on port " +gatewayServer.getMyPort()+ " has the leader as port: "+ gatewayServer.getCurrentLeader().getPort());

    }

    private void stopServers(GatewayServer gatewayServer) {
        for (ZooKeeperPeerServer server : this.servers) {
            server.shutdown();
        }
        gatewayServer.shutdown();
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
        this.map = new HashMap<>();
        this.servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            if(entry.getKey()==0L){
                continue;
            }
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();

            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map,1);
            this.map.put(entry.getKey(),server);
            map.remove(entry.getKey());
            //System.out.println(server);
            this.servers.add(server);

            new Thread( server, "Server on port " + server.getAddress().getPort()).start();
        }
        new Thread(gatewayServer, "Observer on port " + gatewayServer.getMyPort()).start();
        port+=x*4;
        return gatewayServer;
    }


    @Test
    public void Demo8kill1()  throws Exception {
        System.out.println("Demo8kill1");
        logger = initializeLogging("demo8_kill1");
        //step 1: create sender & sending queue



        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        ZooKeeperPeerServer server = this.servers.get(1);
        System.out.println(server.getServerId());
        server.shutdown();
        Thread.sleep(45000);
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
        stopServers(gatewayServer);


    }
    @Test
    public void Demo8killLeader()  throws Exception {
        System.out.println("Demo8killLeader");
        logger = initializeLogging("demo8_killLeader");
        //step 1: create sender & sending queue



        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);

        ZooKeeperPeerServer server = this.map.get(29L);
        System.out.println("killing leader "+ server.getServerId());
        server.shutdown();
        Thread.sleep(45000);
        ArrayList<ClientImpl> clients = new ArrayList<>();
        int requests =8;
        for (int i = 0; i < requests; i++) {
            ClientImpl client = makeClient(i);
            clients.add(client);
            String code = this.validClass.replace("world!", "world! from code version " + i);
            client.sendCompileAndRunRequest(code);
            client.start();
        }
        //Thread.sleep(1000);

        //step 4: validate responses from leader
        System.out.println("print");
        printResponses(clients);

        //step 5: stop servers
        stopServers(gatewayServer);


    }
    @Test
    public void Demo8killLeaderInMiddleWithWait()  throws Exception {
        System.out.println("Demo8killLeaderInMiddleWithWait");
        logger = initializeLogging("demo8_killInMiddle");
        //step 1: create sender & sending queue



        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);

        ZooKeeperPeerServer server = this.map.get(29L);


        ArrayList<ClientImpl> clients = new ArrayList<>();
        int requests =8;
        ClientImpl client = null;
        for (int i = 0; i < requests; i++) {
            client = makeClient(i);
            clients.add(client);
            String code = this.validClass.replace("world!", "world! from code version " + i);
            client.sendCompileAndRunRequest(code);

        }
        for (int i = 0; i < 4; i++) {
            clients.get(i).start();
        }
        Thread.sleep(500);
        System.out.println("killing leader "+ server.getServerId());
        server.shutdown();
        Thread.sleep(45000);
        //new thread should take over
        for (int i = 4; i < 8; i++) {
            clients.get(i).start();
        }


        //step 4: validate responses from leader
        System.out.println("print");
        printResponses(clients);

        //step 5: stop servers
        stopServers(gatewayServer);


    }
    @Test
    public void Demo8killLeaderInMiddle()  throws Exception {
        System.out.println("Demo8killLeaderInMiddle");
        logger = initializeLogging("demo8_killInMiddle");
        //step 1: create sender & sending queue



        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);

        ZooKeeperPeerServer server = this.map.get(29L);


        ArrayList<ClientImpl> clients = new ArrayList<>();
        int requests =8;
        ClientImpl client = null;
        for (int i = 0; i < requests; i++) {
            client = makeClient(i);
            clients.add(client);
            String code = this.validClass.replace("world!", "world! from code version " + i);
            client.sendCompileAndRunRequest(code);

        }
        for (int i = 0; i < 4; i++) {
            clients.get(i).start();
        }
        //Thread.sleep(500);
        System.out.println("killing leader "+ server.getServerId());
        server.shutdown();
        //Thread.sleep(45000);
        //new gateway should wait for new leader
        for (int i = 4; i < 8; i++) {
            clients.get(i).start();
        }


        //step 4: validate responses from leader
        System.out.println("print");
        printResponses(clients);

        //step 5: stop servers
        stopServers(gatewayServer);


    }
    @Test
    public void Demo8killLeaderGetQueuedWork()  throws Exception {
        System.out.println("Demo8killLeaderGetQueuedWork");
        String stopClass = "package edu.yu.cs.fall2019.com3800.stage1;\n" +
                "\n" +
                "    public class HelloWorld\n" +
                "    {\n" +
                "        public String run()\n" +
                "        {\n" +
                "            try {\n" +
                "                Thread.sleep(20000);\n" +
                "            } catch (InterruptedException e) {\n" +
                "            }\n" +
                "            return \"Hello world!\";\n" +
                "        }\n" +
                "    }";

        logger = initializeLogging("demo8_killInMiddle");
        //step 1: create sender & sending queue



        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);

        ZooKeeperPeerServer server = this.map.get(29L);


        ArrayList<ClientImpl> clients = new ArrayList<>();
        int requests =8;
        ClientImpl client = null;
        for (int i = 0; i < requests; i++) {
            client = makeClient(i);
            clients.add(client);
            String code = stopClass.replace("world!", "world! from code version " + i);
            client.sendCompileAndRunRequest(code);

        }
        for (int i = 0; i < 4; i++) {
            clients.get(i).start();
        }
        Thread.sleep(5000);
        System.out.println("killing leader "+ server.getServerId());
        server.shutdown();
        //Thread.sleep(45000);
        //new gateway should wait for new leader
        for (int i = 4; i < 8; i++) {
            clients.get(i).start();
        }


        //step 4: validate responses from leader
        System.out.println("print");
        printResponses(clients);

        //step 5: stop servers
        stopServers(gatewayServer);


    }
    @Test
    public void Demo8getGossip()  throws Exception {
        System.out.println("Demo8getGossip");
        String stopClass = "package edu.yu.cs.fall2019.com3800.stage1;\n" +
                "\n" +
                "    public class HelloWorld\n" +
                "    {\n" +
                "        public String run()\n" +
                "        { return \"Hello world!\";\n" +
                "        }\n" +
                "    }";

        logger = initializeLogging("demo8_killInMiddle");
        //step 1: create sender & sending queue



        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);

        ZooKeeperPeerServer server = this.map.get(29L);


        ArrayList<ClientImpl> clients = new ArrayList<>();
        int requests =8;
        ClientImpl client = null;
        port -= 32;
        for (int i = 0; i < requests; i++) {

            System.out.println((port+(i*4)+2));
            client = new ClientImpl("localhost",(port+(i*4)+2),i);
            clients.add(client);
            String code = stopClass.replace("world!", "world! from code version " + i);
            client.sendCompileAndRunRequest(code);
            client.changeURL("gossip");


        }
        port +=32;
        for (int i = 0; i < 8; i++) {
            clients.get(i).start();
        }



        //step 4: validate responses from leader
        System.out.println("print");
        printResponses(clients);

        //step 5: stop servers
        stopServers(gatewayServer);


    }
    @Test
    public void Demo8ServerGossip()  throws Exception {
        System.out.println("Demo8getGossip");
        String stopClass = "package edu.yu.cs.fall2019.com3800.stage1;\n" +
                "\n" +
                "    public class HelloWorld\n" +
                "    {\n" +
                "        public String run()\n" +
                "        { return \"Hello world!\";\n" +
                "        }\n" +
                "    }";

        logger = initializeLogging("demo8_killInMiddle");
        //step 1: create sender & sending queue



        //step 2: create servers
        GatewayServer gatewayServer= createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
        printLeaders(gatewayServer);

        ZooKeeperPeerServer server = this.map.get(29L);


        ArrayList<ClientImpl> clients = new ArrayList<>();
        int requests =8;
        ClientImpl client = null;



            client = new ClientImpl("localhost",myPort,0);
            clients.add(client);
            client.sendCompileAndRunRequest(stopClass);
            client.changeURL("gossip");


        //step 4: validate responses from leader
        System.out.println("print");
        client.start();
        System.out.println(client.getResponse().getBody());

        //step 5: stop servers
        stopServers(gatewayServer);


    }




}
