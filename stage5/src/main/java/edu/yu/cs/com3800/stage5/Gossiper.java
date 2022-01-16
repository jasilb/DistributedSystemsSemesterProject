package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Util;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Gossiper extends Thread implements LoggingServer {
    ZooKeeperPeerServerImpl myPeerServer;
    ConcurrentHashMap<Long, Boolean> isDead;
    LinkedBlockingQueue<Message> incomingMessages;
    LinkedBlockingQueue<Message> gossipQueue;
    long heartbeat;
    long time;
    final int GOSSIP = 3000;
    final int FAIL = GOSSIP * 10;
    final int CLEANUP = FAIL * 2;
    Map<Long, InetSocketAddress> peerIDtoAddress;
    HashMap<Long,Long[]> gossipMap; //{ID, [gossip time, current time]}
    StringBuilder gossipHistory;
    ArrayList<Long> list;
    Logger logger;
    boolean shutdown;

    int send;
    long myID;

    public Gossiper(ZooKeeperPeerServerImpl zooKeeperPeerServer, ConcurrentHashMap<Long, Boolean> isDead, LinkedBlockingQueue<Message> incomingMessages, Map<Long, InetSocketAddress> peerIDtoAddress, LinkedBlockingQueue<Message> gossipQueue) {
        this.setDaemon(true);

        this.myPeerServer =zooKeeperPeerServer;
        this.isDead =isDead;
        this.incomingMessages = incomingMessages;
        this.peerIDtoAddress=peerIDtoAddress;
        this.gossipQueue = gossipQueue;

        this.myID =myPeerServer.getServerId();
        gossipMap = new HashMap<>();
        gossipHistory = new StringBuilder();
        gossipHistory.append("key: [server ID, heartbeat] \n");
        list = new ArrayList<>();


        heartbeat =0;
        send=(int) (Math.random()*(list.size()*2));
        logger = initializeLogging("gossiper "+ myID);

        shutdown = false;



    }

    @Override
    public void run(){
        time = System.currentTimeMillis();
        for (Long l: isDead.keySet()) {
            gossipMap.put(l, new Long[]{0L,time});
            list.add(l);
        }
        gossipMap.put(myID,new Long[]{0L,time} );
        //System.out.println(list);
        while (!this.isInterrupted()&& ! shutdown){
            //System.out.println(System.currentTimeMillis()-time);
            time =System.currentTimeMillis();

            heartbeat++;
            gossipMap.put(myID, new Long[]{heartbeat,time});
            byte[] gossip = makeMessage();
            long g = list.get(send% list.size());
            while (isDead.get(g)==true){ // don't send gossip to dead nodes
                send++;
                g = list.get(send% list.size());
            }
            //System.out.println(g);
            try {
                myPeerServer.sendMessage(Message.MessageType.GOSSIP, gossip, peerIDtoAddress.get(g));
                logger.info("sending gossip to "+ peerIDtoAddress.get(g).getPort());
            } catch (Exception e) {
                logger.info(Util.getStackTrace(e));
                
            }
            
            
            send++;
            Message incoming = null;

            while (time+GOSSIP>System.currentTimeMillis() && !this.isInterrupted()){
                //System.out.println(time+GOSSIP-System.currentTimeMillis());

                Message received = null;
                try {
                    received = gossipQueue.poll(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    logger.info(Util.getStackTrace(e));
                }
                if (received!=null){
                    //System.out.println(myID +" from gossip queue");
                    logger.info("gossip polled \n" + received);
                    validateMessage(received);
                }


            }
            checkFails();

        }
    }

    private void checkFails() {
        ArrayList<Long> remove = new ArrayList<>();
        Long time = System.currentTimeMillis();
        //System.out.println(System.currentTimeMillis()-time);
        StringBuilder gmap =new StringBuilder();
        for (Map.Entry<Long, Long[]> g: gossipMap.entrySet()) {
            gmap.append("["+ g.getKey()+ ": "+ g.getValue()[0] +", " + (time -g.getValue()[1]) + "] ");
            if (time -g.getValue()[1] > FAIL ){
                if (!isDead.get(g.getKey())){ // if he just died
                    System.out.println(myID+ ": no heartbeat from server " +g.getKey()+ " - server failed");
                    logger.info(myID+ ": no heartbeat from server " +g.getKey()+ " - server failed");
                    myPeerServer.reportFailedPeer(g.getKey());
                }
                if (time -g.getValue()[1] > CLEANUP){
                    remove.add(g.getKey());
                }

            }
        }
        for (Long l: remove) { // remove all cleanup nodes
            //System.out.println("removing "+l );
            gossipMap.remove(l);
        }
        logger.info(gmap.toString());


    }

    private void validateMessage(Message incoming) {
        byte[] bytes= incoming.getMessageContents();
        ByteBuffer contents =  ByteBuffer.wrap(bytes);
        Long time = System.currentTimeMillis();
        StringBuilder gmap =new StringBuilder();
        gossipHistory.append(" from: "+ incoming.getSenderPort()+ " at time "+ time + ": {");
        while (contents.hasRemaining()){
            long ID = contents.getLong();
            long gossipHeartbeat = contents.getLong();
            String history = "["+ ID +", "+ gossipHeartbeat +"],";
            gossipHistory.append(history);
            Long[] oldGossip = gossipMap.get(ID);


            if (oldGossip==null || (oldGossip[0]<gossipHeartbeat && time - oldGossip[1] < FAIL)){
                gmap.append(myID + ": updated " + ID+ "'s heartbeat sequence to " +gossipHeartbeat+ " based on message from " + incoming.getSenderPort()+ " at node time "+ time+ "\n");

                gossipMap.put(ID, new Long[]{gossipHeartbeat,time});
            }
        }
        gossipHistory.append("},\n");
        logger.info("received : "+ gmap.toString());
    }

    private byte[] makeMessage() {
        int i = 0;
        for (boolean b:isDead.values()) {
            if (!b){
                i++;
            }
        }
        //System.out.println(i);
        ByteBuffer buffer = ByteBuffer.allocate((i*2*8)+16);
        for (Map.Entry<Long, Long[]> g: gossipMap.entrySet()) {

            if (isDead.getOrDefault(g.getKey(),false)==true){ // only send heartbeats of alive servers
                //System.out.println(myID+ ": "+ g.getKey());
                continue;
            }
            buffer.putLong(g.getKey());
            buffer.putLong(g.getValue()[0]);
        }

        return buffer.array();
    }
    public String getHistory(){
        return gossipHistory.toString();
    }

    public void shutdown() {
        this.shutdown=true;
        interrupt();

    }
}
