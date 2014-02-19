package net.dongliu.vcdiff.diff;

import net.dongliu.vcdiff.vc.CodeTable;
import net.dongliu.vcdiff.vc.Instruction;
import net.dongliu.vcdiff.vc.InstructionMap;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author dongliu
 */
public class InstructionMapTest {

    static CodeTable codeTable;
    static InstructionMap instructionMap;

    @BeforeClass
    public static void init() {
        codeTable = CodeTable.Default;
        instructionMap = InstructionMap.DEFAULT;
    }

    @Test
    public void testLookupSingleOpcode() throws Exception {
        for (int i = 0; i < CodeTable.CodeTableSize; i++) {
            Instruction inst = codeTable.get(i, 0);
            int opcode = instructionMap.lookupSingleOpcode(inst);
            Assert.assertTrue(opcode <= i);
            if (opcode < i) {
                Assert.assertTrue("opcode " + i + " and " + opcode + " is different",
                        codeTable.get(i, 0).equals(codeTable.get(opcode, 0)));
            }
        }
    }

    @Test
    public void testLookupCombinedOpcode() throws Exception {
        for (int i = 0; i < CodeTable.CodeTableSize; i++) {
            Instruction inst1 = codeTable.get(i, 0);
            Instruction inst2 = codeTable.get(i, 1);
            if (inst2.getIst() == Instruction.TYPE_NO_OP) {
                continue;
            }
            short opcode1 = instructionMap.lookupSingleOpcode(inst1);
            Assert.assertEquals(inst1, codeTable.get(opcode1, 0));
            int opcode = instructionMap.lookupCombinedOpcode(opcode1, inst2);
            Assert.assertEquals(i, opcode);
        }
    }
}
