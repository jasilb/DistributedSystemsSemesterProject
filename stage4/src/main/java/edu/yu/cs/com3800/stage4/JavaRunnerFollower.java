package edu.yu.cs.com3800.stage4;

import edu.yu.cs.com3800.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class JavaRunnerFollower extends Thread implements LoggingServer {
    ServerSocket serverSocket;
    private ZooKeeperPeerServer myPeerServer;
    private JavaRunner javaRunner;
    Logger logger;
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

        interrupt();
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
                outputStream = RRL.getOutputStream();
                byte[] bytes = Util.readAllBytesFromNetwork(inputStream);
                message = new Message(bytes);
                logger.info("received message");
                logger.fine(message.toString());

                if (message!=null&&message.getMessageType().equals(Message.MessageType.WORK)) {
                    byte[] contents = message.getMessageContents();
                    InputStream is = new ByteArrayInputStream(contents);
                    byte[] response= null;
                    try {
                       response= javaRunner.compileAndRun(is).getBytes();
                       Message returnWork = new Message(Message.MessageType.COMPLETED_WORK, response, myPeerServer.getAddress().getHostString(), myPeerServer.getUdpPort()+2, message.getSenderHost(), message.getSenderPort(), message.getRequestID());
                       byte[] returnBytes = returnWork.getNetworkPayload();
                       outputStream.write(returnBytes);
                       //System.out.println(returnBytes);
                       logger.info("returning work to leader " + returnWork);
                    } catch (IOException | ReflectiveOperationException | IllegalArgumentException e) {
                        logger.warning("JavaRunner error" + Util.getStackTrace(e));
                        String error = e.getMessage() + '\n' + Util.getStackTrace(e);
                        Message returnWork = new Message(Message.MessageType.COMPLETED_WORK, error.getBytes(StandardCharsets.UTF_8), myPeerServer.getAddress().getHostString(), myPeerServer.getUdpPort()+2, message.getSenderHost(), message.getSenderPort(), message.getRequestID(), true);
                        outputStream.write(returnWork.getNetworkPayload());
                        logger.info("returning work to leader " + returnWork);
                    }
                }

            } catch (IOException e) {
                logger.warning(Util.getStackTrace(e));
            }
        }
    }
}
