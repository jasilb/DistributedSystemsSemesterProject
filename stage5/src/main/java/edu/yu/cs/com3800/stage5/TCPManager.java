package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Util;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


public class TCPManager extends Thread implements LoggingServer {
    Socket gatewaySocket;
    LinkedBlockingQueue<InetSocketAddress> followers;
    Logger logger;
    InetSocketAddress follower;
    ZooKeeperPeerServer myPeerServer;
    ConcurrentHashMap<Long, Message> queuedWork;
    Socket followerSocket;
    ConcurrentMap<Long, TCPManager> threads;
    Long round;

    public TCPManager(Socket socket, LinkedBlockingQueue followers, long round, ZooKeeperPeerServer myPeerServer, ConcurrentHashMap<Long, Message> queuedWork, ConcurrentMap<Long, TCPManager> threads){
        this.gatewaySocket=socket;
        this.followers=followers;
        this.myPeerServer= myPeerServer;
        this.queuedWork =queuedWork;
        follower= (InetSocketAddress) followers.poll();
        followerSocket=null;
        this.setDaemon(true);
        this.setName("TCPManager to server "+ (follower.getPort()+2) + "_"+ round);
        logger = initializeLogging("TCPManager for server " + myPeerServer.getServerId() +" to"+ (follower.getPort()+2) + "_"+ round);
        logger.info("start");
        logger.info("gateway: "+ gatewaySocket.getPort());
        logger.info("follower "+ follower.getPort());
        this.threads =threads;
        this.round =round;


    }
        public void shutdown(){
            logger.info("interrupted");

            try {
                gatewaySocket.close();
            } catch (IOException ex) {
                logger.warning(Util.getStackTrace(ex));
            }
            try {
                followerSocket.close();

            } catch (IOException | NullPointerException ex) {
                logger.warning(Util.getStackTrace(ex));
            }
            this.interrupt();


        }
    @Override
    public void run(){


                InputStream gatewayInputStream = null;
                OutputStream gatewayOutputStream = null;
                try {
                    gatewayInputStream = gatewaySocket.getInputStream();
                    gatewayOutputStream = gatewaySocket.getOutputStream();
                } catch (IOException e) {
                    logger.warning(Util.getStackTrace(e));
                    e.printStackTrace();
                }
                if (isInterrupted()) {
                    try {
                        gatewaySocket.close();
                        followerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                byte[] gatewayWork = Util.readAllBytesFromNetwork(gatewayInputStream);
                Message gatewayMessage = new Message(gatewayWork);
                logger.info("received work from gateway " + gatewayMessage.getSenderPort() + "\n" + gatewayMessage);
                Message q = null;
                if ((q = queuedWork.get(gatewayMessage.getRequestID())) != null) {
                    logger.info("have old work ");

                    Message toGateway = new Message(q.getMessageType(), q.getMessageContents(), myPeerServer.getAddress().getHostName(), (myPeerServer.getAddress().getPort()) + 2, gatewaySocket.getInetAddress().getHostName(), gatewaySocket.getPort(), q.getRequestID(), q.getErrorOccurred());
                    try {
                        gatewayOutputStream.write(toGateway.getNetworkPayload());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    logger.info("returned work to " + gatewaySocket);
                    followers.offer(follower);
                    queuedWork.remove(q.getRequestID());
                    logger.info("end");
                    return;
                }

                InputStream followerIS = null;
                OutputStream followerOS = null;

                try {
                    Message fromFollower = null;
                    logger.fine("potential follower " + follower.getPort());
                    do {
                        while (followerSocket == null) {
                            while (follower==null){
                                follower = followers.poll();
                            }
                            if (follower.getPort() + 2 == gatewayMessage.getSenderPort()) {
                                logger.info("gateway server finding new server");
                                follower = followers.poll();
                                continue;
                            }
                            if (this.myPeerServer.isPeerDead(follower)) {
                                logger.info("dead server finding new server");
                                follower = followers.poll();
                                continue;
                            }

                            try {

                                followerSocket = new Socket(follower.getHostName(), follower.getPort() + 2);


                            } catch (ConnectException e) {
                                if (this.myPeerServer.isPeerDead(follower)) {
                                    logger.info("follower " + follower.getPort() + " is dead. Changing to new follower");
                                    follower = followers.poll();
                                } else {
                                    Thread.sleep(500);
                                }

                            }
                        }


                        logger.info(followerSocket.toString());
                        Message followerWork = new Message(gatewayMessage.getMessageType(), gatewayMessage.getMessageContents(), gatewayMessage.getReceiverHost(), gatewayMessage.getReceiverPort(), follower.getHostString(), follower.getPort() + 2, gatewayMessage.getRequestID());
                        followerIS = followerSocket.getInputStream();
                        followerOS = followerSocket.getOutputStream();
                        followerOS.write(followerWork.getNetworkPayload());
                        //System.out.println("sending work");
                        logger.info("sent work to " + followerSocket + "\n" + followerWork);


                        byte[] returned = Util.readAllBytesFromNetworkAndKill(followerIS, this);
                        if (returned==null) {
                            logger.warning("dead");
                            return;
                        }
                        logger.info("received work from " + followerSocket);

                        fromFollower = new Message(returned);
                    } while (myPeerServer.isPeerDead(follower));
                    if (isInterrupted()) {
                        try {
                            System.out.println("close");
                            gatewaySocket.close();
                            followerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    Message toGateway = new Message(fromFollower.getMessageType(), fromFollower.getMessageContents(), myPeerServer.getAddress().getHostName(), (myPeerServer.getAddress().getPort()) + 2, gatewaySocket.getInetAddress().getHostName(), gatewaySocket.getPort(), fromFollower.getRequestID(), fromFollower.getErrorOccurred());
                    //System.out.println("returning to gateway: "+ new String(fromFollower.getMessageContents()));
                    gatewayOutputStream.write(toGateway.getNetworkPayload());
                    logger.fine(toGateway.toString());
                    logger.info("returned work to " + gatewaySocket);
                    followerSocket.close();
                    followers.offer(follower);


                } catch (IOException | InterruptedException e) {
                    logger.warning(follower.getHostName() + " " + follower.getPort());
                    logger.warning(Util.getStackTrace(e));
                }
                logger.info("end");
                interrupt();
                threads.remove(round);




    }





}
