package edu.yu.cs.com3800.stage3;

import edu.yu.cs.com3800.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class JavaRunnerFollower extends Thread implements LoggingServer {
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private ZooKeeperPeerServer myPeerServer;
    private JavaRunner javaRunner;
    Logger logger;
    public JavaRunnerFollower(LinkedBlockingQueue<Message> outgoingMessages, LinkedBlockingQueue<Message> incomingMessages, ZooKeeperPeerServer myPeerServer) {
        this.outgoingMessages = outgoingMessages;
        this.incomingMessages = incomingMessages;
        this.myPeerServer = myPeerServer;
        this.setDaemon(true);
        try {
            javaRunner = new JavaRunner();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger = initializeLogging("JavaRunnerFollower " + myPeerServer.getUdpPort());
        logger.info("start");
    }

    public void shutdown() {

        interrupt();
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
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

            if (message!=null&&message.getMessageType().equals(Message.MessageType.WORK)) {

                byte[] contents = message.getMessageContents();
                InputStream is = new ByteArrayInputStream(contents);
                try {
                   byte[] response= javaRunner.compileAndRun(is).getBytes();

                   Message returnWork = new Message(Message.MessageType.COMPLETED_WORK, response, myPeerServer.getAddress().getHostString(), myPeerServer.getUdpPort(), message.getSenderHost(), message.getSenderPort(), message.getRequestID());

                   outgoingMessages.put(returnWork);

                   logger.info("returning work to leader " + returnWork);
                } catch (IOException e) {
                    logger.warning("JavaRunner error" + Util.getStackTrace(e));
                } catch (ReflectiveOperationException e) {
                    logger.warning("JavaRunner error" + Util.getStackTrace(e));
                } catch (InterruptedException e) {
                    logger.warning("could not return work " + Util.getStackTrace(e));
                }

            }
        }
    }
}
