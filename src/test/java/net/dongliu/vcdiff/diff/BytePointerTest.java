package net.dongliu.vcdiff.diff;

import net.dongliu.vcdiff.io.ByteBuf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author dongliu
 */
public class BytePointerTest {
    static ByteBuf byteBuf;

    @Before
    public void setUp() throws Exception {
        byteBuf = new ByteBuf();
    }

    @Test
    public void testPush() throws Exception {
        byteBuf.push((byte) 12);
        Assert.assertEquals(1, byteBuf.size());
        byteBuf.push((short) 255);
        Assert.assertEquals(2, byteBuf.size());
        byteBuf.push(new byte[]{1, 2});
        Assert.assertEquals(4, byteBuf.size());
    }

    @Test
    public void testUnPush() throws Exception {
        byteBuf.push((byte) 12);
        Assert.assertEquals(1, byteBuf.size());
        byteBuf.unPush(1);
        Assert.assertEquals(0, byteBuf.size());
        byteBuf.unPush(1);
        Assert.assertEquals(0, byteBuf.size());
    }

    @Test
    public void testGet() throws Exception {
        byteBuf.push(new byte[]{1, 2});
        Assert.assertEquals(2, byteBuf.get(1));
    }

    @Test
    public void testGetUnsigned() throws Exception {
        byteBuf.push(new byte[]{-1, -1});
        Assert.assertEquals(255, byteBuf.getUnsigned(0));
        byteBuf.push((short) 255);
        Assert.assertEquals(255, byteBuf.getUnsigned(2));
    }

    @Test
    public void testSet() throws Exception {
        byteBuf.push(new byte[]{-1, -1});
        Assert.assertEquals(255, byteBuf.getUnsigned(0));
        byteBuf.set(1, (byte)255);
        Assert.assertEquals(255, byteBuf.getUnsigned(1));
    }

    @Test
    public void testClear() throws Exception {
        byteBuf.push(new byte[]{-1, -1});
        Assert.assertEquals(255, byteBuf.getUnsigned(0));
        byteBuf.clear();
        Assert.assertTrue(byteBuf.empty());
    }
}
