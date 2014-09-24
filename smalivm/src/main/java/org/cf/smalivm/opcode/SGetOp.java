package org.cf.smalivm.opcode;

import org.cf.smalivm.StaticFieldAccessor;
import org.cf.smalivm.VirtualMachine;
import org.cf.smalivm.context.ClassContextMap;
import org.cf.smalivm.context.MethodContext;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.util.ReferenceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SGetOp extends FullContextOp {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(SGetOp.class.getSimpleName());

    static SGetOp create(Instruction instruction, int address, VirtualMachine vm) {
        String opName = instruction.getOpcode().name;
        int childAddress = address + instruction.getCodeUnits();

        Instruction21c instr = (Instruction21c) instruction;
        int destRegister = instr.getRegisterA();
        FieldReference reference = (FieldReference) instr.getReference();
        String fieldDescriptor = ReferenceUtil.getFieldDescriptor(reference);

        return new SGetOp(address, opName, childAddress, destRegister, fieldDescriptor, vm);
    }

    private final int destRegister;
    private final String fieldDescriptor;
    private final VirtualMachine vm;

    public SGetOp(int address, String opName, int childAddress, int destRegister, String fieldDescriptor,
                    VirtualMachine vm) {
        super(address, opName, childAddress);

        this.destRegister = destRegister;
        this.fieldDescriptor = fieldDescriptor;
        this.vm = vm;
    }

    @Override
    public int[] execute(MethodContext mctx, ClassContextMap classContextMap) {
        Object value = StaticFieldAccessor.getField(vm, fieldDescriptor);
        mctx.assignRegister(destRegister, value);

        return getPossibleChildren();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getOpName());
        sb.append(" r").append(destRegister).append(", ").append(fieldDescriptor);

        return sb.toString();
    }

}