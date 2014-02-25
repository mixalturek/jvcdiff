package net.dongliu.vcdiff.utils;

import org.junit.Assert;
import net.dongliu.vcdiff.diff.ByteBuf;
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
        ByteBuf byteBuf = new ByteBuf();
        IOUtils.writeVarIntBE(i, byteBuf);
        Assert.assertEquals(byteBuf.toBytes().length, IOUtils.varIntLen(i));
        InputStream in = new ByteArrayInputStream(byteBuf.toBytes());
        int j = IOUtils.readVarIntBE(in);
        in.close();
        Assert.assertEquals(i, j);

        i = 1001000000;
        byteBuf = new ByteBuf();
        IOUtils.writeVarIntBE(i, byteBuf);
        Assert.assertEquals(byteBuf.toBytes().length, IOUtils.varIntLen(i));
        in = new ByteArrayInputStream(byteBuf.toBytes());
        j = IOUtils.readVarIntBE(in);
        in.close();
        Assert.assertEquals(i, j);

        i = 0;
        byteBuf = new ByteBuf();
        IOUtils.writeVarIntBE(i, byteBuf);
        Assert.assertEquals(byteBuf.toBytes().length, IOUtils.varIntLen(i));
        in = new ByteArrayInputStream(byteBuf.toBytes());
        j = IOUtils.readVarIntBE(in);
        in.close();
        Assert.assertEquals(i, j);
    }
}
