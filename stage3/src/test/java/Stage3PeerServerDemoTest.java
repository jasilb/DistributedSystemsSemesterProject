import edu.yu.cs.com3800.*;
import edu.yu.cs.com3800.stage3.ZooKeeperPeerServerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.junit.*;
public class Stage3PeerServerDemoTest implements LoggingServer, ZooKeeperPeerServer {
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
    private LinkedBlockingQueue<Message> outgoingMessages = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Message> incomingMessages = new LinkedBlockingQueue<>();

    private int myPort = 9999;
    private InetSocketAddress myAddress = new InetSocketAddress("localhost", this.myPort);
    private ArrayList<ZooKeeperPeerServer> servers;
    static int port =8000;
    Logger logger;
    @Test
    public void Demo8()  throws Exception {
        logger = initializeLogging("demo8");
        //step 1: create sender & sending queue

        UDPMessageSender sender = new UDPMessageSender(this.outgoingMessages, myPort);
        //step 2: create servers
        createServers(8);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {
        }
        printLeaders();
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        int requests =8;
        for (int i = 0; i < requests; i++) {
            String code = this.validClass.replace("world!", "world! from code version " + i);
            sendMessage(code, port);
        }
        Util.startAsDaemon(sender, "Sender thread");

        UDPMessageReceiver receiver = new UDPMessageReceiver(this.incomingMessages, this.myAddress, this.myPort,this);
        Util.startAsDaemon(receiver, "Receiver thread");
        //step 4: validate responses from leader

        printResponses(requests);

        //step 5: stop servers
        stopServers();
        receiver.shutdown();
        sender.shutdown();
        Thread.sleep(1000);
    }
    @After
    public void After() throws InterruptedException {
        Thread.sleep(1000);
    }
    @Test
    public void Demo10()  throws Exception {
        //step 1: create sender & sending queue
        logger = initializeLogging("demo10");

        UDPMessageSender sender = new UDPMessageSender(this.outgoingMessages, myPort);
        //step 2: create servers
        createServers(10);
        //step2.1: wait for servers to get started
        try {
            Thread.sleep(10000);
        }
        catch (Exception e) {
        }
        printLeaders();
        //step 3: since we know who will win the election, send requests to the leader, this.leaderPort
        for (int i = 0; i < 11; i++) {
            String code = this.validClass.replace("world!", "world! from code version " + i);
            sendMessage(code,port);
        }
        Util.startAsDaemon(sender, "Sender thread");

        UDPMessageReceiver receiver = new UDPMessageReceiver(this.incomingMessages, this.myAddress, this.myPort,this);
        Util.startAsDaemon(receiver, "Receiver thread");
        //step 4: validate responses from leader
        Thread.sleep(10000);
        printResponses(11);

        //step 5: stop servers
        stopServers();
        receiver.shutdown();
        sender.shutdown();
        Thread.sleep(1000);
    }

    private void printLeaders() {
        for (ZooKeeperPeerServer server : this.servers) {
            Vote leader = server.getCurrentLeader();
            System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());

        }
    }

    private void stopServers() {
        for (ZooKeeperPeerServer server : this.servers) {
            server.shutdown();
        }
    }

    private void printResponses(int requests) throws Exception {
        String completeResponse = "";
        for (int i = 0; i < requests; i++) {
            Message msg = this.incomingMessages.take();
            logger.info(msg.toString());
            String response = new String(msg.getMessageContents());
            completeResponse += "Response #" + i + ":\n" + response + "\n";
        }
        System.out.println(completeResponse);
    }

    private void sendMessage(String code,int leaderPort) throws InterruptedException {
        System.out.println(leaderPort);
        Message msg = new Message(Message.MessageType.WORK, code.getBytes(), this.myAddress.getHostString(), this.myPort, "localhost", leaderPort);
        this.outgoingMessages.put(msg);
    }

    private void createServers(int x) {
        //create IDs and addresses
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(x);
        for (int i = 1; i <=x ; i++) {
                peerIDtoAddress.put((long) i,new InetSocketAddress("localhost",port+i));
            }


        System.out.println(peerIDtoAddress);
        //create servers
        this.servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();

            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            map.remove(entry.getKey());
            //System.out.println(server);
            this.servers.add(server);
            new Thread( server, "Server on port " + server.getAddress().getPort()).start();
        }
        port+=x;
    }





    @Override
    public void shutdown() {

    }

    @Override
    public void setCurrentLeader(Vote v) throws IOException {

    }

    @Override
    public Vote getCurrentLeader() {
        return null;
    }

    @Override
    public void sendMessage(Message.MessageType type, byte[] messageContents, InetSocketAddress target) throws IllegalArgumentException {

    }

    @Override
    public void sendBroadcast(Message.MessageType type, byte[] messageContents) {

    }

    @Override
    public ServerState getPeerState() {
        return null;
    }

    @Override
    public void setPeerState(ServerState newState) {

    }

    @Override
    public Long getServerId() {
        return null;
    }

    @Override
    public long getPeerEpoch() {
        return 0;
    }

    @Override
    public InetSocketAddress getAddress() {
        return myAddress;
    }

    @Override
    public int getUdpPort() {
        return myPort;
    }

    @Override
    public InetSocketAddress getPeerByID(long peerId) {
        return null;
    }

    @Override
    public int getQuorumSize() {
        return 0;
    }
}
