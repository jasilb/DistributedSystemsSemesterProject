package edu.yu.cs.com3800.stage5;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.com3800.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


public class ZooKeeperPeerServerImpl extends Thread implements ZooKeeperPeerServer {
    private final InetSocketAddress myAddress;
    private final int myPort;
    private ServerState state;
    private  boolean shutdown;
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private Long id;
    private long peerEpoch;
    private volatile Vote currentLeader;
    Map<Long,InetSocketAddress> peerIDtoAddress;

    private UDPMessageSender senderWorker;
    private UDPMessageReceiver receiverWorker;
    int observers =0;
    volatile boolean aliveLeader = false;
    ZooKeeperLeaderElection zooKeeperLeaderElection;
    JavaRunnerFollower javaRunnerFollower;
    RoundRobinLeader roundRobinLeader;
    Logger logger;
    Gossiper gossiper;
    HttpServer server;
    HashMap<InetSocketAddress,Long> AddressToPeerID;
    ConcurrentHashMap<Long, Boolean> isDead;
    LinkedBlockingQueue<Message> gossipQueue;
    public ZooKeeperPeerServerImpl(int myPort, long peerEpoch, Long id, Map<Long,InetSocketAddress> peerIDtoAddress, int observers){
        //code here...
        this.myPort=myPort;
        this.peerIDtoAddress=peerIDtoAddress;
        this.id=id;
        this.myAddress = peerIDtoAddress.get(id);
        //System.out.println(myAddress);
        this.observers=observers;

        this.outgoingMessages= new LinkedBlockingQueue<>();
        this.incomingMessages = new LinkedBlockingQueue<>();
        this.peerIDtoAddress.remove(this.id);
        this.peerEpoch=peerEpoch;
        this.state= ServerState.LOOKING;
        logger = initializeLogging("server "+ myPort);
        logger.info("start");
        this.currentLeader= new Vote(id,peerEpoch);
        AddressToPeerID = new HashMap<>();
        isDead=new ConcurrentHashMap<>();
        for (Map.Entry< Long, InetSocketAddress> i : peerIDtoAddress.entrySet()) {
            AddressToPeerID.put(i.getValue(),i.getKey());
            isDead.put(i.getKey(), false);
        }
        //System.out.println(isDead);
        //System.out.println(AddressToPeerID);
        gossipQueue = new LinkedBlockingQueue<>();
        gossiper = new Gossiper(this, isDead, incomingMessages, peerIDtoAddress, gossipQueue);
        try {
            server = HttpServer.create(new InetSocketAddress(myPort+1), 10 );
            server.createContext("/gossip", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    Logger l = initializeLogging("logging gossip for server "+ id);
                    String history = gossiper.getHistory();
                    exchange.sendResponseHeaders(200, history.length());
                    OutputStream os = exchange.getResponseBody();
                    logger.info("history: " + history);
                    l.info(history);
                    os.write(history.getBytes());
                    os.close();
                }
            });
            server.start();

        } catch (IOException e) {
            logger.warning(Util.getStackTrace(e));
        }
    }


        @Override
    public void shutdown(){
        this.shutdown = true;
        this.senderWorker.shutdown();
        this.receiverWorker.shutdown();

        if (this.state.equals(ServerState.LEADING)){
            this.roundRobinLeader.shutdown();

        }
        if (this.state.equals(ServerState.FOLLOWING)){

            this.javaRunnerFollower.shutdown();

        }
        gossiper.shutdown();
        server.stop(1);
        logger.info("shutdown");



    }

    @Override
    public void setCurrentLeader(Vote v) throws IOException {
        this.currentLeader=v;
        this.peerEpoch = v.getPeerEpoch();
    }

    @Override
    public Vote getCurrentLeader() {

        return currentLeader;
    }

    @Override
    public void sendMessage(Message.MessageType type, byte[] messageContents, InetSocketAddress target) throws IllegalArgumentException {

            Message message = new Message(type,messageContents,myAddress.getHostString(),myPort,target.getHostString(),target.getPort());
            //System.out.println(outgoingMessages.offer(message));
            logger.info("sending message " + message.toString());
        try {
            outgoingMessages.put(message);
        } catch (InterruptedException e) {
            logger.warning(Util.getStackTrace(e));
        }
    }

    @Override
    public void sendBroadcast(Message.MessageType type, byte[] messageContents) {

        for (InetSocketAddress address :peerIDtoAddress.values()) {
            sendMessage(type,messageContents,address);
        }
    }

    @Override
    public ServerState getPeerState() {

        return this.state;
    }

    @Override
    public void setPeerState(ServerState newState) {
        this.state=newState;
        logger.info("new state "+ newState);
    }

    @Override
    public Long getServerId() {

        return this.id;
    }

    @Override
    public long getPeerEpoch() {

        return this.peerEpoch;
    }

    @Override
    public InetSocketAddress getAddress() {

        return this.myAddress;
    }

    @Override
    public int getUdpPort() {
        return myPort;
    }

    @Override
    public InetSocketAddress getPeerByID(long peerId) {

        return this.peerIDtoAddress.get(peerId);
    }

    @Override
    public int getQuorumSize() {
        int count=0;
        for (boolean b: isDead.values()) {
            if (b ==false){
                count++;
            }
        }
        return ((count-observers)/2)+1 ;
    }
    @Override
    public void reportFailedPeer(long peerID){
        isDead.put(peerID,true);
        if (currentLeader.getProposedLeaderID()==peerID){
            logger.info("leader died");
            this.currentLeader= new Vote(id,peerEpoch);
            if (state!= ServerState.OBSERVER){
                setPeerState(ServerState.LOOKING);
            }
            aliveLeader=false;
        }
    }
    @Override
    public boolean isPeerDead(long peerID){

        //System.out.println(peerID +": "+ isDead.get(peerID));
        if (isDead.getOrDefault(peerID,true)==true){
            return true;
        }
        return false;
    }
    
    @Override
    public boolean isPeerDead(InetSocketAddress address){
        //System.out.println(address);
        return isPeerDead(AddressToPeerID.get(address));
    }


    @Override
    public void run(){


        //step 1: create and run thread that sends broadcast messages
        senderWorker = new UDPMessageSender(outgoingMessages, myPort);
        senderWorker.start();
        logger.info("start sender");
        //step 2: create and run thread that listens for messages sent to this server
        try {
            receiverWorker = new UDPMessageReceiver(incomingMessages,myAddress,myPort,this, gossipQueue);
            receiverWorker.start();
            logger.info("start receiver");
        } catch (IOException e) {
            logger.warning(e.toString());
        }
        gossiper.start();
        //step 3: main server loop

        try{
            while (!this.shutdown){
                switch (getPeerState()){
                    case LOOKING:
                        logger.info("election");
                        peerEpoch++;
                        //start leader election, set leader to the election winner
                        zooKeeperLeaderElection= new ZooKeeperLeaderElection(this,incomingMessages,gossipQueue);
                        currentLeader= zooKeeperLeaderElection.lookForLeader();
                        aliveLeader = true;

                        logger.info("server "+ id+ " is "+ state + " server "+ currentLeader.getProposedLeaderID());
                        System.out.println(id +": switching from LOOKING to "+ state);
                        break;
                    case OBSERVER:

                        logger.info("election");
                        peerEpoch++;
                        zooKeeperLeaderElection= new ZooKeeperLeaderElection(this,incomingMessages,gossipQueue);
                        currentLeader= zooKeeperLeaderElection.lookForLeader();
                        logger.info("server "+ id+ " is "+ state + " server "+ currentLeader.getProposedLeaderID());
                        System.out.println(id +": is "+ state + " - " +currentLeader.getProposedLeaderID() + " is leader");
                        aliveLeader = true;

                        while (aliveLeader && !this.shutdown){
                            Thread.sleep(1000);
                        }
                        if (!this.shutdown){
                            aliveLeader = false;
                            System.out.println(id +": leader died "+ state);
                            this.currentLeader= new Vote(-1,peerEpoch);

                        }

                        break;
                    case LEADING:
                        //System.out.println("leading "+ this.myPort);
                        //make thread
                        logger.info("server "+ id+ " is "+ state + " server "+ currentLeader.getProposedLeaderID());
                        ArrayList<InetSocketAddress> list = new ArrayList<>();
                        for (Long l: peerIDtoAddress.keySet()) {
                            list.add(peerIDtoAddress.get(l));
                        }
                        if (javaRunnerFollower!=null){
                            javaRunnerFollower.shutdown();
                        }
                        roundRobinLeader = new RoundRobinLeader(this, list);
                        //start thread
                        roundRobinLeader.start();

                        //get stuck here until shutdown
                        while (!this.shutdown){
                            Thread.sleep(1000);
                        }
                        aliveLeader = false;
                        break;
                    case FOLLOWING:
                        //make thread
                        logger.info("server "+ id+ " is "+ state + " server "+ currentLeader.getProposedLeaderID());

                        //start thread
                        if (javaRunnerFollower==null){
                            javaRunnerFollower = new JavaRunnerFollower(this);
                            javaRunnerFollower.start();
                        }

                        //wait here until shutdown or leader dies
                        while (aliveLeader && !this.shutdown){
                            Thread.sleep(1000);
                        }
                        if (!this.shutdown){
                            setPeerState(ServerState.LOOKING);
                            aliveLeader = false;
                            this.currentLeader= new Vote(id,peerEpoch);
                            System.out.println(id +": switching from FOLLOWING to LOOKING");
                        }

                        break;
                }
            }
        }
        catch (Exception e) {
           //code...
            logger.warning(Util.getStackTrace(e));
        }
        System.out.println("shutdown "+ id);
    }

}
