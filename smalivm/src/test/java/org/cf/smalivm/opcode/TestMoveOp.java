package org.cf.smalivm.opcode;

import static org.junit.Assert.assertTrue;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;

import java.util.Arrays;

import org.cf.smalivm.VMTester;
import org.cf.smalivm.context.ExecutionGraph;
import org.cf.smalivm.context.MethodState;
import org.cf.smalivm.type.UnknownValue;
import org.junit.Test;

public class TestMoveOp {

    private static final String CLASS_NAME = "Lmove_test;";

    @Test
    public void testMoveException() {
        TIntObjectMap<Object> expected = VMTester.buildRegisterState(0, new UnknownValue("Ljava/lang/Exception;"));

        VMTester.testMethodState(CLASS_NAME, "TestMoveException()V", expected);
    }

    @Test
    public void testMoveRegisterObject() {
        TIntObjectMap<Object> initial = VMTester.buildRegisterState(0, new Object());

        // Must invoke VM directly to ensure reference identity
        ExecutionGraph graph = VMTester.execute(CLASS_NAME, "TestMoveRegisterObject()V", initial);
        TIntList addresses = graph.getConnectedTerminatingAddresses();
        assertTrue("Should terminate when expected: " + addresses + " == {1}",
                        Arrays.equals(addresses.toArray(), new int[] { 1 }));

        Object register0 = graph.getRegisterConsensus(1, 0);
        Object register1 = graph.getRegisterConsensus(1, 1);

        assertTrue(register0 == register1);
        assertTrue(register0 instanceof Object);
    }

    @Test
    public void testMoveRegisterPrimitive() {
        TIntObjectMap<Object> initial = VMTester.buildRegisterState(0, 42);
        TIntObjectMap<Object> expected = VMTester.buildRegisterState(0, 42, 1, 42);

        VMTester.testMethodState(CLASS_NAME, "TestMoveRegisterPrimitive()V", initial, expected);
    }

    @Test
    public void testMoveResult() {
        TIntObjectMap<Object> initial = VMTester.buildRegisterState(MethodState.ResultRegister, 42);
        TIntObjectMap<Object> expected = VMTester.buildRegisterState(0, 42);

        VMTester.testMethodState(CLASS_NAME, "TestMoveResult()V", initial, expected);
    }

}
