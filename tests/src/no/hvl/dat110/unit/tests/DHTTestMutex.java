package no.hvl.dat110.unit.tests;


import no.hvl.dat110.middleware.Message;
import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.FileManager;
import no.hvl.dat110.util.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class DHTTestMutex {

    @BeforeEach
    void setUp() throws Exception {


    }

    @Test
    void test() throws InterruptedException, RemoteException {

        // retrieve the processes stubs
        NodeInterface p1 = Util.getProcessStub("process1", 9091);
        NodeInterface p2 = Util.getProcessStub("process2", 9092);
        NodeInterface p3 = Util.getProcessStub("process3", 9093);
        NodeInterface p4 = Util.getProcessStub("process4", 9094);
        NodeInterface p5 = Util.getProcessStub("process5", 9095);

        FileManager fm = new FileManager(p3, Util.numReplicas);        // we are using p3
        String filename = "file1";
        String newupdate = "overwrite the content of existing file - i.e. file1";

        Set<Message> activepeers = fm.requestActiveNodesForFile(filename);

        activepeers.forEach(m -> System.out.println(m.getNodeIP() + " " + m.getNodeID()));// p1, p2, p4, and p5 are holding file1

        p3.multicastReleaseLocks(activepeers);        // reset all locks before voting for mutual exclusion

        // request to update the same file will happen concurrently - only one process should get the vote
        FileUpdater fu1 = new FileUpdater(p1, getPeerMessage(activepeers, p1.getNodeName()), newupdate.getBytes(), activepeers);
        FileUpdater fu2 = new FileUpdater(p2, getPeerMessage(activepeers, p2.getNodeName()), newupdate.getBytes(), activepeers);
        FileUpdater fu4 = new FileUpdater(p4, getPeerMessage(activepeers, p4.getNodeName()), newupdate.getBytes(), activepeers);
        FileUpdater fu5 = new FileUpdater(p5, getPeerMessage(activepeers, p5.getNodeName()), newupdate.getBytes(), activepeers);


        fu1.start();
        fu2.start();
        fu4.start();
        fu5.start();

        fu1.join();
        fu2.join();
        fu4.join();
        fu5.join();

        System.out.println("fu1: " + fu1.getReply());
        System.out.println("fu2: " + fu2.getReply());
        System.out.println("fu4: " + fu4.getReply());
        System.out.println("fu5: " + fu5.getReply());

        List<Boolean> replies = new ArrayList<>();
        replies.add(fu1.getReply());
        replies.add(fu2.getReply());
        replies.add(fu4.getReply());
        replies.add(fu5.getReply());

        Boolean[] expected = {true, false, false, false};
        List<Boolean> e = Arrays.asList(expected);
        Collections.sort(replies);
        Collections.sort(e);

        System.out.println(replies + "|" + e);

        assertArrayEquals(e.toArray(), replies.toArray());
    }

    static class FileUpdater extends Thread {

        boolean reply;
        NodeInterface node;
        Message peer;
        byte[] updates;
        Set<Message> activepeers;

        FileUpdater(NodeInterface node, Message peer, byte[] updates, Set<Message> activepeers) throws RemoteException {
            this.node = node;
            this.peer = peer;
            this.updates = updates;
            this.activepeers = activepeers;
        }

        public void run() {
            try {
                reply = node.requestMutexWriteOperation(peer, updates, activepeers);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public boolean getReply() {

            return reply;
        }
    }

    private Message getPeerMessage(Set<Message> activenodes, String peer) {

        for (Message n : activenodes) {
            if (n.getNodeIP().equals(peer))
                return n;
        }

        return null;
    }

}
