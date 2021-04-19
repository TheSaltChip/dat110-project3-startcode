/**
 *
 */
package no.hvl.dat110.middleware;

import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.LamportClock;
import no.hvl.dat110.util.Util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author tdoy
 *
 */
public class MutualExclusion {

    /** lock variables */
    private boolean CS_BUSY = false;                        // indicate to be in critical section (accessing a shared resource)
    private boolean WANTS_TO_ENTER_CS = false;                // indicate to want to enter CS
    private final List<Message> queueack;                        // queue for acknowledged messages
    private final List<Message> mutexqueue;                        // queue for storing process that are denied permission. We really don't need this for quorum-protocol

    private final LamportClock clock;                                // lamport clock
    private final Node node;

    public MutualExclusion(Node node) throws RemoteException {
        this.node = node;

        clock = new LamportClock();
        queueack = new ArrayList<Message>();
        mutexqueue = new ArrayList<Message>();
    }

    public void acquireLock() {
        CS_BUSY = true;
    }

    public void releaseLocks() {
        WANTS_TO_ENTER_CS = false;
        CS_BUSY = false;
    }

    public boolean doMutexRequest(Message message, byte[] updates) throws RemoteException {

        System.out.println(node.nodename + " wants to access CS");

        // clear the queueack before requesting for votes

        // clear the mutexqueue

        // increment clock

        // adjust the clock on the message, by calling the setClock on the message

        // wants to access resource - set the appropriate lock variable

        queueack.clear();
        mutexqueue.clear();

        clock.increment();
        message.setClock(clock.getClock());

        WANTS_TO_ENTER_CS = true;

        // start MutualExclusion algorithm

        // first, removeDuplicatePeersBeforeVoting. A peer can contain 2 replicas of a file. This peer will appear twice

        // multicast the message to activenodes (hint: use multicastMessage)

        // check that all replicas have replied (permission)

        // if yes, acquireLock

        // node.broadcastUpdatetoPeers

        // clear the mutexqueue

        // return permission

        List<Message> nonDup = removeDuplicatePeersBeforeVoting();

        multicastMessage(message, nonDup);

        if (areAllMessagesReturned(nonDup.size())) {
            acquireLock();
            node.broadcastUpdatetoPeers(updates);
            mutexqueue.clear();
            return true;
        }
        return false;
    }

    // multicast message to other processes including self
    private void multicastMessage(Message message, List<Message> activenodes) throws RemoteException {

        // iterate over the activenodes

        // obtain a stub for each node from the registry

        // call onMutexRequestReceived()

        Collections.shuffle(activenodes);

        for (Message m : activenodes) {
            Util.getProcessStub(m.getNodeIP(), m.getPort()).onMutexRequestReceived(message);
        }

    }

    public void onMutexRequestReceived(Message message) throws RemoteException {

        // increment the local clock
        clock.increment();
        // if message is from self, acknowledge, and call onMutexAcknowledgementReceived()

        if (message.getNodeID().equals(node.getNodeID())) {
            message.setAcknowledged(true);
            onMutexAcknowledgementReceived(message);
            return;
        }

        int caseid = -1;

        // write if statement to transition to the correct caseid
        // caseid=0: Receiver is not accessing shared resource and does not want to (send OK to sender)
        // caseid=1: Receiver already has access to the resource (dont reply but queue the request)
        // caseid=2: Receiver wants to access resource but is yet to - compare own message clock to received message's clock

        if (!WANTS_TO_ENTER_CS && !CS_BUSY) caseid = 0;
        else if (CS_BUSY) caseid = 1;
        else caseid = 2;

        // check for decision
        doDecisionAlgorithm(message, mutexqueue, caseid);
    }

    public void doDecisionAlgorithm(Message message, List<Message> queue, int condition) throws RemoteException {

        String procName = message.getNodeIP();            // this is the same as nodeName in the Node class
        int port = message.getPort();                    // port on which the registry for this stub is listening

        switch (condition) {

            /* case 1: Receiver is not accessing shared resource and does not want to (send OK to sender) */
            case 0: {
                // get a stub for the sender from the registry
                // acknowledge message
                // send acknowledgement back by calling onMutexAcknowledgementReceived()

                NodeInterface stub = Util.getProcessStub(procName, port);
                message.setAcknowledged(true);
                stub.onMutexAcknowledgementReceived(message);
                break;
            }

            /* case 2: Receiver already has access to the resource (dont reply but queue the request) */
            case 1: {
                // queue this message
                queue.add(message);
                break;
            }

            /*
             *  case 3: Receiver wants to access resource but is yet to (compare own message clock to received message's clock
             *  the message with lower timestamp wins) - send OK if received is lower. Queue message if received is higher
             */
            case 2: {
                // check the clock of the sending process
                // own clock for the multicast message
                // compare clocks, the lowest wins
                // if clocks are the same, compare nodeIDs, the lowest wins
                // if sender wins, acknowledge the message, obtain a stub and call onMutexAcknowledgementReceived()
                // if sender looses, queue it
                if (node.getMessage().getClock() < message.getClock()
                        || (node.getMessage().getClock() == message.getClock()
                        && node.getNodeID().compareTo(message.getNodeID()) < 0)) {
                    queue.add(message);
                    break;
                }

                message.setAcknowledged(true);
                Util.getProcessStub(message.getNodeIP(), message.getPort()).onMutexAcknowledgementReceived(message);

                break;
            }

            default:
                break;
        }

    }

    public void onMutexAcknowledgementReceived(Message message) throws RemoteException {

        // add message to queueack
        queueack.add(message);

    }

    // multicast release locks message to other processes including self
    public void multicastReleaseLocks(Set<Message> activenodes) throws RemoteException {

        // iterate over the activenodes

        // obtain a stub for each node from the registry

        // call releaseLocks()

        for (Message m :
                activenodes) {
            NodeInterface stub = Util.getProcessStub(m.getNodeIP(), m.getPort());
            stub.releaseLocks();
        }

    }

    private boolean areAllMessagesReturned(int numvoters) {
        // check if the size of the queueack is same as the numvoters

        // clear the queueack

        // return true if yes and false if no

        boolean cond = queueack.stream().filter(Message::isAcknowledged).count() == numvoters;
        queueack.clear();

        return cond;
    }

    private List<Message> removeDuplicatePeersBeforeVoting() {

        List<Message> uniquepeer = new ArrayList<>();
        for (Message p : node.activenodesforfile) {
            boolean found = false;
            for (Message p1 : uniquepeer) {
                if (p.getNodeIP().equals(p1.getNodeIP())) {
                    found = true;
                    break;
                }
            }
            if (!found)
                uniquepeer.add(p);
        }
        return uniquepeer;
    }
}
