import edu.yu.cs.com3800.stage4.GatewayServer;
import edu.yu.cs.com3800.stage4.ZooKeeperPeerServerImpl;
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
        for (int i = 1; i <=x*4 ; i+=4) {
            peerIDtoAddress.put((long) i,new InetSocketAddress("localhost",port+i));
        }
        peerIDtoAddress.put(0L,new InetSocketAddress("localhost",11090+x));
        return peerIDtoAddress;
    }
    public void start(HashMap<Long, InetSocketAddress> peerIDtoAddress, int i){


        //create servers
        ArrayList<ZooKeeperPeerServer> servers = new ArrayList<>(3);
        GatewayServer gatewayServer = new GatewayServer(11088+i,0,peerIDtoAddress,1);

        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            if(entry.getKey()==0L){
                continue;
            }
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            ZooKeeperPeerServerImpl server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map,1);
            map.remove(entry.getKey());
            servers.add(server);
            new Thread(server, "Server on port " + server.getAddress().getPort()).start();
        }
        new Thread(gatewayServer, "Server on port " + gatewayServer.getMyPort()).start();



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
        servers.add(gatewayServer.getGatewayPeerServer());
        for (ZooKeeperPeerServer server : servers) {
            Vote leader = server.getCurrentLeader();
            System.out.println("Server on port " + server.getAddress().getPort() + " whose ID is " + server.getServerId() + " has the following ID as its leader: " + leader.getProposedLeaderID() + " and its state is " + server.getPeerState().name());
            server.shutdown();
        }
        gatewayServer.shutdown();
        System.out.println();
    }
    @Test
    public void test3(){
        start(makeAddresses(3,11000),3);
    }
    @Test
    public void test4(){
        start(makeAddresses(4,11010),4);
    }
    @Test
    public void test5(){
        start(makeAddresses(5,11020),5);
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
//    @Test
//    public void test19(){
//    start(makeAddresses(19,8000));
//}
}
