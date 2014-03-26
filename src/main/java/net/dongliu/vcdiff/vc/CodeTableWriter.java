package net.dongliu.vcdiff.vc;

import net.dongliu.vcdiff.io.ByteVector;
import net.dongliu.vcdiff.diff.Pointer;
import net.dongliu.vcdiff.exception.VcdiffEncodeException;
import net.dongliu.vcdiff.utils.IOUtils;
import net.dongliu.vcdiff.utils.Misc;

import java.io.IOException;
import java.io.OutputStream;

/**
 * write the instruction into decode file.
 *
 * @author dongliu
 */
public class CodeTableWriter {
    /**
     * The maximum value for the mode of a COPY instruction.
     */
    private final short maxMode;

    /**
     * A series of instruction opcodes, each of which may be followed by one or two Varint values
     * representing the size parameters of the first and second instruction in the opcode.
     */
    ByteVector instructions = new ByteVector();

    /**
     * A series of data arguments (byte values) used for ADD and RUN instructions.
     */

    ByteVector data;

    /**
     * A series of Varint addresses used for COPY instructions. For the SAME mode,
     * a byte value is stored instead of a Varint.
     */
    ByteVector addresses;

    private AddressCache addressCache;

    private int sourceSegSize;

    /**
     * The number of bytes of target data that has been encoded so far.
     * Used to compute HERE mode addresses for COPY instructions, and is also written into the header
     * of the delta window when Output() is called.
     */
    private int targetLength;

    private CodeTable codeTableData;

    private InstructionMap instructionMap;

    /**
     * The zero-based index within instructionsAndSizes of the byte that contains the last
     * single-instruction opcode generated by encodeInstruction().
     */
    int lastOpcodeIndex;

    /**
     * If true, an Adler32 checksum of the target window data will be written as a variable-length integer,
     * just after the size of the addresses section.
     */

    private boolean addChecksum;

    /**
     * The checksum to be written to the current target window, if addChecksum is true.
     */
    private long checksum;

    final int BYTE_MAX = 255;

    /**
     * the default code table
     */
    public CodeTableWriter() {
        this.maxMode = AddressCache.defaultLastMode();
        this.sourceSegSize = 0;
        this.targetLength = 0;
        this.codeTableData = CodeTable.Default;
        this.instructionMap = null;
        this.lastOpcodeIndex = -1;
        this.addChecksum = false;
        this.checksum = 0;
        InitSectionPointers();
    }

    /**
     * for custom cate table
     */
    public CodeTableWriter(int nearCacheSize, int sameCacheSize, CodeTable codeTable,
                           short maxMode) {
        this.maxMode = maxMode;
        this.addressCache = new AddressCache(nearCacheSize, sameCacheSize);
        this.sourceSegSize = 0;
        this.targetLength = 0;
        this.codeTableData = codeTable;
        this.instructionMap = null;
        this.lastOpcodeIndex = -1;
        this.addChecksum = false;
        this.checksum = 0;
        InitSectionPointers();
    }


    void InitSectionPointers() {
        data = new ByteVector();
        addresses = new ByteVector();
    }

    public void init(int dictionarySize) {
        this.sourceSegSize = dictionarySize;
        if (codeTableData == CodeTable.Default) {
            instructionMap = InstructionMap.DEFAULT;
        } else {
            instructionMap = new InstructionMap(codeTableData, maxMode);
        }
        addressCache = new AddressCache();
        targetLength = 0;
        lastOpcodeIndex = -1;
    }

    /**
     * we only write the standard header.
     *
     * @param out
     * @throws IOException
     */
    public void writeHeader(OutputStream out) throws IOException {
        out.write(Vcdiff.MAGIC_HEADER);
        //Draft standard format
        out.write(0);
        // Hdr_Indicator: No compression, no custom code table
        out.write(0);
    }

    /**
     * the size may be larger than 255.
     *
     * @param inst
     * @param size
     */
    public void encodeInstruction(byte inst, int size) throws IOException, VcdiffEncodeException {
        encodeInstruction(inst, size, (short) 0);
    }

    /**
     * the size may be larger than 255.
     */
    private void encodeInstruction(byte ist, int size, short mode)
            throws IOException, VcdiffEncodeException {
        if (lastOpcodeIndex >= 0) {
            // try to combine the last instruction and this instruction
            short lastOpcode = Misc.b(instructions.get(lastOpcodeIndex));
            short compoundOpcode;
            if (size <= BYTE_MAX) {
                compoundOpcode = instructionMap.lookupCombinedOpcode(lastOpcode,
                        new Instruction(ist, (short) size, mode));
                if (compoundOpcode != -1) {
                    instructions.set(lastOpcodeIndex, compoundOpcode);
                    lastOpcodeIndex = -1;
                    return;
                }
            }
            // Try finding a compound opcode with size 0.
            compoundOpcode = instructionMap.lookupCombinedOpcode(lastOpcode,
                    new Instruction(ist, (short) 0, mode));
            if (compoundOpcode != -1) {
                instructions.set(lastOpcodeIndex, compoundOpcode);
                lastOpcodeIndex = -1;
                IOUtils.writeVarIntBE(size, instructions);
                return;
            }
        }
        short opcode;
        if (size <= BYTE_MAX) {
            opcode = instructionMap.lookupSingleOpcode(new Instruction(ist, (short) size, mode));
            if (opcode != -1) {
                instructions.push((byte) opcode);
                lastOpcodeIndex = instructions.size() - 1;
                return;
            }
        }
        // There should always be an opcode with size 0.
        opcode = instructionMap.lookupSingleOpcode(new Instruction(ist, (short) 0, mode));
        if (opcode == -1) {
            throw new VcdiffEncodeException("No matching opcode found for inst:" + ist
                    + ", size:" + size + ", mode:" + mode);
        }
        instructions.push((byte) opcode);
        lastOpcodeIndex = instructions.size() - 1;
        IOUtils.writeVarIntBE(size, instructions);
    }

    public void add(Pointer data, int size) throws IOException, VcdiffEncodeException {
        encodeInstruction(Instruction.TYPE_ADD, size);
        this.data.push(data, size);
        targetLength += size;
    }

    public void copy(int offset, int size)
            throws IOException, VcdiffEncodeException {
        int[] encodedAddress = new int[1];
        short mode = addressCache.encodeAddress(offset, sourceSegSize + targetLength,
                encodedAddress);
        encodeInstruction(Instruction.TYPE_COPY, size, mode);
        if (addressCache.writeAddressAsVarIntForMode(mode)) {
            IOUtils.writeVarIntBE(encodedAddress[0], addresses);
        } else {
            addresses.push((byte) encodedAddress[0]);
        }
        targetLength += size;
    }

    public void run(int size, byte b) throws IOException, VcdiffEncodeException {
        encodeInstruction(Instruction.TYPE_RUN, size);
        data.push(b);
        targetLength += size;
    }

    public void output(OutputStream out) throws IOException {
        if (instructions.empty()) {
            init(sourceSegSize);
            return;
        }

        // Add first element: Win_Indicator
        if (addChecksum) {
            out.write(Vcdiff.VCD_SOURCE | Vcdiff.VCD_CHECKSUM);
        } else {
            out.write(Vcdiff.VCD_SOURCE);
        }
        // Source segment size
        IOUtils.writeVarIntBE(sourceSegSize, out);
        // Source segment position
        IOUtils.writeVarIntBE(0, out);

        // the delta len
        int deltaEncodingLen = calculateLengthOfTheDeltaEncoding();
        int deltaWindowSize = deltaEncodingLen +
                1 +  // Win_Indicator
                IOUtils.varIntLen(sourceSegSize) +
                IOUtils.varIntLen(0) +
                IOUtils.varIntLen(deltaEncodingLen);

        IOUtils.writeVarIntBE(deltaEncodingLen, out);
        //[Here is where a secondary compressor would be used
        //  if the encoder and decoder supported that feature.]


        // Start of Delta Encoding
        //const size_t size_before_delta_encoding = out->size();
        IOUtils.writeVarIntBE(targetLength, out);
        out.write(0x00);  // Delta_Indicator: no compression
        IOUtils.writeVarIntBE(data.size(), out);
        IOUtils.writeVarIntBE(instructions.size(), out);
        IOUtils.writeVarIntBE(addresses.size(), out);
        if (addChecksum) {
            // The checksum is a 32-bit *unsigned* integer.  VarintBE requires a
            // signed type, so use a 64-bit signed integer to store the checksum.
            IOUtils.writeVarLongBE(out, checksum);
        }
        out.write(data.data(), 0, data.size());
        out.write(instructions.data(), 0, instructions.size());
        out.write(addresses.data(), 0, addresses.size());

        data.clear();
        instructions.clear();
        addresses.clear();

        init(sourceSegSize);
    }

    private int calculateLengthOfTheDeltaEncoding() {
        int length_of_the_delta_encoding =
                IOUtils.varIntLen(targetLength) +
                        1 +  // Delta_Indicator
                        IOUtils.varIntLen(data.size()) +
                        IOUtils.varIntLen(instructions.size()) +
                        IOUtils.varIntLen(addresses.size()) +
                        data.size() +
                        instructions.size() +
                        addresses.size();
        if (addChecksum) {
            length_of_the_delta_encoding += IOUtils.varLongLength(checksum);
        }
        return length_of_the_delta_encoding;
    }

    public void addChecksum(long checksum) {
        addChecksum = true;
        this.checksum = checksum;
    }

    public int targetLength() {
        return this.targetLength;
    }
}
