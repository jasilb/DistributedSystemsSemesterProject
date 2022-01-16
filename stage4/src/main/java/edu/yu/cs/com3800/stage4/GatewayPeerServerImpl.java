package edu.yu.cs.com3800.stage4;

import edu.yu.cs.com3800.Util;
import edu.yu.cs.com3800.Vote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

public class GatewayPeerServerImpl extends ZooKeeperPeerServerImpl {
    public GatewayPeerServerImpl(int port, int epoc, long id, Map<Long, InetSocketAddress> peerIDtoAddress, int observers) {
        super(port,epoc,id,peerIDtoAddress,observers);
        this.setPeerState(ServerState.OBSERVER);
        logger.info("OBSERVER");
        try {
            setCurrentLeader(new Vote(-1,epoc));
        } catch (IOException e) {
            logger.warning(Util.getStackTrace(e));
        }
    }
}
