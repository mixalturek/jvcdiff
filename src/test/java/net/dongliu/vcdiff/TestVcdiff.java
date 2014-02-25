package net.dongliu.vcdiff;

import net.dongliu.vcdiff.exception.VcdiffDecodeException;
import net.dongliu.vcdiff.exception.VcdiffEncodeException;
import net.dongliu.vcdiff.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author dongliu
 */
public class TestVcdiff {

    @Test
    public void test() throws IOException, VcdiffDecodeException, VcdiffEncodeException {
        String path = (new File("")).getAbsolutePath();
        String base = path + "/src/test/file/";
        String sourceFilePath = base + "asm.apk";
        String targetFilePath = base + "asm_t.apk";
        String patchFilePath = base + "asm.patch";
        String patchedTargetFilePath = base + "asm_j.apk";
        VcdiffEncoder.encode(new File(sourceFilePath), new File(targetFilePath), new File(patchFilePath));
        VcdiffDecoder.decode(new File(sourceFilePath), new File(patchFilePath),
                new File(patchedTargetFilePath));

        byte[] bytes = IOUtils.readAll(new FileInputStream(targetFilePath));
        byte[] generatedBytes = IOUtils.readAll(new FileInputStream(patchedTargetFilePath));
        Assert.assertArrayEquals(bytes, generatedBytes);

        new File(patchFilePath).delete();
        new File(patchedTargetFilePath).delete();
    }
}
