package comp370.srms;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class TestUtilities {

    public static void assertEquals(Object expected, Object actual, String message) {
        if ((expected == null && actual != null) || (expected != null && !expected.equals(actual))) {
            throw new AssertionError(message + " | expected=" + expected + " actual=" + actual);
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
    
    public static void fail(String message) {
        throw new AssertionError(message);
    }

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }

    public static void assertThrows(Class<? extends Throwable> expectedType, CheckedRunnable fn, String message) {
        try {
            fn.run();
            fail(message + " | expected exception " + expectedType.getSimpleName());
        } catch (Throwable t) {
            Throwable actual = unwrapInvocationTarget(t);
            if (!expectedType.isInstance(actual)) {
                throw new AssertionError(
                        message + " | expected " + expectedType.getSimpleName()
                                + " but got " + actual.getClass().getSimpleName(),
                        actual);
            }
        }
    }

    private static Throwable unwrapInvocationTarget(Throwable t) {
        if (t instanceof InvocationTargetException e && e.getCause() != null) {
            return e.getCause();
        }
        return t;
    }

    // Hacky to allow us to test private methods
    public static Method privateMethod(Class<?> type, String name, Class<?>... argTypes) throws Exception {
        Method method = type.getDeclaredMethod(name, argTypes);
        method.setAccessible(true);
        return method;
    }

    // Hacky to allow us to test private constructors
    public static <T> T newPrivateInstance(Class<T> type, Class<?>[] argTypes, Object... args) throws Exception {
        Constructor<T> obj = type.getDeclaredConstructor(argTypes);
        obj.setAccessible(true);
        return obj.newInstance(args);
    }

    // Overload for no-arg constructors
    public static <T> T newPrivateInstance(Class<T> type) throws Exception {
        return newPrivateInstance(type, new Class<?>[0]);
    }
}
