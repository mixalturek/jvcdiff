package net.dongliu.vcdiff;

import net.dongliu.vcdiff.exception.VcdiffDecodeException;
import net.dongliu.vcdiff.exception.VcdiffEncodeException;
import net.dongliu.vcdiff.io.FixedByteArrayStream;
import net.dongliu.vcdiff.io.RandomAccessStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

public class EncoderDecoderTest {
    /**
     * Diff two byte arrays.
     *
     * @param source source byte array
     * @param target modified source array
     * @return diff
     * @throws IOException           if something fails
     * @throws VcdiffEncodeException if something fails
     */
    private byte[] diff(byte[] source, byte[] target) throws IOException, VcdiffEncodeException {
        try (ByteArrayInputStream sourceStream = new ByteArrayInputStream(source);
             ByteArrayInputStream targetStream = new ByteArrayInputStream(target);
             ByteArrayOutputStream diffStream = new ByteArrayOutputStream()) {

            new VcdiffEncoder(sourceStream, targetStream, diffStream).encode();
            return diffStream.toByteArray();
        }
    }

    /**
     * Apply patch to the byte array.
     *
     * @param source             source byte array
     * @param patch              patch to be applied
     * @param expectedResultSize expected size of the result
     * @return patched source
     * @throws IOException           if something fails
     * @throws VcdiffDecodeException if something fails
     */
    private byte[] applyPatch(byte[] source, byte[] patch, int expectedResultSize)
            throws IOException, VcdiffDecodeException {
        ByteBuffer outputBuffer = ByteBuffer.allocate(expectedResultSize);

        try (RandomAccessStream sourceStream = new FixedByteArrayStream(source, true);
             ByteArrayInputStream patchStream = new ByteArrayInputStream(patch);
             RandomAccessStream targetStream = new FixedByteArrayStream(outputBuffer)) {

            VcdiffDecoder.decode(sourceStream, patchStream, targetStream);

            outputBuffer.rewind();
            byte[] result = new byte[outputBuffer.remaining()];
            outputBuffer.get(result);
            return result;
        }
    }

    /**
     * Fill random data.
     *
     * @param array array to be initialized
     * @return source array
     */
    private byte[] fill(byte[] array) {
        new Random().nextBytes(array);
        return array;
    }

    /**
     * Modify array with random data.
     *
     * @param array        array to modify
     * @param blocks       number of blocks to be modified
     * @param maxBlockSize maximal number of bytes in each block to modify
     * @return source array
     */
    private byte[] modify(byte[] array, int blocks, int maxBlockSize) {
        Random random = new Random();

        for (int i = 0; i < blocks; ++i) {
            int bytes = random.nextInt(maxBlockSize);
            int blockPtr = random.nextInt(array.length - bytes);

            for (int j = 0; j < bytes; ++j) {
                array[blockPtr + j] = (byte) random.nextInt(256);
            }
        }

        return array;
    }

    @Test
    public void testEncodeDecode_RealWordTest() throws Exception {
        byte[] source = fill(new byte[10 * 1024 * 1024]);
        byte[] target = modify(Arrays.copyOf(source, source.length), 100, 1000);
        byte[] patch = diff(source, target);
        byte[] patchedSource = applyPatch(source, patch, target.length);

        assertArrayEquals(target, patchedSource);
    }

    @Test
    public void testEncodeDecode_SameSourceAndTarget() throws Exception {
        byte[] source = fill(new byte[10 * 1024 * 1024]);
        byte[] patch = diff(source, source);
        byte[] patchedSource = applyPatch(source, patch, source.length);

        assertArrayEquals(source, patchedSource);
    }

    @Test
    public void testEncodeDecode_EmptySourceAndTarget() throws Exception {
        byte[] source = {};
        byte[] target = {};
        byte[] patch = diff(source, target);
        byte[] patchedSource = applyPatch(source, patch, target.length);

        assertArrayEquals(target, patchedSource);
    }

    @Test
    public void testEncodeDecode_EmptySource() throws Exception {
        byte[] source = {};
        byte[] target = fill(new byte[10 * 1024 * 1024]);
        byte[] patch = diff(source, target);
        byte[] patchedSource = applyPatch(source, patch, target.length);

        assertArrayEquals(target, patchedSource);
    }

    @Test
    public void testEncodeDecode_EmptyTarget() throws Exception {
        byte[] source = fill(new byte[10 * 1024 * 1024]);
        byte[] target = {};
        byte[] patch = diff(source, target);
        byte[] patchedSource = applyPatch(source, patch, target.length);

        assertArrayEquals(target, patchedSource);
    }

    @Test
    public void testEncodeDecode_FirstByteChanged() throws Exception {
        byte[] source = fill(new byte[10 * 1024 * 1024]);
        byte[] target = Arrays.copyOf(source, source.length);
        ++target[0];
        byte[] patch = diff(source, target);
        byte[] patchedSource = applyPatch(source, patch, target.length);

        assertArrayEquals(target, patchedSource);
    }

    @Test
    public void testEncodeDecode_LastByteChanged() throws Exception {
        byte[] source = fill(new byte[10 * 1024 * 1024]);
        byte[] target = Arrays.copyOf(source, source.length);
        ++target[target.length - 1];
        byte[] patch = diff(source, target);
        byte[] patchedSource = applyPatch(source, patch, target.length);

        assertArrayEquals(target, patchedSource);
    }

    @Test
    public void testEncodeDecode_HardcodedData() throws Exception {
        byte[] source = {
                0, 1, 2, 3,
                4, 5, 6, 7,
                8, 9, 10, 11,
                12, 13, 14, 15
        };

        byte[] target = {
                0, 1, 2, 3,
                22, 33, 44, 55,
                4, 5, 6, 7,
                4, 5, 6, 7,
                4, 5, 6, 7,
                4, 5, 6, 7,
                55, 55, 55, 55
        };

        byte[] patch = diff(source, target);
        byte[] patchedSource = applyPatch(source, patch, target.length);

        assertArrayEquals(target, patchedSource);
    }
}
