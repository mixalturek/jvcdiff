package net.dongliu.vcdiff;

import net.dongliu.vcdiff.diff.Pointer;
import net.dongliu.vcdiff.diff.VcdiffEngine;
import net.dongliu.vcdiff.exception.VcdiffEncodeException;
import net.dongliu.vcdiff.utils.IOUtils;
import net.dongliu.vcdiff.vc.CodeTableWriter;

import java.io.*;

/**
 * vcdiff encoder, based on Bentley-McIlroy 99: "Data Compression Using Long Common Strings.",
 * from open-vcdiff@googlecode.om
 *
 * @author dongliu
 */
public class VcdiffEncoder {

    private final InputStream source;
    private final InputStream target;
    private final OutputStream diff;
    /**
     * Determines whether to look for matches within the previously encoded
     * target data, or just within the source (dictionary) data.
     */
    private boolean lookForTargetMatches = true;

    private CodeTableWriter coder;

    private boolean addChecksum;

    private int windowSize = DEFAULT_WINDOW_SIZE;

    private static final int DEFAULT_WINDOW_SIZE = 16 * 1024 * 1024;

    /**
     * Constructor. The caller is responsible for close of the passed streams.
     *
     * @param source older data
     * @param target newer data
     * @param diff   diff between older and newer data (output)
     */
    public VcdiffEncoder(InputStream source, InputStream target, OutputStream diff) {
        this.source = source;
        this.target = target;
        this.diff = diff;
        coder = new CodeTableWriter();
    }

    /**
     * Convenient method for encode decode file, use default setting and code tables.
     *
     * @param sourceFile older file
     * @param targetFile newer file
     * @param patchFile  diff between older and newer file (output)
     */
    public static void encode(File sourceFile, File targetFile, File patchFile)
            throws IOException, VcdiffEncodeException {
        try (FileInputStream sourceStream = new FileInputStream(sourceFile);
             FileInputStream targetStream = new FileInputStream(targetFile);
             FileOutputStream patchStream = new FileOutputStream(patchFile)
        ) {
            VcdiffEncoder encoder = new VcdiffEncoder(sourceStream, targetStream, patchStream);
            encoder.encode();
        }
    }

    public void encode() throws IOException, VcdiffEncodeException {
        byte[] sourceData = IOUtils.readAll(source);
        VcdiffEngine engine = new VcdiffEngine(new Pointer(sourceData), sourceData.length);
        engine.init();

        coder.init(engine.getSourceSize());
        coder.writeHeader(diff);

        byte[] window = new byte[windowSize];
        int len;
        while ((len = target.read(window)) > 0) {
            if (addChecksum) {
                coder.addChecksum(computeAdler32(window, len));
            }
            engine.encode(window, len, lookForTargetMatches, diff, coder);
        }
        diff.flush();
    }

    public void setAddChecksum(boolean addChecksum) {
        this.addChecksum = addChecksum;
    }

    public void setLookForTargetMatches(boolean lookForTargetMatches) {
        this.lookForTargetMatches = lookForTargetMatches;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    private long computeAdler32(byte[] data, int len) {
        //TODO: add check sum
        return 0;
    }
}
