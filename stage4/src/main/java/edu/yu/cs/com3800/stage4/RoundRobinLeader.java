package edu.yu.cs.com3800.stage4;

import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Util;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class RoundRobinLeader extends Thread implements LoggingServer {

    private ZooKeeperPeerServer myPeerServer;
    private ArrayList<InetSocketAddress> servers;
    LinkedBlockingQueue<InetSocketAddress> addressBlockingQueue;
    ServerSocket serverSocket;
    long round = 0;
    Logger logger;
    private final ExecutorService pool;


    public RoundRobinLeader(ZooKeeperPeerServer myPeerServer, ArrayList<InetSocketAddress> servers) {

        this.myPeerServer = myPeerServer;
        this.servers = servers;

        this.setDaemon(true);
        logger = initializeLogging("RoundRobinLeader ID " + myPeerServer.getServerId()+ " on TCP port "+(myPeerServer.getUdpPort()+2));
        logger.info("start");
        addressBlockingQueue= new LinkedBlockingQueue<>();
        try {
            serverSocket = new ServerSocket(myPeerServer.getUdpPort()+2);
            logger.fine("starting ServerSocket");
            for (InetSocketAddress a: servers) {
                addressBlockingQueue.put(a);
            }
        } catch (IOException | InterruptedException e) {
            logger.warning(Util.getStackTrace(e));
        }
        pool = newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);

    }




    public void shutdown() {
        interrupt();
    }
    @Override
    public void run(){
            while(!this.isInterrupted()){

                try {
                    Socket gateway = serverSocket.accept();
                    logger.info(gateway.toString());
                    TCPManager TCPManager = new TCPManager(gateway,addressBlockingQueue, round);
                    pool.execute(TCPManager);
                    logger.info("giving TCP work");
                    round++;

                } catch (IOException e) {
                    logger.warning(Util.getStackTrace(e));
                }

            }


    }


}
