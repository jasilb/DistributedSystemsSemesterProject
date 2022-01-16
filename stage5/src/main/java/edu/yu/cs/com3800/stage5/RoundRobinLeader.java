package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Util;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class RoundRobinLeader extends Thread implements LoggingServer {

    private ZooKeeperPeerServer myPeerServer;
    private ArrayList<InetSocketAddress> servers;
    LinkedBlockingQueue<InetSocketAddress> addressBlockingQueue;
    ServerSocket serverSocket;
    long round = 0;
    Logger logger;

    ConcurrentHashMap<Long, Message> queuedWork;
    ConcurrentMap<Long, TCPManager> threads;



    public RoundRobinLeader(ZooKeeperPeerServer myPeerServer, ArrayList<InetSocketAddress> servers) {

        this.myPeerServer = myPeerServer;
        this.servers = servers;

        this.setDaemon(true);
        logger = initializeLogging("RoundRobinLeader ID " + myPeerServer.getServerId()+ " on TCP port "+(myPeerServer.getUdpPort()+2));
        logger.info("start");
        addressBlockingQueue= new LinkedBlockingQueue<>();
        try {

            logger.fine("starting ServerSocket");
            for (InetSocketAddress a: servers) {
                addressBlockingQueue.put(a);
            }
        } catch (InterruptedException e) {
            logger.warning(Util.getStackTrace(e));
        }

        threads = new ConcurrentHashMap<>();
        queuedWork = new ConcurrentHashMap<>();
        try {
            serverSocket = new ServerSocket(myPeerServer.getUdpPort()+2, 50);
        } catch (IOException e) {
            logger.warning(Util.getStackTrace(e));
        }




    }




    public void shutdown() {
        interrupt();

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



        for ( TCPManager t: threads.values()) {

            try {
                t.shutdown();
            }catch (Exception e){

            }
        }
        threads.clear();
        logger.info("shutdown pool");

    }
    @Override
    public void run(){

        getOldWork();


        while(!this.isInterrupted()){

                try {
                    Socket gateway = serverSocket.accept();
                    logger.info(gateway.toString());
                    TCPManager TCPManager = new TCPManager(gateway,addressBlockingQueue, round,myPeerServer, queuedWork, threads);
                    TCPManager.start();
                    threads.put(round, TCPManager);
                    logger.info("giving TCP work");
                    round++;


                } catch (Exception e) {
                    logger.warning(Util.getStackTrace(e));
                }

            }
            logger.info("close");


    }




        private void getOldWork(){
            for (InetSocketAddress i: servers) {
                try {
                    if (isInterrupted()){
                        logger.info("NewLeader is interrupted");
                        interrupt();
                        break;
                    }

                    //System.out.println("old work");
                    Socket socket = new Socket(i.getHostName(),i.getPort()+2);

                    logger.info("getting old work from "+socket.toString());
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();
                    Message followerWork = new Message(Message.MessageType.NEW_LEADER_GETTING_LAST_WORK, new byte[0], "localhost", serverSocket.getLocalPort(), i.getHostString(), i.getPort() + 2);

                    os.write(followerWork.getNetworkPayload());
                    byte[] returned = Util.readAllBytesFromNetwork(is);
                    Message queue = new Message(returned);
                    //System.out.println(queue);
                    if (queue.getRequestID()!=0){
                        logger.info("old work "+ queue);
                        queuedWork.put(queue.getRequestID(), queue);
                    }
                    else {
                        logger.info("no queued messages");
                    }
                } catch (UnknownHostException e) {
                    logger.info(Util.getStackTrace(e));
                   continue;
                } catch (IOException e) {
                    logger.info(Util.getStackTrace(e));
                    continue;
                }
            }
        }
    }

