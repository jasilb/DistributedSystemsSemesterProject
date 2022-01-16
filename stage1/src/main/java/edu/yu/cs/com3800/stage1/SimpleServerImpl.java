package edu.yu.cs.com3800.stage1;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.com3800.JavaRunner;
import edu.yu.cs.com3800.SimpleServer;

public class SimpleServerImpl implements SimpleServer{
    int port;
    HttpServer server;
    JavaRunner javaRunner;
    Logger logger;

    public SimpleServerImpl(int port) throws IOException {
        logger = Logger.getLogger("MyLog");
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler("LogFile.log");
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        javaRunner = new JavaRunner();
        this.port =port;
        server = HttpServer.create(new InetSocketAddress(port), 0);

        //Executor exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        //server.setExecutor(exec);
        server.createContext("/compileandrun", new GetHandler());


        //System.out.println(server.getExecutor());
        logger.info("start");
        logger.info(InetAddress.getLocalHost().getHostAddress());
    }


    @Override
    public void start() {
        // TODO Auto-generated method stub
        logger.info("starting server");
        server.start();




    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        logger.info("stopping server");
        server.stop(0);
    }
    public static void main(String[] args) {
        int port = 9000;
        if(args.length >0) {
            port = Integer.parseInt(args[0]);
        }
        SimpleServer myserver = null;
        try {
            myserver = new SimpleServerImpl(port);

            myserver.start();

        }
        catch(Exception e) {
            System.err.println(e.getMessage());
            myserver.stop();
        }
    }


    private class GetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            //TODO write code for this

            logger.info(exchange.getRequestHeaders().entrySet().toString());
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            InputStream is = new ByteArrayInputStream(bytes);

            logger.info(new String(bytes));


            if(!exchange.getRequestHeaders().get("Content-Type").contains("text/x-java-source")){
                //String response = "Content-type need to be text/x-java-source";
                logger.warning("wrong Content-Type: " +exchange.getRequestHeaders().get("Content-Type").toString());

                exchange.sendResponseHeaders(400, 0);
                //OutputStream os = exchange.getResponseBody();
                //os.write(null);

                //os.close();
                is.close();
                exchange.close();
                logger.info("close");

            }
            else {


                try {
                    String response = javaRunner.compileAndRun(is);

                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    logger.info("code: "+200 + ", body "+ response);


                } catch (Exception e) {
                    ByteArrayOutputStream printOut= new ByteArrayOutputStream();
                    PrintStream printStream = new PrintStream(printOut);
                    e.printStackTrace(printStream);
                    byte[] errorBytes = printOut.toByteArray();
                    String error = e.getMessage() + '\n' + new String(errorBytes);
                    logger.warning("code: "+400 +", body:" + e.toString());
                    exchange.sendResponseHeaders(400, error.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(error.getBytes());
                    os.close();

                }


                //exchange.getRequestBody().reset();
                //System.out.println(is.available());


                //System.out.println("done");
            }
            is.close();
            exchange.close();
            logger.info("close");
        }

    }
}
