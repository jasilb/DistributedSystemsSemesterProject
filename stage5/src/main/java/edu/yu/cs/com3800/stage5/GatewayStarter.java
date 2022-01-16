package edu.yu.cs.com3800.stage5;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class GatewayStarter {
    public static void main(String[] args) {
        Long l = Long.parseLong(args[0]);
        //System.out.println(l);
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(7);

        for (int i = 1; i <=7*4 ; i+=4) {
            peerIDtoAddress.put((long) i,new InetSocketAddress("localhost",8000+i));
        }



        //create servers
        peerIDtoAddress.put(0L,new InetSocketAddress("localhost",8888+2));


        InetSocketAddress address = peerIDtoAddress.get(l);
        //peerIDtoAddress.remove(l);

        GatewayServer server = new GatewayServer(8888, l , peerIDtoAddress,1);
        server.start();





    }

}
