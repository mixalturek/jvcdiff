package net.dongliu.vcdiff.diff;

import net.dongliu.vcdiff.io.ByteVector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author dongliu
 */
public class BytePointerTest {
    static ByteVector byteVector;

    @Before
    public void setUp() throws Exception {
        byteVector = new ByteVector();
    }

    @Test
    public void testPush() throws Exception {
        byteVector.push((byte) 12);
        Assert.assertEquals(1, byteVector.size());
        byteVector.push((byte) 255);
        Assert.assertEquals(2, byteVector.size());
        byteVector.push(new byte[]{1, 2});
        Assert.assertEquals(4, byteVector.size());
    }

    @Test
    public void testGet() throws Exception {
        byteVector.push(new byte[]{1, 2});
        Assert.assertEquals(2, byteVector.get(1));
    }

    @Test
    public void testGetUnsigned() throws Exception {
        byteVector.push(new byte[]{-1, -1});
        Assert.assertEquals(255, byteVector.getUnsigned(0));
        byteVector.push((byte) 255);
        Assert.assertEquals(255, byteVector.getUnsigned(2));
    }

    @Test
    public void testSet() throws Exception {
        byteVector.push(new byte[]{-1, -1});
        Assert.assertEquals(255, byteVector.getUnsigned(0));
        byteVector.set(1, (byte)255);
        Assert.assertEquals(255, byteVector.getUnsigned(1));
    }

    @Test
    public void testClear() throws Exception {
        byteVector.push(new byte[]{-1, -1});
        Assert.assertEquals(255, byteVector.getUnsigned(0));
        byteVector.clear();
        Assert.assertTrue(byteVector.empty());
    }
}
