package net.dongliu.vcdiff.utils;

/**
 * for process unsigned values
 *
 * @author dongliu
 */
public class U {

    /**
     * convert byte as unsigned value to short
     *
     * @param b the byte
     * @return short value ,always positive
     */
    public static short b(byte b) {
        return (short) (b & 0xFF);
    }
}
