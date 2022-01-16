package edu.yu.cs.com3800.stage4;

import edu.yu.cs.com3800.LoggingServer;
import edu.yu.cs.com3800.Message;
import edu.yu.cs.com3800.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


public class TCPManager extends Thread implements LoggingServer {
    Socket gatewaySocket;
    LinkedBlockingQueue<InetSocketAddress> followers;
    Logger logger;
    InetSocketAddress follower;
    public TCPManager(Socket socket, LinkedBlockingQueue followers, long round){
        this.gatewaySocket=socket;
        this.followers=followers;
        follower= (InetSocketAddress) followers.poll();
        this.setDaemon(true);
        this.setName("TCPManager to server "+ (follower.getPort()+2) + "_"+ round);
        logger = initializeLogging("TCPManager to server "+ (follower.getPort()+2) + "_"+ round);
        logger.info("start");
        logger.info("gateway: "+ gatewaySocket.getPort());
        logger.info("follower "+ follower.getPort());
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
            byte[] work = Util.readAllBytesFromNetwork(gatewayInputStream);
            Message message = new Message(work);
            Socket followerSocket = null;
            InputStream followerIS = null;
            OutputStream followerOS= null;
            try {
                while (followerSocket==null) {
                    try {

                        followerSocket = new Socket(follower.getHostName(), follower.getPort() + 2);


                    } catch (ConnectException e) {
                        logger.info("change port");
                        follower = followers.poll();
                    }
                }


                logger.info(followerSocket.toString());
                followerIS = followerSocket.getInputStream();
                followerOS = followerSocket.getOutputStream();
                followerOS.write(work);

                logger.info("sent work to "+ followerSocket + "\n"+ message);

                byte[] returned = Util.readAllBytesFromNetwork(followerIS);

                logger.info("received work to "+ followerSocket);

                gatewayOutputStream.write(returned);
                logger.info("returned work to "+ gatewaySocket);
                followerSocket.close();
                followers.offer(follower);

            } catch (IOException e) {
                logger.warning(follower.getHostName()+ " "+ follower.getPort());
                e.printStackTrace();
            }
            logger.info("end");
        }

}
