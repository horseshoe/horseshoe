package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import horseshoe.internal.MethodBuilder;

public class MethodBuilderTests {

	private static final AtomicInteger classCounter = new AtomicInteger(10);

	public interface SimpleInterface {
		public String run();
	}

	@Test
	public void simpleTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		// Hello, world!
		mb.pushConstant("Hello, world!").addCode(ARETURN);

		final SimpleInterface instance = mb.load(name, SimpleInterface.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();
		assertEquals("Hello, world!", instance.run());
	}

	@Test
	public void methodTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		// Test method calls
		mb.addCode(ALOAD_0).addInvoke(Object.class.getMethod("getClass")).addInvoke(Class.class.getMethod("getName")).addCode(ARETURN);

		final SimpleInterface instance = mb.load(name, SimpleInterface.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();
		assertEquals(name, instance.run());
	}

	public static abstract class ComplexInterface {
		public abstract double calculate(final SimpleInterface getter, final int[] extras, final List<Double> more, final double last);
	}

	@Test
	public void complexTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		mb.addCode(ALOAD_1)
				.addInvoke(SimpleInterface.class.getDeclaredMethod("run"))              // Call getter.run() -> String
				.addInvoke(Double.class.getDeclaredMethod("parseDouble", String.class)) // Convert String -> double
				.addCode(ALOAD_2, ICONST_0, IALOAD, I2D, DADD)                          // double + (double)extra[0] -> double
				.addCode(ALOAD_2, ICONST_1, IALOAD, I2D, DADD)                          // double + (double)extra[1] -> double
				.addCode(ALOAD_3, ICONST_0).addInvoke(List.class.getDeclaredMethod("get", int.class))
				.addCast(Double.class)
				.addInvoke(Double.class.getMethod("doubleValue"))
				.addCode(DADD, DLOAD, (byte)4, DADD, DRETURN);

		final ComplexInterface instance = mb.load(name, ComplexInterface.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();

		assertEquals(3.14159 + 5 + 6 + 15.4 + 1.0, instance.calculate(new SimpleInterface() {
			@Override
			public String run() {
				return "3.14159";
			} }, new int[] { 5, 6 }, Arrays.asList(15.4), 1.0), 0.0001);
	}

}
