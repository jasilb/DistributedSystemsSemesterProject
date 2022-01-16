package edu.yu.cs.com3800.stage5;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.com3800.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
        //System.out.println(myPort);
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
            server = HttpServer.create(new InetSocketAddress("localhost", myPort), 50 );
            //System.out.println(server.getAddress());
        } catch (IOException e) {
            logger.warning(Util.getStackTrace(e));
        }


        Executor exec = Executors.newFixedThreadPool(8);
        server.setExecutor(exec);
        server.createContext("/compileandrun", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {

                    long request = requestID.incrementAndGet();
                    Logger handlerLogger = initializeLogging("HttpHandler " + request);
                    handlerLogger.info("start");
                    logger.fine("start handler " + request);

                    logger.info(exchange.getRequestHeaders().entrySet().toString());
                    handlerLogger.info(exchange.getRequestHeaders().entrySet().toString());
                    handlerLogger.info("getting inputStream");
                    byte[] bytes =  exchange.getRequestBody().readNBytes(Integer.parseInt(exchange.getRequestHeaders().get("Content-length").get(0)) );
                //System.out.println(b);
                    //InputStream is = exchange.getRequestBody();
                //byte[] bytes = Util.readAllBytesFromNetwork(is);

                    logger.info(new String(bytes));
                    handlerLogger.info(new String(bytes));
                    if (!exchange.getRequestHeaders().get("Content-Type").contains("text/x-java-source")) {
                        //String response = "Content-type need to be text/x-java-source";
                        logger.warning("wrong Content-Type: " + exchange.getRequestHeaders().get("Content-Type").toString());

                        exchange.sendResponseHeaders(400, 0);
                        //OutputStream os = exchange.getResponseBody();
                        //os.write(null);
                        exchange.close();
                        handlerLogger.info("close");
                        logger.info("close");

                    } else {
                        Message response;
                        while (true) {
                            while (currentLeader==null) {
                                try {
                                    //System.out.println("sleep");
                                    logger.info("waiting for a leader");
                                    handlerLogger.info("waiting for a leader");
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    logger.warning(Util.getStackTrace(e));
                                }
                            }
                            logger.info(currentLeader.toString());
                            handlerLogger.info(currentLeader.toString());
                            Socket socket = null;
                            while (socket == null) {
                                try {
                                    if (currentLeader==null){
                                        Thread.sleep(1000);
                                    }
                                    socket = new Socket(getLeader().getHostName(), getLeader().getPort() + 2);
                                    logger.info(socket.toString());

                                    if (socket.isBound()&& socket.isConnected()) {
                                        handlerLogger.info(socket.toString());
                                        break;
                                    }
                                    else {
                                        socket =null;
                                    }

                                } catch (IOException | NullPointerException | InterruptedException e) {
                                    handlerLogger.info(Util.getStackTrace(e));
                                    socket=null;
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ex) {
                                        handlerLogger.info(Util.getStackTrace(e));
                                    }
                                }
                            }

                            InputStream socketInputStream = socket.getInputStream();

                            OutputStream socketOutputStream = socket.getOutputStream();
                            Message message = new Message(Message.MessageType.WORK, bytes, "localhost", myPort + 4, currentLeader.getHostName(), currentLeader.getPort() + 2, request);
                            handlerLogger.info(message.toString());
                            socketOutputStream.write(message.getNetworkPayload());
                            handlerLogger.info("sending work to leader " + currentLeader.getPort());
                            byte[] returned = Util.readAllBytesFromNetworkAndCheckForLeader(socketInputStream, gatewayPeerServer, currentLeader);
                            if (returned==null) {
                                logger.info("leader died");
                                //System.out.println("leader died");
                                continue;
                            }


                            response = new Message(returned);

                            logger.info("received work");
                            logger.fine(message.toString());
                            handlerLogger.info("received work");
                            handlerLogger.fine(message.toString());

                            socket.close();
                            break;
                        }

                        try {
                            //set up tcp connection with leader

                            OutputStream os = exchange.getResponseBody();
                            if (response.getErrorOccurred() == true) {
                                exchange.sendResponseHeaders(400, response.getMessageContents().length);
                                logger.info("code: " + 400 + ", body " + response.toString());
                            } else {
                                exchange.sendResponseHeaders(200, response.getMessageContents().length);
                                logger.info("code: " + 200 + ", body " + response.toString());
                            }

                            os.write(response.getMessageContents());


                        } catch (Exception e) {
                            e.printStackTrace();
                            String error = e.getMessage() + '\n' + Util.getStackTrace(e);
                            logger.warning("code: " + 400 + ", body:" + e.toString());
                            exchange.sendResponseHeaders(400, error.length());
                            OutputStream os = exchange.getResponseBody();
                            logger.warning(currentLeader.getHostName() + ", " + (currentLeader.getPort() + 2));
                            os.write(error.getBytes());
                            os.close();

                        }

                    }

                    logger.info("close");
                    handlerLogger.info("close");
            }
        });
        server.createContext("/gossip", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {

                while (currentLeader==null) {
                    try {

                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.warning(Util.getStackTrace(e));
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (Map.Entry<Long,InetSocketAddress> server :gatewayPeerServer.peerIDtoAddress.entrySet()) {
                    if (gatewayPeerServer.isPeerDead(server.getKey())){
                        continue;
                    }
                    else if (currentLeader.getPort()==server.getValue().getPort()){
                        stringBuilder.append("Server on port " + server.getValue().getPort() + " whose ID is " + server.getKey() + " has the following ID as its leader: " + currentLeader.getPort() + " and its state is LEADING \n");

                    }
                    else {
                        stringBuilder.append("Server on port " + server.getValue().getPort() + " whose ID is " + server.getKey() + " has the following ID as its leader: " + currentLeader.getPort() + " and its state is FOLLOWING \n");
                    }

                }
                String history =stringBuilder.toString();
                exchange.sendResponseHeaders(200, history.length());
                OutputStream os = exchange.getResponseBody();
                logger.info("Status " + history);
                os.write(history.getBytes());
                os.close();
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

    private InetSocketAddress getLeader(){
        while (currentLeader==null){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return currentLeader;
    }


    public void shutdown(){

        this.interrupt();
        logger.info("shutdown gateway");
        gatewayPeerServer.shutdown();
        server.stop(10);


    }

    @Override
    public void run(){
        gatewayPeerServer = new GatewayPeerServerImpl(myPort + 2, 0, myID, peerIDtoAddress, observers);
        new Thread(gatewayPeerServer, "Server on port " + peerIDtoAddress).start();
        server.start();
        logger.info("starting httpserver");
        while (!this.isInterrupted()) {

            while (gatewayPeerServer.aliveLeader == false) {
                try {

                    Thread.sleep(500);
                    logger.info("waiting for new leader");
                } catch (InterruptedException e) {
                    logger.warning(Util.getStackTrace(e));
                }
            }
            currentLeader = peerIDtoAddress.get(gatewayPeerServer.getCurrentLeader().getProposedLeaderID());
            logger.info(currentLeader.getHostName()+ ", "+ currentLeader.getPort());
            while (gatewayPeerServer.aliveLeader && !this.isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.warning(Util.getStackTrace(e));
                }
            }
            currentLeader = null;
            if (gatewayPeerServer.aliveLeader){
                logger.info("shutdown");
            }
            else {
                logger.info("leader died");
            }
        }
    }
}
