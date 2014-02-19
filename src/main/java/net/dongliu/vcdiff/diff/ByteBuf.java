package net.dongliu.vcdiff.diff;

import java.nio.BufferOverflowException;

/**
 * @author dongliu
 */
public class ByteBuf {
    private byte[] data;
    // the data size current in use.
    private int size;
    private static final int INIT_SIZE = 32;

    public ByteBuf() {
        this.data = new byte[INIT_SIZE];
    }

    public int size() {
        return size;
    }

    public ByteBuf size(int size) {
        if (size > this.data.length) {
            expand(size);
        }
        this.size = size;
        return this;
    }

    public ByteBuf push(byte b) {
        if (size + 1 > data.length) {
            expand(size + 1);
        }
        data[size++] = b;
        return this;
    }

    public ByteBuf push(byte[] bytes) {
        push(bytes, 0, bytes.length);
        return this;
    }

    public ByteBuf push(byte[] bytes, int offset, int len) {
        if (size + len > data.length) {
            expand(size + len);
        }
        System.arraycopy(bytes, offset, data, size, len);
        size += len;
        return this;
    }

    private void expand(int targetSize) {
        int newSize = Math.max(data.length * 2, targetSize);
        byte[] newData = new byte[newSize];
        System.arraycopy(data, 0, newData, 0, size);
        this.data = newData;
    }

    public ByteBuf unPush() {
        if (size >= 1) {
            size--;
        }
        return this;
    }

    public ByteBuf unPush(int len) {
        if (size >= len) {
            size -= len;
        }
        return this;
    }

    public byte get(int i) {
        if (i >= size) {
            throw new BufferOverflowException();
        }
        return this.data[i];
    }

    public short getUnsigned(int i) {
        if (i >= size) {
            throw new BufferOverflowException();
        }
        return (short) (this.data[i] & 0xFF);
    }

    public ByteBuf set(int i, byte b) {
        if (i >= size) {
            throw new BufferOverflowException();
        }
        this.data[i] = b;
        return this;
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[size];
        System.arraycopy(data, 0, bytes, 0, size);
        return bytes;
    }

    public ByteBuf set(int i, short s) {
        set(i, (byte) s);
        return this;
    }

    public ByteBuf push(short opcode) {
        push((byte) opcode);
        return this;
    }

    public ByteBuf push(Pointer data, int size) {
        push(data.getData(), data.offset(), size);
        return this;
    }

    public boolean empty() {
        return this.size == 0;
    }

    public byte[] data() {
        return this.data;
    }

    public ByteBuf clear() {
        this.size = 0;
        return this;
    }
}
