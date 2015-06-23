package net.dongliu.vcdiff.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * A stream has seek , length and pos method.(so it is NOT a stream in fact).
 *
 * @author dongliu
 */
public interface RandomAccessStream extends Closeable {

    /**
     * Sets the position for the next read.
     */
    void seek(int pos) throws IOException;

    /**
     * get current pos.
     *
     * @return
     * @throws IOException
     */
    int pos() throws IOException;

    int read(byte[] data, int offset, int length) throws IOException;

    void write(byte[] data, int offset, int length) throws IOException;

    void write(byte[] data) throws IOException;

    void write(byte b) throws IOException;

    /**
     * The data size of this stream
     *
     * @throws IOException
     */
    int length() throws IOException;

    /**
     * get a readonly stream, share data with origin stream.
     * the new data range: [pos, pos+offset).
     * side effect: pos += offset.
     *
     * @return
     * @throws IOException
     */
    RandomAccessStream slice(int length) throws IOException;

    /**
     * If is read only
     *
     * @return
     */
    boolean isReadOnly();

    /**
     * Read one next byte
     *
     * @return -1 if reach the end of stream, otherwise the next byte value in int
     * @throws IOException
     */
    int read() throws IOException;

}
