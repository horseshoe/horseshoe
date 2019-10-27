package horseshoe;

import static horseshoe.internal.MethodBuilder.*;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import horseshoe.internal.MethodBuilder;

public class InternalMethodBuilderTest {

	private static final AtomicInteger classCounter = new AtomicInteger(10);

	public interface SimpleInterface {
		public String run();
	}

	@Test
	public void simpleTest() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder<SimpleInterface> mb = new MethodBuilder<>(name, SimpleInterface.class);

		// Hello, world!
		final short helloWorld = mb.addConstant("Hello, world!");
		final byte bytecode[] = { LDC, (byte)helloWorld, ARETURN };

		final SimpleInterface instance = mb.load(getClass().getClassLoader(), bytecode, (short)1, (short)1).getConstructor().newInstance();
		assertEquals("Hello, world!", instance.run());
	}

	@Test
	public void methodTest() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder<SimpleInterface> mb = new MethodBuilder<>(name, SimpleInterface.class);

		// Test method calls
		final short getClassMethod = mb.addConstant(Object.class.getMethod("getClass"));
		final short getNameMethod = mb.addConstant(Class.class.getMethod("getName"));
		final byte bytecode[] = {
				ALOAD_0,
				INVOKEVIRTUAL, (byte)(getClassMethod >>> 8), (byte)getClassMethod,
				INVOKEVIRTUAL, (byte)(getNameMethod >>> 8), (byte)getNameMethod,
				ARETURN
		};

		final SimpleInterface instance = mb.load(getClass().getClassLoader(), bytecode, (short)1, (short)1).getConstructor().newInstance();
		assertEquals(name, instance.run());
	}

	public interface ComplexInterface {
		public double calculate(SimpleInterface getter, int[] extras, List<Double> more, double last);
	}

	@Test
	public void complexTest() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder<ComplexInterface> mb = new MethodBuilder<>(name, ComplexInterface.class);

		final short run = mb.addConstant(SimpleInterface.class.getDeclaredMethod("run"));
		final short javalangDouble = mb.addConstant(Double.class);
		final short parseDouble = mb.addConstant(Double.class.getDeclaredMethod("parseDouble", String.class));
		final short doubleValue = mb.addConstant(Double.class.getMethod("doubleValue"));
		final short get = mb.addConstant(List.class.getDeclaredMethod("get", int.class));
		final byte bytecode[] = {
				ALOAD_1, INVOKEINTERFACE, (byte)(run >>> 8), (byte)run, 1, 0, // Call getter.run() -> String
				INVOKESTATIC, (byte)(parseDouble >>> 8), (byte)parseDouble,   // Convert String -> double
				ALOAD_2, ICONST_0, IALOAD, I2D, DADD,                         // double + (double)extra[0] -> double
				ALOAD_2, ICONST_1, IALOAD, I2D, DADD,                         // double + (double)extra[1] -> double
				ALOAD_3, ICONST_0, INVOKEINTERFACE, (byte)(get >>> 8), (byte)get, 2, 0,
				CHECKCAST, (byte)(javalangDouble >>> 8), (byte)javalangDouble,
				INVOKEVIRTUAL, (byte)(doubleValue >>> 8), (byte)doubleValue,
				DADD,
				DLOAD, 4, DADD,
				DRETURN
		};

		final ComplexInterface instance = mb.load(getClass().getClassLoader(), bytecode, (short)4, (short)6).getConstructor().newInstance();

		assertEquals(3.14159 + 5 + 6 + 15.4 + 1.0, instance.calculate(new SimpleInterface() {
			@Override
			public String run() {
				return "3.14159";
			} }, new int[] { 5, 6 }, Arrays.asList(15.4), 1.0), 0.0001);
	}

}
