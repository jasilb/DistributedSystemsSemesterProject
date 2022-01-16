import edu.yu.cs.com3800.stage2.ZooKeeperPeerServerImpl;
import org.junit.*;
import edu.yu.cs.com3800.Vote;
import edu.yu.cs.com3800.ZooKeeperPeerServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class DemoTest {
    public HashMap<Long,InetSocketAddress> makeAddresses(int x, int port){
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>();
        for (int i = 1; i <=x ; i++) {
            peerIDtoAddress.put((long) i,new InetSocketAddress("localhost",port+i));
        }
        return peerIDtoAddress;
    }
    public void start(HashMap<Long,InetSocketAddress> peerIDtoAddress){


        //create servers
        ArrayList<ZooKeeperPeerServer> servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            map.remove(entry.getKey());
            servers.add(server);
            new Thread(server, "Server on port " + server.getAddress().getPort()).start();
        }
        //wait for threads to start
        try {
            if(servers.size()<15){
                Thread.sleep(10000);
            }
            else {
                Thread.sleep(30000);
            }
        }
        catch (Exception e) {
        }
        //print out the leaders and shutdown
        for (ZooKeeperPeerServer server : servers) {
            Vote leader = server.getCurrentLeader();
            System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            server.shutdown();

        }
    }
    @Test
    public void test3(){
        start(makeAddresses(3,8000));
    }
    @Test
    public void test4(){
        start(makeAddresses(4,8000));
    }
    @Test
    public void test5(){
        start(makeAddresses(5,8000));
    }
//    @Test
//    public void testalot() throws InterruptedException {
//        int port=8000;
//        for (int i = 2; i < 20; i++) {
//            System.out.println(i+" servers:");
//            start(makeAddresses(i,port));
//            port+=i;
//            System.out.println();
//            Thread.sleep(500);
//        }
//    }
    @Test
    public void test19(){
    start(makeAddresses(19,8000));
}
}
