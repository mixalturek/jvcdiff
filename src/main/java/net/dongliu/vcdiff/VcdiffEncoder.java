package net.dongliu.vcdiff;

import net.dongliu.vcdiff.diff.Pointer;
import net.dongliu.vcdiff.diff.VcdiffEngine;
import net.dongliu.vcdiff.vc.CodeTableWriter;
import net.dongliu.vcdiff.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * vcdiff encoder, based on Bentley-McIlroy 99: "Data Compression Using Long Common Strings.",
 * from open-vcdiff@googlecode.om
 *
 * @author dongliu
 */
public class VcdiffEncoder {

    private final File source;
    private final File target;
    private final File diff;
    /**
     * Determines whether to look for matches within the previously encoded
     * target data, or just within the source (dictionary) data.
     */
    private boolean lookForTargetMatches = true;

    private CodeTableWriter coder;

    private boolean addChecksum;

    private int windowSize = DEFAULT_WINDOW_SIZE;

    private static final int DEFAULT_WINDOW_SIZE = 16 * 1024 * 1024;

    public VcdiffEncoder(File source, File target, File diff) {
        this.source = source;
        this.target = target;
        this.diff = diff;
        coder = new CodeTableWriter();
    }

    private long computeAdler32(byte[] data, int len) {
        //TODO: add check sum
        return 0;
    }

    public void encode() throws IOException {
        byte[] sourceData = IOUtils.readAll(new FileInputStream(source));
        VcdiffEngine engine = new VcdiffEngine(new Pointer(sourceData), sourceData.length);
        engine.init();
        FileInputStream targetStream = new FileInputStream(target);
        FileOutputStream diffStream = new FileOutputStream(diff);

        coder.init(engine.getSourceSize());
        coder.writeHeader(diffStream);

        byte[] window = new byte[windowSize];
        int len;
        while ((len = targetStream.read(window)) > 0) {
            if (addChecksum) {
                coder.addChecksum(computeAdler32(window, len));
            }
            engine.encode(window, len, lookForTargetMatches, diffStream, coder);
        }
        diffStream.flush();
        diffStream.close();
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
}
