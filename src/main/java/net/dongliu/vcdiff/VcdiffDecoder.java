package net.dongliu.vcdiff;

import net.dongliu.vcdiff.exception.VcdiffDecodeException;
import net.dongliu.vcdiff.io.ByteArrayStream;
import net.dongliu.vcdiff.io.FileStream;
import net.dongliu.vcdiff.io.FixedByteArrayStream;
import net.dongliu.vcdiff.io.RandomAccessStream;
import net.dongliu.vcdiff.utils.IOUtils;
import net.dongliu.vcdiff.utils.Misc;
import net.dongliu.vcdiff.vc.AddressCache;
import net.dongliu.vcdiff.vc.CodeTable;
import net.dongliu.vcdiff.vc.Instruction;
import net.dongliu.vcdiff.vc.Vcdiff;

import java.io.*;

/**
 * vcdiff decode.
 *
 * @author dongliu
 */
public class VcdiffDecoder {

    private RandomAccessStream sourceStream;

    private InputStream patchStream;

    private RandomAccessStream targetStream;

    /**
     * code table
     */
    private CodeTable codeTable = CodeTable.Default;

    private AddressCache cache = new AddressCache(4, 3);

    /**
     * Constructor. The caller is responsible for close of the passed streams.
     *
     * @param sourceStream older data
     * @param patchStream  diff between older and newer data
     * @param targetStream result of patch application to the older data (output)
     */
    public VcdiffDecoder(RandomAccessStream sourceStream, InputStream patchStream, RandomAccessStream targetStream) {
        this.sourceStream = sourceStream;
        this.patchStream = patchStream;
        this.targetStream = targetStream;
    }

    /**
     * Convenient static method for caller. Apply vcdiff decode file to source file.
     *
     * @param sourceFile older file
     * @param patchFile  diff between older and newer file
     * @param targetFile result of patch application to the older file (output)
     */
    public static void decode(File sourceFile, File patchFile, File targetFile)
            throws IOException, VcdiffDecodeException {
        try (RandomAccessStream sourceStream = new FileStream(new RandomAccessFile(sourceFile, "r"), true);
             InputStream patchStream = new FileInputStream(patchFile);
             RandomAccessStream targetStream = new FileStream(new RandomAccessFile(targetFile, "rw"))
        ) {
            decode(sourceStream, patchStream, targetStream);
        }
    }

    /**
     * Convenient static method for caller. Apply vcdiff patch to source.
     * The caller is responsible for close of the passed streams.
     *
     * @param sourceStream older data
     * @param patchStream  diff between older and newer data
     * @param targetStream result of patch application to the older data (output)
     */
    public static void decode(RandomAccessStream sourceStream, InputStream patchStream,
                              RandomAccessStream targetStream)
            throws IOException, VcdiffDecodeException {
        VcdiffDecoder decoder = new VcdiffDecoder(sourceStream, patchStream, targetStream);
        decoder.decode();
    }

    /**
     * do vcdiff decode.
     *
     * @throws IOException
     * @throws net.dongliu.vcdiff.exception.VcdiffDecodeException
     */
    public void decode() throws IOException, VcdiffDecodeException {
        readHeader();
        while (decodeWindow()) ;
    }

    /*
             Header1                                  - byte = 0xD6
             Header2                                  - byte = 0xC3
             Header3                                  - byte = 0xC4
             Header4                                  - byte
             Hdr_Indicator                            - byte
             [Secondary compressor ID]                - byte
             [Length of code table data]              - integer
             [Code table data]
     */
    private void readHeader() throws IOException, VcdiffDecodeException {
        byte[] magic = IOUtils.readBytes(patchStream, 4);
        //magic num.
        if (!Misc.ArrayEqual(magic, Vcdiff.MAGIC_HEADER, 3)) {
            // not vcdiff file.
            throw new VcdiffDecodeException("The file is not valid vcdiff file.");
        }
        if (magic[3] != 0) {
            // version num.for standard vcdiff file, is always 0.
            throw new UnsupportedOperationException("Unsupported vcdiff version.");
        }
        byte headerIndicator = (byte) patchStream.read();

        boolean secondaryCompress = (headerIndicator & Vcdiff.VCD_DECOMPRESS) != 0;
        boolean customCodeTable = ((headerIndicator & Vcdiff.VCD_CODETABLE) != 0);
        boolean applicationHeader = ((headerIndicator & Vcdiff.VCD_EXT_APPLICATION_HEADER) != 0);

        // read Secondary compressor ID
        byte secondaryCompressorID = 0;
        if (secondaryCompress) {
            secondaryCompressorID = (byte) patchStream.read();
            // now support now
            throw new UnsupportedOperationException("Vcdiff with secondary compressors not supported");
        }

        // other bits should be zero.
        if ((headerIndicator & 0xf8) != 0) {
            throw new VcdiffDecodeException("Invalid header indicator - bits 3-7 not all zero.");
        }

        // if has custom code table.
        if (customCodeTable) {
            // load custom code table
            readCodeTable();
        }

        // Ignore the application header if we have one.
        if (applicationHeader) {
            int appHeaderLength = IOUtils.readVarIntBE(patchStream);
            // skip bytes.
            IOUtils.readBytes(patchStream, appHeaderLength);
        }

    }

    /**
     * load custom code table.
     *
     * @throws net.dongliu.vcdiff.exception.VcdiffDecodeException
     * @throws IOException
     */
    private void readCodeTable() throws IOException, VcdiffDecodeException {
        int compressedTableLen = IOUtils.readVarIntBE(patchStream) - 2;
        int nearSize = patchStream.read();
        int sameSize = patchStream.read();
        byte[] compressedTableData = IOUtils.readBytes(patchStream, compressedTableLen);
        byte[] defaultTableData = CodeTable.Default.getBytes();
        byte[] decompressedTableData = new byte[1536];

        try (RandomAccessStream tableOriginal = new FixedByteArrayStream(defaultTableData, true);
             InputStream tableDelta = new ByteArrayInputStream(compressedTableData);
             RandomAccessStream tableOutput = new ByteArrayStream(decompressedTableData)
        ) {
            VcdiffDecoder.decode(tableOriginal, tableDelta, tableOutput);

            if (tableOutput.pos() != 1536) {
                throw new VcdiffDecodeException("Compressed code table was incorrect size");
            }
        }

        codeTable = new CodeTable(decompressedTableData);
        cache = new AddressCache(nearSize, sameSize);
    }

    /*
          Win_Indicator                            - byte
          [Source segment length]                  - integer
          [Source segment position]                - integer
          The delta encoding of the target window
     */
    private boolean decodeWindow() throws IOException, VcdiffDecodeException {

        int windowIndicator = patchStream.read();
        // finished.
        if (windowIndicator == -1) {
            return false;
        }

        // xdelta3 uses an undocumented extra bit which indicates that there are
        // an extra 4 bytes at the end of the encoding for the window
        boolean hasAdler32Checksum = ((windowIndicator & Vcdiff.VCD_EXT_CHECKSUM) != 0);

        // Get rid of the checksum bit for the rest
        windowIndicator &= 0xfb;

        // Work out what the source data is, and detect invalid window indicators
        RandomAccessStream sourceWindowStream;
        int tempTargetStreamPos = -1;
        switch (windowIndicator) {
            // No source data used in this window
            case 0:
                sourceWindowStream = null;
                break;
            // Source data comes from the original stream
            case Vcdiff.VCD_SOURCE:
                if (this.sourceStream == null) {
                    throw new VcdiffDecodeException("Source stream required.");
                }
                sourceWindowStream = this.sourceStream;
                break;
            // Source data comes from the target stream
            case Vcdiff.VCD_TARGET:
                sourceWindowStream = targetStream;
                tempTargetStreamPos = targetStream.pos();
                break;
            case 3:
            default:
                throw new VcdiffDecodeException("Invalid window indicator.");
        }

        // Read the source data, if any
        RandomAccessStream sourceData = null;
        int sourceWindowLen = 0;
        if (sourceWindowStream != null) {
            sourceWindowLen = IOUtils.readVarIntBE(patchStream);
            int sourceWindowPos = IOUtils.readVarIntBE(patchStream);

            sourceWindowStream.seek(sourceWindowPos);

            sourceData = IOUtils.slice(sourceWindowStream, sourceWindowLen, false);

            // restore the position the source stream if appropriate
            if (tempTargetStreamPos != -1) {
                targetStream.seek(tempTargetStreamPos);
            }
        }
        //sourceStream = null;
        deltaEncoding(hasAdler32Checksum, sourceData, sourceWindowLen);
        return true;
    }


    /*
              Length of the delta encoding        - integer
              The delta encoding
              Length of the target window         - integer
              Delta_Indicator                     - byte
              Length of data for ADDs and RUNs    - integer
              Length of instructions section      - integer
              Length of addresses for COPYs       - integer
              Data section for ADDs and RUNs      - array of bytes
              Instructions and sizes section      - array of bytes
              Addresses section for COPYs         - array of bytes
     */
    private void deltaEncoding(boolean hasAdler32Checksum, RandomAccessStream sourceData, int sourceLen)
            throws IOException, VcdiffDecodeException {
        // Length of the delta encoding
        IOUtils.readVarIntBE(patchStream);

        //  Length of the target window.the actual size of the target window after decompression
        int targetLen = IOUtils.readVarIntBE(patchStream);

        // Delta_Indicator.
        int deltaIndicator = patchStream.read();
        boolean dataCompress = (deltaIndicator & Vcdiff.VCD_DATA_COMP) != 0;
        boolean instCompress = (deltaIndicator & Vcdiff.VCD_INST_COMP) != 0;
        boolean addrCompress = (deltaIndicator & Vcdiff.VCD_ADDR_COMP) != 0;

        byte[] targetData = new byte[targetLen];
        RandomAccessStream targetDataStream = new ByteArrayStream(targetData);

        // Length of data for ADDs and RUNs
        int addRunDataLen = IOUtils.readVarIntBE(patchStream);
        // Length of instructions and sizes
        int instructionsLen = IOUtils.readVarIntBE(patchStream);
        // Length of addresses for COPYs
        int addressesLen = IOUtils.readVarIntBE(patchStream);

        // If we've been given a checksum, we have to read it and we might as well
        int checksumInFile = 0;
        if (hasAdler32Checksum) {
            byte[] checksumBytes = IOUtils.readBytes(patchStream, 4);
            checksumInFile = (checksumBytes[0] << 24)
                    | (checksumBytes[1] << 16) | (checksumBytes[2] << 8)
                    | checksumBytes[3];
        }

        // Data section for ADDs and RUNs
        byte[] addRunData = IOUtils.readBytes(patchStream, addRunDataLen);
        int addRunDataIndex = 0;
        // Instructions and sizes section
        byte[] instructions = IOUtils.readBytes(patchStream, instructionsLen);
        // Addresses section for COPYs
        byte[] addresses = IOUtils.readBytes(patchStream, addressesLen);

        RandomAccessStream instructionStream = new FixedByteArrayStream(instructions, true);

        cache.reset(addresses);

        while (true) {
            int instructionIndex = instructionStream.read();
            if (instructionIndex == -1) {
                break;
            }

            for (int i = 0; i < 2; i++) {
                Instruction instruction = codeTable.get(instructionIndex, i);
                int size = instruction.getSize();
                // separated encoded size
                if (size == 0 && instruction.getIst() != Instruction.TYPE_NO_OP) {
                    size = IOUtils.readVarIntBE(instructionStream);
                }
                switch (instruction.getIst()) {
                    case Instruction.TYPE_NO_OP:
                        break;
                    case Instruction.TYPE_ADD:
                        targetDataStream.write(addRunData, addRunDataIndex, size);
                        addRunDataIndex += size;
                        break;
                    case Instruction.TYPE_COPY:
                        int addr = cache.decodeAddress(targetDataStream.pos() + sourceLen, instruction.getMode());
                        if (sourceData != null && addr < sourceLen) {
                            sourceData.seek(addr);
                            IOUtils.copy(sourceData, targetDataStream, size);
                        } else {
                            // Data is in target data, Get rid of the offset
                            addr -= sourceLen;
                            if (addr + size < targetDataStream.pos()) {
                                targetDataStream.write(targetData, addr, size);
                            } else {
                                // If overlap. Can we just ignore overlap issues?
                                for (int j = 0; j < size; j++) {
                                    targetDataStream.write(targetData[addr++]);
                                }
                            }
                        }
                        break;
                    case Instruction.TYPE_RUN:
                        byte data = addRunData[addRunDataIndex++];
                        for (int j = 0; j < size; j++) {
                            targetDataStream.write(data);
                        }
                        break;
                    default:
                        throw new VcdiffDecodeException("Invalid instruction type found.");
                }
            }
        }
        IOUtils.closeQuietly(targetDataStream);
        IOUtils.closeQuietly(sourceData);
        targetStream.write(targetData, 0, targetLen);

        if (hasAdler32Checksum) {
            // check sum
            check(checksumInFile, targetData);
        }
    }

    private void check(int checksumInFile, byte[] targetData) {
        //TODO: adler32 check.
    }
}
