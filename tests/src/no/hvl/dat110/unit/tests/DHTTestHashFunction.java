package no.hvl.dat110.unit.tests;


import no.hvl.dat110.util.Hash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DHTTestHashFunction {

    @BeforeEach
    void setUp() throws Exception {


    }

    @Test
    void test() throws InterruptedException, RemoteException {

        // actual
        BigInteger hash1 = Hash.hashOf("process1");
        BigInteger hash2 = Hash.hashOf("process2");
        BigInteger hash3 = Hash.hashOf("process3");
        BigInteger hash4 = Hash.hashOf("process4");
        BigInteger hash5 = Hash.hashOf("process5");

        System.out.println(
                "hash1           " + hash1 + "\n"
                        + "hash2           " + hash2 + "\n"
                        + "hash3           " + hash3 + "\n"
                        + "hash4           " + hash4 + "\n"
                        + "hash5           " + hash5 + "\n"
        + "Address size: = " + Hash.addressSize());

        // expected
        BigInteger hash1expected = new BigInteger("53937554629190552131995290006614509577");
        BigInteger hash2expected = new BigInteger("15618062003214643351512781541041391612");
        BigInteger hash3expected = new BigInteger("66910184482037901621933403444034052414");
        BigInteger hash4expected = new BigInteger("210821560651360572675896360671414673172");
        BigInteger hash5expected = new BigInteger("121411138451101288395601026024677976156");

        assertEquals(hash1, hash1expected);
        assertEquals(hash2, hash2expected);
        assertEquals(hash3, hash3expected);
        assertEquals(hash4, hash4expected);
        assertEquals(hash5, hash5expected);

    }

    @Test
    void test1() {
        Hash.hashOf("process4");
        assertEquals(BigInteger.valueOf(2L).pow(128), Hash.addressSize());
    }

}
