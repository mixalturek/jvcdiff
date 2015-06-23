package net.dongliu.vcdiff.vc;

/**
 * @author dongliu
 */
public class Vcdiff {
    public static final byte[] MAGIC_HEADER = new byte[]{
            (byte) ('V' | 0x80),
            (byte) ('C' | 0x80),
            (byte) ('D' | 0x80),
    };

    // If this flag is set, the delta window includes an Adler32 checksum
    // of the target window data.  Not part of the RFC draft standard.
    public static final byte VCD_CHECKSUM = 0x04;


    //vcdiff modes
    public static final short VCD_SELF_MODE = 0;
    public static final short VCD_HERE_MODE = 1;
    public static final short VCD_FIRST_NEAR_MODE = 2;
    public static final short VCD_MAX_MODES = 256;

    // flags for Hdr_Indicator
    public static final int VCD_DECOMPRESS = 1;
    public static final int VCD_CODETABLE = 1 << 1;
    // not standard header, ignore it
    public static final int VCD_EXT_APPLICATION_HEADER = 1 << 2;

    // flags for Win_Indicator
    public static final int VCD_SOURCE = 1;
    public static final int VCD_TARGET = 1 << 1;
    public static final int VCD_EXT_CHECKSUM = 1 << 2;

    // compress flag
    public static final int VCD_DATA_COMP = 1;
    public static final int VCD_INST_COMP = 1 << 1;
    public static final int VCD_ADDR_COMP = 1 << 2;
}
