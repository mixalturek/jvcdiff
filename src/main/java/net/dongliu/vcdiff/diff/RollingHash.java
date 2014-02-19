package net.dongliu.vcdiff.diff;

/**
 * @author dongliu
 */
public class RollingHash {

    /**
     * Multiplier for incremental hashing.
     */
    private static final int kMult = 257;

    /**
     * All hashes are returned modulo "kBase".  Current implementation requires
     * kBase <= 2^32/kMult to avoid overflow.  Also, kBase must be a power of two
     * so that we can compute modulus efficiently.
     */
    protected static final int kBase = 1 << 22;


    /**
     * windowSize must be >= 2.
     */
    private final int windowSize;

    /**
     * For each windowSize, fill a 256-entry table such that:
     * hash(buffer[0] ... buffer[windowSize - 1]) + removeTable[buffer[0]] == hash(buffer[1] ... buffer[windowSize - 1])
     */
    private final int[] removeTable;

    public RollingHash(int windowSize) {
        this.windowSize = windowSize;
        // You need an instance of this type to use UpdateHash(), but hash()does not depend on removeTable
        removeTable = init();
    }

    /**
     * Compute a hash of the window [0, windowSize).
     */
    public int hash(Pointer pointer) {
        int h = 0;
        for (int i = 0; i < windowSize; ++i) {
            h = hashStep(h, pointer.get(i));
        }
        return h;
    }

    private int hashStep(int partialHash, byte nextByte) {
        return modBase((partialHash * kMult) + (nextByte & 0XFF));
    }

    /**
     * Returns operand % kBase, assuming that kBase is a power of two.
     */
    private int modBase(int i) {
        return i & (kBase - 1);
    }

    /**
     * init removeTable
     */
    private int[] init() {
        int[] removeTable = new int[256];
        // Compute multiplier.  Concisely, it is:
        //     pow(kMult, (windowSize - 1)) % kBase,
        // but we compute the power in integer form.
        int multiplier = 1;
        for (int i = 0; i < windowSize - 1; ++i) {
            multiplier = modBase(multiplier * kMult);
        }
        int byteTimesMultiplier = 0;
        for (int b = 0; b < 256; ++b) {
            removeTable[b] = findModBaseInverse(byteTimesMultiplier);
            byteTimesMultiplier = modBase(byteTimesMultiplier + multiplier);
        }
        return removeTable;
    }

    // Given an unsigned integer "operand", returns an unsigned integer "result"
    // such that
    //     result < kBase
    // and
    //     modBase(operand + result) == 0
    protected int findModBaseInverse(int i) {
        return modBase(0 - i);
    }

    /**
     * Update a hash by removing the oldest byte and adding a new byte.
     * <p/>
     * UpdateHash takes the hash value of buffer[0] ... buffer[windowSize -1]
     * along with the value of buffer[0] (the "old_first_byte" argument)
     * and the value of buffer[windowSize] (the "new_last_byte" argument).
     * It quickly computes the hash value of buffer[1] ... buffer[windowSize]
     * without having to run hash() on the entire window.
     */
    public int updateHash(int oldHash, byte oldFirstByte, byte newLastByte) {
        int partial_hash = removeFirstByteFromHash(oldHash, oldFirstByte);
        return hashStep(partial_hash, newLastByte);
    }

    /**
     * Given a full hash value for buffer[0] ... buffer[windowSize -1], plus the
     * value of the first byte buffer[0], this function returns a *partial* hash
     * value for buffer[1] ... buffer[windowSize -1].
     */
    private int removeFirstByteFromHash(int fullHash, byte firstByte) {
        return modBase(fullHash + removeTable[(firstByte & 0xff)]);
    }

}