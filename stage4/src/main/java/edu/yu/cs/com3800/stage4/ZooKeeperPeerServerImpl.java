package edu.yu.cs.com3800.stage4;
import edu.yu.cs.com3800.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


public class ZooKeeperPeerServerImpl extends Thread implements ZooKeeperPeerServer {
    private final InetSocketAddress myAddress;
    private final int myPort;
    private ServerState state;
    private volatile boolean shutdown;
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private Long id;
    private long peerEpoch;
    private volatile Vote currentLeader;
    private Map<Long,InetSocketAddress> peerIDtoAddress;

    private UDPMessageSender senderWorker;
    private UDPMessageReceiver receiverWorker;
    int observers =0;
    boolean concluded = false;
    ZooKeeperLeaderElection zooKeeperLeaderElection;
    JavaRunnerFollower javaRunnerFollower;
    RoundRobinLeader roundRobinLeader;
    Logger logger;
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
        //this.start();

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
        logger.info("shutdown");



    }

    @Override
    public void setCurrentLeader(Vote v) throws IOException {
        this.currentLeader=v;
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
            e.printStackTrace();
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

        return ((peerIDtoAddress.size()-observers )+1)/2 ;
    }

    @Override
    public void run(){
        //step 1: create and run thread that sends broadcast messages
        senderWorker = new UDPMessageSender(outgoingMessages, myPort);
        senderWorker.start();
        logger.info("start sender");
        //step 2: create and run thread that listens for messages sent to this server
        try {
            receiverWorker = new UDPMessageReceiver(incomingMessages,myAddress,myPort,this);
            receiverWorker.start();
            logger.info("start receiver");
        } catch (IOException e) {
            logger.warning(e.toString());
        }
        //step 3: main server loop
        try{
            while (!this.shutdown){
                switch (getPeerState()){
                    case LOOKING:
                        //start leader election, set leader to the election winner
                        zooKeeperLeaderElection= new ZooKeeperLeaderElection(this,incomingMessages);
                        currentLeader= zooKeeperLeaderElection.lookForLeader();
                        concluded= true;
                        logger.info("server "+ id+ " is "+ state + " server "+ currentLeader.getProposedLeaderID());

                        break;
                    case OBSERVER:
                        zooKeeperLeaderElection= new ZooKeeperLeaderElection(this,incomingMessages);
                        currentLeader= zooKeeperLeaderElection.lookForLeader();
                        logger.info("server "+ id+ " is "+ state + " server "+ currentLeader.getProposedLeaderID());
                        concluded= true;
                        while (!this.shutdown){
                            Thread.sleep(500);
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
                        roundRobinLeader = new RoundRobinLeader(this, list  );
                        //start thread
                        roundRobinLeader.start();

                        //get stuck here until shutdown
                        while (!this.shutdown){
                            Thread.sleep(500);
                        }

                        break;
                    case FOLLOWING:
                        //make thread
                        logger.info("server "+ id+ " is "+ state + " server "+ currentLeader.getProposedLeaderID());
                        javaRunnerFollower = new JavaRunnerFollower(this);
                        //start thread
                        javaRunnerFollower.start();

                        //get stuck here until shutdown
                        while (!this.shutdown){
                            Thread.sleep(500);
                        }

                        break;
                }
            }
        }
        catch (Exception e) {
           //code...
            logger.warning(Util.getStackTrace(e));
        }

    }

}
