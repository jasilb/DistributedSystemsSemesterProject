import edu.yu.cs.com3800.stage3.ZooKeeperPeerServerImpl;
import org.junit.*;
import edu.yu.cs.com3800.Vote;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ElectionTest {
    static int port =8000;
    public HashMap<Long,InetSocketAddress> makeAddresses(int x){
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>();
        for (int i = 1; i <=x ; i++) {
            peerIDtoAddress.put((long) i,new InetSocketAddress("localhost",port+i));
        }

        return peerIDtoAddress;
    }
    public void start(HashMap<Long,InetSocketAddress> peerIDtoAddress){


        //create servers
        ArrayList<ZooKeeperPeerServerImpl> servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            map.remove(entry.getKey());
            servers.add(server);
            new Thread(server, "Server on port " + server.getAddress().getPort()).start();
        }
        //wait for threads to start
        try {
            Thread.sleep(11000);
        }
        catch (Exception e) {
        }
        //print out the leaders and shutdown
        for (ZooKeeperPeerServerImpl server : servers) {
            Vote leader = server.getCurrentLeader();
            System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            server.shutdown();

        }

    }
    @Test
    public void test3(){
        start(makeAddresses(3));
        port+=3;
    }
    @Test
    public void test10(){
        start(makeAddresses(10));
        port+=10;
    }
    @Test
    public void test5(){
        start(makeAddresses(5));
        port+=5;

    }





}
