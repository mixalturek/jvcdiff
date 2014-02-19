package net.dongliu.vcdiff.test;

import net.dongliu.vcdiff.VcdiffDecoder;
import net.dongliu.vcdiff.VcdiffEncoder;
import net.dongliu.vcdiff.exception.VcdiffDecodeException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author dongliu
 */
public class Test {

    private void testEncoding() throws IOException {
        String base = "/Users/dongliu/Downloads/test/";
        VcdiffEncoder encoder = new VcdiffEncoder(
                new File(base + "asm.apk"),
                new File(base + "asm_t.apk"),
                new File(base + "asm.patch"));
        encoder.encode();
    }

    private void testDecoding() throws IOException, VcdiffDecodeException {
        String base = "/Users/dongliu/Downloads/test/";
        VcdiffDecoder.patch(new RandomAccessFile(base + "asm.apk", "r")
                , new File(base + "asm.patch"),
                new RandomAccessFile(base + "asm_t_j.apk", "rw"));
    }

    public static void main(String[] args) throws IOException, VcdiffDecodeException {
        new Test().testEncoding();
        new Test().testDecoding();
    }
}
