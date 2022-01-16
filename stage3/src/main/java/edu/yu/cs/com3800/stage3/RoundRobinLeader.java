package edu.yu.cs.com3800.stage3;

import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Util;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RoundRobinLeader extends Thread implements LoggingServer {
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private ZooKeeperPeerServer myPeerServer;
    private ArrayList<InetSocketAddress> servers;
    private HashMap<Long,Request > requests = new HashMap<>();
    int round =0;
    long requestID=0;
    Logger logger;

    public RoundRobinLeader(LinkedBlockingQueue<Message> outgoingMessages, LinkedBlockingQueue<Message> incomingMessages, ZooKeeperPeerServer myPeerServer, ArrayList<InetSocketAddress> servers) {
        this.outgoingMessages = outgoingMessages;
        this.incomingMessages = incomingMessages;
        this.myPeerServer = myPeerServer;
        this.servers = servers;
        this.setDaemon(true);
        logger = initializeLogging("Leader RoundRobin"+myPeerServer.getUdpPort());
        logger.info("start");
    }

    public void shutdown() {
        interrupt();
    }
    @Override
    public void run(){
            while(!this.isInterrupted()){
                Message message = null;

                try {
                    message = incomingMessages.poll(200, TimeUnit.MILLISECONDS);
                    while (message == null) {
                        message = incomingMessages.poll(200, TimeUnit.MILLISECONDS);
                    }
                    logger.info("received message "+message.toString());
                } catch (InterruptedException e) {
                    logger.warning(Util.getStackTrace(e));
                }



                if(message!=null&& message.getMessageType().equals(Message.MessageType.WORK)){

                    Request request = new Request(message.getSenderHost(),message.getSenderPort());
                    requests.put(requestID,request);
                    byte[] contents = message.getMessageContents();
                    InetSocketAddress address = servers.get(round%servers.size());
                    Message sendWork = new Message(message.getMessageType(),contents,myPeerServer.getAddress().getHostString(),myPeerServer.getUdpPort(),address.getHostString(),address.getPort(), requestID);
                    try {
                        outgoingMessages.put(sendWork);
                        round++;
                        requestID++;
                        logger.info("sending work to server "+sendWork);
                    } catch (InterruptedException e) {
                        logger.warning("could not send work "+ sendWork.toString());
                    }
                }
                else if( message!=null&& message.getMessageType().equals(Message.MessageType.COMPLETED_WORK)){
                    Request request = requests.get(message.getRequestID());
                    //System.out.println(request.toString());
                    byte[] contents = message.getMessageContents();
                    Message returnWork = new Message(message.getMessageType(),contents,myPeerServer.getAddress().getHostString(),myPeerServer.getUdpPort(),request.getHostString(),request.getPort(), requestID);
                    try {
                        outgoingMessages.put(returnWork);
                        logger.info("returning work "+ returnWork.toString());
                    } catch (InterruptedException e) {
                        logger.warning("could not return "+ returnWork.toString());
                    }
                }
            }
    }
    class Request{
        String host;
        int port;

         Request(String host, int port) {
            this.host = host;
            this.port = port;
         }

        String getHostString() {
            return host;
        }

        int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

}
