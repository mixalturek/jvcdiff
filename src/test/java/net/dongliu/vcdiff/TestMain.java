package net.dongliu.vcdiff;

import net.dongliu.vcdiff.VcdiffDecoder;
import net.dongliu.vcdiff.VcdiffEncoder;
import net.dongliu.vcdiff.exception.VcdiffDecodeException;
import net.dongliu.vcdiff.exception.VcdiffEncodeException;
import net.dongliu.vcdiff.io.ByteArrayStream;
import net.dongliu.vcdiff.io.FileStream;

import java.io.*;

/**
 * @author dongliu
 */
public class TestMain {

    public static void main(String[] args) throws IOException, VcdiffDecodeException, VcdiffEncodeException {
        VcdiffEncoder.encode(new File("/Users/dongliu/Documents/apks/qq.apk"),
                new File("/Users/dongliu/Documents/apks/qq2.apk"),
                new File("/Users/dongliu/Documents/apks/qq.patch"));
        ByteArrayStream bas = new ByteArrayStream();
        VcdiffDecoder.decode(new FileStream(new RandomAccessFile("/Users/dongliu/Documents/apks/qq.apk", "rw")),
                new FileInputStream("/Users/dongliu/Documents/apks/qq.patch"),
                bas);
        FileOutputStream fos = new FileOutputStream("/Users/dongliu/Documents/apks/qq2b.apk");
        fos.write(bas.toBytes());
        fos.close();
    }
}
