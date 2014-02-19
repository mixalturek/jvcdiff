package net.dongliu.vcdiff.utils;

import net.dongliu.vcdiff.io.ByteBufferSeekableStream;
import net.dongliu.vcdiff.io.SeekableStream;
import net.dongliu.vcdiff.diff.ByteBuf;

import java.io.*;

/**
 * IOUtils form vcdiff.
 *
 * @author dongliu
 */
public class IOUtils {

    /**
     * read N bytes from input stream.
     * throw exception when not enough data in is.
     *
     * @param is
     * @param size
     * @throws IOException
     */
    public static byte[] readBytes(InputStream is, int size) throws IOException {
        byte[] data = new byte[size];
        int offset = 0;
        while (offset < size) {
            int readSize = is.read(data, offset, size - offset);
            if (readSize < 0) {
                // end of is
                throw new IndexOutOfBoundsException("Not enough data in inputStream.");
            }
            offset += readSize;
        }
        return data;
    }

    /**
     * read one byte from input stream.
     *
     * @return
     * @throws IOException
     */
    public static int readByte(SeekableStream ss) throws IOException {
        int b = ss.read();
        if (b == -1) {
            // end of is
            throw new IndexOutOfBoundsException("Not enough data in inputStream.");
        }
        return b;
    }

    /**
     * read 7 bit encoded int.by big endian.
     *
     * @return
     * @throws IOException
     */
    public static int readVarIntBE(SeekableStream ss) throws IOException {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            int b = ss.read();
            if (b == -1) {
                throw new IndexOutOfBoundsException("Not enough data in inputStream.");
            }
            ret = (ret << 7) | (b & 0x7f);
            // end of int encoded.
            if ((b & 0x80) == 0) {
                return ret;
            }
        }
        // Still haven't seen a byte with the high bit unset? Dodgy data.
        throw new IOException("Invalid 7-bit encoded integer in stream.");
    }

    /**
     * read 7 bit encoded int.by big endian.
     *
     * @return
     * @throws IOException
     */
    public static int readVarIntBE(InputStream is) throws IOException {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            int b = is.read();
            if (b == -1) {
                throw new IndexOutOfBoundsException("Not enough data in inputStream.");
            }
            ret = (ret << 7) | (b & 0x7f);
            // end of int encoded.
            if ((b & 0x80) == 0) {
                return ret;
            }
        }
        // Still haven't seen a byte with the high bit unset? Dodgy data.
        throw new IOException("Invalid 7-bit encoded integer in stream.");
    }

    /**
     * @param source
     * @param size
     * @return
     * @throws IOException
     */
    public static byte[] readBytes(SeekableStream source, int size) throws IOException {
        byte[] data = new byte[size];
        int offset = 0;
        while (offset < size) {
            int readSize = source.read(data, offset, size - offset);
            if (readSize < 0) {
                // end of is
                throw new IndexOutOfBoundsException(
                        "Not enough data in inputStream, require:" + (size - offset));
            }
            offset += readSize;
        }
        return data;
    }

    /**
     * 从stream中获得一个指定大小为length，从当前pos处开始的stream.
     * 副作用：ss的position会增加length.
     *
     * @param ss
     * @return
     * @throws IOException
     */
    public static SeekableStream getStreamView(SeekableStream ss, int length, boolean shareData)
            throws IOException {
        if (shareData) {
            return ss.slice(length);
        } else {
            byte[] bytes = readBytes(ss, length);
            return new ByteBufferSeekableStream(bytes, true);
        }
    }

    /**
     * close quietly.
     *
     * @param closeable
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignore) {
        }
    }

    public static void copy(SeekableStream sourceStream, SeekableStream targetDataStream, int size)
            throws IOException {
        byte[] bytes = readBytes(sourceStream, size);
        targetDataStream.write(bytes, 0, bytes.length);
    }

    /**
     * bytes to int.
     *
     * @return
     */
    public static int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff)));
    }

    /**
     * bytes to short.
     *
     * @return
     */
    public static short makeShort(byte b1, byte b0) {
        return (short) (((b1 & 0xff) << 8) | ((b0 & 0xff)));
    }

    /**
     * read int, Big-endian.
     *
     * @return
     */
    public static int makeIntB(byte[] ba, int pos) {
        if (ba == null || ba.length < 4 + pos) {
            throw new IllegalArgumentException("Need at lease four bytes.");
        }
        return makeInt(ba[pos], ba[pos + 1], ba[pos + 2], ba[pos + 3]);
    }

    /**
     * read int, Small-endian.
     *
     * @return
     */
    public static int makeIntS(byte[] ba, int pos) {
        if (ba == null || ba.length < 4 + pos) {
            throw new IllegalArgumentException("Need at lease four bytes.");
        }
        return makeInt(ba[pos + 3], ba[pos + 2], ba[pos + 1], ba[pos]);
    }


    public static short makeShortS(byte[] ba, int pos) {
        if (ba == null || ba.length < 2 + pos) {
            throw new IllegalArgumentException("Need at lease two bytes.");
        }
        return makeShort(ba[pos + 1], ba[pos]);
    }

    public static boolean ArrayEqual(byte[] a, byte[] b, int size) {
        for (int i = 0; i < size; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * write int as var-len int
     */
    public static void writeVarIntBE(ByteBuf byteBuf, int i) {
        boolean flag = false;
        int shift = 4 * 7;
        for (int idx = 4; idx >= 0; idx--, shift = shift - 7) {
            int j = i >>> shift;
            byte b = (byte) (j & 0x7f);
            if (b != 0) {
                if (idx != 0) {
                    b = (byte) (b ^ 0x80);
                }
                byteBuf.push(b);
                flag = true;
            } else if (flag) {
                if (idx != 0) {
                    b = (byte) 0x80;
                }
                byteBuf.push(b);
            }
        }
        if (!flag) {
            // zero
            byteBuf.push((byte)0);
        }
    }

    public static int varIntLen(int i) {
        boolean flag = false;
        int shift = 4 * 7;
        for (int idx = 4; idx >= 0; idx--, shift = shift - 7) {
            int j = i >>> shift;
            byte b = (byte) (j & 0x7f);
            if (b != 0) {
                return idx + 1;
            }
        }
        return 1;
    }

    /**
     * write int as var-len int
     */
    public static void writeVarIntBE(int i, OutputStream out) throws IOException {
        boolean flag = false;
        int shift = 4 * 7;
        for (int idx = 4; idx >= 0; idx--, shift = shift - 7) {
            int j = i >>> shift;
            int b = j & 0x7f;
            if (b != 0) {
                if (idx != 0) {
                    b = b ^ 0x80;
                }
                out.write(b);
                flag = true;
            } else if (flag) {
                if (idx != 0) {
                    b = 0x80;
                }
                out.write(b);
            }
        }
        if (!flag) {
            // zero
            out.write(0);
        }
    }

    public static int varLongLength(long checksum_) {
        //TODO: to be implemented
        throw new UnsupportedOperationException();
    }

    public static void writeVarLongBE(OutputStream out, long checksum_) {
        //TODO: to be implemented
        throw new UnsupportedOperationException();
    }

    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        out.flush();
        return out.toByteArray();
    }
}
