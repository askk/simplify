package org.cf.smalivm.opcode;

import org.cf.smalivm.context.MethodState;
import org.cf.smalivm.type.UnknownValue;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;

class UnimplementedOp extends MethodStateOp {

    static UnimplementedOp create(Instruction instruction, int address) {
        UnimplementedOp result = null;

        int childAddress = address + instruction.getCodeUnits();
        Opcode op = instruction.getOpcode();

        if (instruction instanceof OneRegisterInstruction) {
            OneRegisterInstruction instr = (OneRegisterInstruction) instruction;
            int registerA = instr.getRegisterA();

            result = new UnimplementedOp(address, op.name, childAddress, op.canContinue(), op.canThrow(),
                            op.setsResult(), op.setsRegister(), registerA);
        } else {
            result = new UnimplementedOp(address, op.name, childAddress, op.canContinue(), op.canThrow(),
                            op.setsResult());
        }

        return result;
    }

    private final boolean canContinue;
    private final boolean canThrow;
    private final int registerA;
    private final boolean setsRegister;
    private final boolean setsResult;

    UnimplementedOp(int address, String opName, int childAddress, boolean canContinue, boolean canThrow,
                    boolean setsResult) {
        this(address, opName, childAddress, canContinue, canThrow, setsResult, false, -1);
    }

    UnimplementedOp(int address, String opName, int childAddress, boolean canContinue, boolean canThrow,
                    boolean setsResult, boolean setsRegister, int registerA) {
        super(address, opName, canContinue ? childAddress : 0);

        this.canContinue = canContinue;
        this.canThrow = canThrow;
        this.setsResult = setsResult;
        this.setsRegister = setsRegister;
        this.registerA = registerA;
    }

    @Override
    public int[] execute(MethodState mState) {
        if (setsResult) {
            mState.assignResultRegister(new UnknownValue("?"));
        }

        if (registerA >= 0) {
            if (setsRegister) {
                mState.assignRegister(registerA, new UnknownValue("?"));
            } else {
                mState.readRegister(registerA);
            }
        }

        return getPossibleChildren();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());

        if (registerA >= 0) {
            sb.append(" r").append(registerA);
        }

        sb.append(" (unimplmented)");

        return sb.toString();
    }

}
