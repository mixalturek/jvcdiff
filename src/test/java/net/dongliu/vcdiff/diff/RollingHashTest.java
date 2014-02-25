package net.dongliu.vcdiff.diff;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author dongliu
 */

public class RollingHashTest {

    private static RollingHash rollingHash;

    @BeforeClass
    public static void init() {
        rollingHash = new RollingHash(32);
    }

    @Test
    public void testHash() throws Exception {
        String s = "1234567890asdfghjklpqwertyuio,zx";
        Pointer pointer = new Pointer(s.getBytes());
        int hash = rollingHash.hash(pointer);
        Assert.assertTrue(hash >= 0);
    }

    @Test
    public void testFindModBaseInverse() {
        int result = rollingHash.findModBaseInverse(10);
        Assert.assertTrue(result < RollingHash.HASH_BASE);
        Assert.assertEquals(0, (result + 10) % RollingHash.HASH_BASE);
    }

    @Test
    public void testUpdateHash() {
        String s = "1234567890asdfghjklpqwertyuio,zxt";
        byte[] data = s.getBytes();
        Pointer pointer = new Pointer(s.getBytes());
        int hash = rollingHash.hash(pointer);
        hash = rollingHash.updateHash(hash, data[0], data[data.length - 1]);
        Pointer pointer2 = pointer.slice(1);
        int hash2 = rollingHash.hash(pointer2);
        Assert.assertEquals(hash2, hash);
    }
}
