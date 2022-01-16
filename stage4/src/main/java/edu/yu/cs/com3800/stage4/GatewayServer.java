package edu.yu.cs.com3800.stage4;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.com3800.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class GatewayServer extends Thread implements LoggingServer{

    private final int myPort;
    private final long myID;
    private InetSocketAddress currentLeader;
    private final Map<Long,InetSocketAddress> peerIDtoAddress;
    private int observers;
    HttpServer server;
    JavaRunner javaRunner;
    Logger logger;
    AtomicLong requestID = new AtomicLong();
    GatewayPeerServerImpl gatewayPeerServer;
    public GatewayServer(int myPort, long ID, Map<Long, InetSocketAddress> peerIDtoAddress, int observers){
        logger= initializeLogging("Gateway_Server");
        this.peerIDtoAddress=peerIDtoAddress;
        this.observers= observers;
        this.myID = ID;
        try {
            javaRunner = new JavaRunner();
        } catch (IOException e) {
            logger.warning(Util.getStackTrace(e));
        }
        this.myPort =myPort;
        try {
            server = HttpServer.create(new InetSocketAddress(myPort), 10 );
        } catch (IOException e) {
            logger.warning(Util.getStackTrace(e));
        }


        Executor exec = Executors.newFixedThreadPool(5);
        server.setExecutor(exec);
        server.createContext("/compileandrun", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                long request = requestID.getAndIncrement();
                Logger handlerLogger = initializeLogging("HttpHandler "+ request);
                handlerLogger.info("start");
                logger.fine("start handler "+ request);
                while (gatewayPeerServer.concluded == false) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        logger.warning(Util.getStackTrace(e));
                    }
                }
                logger.info(exchange.getRequestHeaders().entrySet().toString());
                handlerLogger.info(exchange.getRequestHeaders().entrySet().toString());
                byte[] bytes = Util.readAllBytesFromNetwork(exchange.getRequestBody());
                logger.info(new String(bytes));
                handlerLogger.info(new String(bytes));
                if(!exchange.getRequestHeaders().get("Content-Type").contains("text/x-java-source")){
                    //String response = "Content-type need to be text/x-java-source";
                    logger.warning("wrong Content-Type: " +exchange.getRequestHeaders().get("Content-Type").toString());

                    exchange.sendResponseHeaders(400, 0);
                    //OutputStream os = exchange.getResponseBody();
                    //os.write(null);
                    exchange.close();
                    handlerLogger.info("close");
                    logger.info("close");

                }
                else {


                    try {
                        //set up tcp connection with leader
                        int tries =0;
                        Socket socket = null;
                        while (tries<5) {
                            try {
                                socket = new Socket(currentLeader.getHostName(), currentLeader.getPort()+2);
                                logger.info(socket.toString());
                                if (socket.isConnected()) {
                                    logger.info("none");
                                    break;
                                }

                            }

                            catch (IOException e) {
                                tries++;
                                Thread.sleep(500);
                            }
                        }


                        InputStream socketInputStream = socket.getInputStream();

                        OutputStream socketOutputStream = socket.getOutputStream();
                        Message message = new Message(Message.MessageType.WORK,bytes,"localhost", myPort+4, currentLeader.getHostName(), currentLeader.getPort()+2,request);
                        socketOutputStream.write(message.getNetworkPayload());
                        byte[] returned= Util.readAllBytesFromNetwork(socketInputStream);



                        Message response = new Message(returned);

                        logger.info("received work");
                        logger.fine(message.toString());
                        handlerLogger.info("received work");
                        handlerLogger.fine(message.toString());

                        socket.close();
                        OutputStream os = exchange.getResponseBody();
                        if (response.getErrorOccurred()==true){
                            exchange.sendResponseHeaders(400, response.getMessageContents().length);
                            logger.info("code: "+400 + ", body "+ response.toString());
                        }
                        else {
                            exchange.sendResponseHeaders(200, response.getMessageContents().length);
                            logger.info("code: "+200 + ", body "+ response.toString());
                        }

                        os.write(response.getMessageContents());




                    } catch (Exception e) {
                        e.printStackTrace();
                        String error = e.getMessage() + '\n' + Util.getStackTrace(e);
                        logger.warning("code: "+400 +", body:" + e.toString());
                        exchange.sendResponseHeaders(400, error.length());
                        OutputStream os = exchange.getResponseBody();
                        logger.warning(currentLeader.getHostName()+ ", "+ (currentLeader.getPort()+2));
                        os.write(error.getBytes());
                        os.close();

                    }

                }

                logger.info("close");
                handlerLogger.info("close");
            }
        });

        logger.info("start");
        try {
            logger.info(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public GatewayPeerServerImpl getGatewayPeerServer() {
        return gatewayPeerServer;
    }

    public int getMyPort() {
        return myPort;
    }

    public long getMyID() {
        return myID;
    }

    public InetSocketAddress getCurrentLeader() {
        return currentLeader;
    }

    public Map<Long, InetSocketAddress> getPeerIDtoAddress() {
        return peerIDtoAddress;
    }

    public int getObservers() {
        return observers;
    }

    public void shutdown(){

        this.interrupt();
        logger.info("shutdown");
        gatewayPeerServer.shutdown();
        server.stop(10);


    }

    @Override
    public void run(){
        while (!this.isInterrupted()) {

//server logic goes here

            gatewayPeerServer = new GatewayPeerServerImpl(myPort + 2, 0, myID, peerIDtoAddress, observers);
            new Thread(gatewayPeerServer, "Server on port " + peerIDtoAddress).start();
            server.start();
            while (gatewayPeerServer.concluded == false) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.warning(Util.getStackTrace(e));
                }
            }
            currentLeader = peerIDtoAddress.get(gatewayPeerServer.getCurrentLeader().getProposedLeaderID());
            logger.info(currentLeader.getHostName()+ ", "+ currentLeader.getPort());
            while (!this.isInterrupted()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.warning(Util.getStackTrace(e));
                }
            }


        }
    }
}
