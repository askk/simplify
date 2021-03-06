package org.cf.smalivm.emulate;

import static org.junit.Assert.assertEquals;

import org.cf.smalivm.VMTester;
import org.cf.smalivm.VirtualMachine;
import org.cf.smalivm.context.ExecutionContext;
import org.cf.smalivm.context.MethodState;
import org.cf.smalivm.type.LocalClass;
import org.cf.smalivm.type.LocalMethod;
import org.cf.smalivm.type.UnknownValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class Test_java_lang_Class_getMethod {

    private static EmulatedMethod emulatedMethod = new java_lang_Class_getMethod();

    private static MethodState getMethodState(VirtualMachine vm, Object klazz, String methodName,
                    Class<?>[] parameterTypes) {
        ExecutionContext ectx = new ExecutionContext(vm);
        int registerCount = 3;
        MethodState mState = new MethodState(ectx, registerCount);
        mState.assignRegister(0, klazz);
        mState.assignRegister(1, methodName);
        mState.assignRegister(2, parameterTypes);

        return mState;
    }

    public static class TestLocalClass {
        private static final String CLASS_NAME = "Lchild_class;";
        private static final LocalClass CLASS = new LocalClass(CLASS_NAME);

        private VirtualMachine vm;

        @Before
        public void getVM() {
            vm = VMTester.getTestVM();
        }

        @Test
        public void testGetExistantMethodWithNoParameterTypesReturnsExpectedLocalMethod() throws Exception {
            MethodState mState = getMethodState(vm, CLASS, "someString", null);
            emulatedMethod.execute(vm, mState);
            Object expected = new LocalMethod(CLASS_NAME + "->someString()Ljava/lang/String;");
            Object actual = mState.readRegister(MethodState.ReturnRegister);

            assertEquals(expected.toString(), actual.toString());
        }

        @Test
        public void testGetExistantMethodWithOneParameterTypesReturnsExpectedLocalMethod() throws Exception {
            MethodState mState = getMethodState(vm, CLASS, "takesParameter", new Class<?>[] { int.class });
            emulatedMethod.execute(vm, mState);
            Object expected = new LocalMethod(CLASS_NAME + "->takesParameter(I)V");
            Object actual = mState.readRegister(MethodState.ReturnRegister);

            assertEquals(expected.toString(), actual.toString());
        }

        @Test
        public void testGetNonExistantMethodReturnsUnknownValue() throws Exception {
            // TODO: should return unknown value and also say it "may" have thrown an exception
            MethodState mState = getMethodState(vm, CLASS, "yoDawgThisMethodWillNeverExist", null);
            emulatedMethod.execute(vm, mState);
            Object expected = new UnknownValue("Ljava/lang/reflect/Method;");
            Object actual = mState.readRegister(MethodState.ReturnRegister);

            assertEquals(expected.toString(), actual.toString());
        }
    }

    public static class TestNonLocalClass {
        private static final Class<?> CLASS = System.class;

        private VirtualMachine vm;

        @Before
        public void getVM() {
            vm = VMTester.getTestVM();
        }

        @Test
        public void testGetExistantMethodWithNoParameterTypesReturnsExpectedMethod() throws Exception {
            MethodState mState = getMethodState(vm, CLASS, "gc", null);
            emulatedMethod.execute(vm, mState);
            Object expected = System.class.getMethod("gc");
            Object actual = mState.readRegister(MethodState.ReturnRegister);

            assertEquals(expected, actual);
        }

        @Test
        public void testGetExistantMethodWithOneParameterTypesReturnsExpectedLocalMethod() throws Exception {
            MethodState mState = getMethodState(vm, CLASS, "getProperty", new Class<?>[] { String.class });
            emulatedMethod.execute(vm, mState);
            Object expected = System.class.getMethod("getProperty", new Class<?>[] { String.class });
            Object actual = mState.readRegister(MethodState.ReturnRegister);

            assertEquals(expected, actual);
        }

        @Test
        public void testGetNonExistantMethodReturnsUnknownValue() throws Exception {
            MethodState mState = getMethodState(vm, CLASS, "yoDawgThisMethodWillNeverExist", null);
            emulatedMethod.execute(vm, mState);
            Object expected = new UnknownValue("Ljava/lang/reflect/Method;");
            Object actual = mState.readRegister(MethodState.ReturnRegister);

            assertEquals(expected.toString(), actual.toString());
        }
    }

}
