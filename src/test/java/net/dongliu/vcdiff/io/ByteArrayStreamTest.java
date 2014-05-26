package net.dongliu.vcdiff.io;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class ByteArrayStreamTest {

    @Test
    public void testWrite() throws Exception {

        ByteArrayStream stream = new ByteArrayStream(2);
        for (int i = 0; i < 4; i++) {
            stream.write((byte) -1);
        }
        Assert.assertArrayEquals(new byte[]{-1, -1, -1, -1}, stream.toBytes());

        stream.seek(2);
        for (int i = 0; i < 3; i++) {
            stream.write((byte) 1);
        }
        Assert.assertArrayEquals(new byte[]{-1, -1, 1, 1, 1}, stream.toBytes());

        byte[] data = new byte[]{0, 1, 3, 5, 8, 10, 12, 34, 12, 54, 12, -4, 6, 8};
        stream.seek(1);
        stream.write(data);
        Assert.assertArrayEquals(new byte[]{-1, 0, 1, 3, 5, 8, 10, 12, 34, 12, 54, 12, -4, 6, 8},
                stream.toBytes());
    }
}