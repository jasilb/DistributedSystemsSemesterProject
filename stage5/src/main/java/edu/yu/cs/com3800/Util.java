package edu.yu.cs.com3800;

import edu.yu.cs.com3800.stage5.GatewayPeerServerImpl;
import edu.yu.cs.com3800.stage5.TCPManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;

public class Util {

    public static byte[] readAllBytesFromNetwork(InputStream in)  {
        try {
            while (in.available() == 0) {
                try {
                    Thread.currentThread().sleep(500);
                }
                catch (InterruptedException e) {
                }
            }
        }
        catch(IOException e){}
        return readAllBytes(in);
    }

    public static byte[] readAllBytes(InputStream in) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int numberRead;
        byte[] data = new byte[40960];
        try {
            while (in.available() > 0 && (numberRead = in.read(data, 0, data.length)) != -1   ) {
                buffer.write(data, 0, numberRead);
            }
        }catch(IOException e){}
        return buffer.toByteArray();
    }

    public static Thread startAsDaemon(Thread run, String name) {
        Thread thread = new Thread(run, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public static String getStackTrace(Exception e){
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        PrintStream myErr = new PrintStream(bas,true);
        e.printStackTrace(myErr);
        myErr.flush();
        myErr.close();
        return bas.toString();
    }

    public static byte[] readAllBytesFromNetworkAndCheckForLeader(InputStream in, GatewayPeerServerImpl gatewayPeerServer, InetSocketAddress currentLeader) {
        try {

            while (in.available() == 0&& !gatewayPeerServer.isPeerDead(currentLeader)) {
                try {
                    Thread.currentThread().sleep(500);
                }
                catch (InterruptedException e) {
                }
            }
        }
        catch(IOException e){}
        if (gatewayPeerServer.isPeerDead(currentLeader)){
            return null;
        }
        return readAllBytes(in);
    }

    public static byte[] readAllBytesFromNetworkAndKill(InputStream in, TCPManager tcpManager) {
        try {

            while (in.available() == 0&& !tcpManager.interrupted()) {
                try {
                    Thread.currentThread().sleep(500);

                }
                catch (InterruptedException e) {
                }
            }
        }
        catch(IOException e){return null;}
        if (tcpManager.isInterrupted()){
            System.out.println("close");
            return null;
        }
        return readAllBytes(in);
    }
}
