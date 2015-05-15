package net.dongliu.vcdiff.utils;

import net.dongliu.vcdiff.io.ByteVector;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * @author dongliu
 */
public class IOUtilsTest {

    @Test
    public void testVarInt() throws IOException {
        int i = 100;
        ByteVector byteVector = new ByteVector();
        IOUtils.writeVarIntBE(i, byteVector);
        Assert.assertEquals(byteVector.toBytes().length, IOUtils.varIntLen(i));
        InputStream in = new ByteArrayInputStream(byteVector.toBytes());
        int j = IOUtils.readVarIntBE(in);
        in.close();
        Assert.assertEquals(i, j);

        i = 1001000000;
        byteVector = new ByteVector();
        IOUtils.writeVarIntBE(i, byteVector);
        Assert.assertEquals(byteVector.toBytes().length, IOUtils.varIntLen(i));
        in = new ByteArrayInputStream(byteVector.toBytes());
        j = IOUtils.readVarIntBE(in);
        in.close();
        Assert.assertEquals(i, j);

        i = 0;
        byteVector = new ByteVector();
        IOUtils.writeVarIntBE(i, byteVector);
        Assert.assertEquals(byteVector.toBytes().length, IOUtils.varIntLen(i));
        in = new ByteArrayInputStream(byteVector.toBytes());
        j = IOUtils.readVarIntBE(in);
        in.close();
        Assert.assertEquals(i, j);
    }

    @Test
    public void testReadAll_ShortData() throws Exception {
        byte[] expected = {0x00, 0x01, 0x02, 0x03, 0x04};

        try(ByteArrayInputStream stream = new ByteArrayInputStream(expected)) {
            byte[] actual = IOUtils.readAll(stream);
            Assert.assertArrayEquals(actual, expected);
        }
    }

    @Test
    public void testReadAll_LongData() throws Exception {
        byte[] expected = new byte[1024 * 1024];
        new Random().nextBytes(expected);

        try(ByteArrayInputStream stream = new ByteArrayInputStream(expected)) {
            byte[] actual = IOUtils.readAll(stream);
            Assert.assertArrayEquals(actual, expected);
        }
    }
}
