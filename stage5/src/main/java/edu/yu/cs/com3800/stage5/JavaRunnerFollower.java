package edu.yu.cs.com3800.stage5;

import edu.yu.cs.com3800.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Logger;

public class JavaRunnerFollower extends Thread implements LoggingServer {
    ServerSocket serverSocket;
    private ZooKeeperPeerServer myPeerServer;
    private JavaRunner javaRunner;
    Logger logger;
    Message queuedWork;
    public JavaRunnerFollower(ZooKeeperPeerServer myPeerServer) {
        logger = initializeLogging("JavaRunnerFollower ID "+myPeerServer.getServerId()+ " on TCP port " + (myPeerServer.getUdpPort()+2));
        logger.info("start");
        this.myPeerServer = myPeerServer;
        this.setDaemon(true);
        try {
            javaRunner = new JavaRunner();
            serverSocket = new ServerSocket(myPeerServer.getUdpPort()+2);
        } catch (IOException e) {
            logger.warning(Util.getStackTrace(e));
        }

    }

    public void shutdown() {
        logger.info("shutdown");
        //System.out.println("shutdown JRF");
        try {
            this.interrupt();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            Message message = null;
            InputStream inputStream = null;
            OutputStream outputStream =null;
            try {
                Socket RRL = serverSocket.accept();
                //System.out.println("accepted server "+ myPeerServer.getUdpPort()+2);
                inputStream = RRL.getInputStream();

                byte[] bytes = Util.readAllBytesFromNetwork(inputStream);
                message = new Message(bytes);
                logger.info("received message "+ message);


                if (message!=null&&message.getMessageType().equals(Message.MessageType.WORK)) {
                    byte[] contents = message.getMessageContents();
                    InputStream is = new ByteArrayInputStream(contents);
                    byte[] response= null;
                    Message returnWork= null;
                    logger.info("starting work");
                    try {
                       response= javaRunner.compileAndRun(is).getBytes();
                       returnWork = new Message(Message.MessageType.COMPLETED_WORK, response, myPeerServer.getAddress().getHostString(), myPeerServer.getUdpPort()+2, message.getSenderHost(), message.getSenderPort(), message.getRequestID());
                       byte[] returnBytes = returnWork.getNetworkPayload();

                    } catch (IOException | ReflectiveOperationException | IllegalArgumentException e) {
                        logger.warning("JavaRunner error" + Util.getStackTrace(e));
                        String error = e.getMessage() + '\n' + Util.getStackTrace(e);
                        returnWork = new Message(Message.MessageType.COMPLETED_WORK, error.getBytes(StandardCharsets.UTF_8), myPeerServer.getAddress().getHostString(), myPeerServer.getUdpPort()+2, message.getSenderHost(), message.getSenderPort(), message.getRequestID(), true);

                    }
                    logger.info("finished work");

                    //System.out.println(RRL.isClosed());


                    RRL.setSoTimeout(10);
                    try {
                        if (RRL.getInputStream().read()!=-1&& !myPeerServer.isPeerDead(new InetSocketAddress(returnWork.getReceiverHost(), (returnWork.getReceiverPort())-2))){

                            try{
                                logger.info("returning work to leader " + returnWork);
                                outputStream = RRL.getOutputStream();
                                outputStream.write(returnWork.getNetworkPayload());


                            }
                            catch (IOException e){
                                logger.info(Util.getStackTrace(e));
                                logger.info("leader died");
                                queuedWork = returnWork;
                            }

                        }
                        else{
                            //System.out.println("leader died");
                            logger.info("leader died");
                            RRL.close();
                            queuedWork= returnWork;
                        }
                    }catch (SocketTimeoutException e){
                        try{
                            logger.info("returning work to leader " + returnWork);
                            outputStream = RRL.getOutputStream();
                            outputStream.write(returnWork.getNetworkPayload());


                        }
                        catch (IOException ex){
                            logger.info(Util.getStackTrace(ex));
                            logger.info("leader died");
                            queuedWork = returnWork;
                        }
                    }


                }
                else if(message!=null&&message.getMessageType().equals(Message.MessageType.NEW_LEADER_GETTING_LAST_WORK)){
                    outputStream = RRL.getOutputStream();
                    if (queuedWork==null){
                        logger.info("no queued work");
                        outputStream.write((new Message(Message.MessageType.COMPLETED_WORK,new byte[0],serverSocket.getInetAddress().getHostAddress(),serverSocket.getLocalPort(),message.getSenderHost(),message.getSenderPort(),0,true)).getNetworkPayload());

                    }
                    else {

                        Message qWork = new Message(queuedWork.getMessageType(),queuedWork.getMessageContents(), queuedWork.getSenderHost(), queuedWork.getSenderPort(), message.getSenderHost(),message.getSenderPort(), queuedWork.getRequestID());
                        logger.info("sending queued work to new leader "+ qWork.toString());
                        outputStream.write(qWork.getNetworkPayload());
                        queuedWork=null;

                    }

                }
                //System.out.println(myPeerServer.getServerId() + " done with work");
                logger.info("done with work");

            } catch (IOException e) {
                logger.warning(Util.getStackTrace(e));
            } catch (Exception e){
                logger.warning(Util.getStackTrace(e));
            }
        }
    }
}
