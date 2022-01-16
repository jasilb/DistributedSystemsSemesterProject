package edu.yu.cs.com3800;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static edu.yu.cs.com3800.ZooKeeperPeerServer.ServerState.*;

public class ZooKeeperLeaderElection {
    /**
     * time to wait once we believe we've reached the end of leader election.
     */
    private final static int finalizeWait = 200;

    /**
     * Upper bound on the amount of time between two consecutive notification checks.
     * This impacts the amount of time to get the system up again after long partitions. Currently 60 seconds.
     */
    private final static int maxNotificationInterval = 60000;
    private final LinkedBlockingQueue<Message> incomingMessages;
    private final ZooKeeperPeerServer myPeerServer;
    private long proposedLeader;
    private long proposedEpoch;
    HashMap<Long, ElectionNotification> map = new HashMap<>();
    int backoff= 200;
    LinkedBlockingQueue<Message> gossipQueue;


    public ZooKeeperLeaderElection(ZooKeeperPeerServer server, LinkedBlockingQueue<Message> incomingMessages, LinkedBlockingQueue<Message> gossipQueue) {
        this.incomingMessages = incomingMessages;
        this.myPeerServer = server;
        proposedLeader= server.getCurrentLeader().getProposedLeaderID();
        proposedEpoch = server.getCurrentLeader().getPeerEpoch()+1;
        this.gossipQueue =gossipQueue;


    }

    public static byte[] buildMsgContent(ElectionNotification notification) {
        ByteBuffer buffer = ByteBuffer.allocate(26);
        buffer.putLong(notification.getProposedLeaderID());
        buffer.putChar(notification.getState().getChar());
        buffer.putLong(notification.getSenderID());
        buffer.putLong(notification.getPeerEpoch());
        byte[] arr = buffer.array();

        return arr;
    }


    public static ElectionNotification getNotificationFromMessage(Message received) {
        ByteBuffer contents =  ByteBuffer.wrap(received.getMessageContents());
        long leader =contents.getLong(0);
        char c = contents.getChar(8);
        ZooKeeperPeerServer.ServerState state= getServerState(c);
        long senderID = contents.getLong(10);
        long epoch = contents.getLong(18);
        return  new ElectionNotification(leader, state,senderID,epoch);
    }


    private synchronized Vote getCurrentVote() {
        return new Vote(this.proposedLeader, this.proposedEpoch);
    }

    public synchronized Vote lookForLeader() {
        //send initial notifications to other peers to get things started
        sendNotifications();
        //Loop, exchanging notifications with other servers until we find a leader
        while ((this.myPeerServer.getPeerState() == LOOKING)||(this.myPeerServer.getPeerState() == OBSERVER) ) {
            Message incoming = null;
            //Remove next notification from queue, timing out after 2 times the termination time
            //if no notifications received..
            //..resend notifications to prompt a reply from others..
            //.and implement exponential back-off when notifications not received..
            while (incoming==null){


                try {

                    incoming = incomingMessages.poll(Math.min(backoff,maxNotificationInterval), TimeUnit.SECONDS);
                    if (backoff< maxNotificationInterval){
                        backoff*=2;
                    }
                    sendNotifications();
                    if (incoming != null && incoming.getMessageType()== Message.MessageType.GOSSIP){
                        //System.out.println("gossip");
                        gossipQueue.put(incoming);
                        incoming = null;
                        continue;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();

                }
            }

            backoff=200;

            //if/when we get a message and it's from a valid server and for a valid server..
            ElectionNotification electionNotification = getNotificationFromMessage(incoming);

            switch (electionNotification.getState()) {
                //on the state of the sender:
                case LOOKING: //if the sender is also looking
                    //if the received message has a vote for a leader which supersedes mine, change my vote and tell all my peers what my new vote is.
                    //keep track of the votes I received and who I received them from.
                    map.put(electionNotification.getSenderID(),electionNotification);
                    //System.out.println(map);
                    if(supersedesCurrentVote(electionNotification.getSenderID(), electionNotification.getPeerEpoch())) {
                        ////System.out.println("better");
                        proposedLeader = electionNotification.getProposedLeaderID();
                        //System.out.println(myPeerServer.getServerId()+ ": "+ proposedLeader);
                        try {
                            myPeerServer.setCurrentLeader(electionNotification);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        sendNotifications();


                    }
                    if(haveEnoughVotes(map, electionNotification) && electionNotification.getProposedLeaderID()>=proposedLeader){
                       //System.out.println("enough " +myPeerServer.getServerId() + ": "+ map.toString());
                        //System.out.println(myPeerServer.getServerId()+ ": "+ electionNotification);
                        Message message= null;

                        try {


                            //System.out.println(incomingMessages.size());
                            while ((message = incomingMessages.poll(finalizeWait, TimeUnit.MILLISECONDS)) != null) {

                                if (message.getMessageType() != Message.MessageType.ELECTION) {
                                    //System.out.println("gossip");
                                    gossipQueue.put(message);
                                    message=null;
                                    continue;


                                }
                                ElectionNotification notification2 = getNotificationFromMessage(message);
                               //System.out.println(myPeerServer.getServerId() +" note "+ notification2);
                                map.put(notification2.getSenderID(),notification2);
                                if(supersedesCurrentVote(notification2.getSenderID(), notification2.getPeerEpoch())){
                                   //System.out.println(myPeerServer.getServerId() +" change to "+notification2 );
                                   //System.out.println(map);
                                    proposedLeader = notification2.getProposedLeaderID();
                                    try {
                                        myPeerServer.setCurrentLeader(notification2);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                        sendNotifications();

                                    incomingMessages.offer(message);

                                   //System.out.println("more");
                                    break;
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if(message==null){

                            return acceptElectionWinner(electionNotification);
                        }


                    }

                    ////System.out.println("continue");
                    break;
                    ////if I have enough votes to declare my currently proposed leader as the leader:
                    //first check if there are any new votes for a higher ranked possible leader before I declare a leader. If so, continue in my election loop
                    //If not, set my own state to either LEADING (if I won the election) or FOLLOWING (if someone lese won the election) and exit the election
                case OBSERVER:
                    break;
                case FOLLOWING: case LEADING: //if the sender is following a leader already or thinks it is the leader
                    //IF: see if the sender's vote allows me to reach a conclusion based on the election epoch that I'm in, i.e. it gives the majority to the vote of the FOLLOWING or LEADING peer whose vote I just received.
                    //if so, accept the election winner.
                    //As, once someone declares a winner, we are done. We are not worried about / accounting for misbehaving peers.
                    //ELSE: if n is from a LATER election epoch
                    //IF a quorum from that epoch are voting for the same peer as the vote of the FOLLOWING or LEADING peer whose vote I just received.
                    //THEN accept their leader, and update my epoch to be their epoch
                    //ELSE:
                    //keep looping on the election loop.
                    map.put(electionNotification.getSenderID(),electionNotification);
                    if(supersedesCurrentVote(electionNotification.getSenderID(), electionNotification.getPeerEpoch())){
                        proposedLeader = electionNotification.getProposedLeaderID();
                        //System.out.println(electionNotification);
                        if(haveEnoughVotes(map, getCurrentVote())){
                            return acceptElectionWinner(electionNotification);
                        }
                    }

                    break;


            }

        }
        return getCurrentVote();
    }

    private void sendNotifications() {
        ByteBuffer buffer = ByteBuffer.allocate(26);
        buffer.putLong(proposedLeader);
        if (myPeerServer.getPeerState().equals(LOOKING)){
            buffer.putChar(LOOKING.getChar());
        }
        else {
            buffer.putChar(OBSERVER.getChar());
        }
        buffer.putLong(myPeerServer.getServerId());
        buffer.putLong(proposedEpoch);
        myPeerServer.sendBroadcast(Message.MessageType.ELECTION, buffer.array());
    }


    private Vote acceptElectionWinner(ElectionNotification n) {
        //System.out.println(myPeerServer.getServerId()+ ": "+ map);
        //set my state to either LEADING or FOLLOWING
        //clear out the incoming queue before returning
        //System.out.println(myPeerServer.getServerId()+ ": "+n.getProposedLeaderID() );
       //System.out.println(map.toString());
        if (myPeerServer.getPeerState()==OBSERVER){
            return n;
        }
        if(myPeerServer.getServerId()==n.getProposedLeaderID()){
            //System.out.println("leading "+ myPeerServer.getServerId());
            myPeerServer.setPeerState(LEADING);
        }
        else {
            //System.out.println("following "+ myPeerServer.getServerId());
            myPeerServer.setPeerState(FOLLOWING);
        }
        //incomingMessages.clear();
        //System.out.println("clear");
        return n;
    }

    /*
     * We return true if one of the following three cases hold:
     * 1- New epoch is higher
     * 2- New epoch is the same as current epoch, but server id is higher.
     */
    protected boolean supersedesCurrentVote(long newId, long newEpoch) {
        return (newEpoch > this.proposedEpoch) || ((newEpoch == this.proposedEpoch) && (newId > this.proposedLeader));
    }

    /**
     * Termination predicate. Given a set of votes, determines if have sufficient support for the proposal to declare the end of the election round.
     * Who voted for who isn't relevant, we only care that each server has one current vote
     */
    protected boolean haveEnoughVotes(Map<Long, ElectionNotification> votes, Vote proposal) {
        //is the number of votes for the proposal > the size of my peer server’s quorum?
        int count=1;
        //System.out.println( votes);

        for (ElectionNotification leader:votes.values()) {


            if (leader.getProposedLeaderID() ==proposal.getProposedLeaderID()){

                count++;
            }
//
        }
        //System.out.println("count "+ count);
        if (count>myPeerServer.getQuorumSize()){

            return true;
        }
        return false;
    }
}
