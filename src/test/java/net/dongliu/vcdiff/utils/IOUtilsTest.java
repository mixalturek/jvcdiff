package net.dongliu.vcdiff.utils;

import net.dongliu.vcdiff.io.ByteVector;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
}
