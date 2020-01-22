package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import horseshoe.internal.MethodBuilder.Label;

public class MethodBuilderTests {

	private static final byte B0 = 0;
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
		assertNotNull(mb.toString());
		System.out.println(mb);
		assertEquals("", new MethodBuilder().toString());

		final SimpleInterface instance = mb.load(name, SimpleInterface.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();
		assertEquals("Hello, world!", instance.run());
	}

	@Test
	public void methodTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		// Test method calls
		mb.addCode(ALOAD_0).addInvoke(Object.class.getMethod("getClass")).addInvoke(Class.class.getMethod("getName")).addCode(ARETURN);
		assertNotNull(mb.toString());

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
		assertNotNull(mb.toString());
		System.out.println(mb);

		final ComplexInterface instance = mb.load(name, ComplexInterface.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();

		assertEquals(3.14159 + 5 + 6 + 15.4 + 1.0, instance.calculate(new SimpleInterface() {
			@Override
			public String run() {
				return "3.14159";
			} }, new int[] { 5, 6 }, Arrays.asList(15.4), 1.0), 0.0001);
	}

	public static abstract class FieldClass {
		public static int b = 5;
		public double a = 10.5;

		protected void doNothing() {
		}

		public abstract double calculate();
	}

	@Test
	public void fieldTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		mb.pushConstant(-1).pushConstant(0).pushConstant(1).pushConstant(2).pushConstant(3).pushConstant(4).pushConstant(5).pushConstant(127).pushConstant(128).pushConstant(Short.MIN_VALUE).pushConstant(Short.MAX_VALUE).pushConstant(0x80000000).addCode(ISTORE, (byte)100, IINC, (byte)100, (byte)2)
				.pushConstant(0L).pushConstant(1L).pushConstant(2L).pushConstant(0x80000000L).pushConstant(0x8000000000000000L)
				.pushConstant(0.0f).pushConstant(1.0f).pushConstant(2.0f).pushConstant(5.0f)
				.pushConstant(0.0).pushConstant(1.0).pushConstant(2.0).pushConstant(999.666)
				.addCode(ALOAD_0).pushConstant(3.2).addFieldAccess(FieldClass.class.getDeclaredField("a"), false)
				.addCode(ALOAD_0).addFieldAccess(FieldClass.class.getDeclaredField("a"), true)
				.addCode(ALOAD_0).addInvoke(FieldClass.class.getDeclaredMethod("doNothing"), true)
				.addCode(DUP2, DADD)
				.addFieldAccess(FieldClass.class.getDeclaredField("b"), true)
				.addCode(I2D, DADD, DUP2, D2I)
				.addFieldAccess(FieldClass.class.getDeclaredField("b"), false)
				.addCode(DRETURN);
		assertNotNull(mb.toString());
		System.out.println(mb);

		final FieldClass instance = mb.load(name, FieldClass.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();

		assertEquals(3.2 + 3.2 + 5, instance.calculate(), 0.0001);
		assertEquals(11, FieldClass.b);
	}

	@Test
	public void allOpcodes() throws ReflectiveOperationException {
		for (int i = 0; i < 256; i++) {
			try {
				final MethodBuilder mb = new MethodBuilder().addCode((byte)i, B0, B0, B0, B0, B0);
				assertNotNull(mb.toString());
			} catch (final RuntimeException e) {
			}

			try {
				final MethodBuilder mb = new MethodBuilder().addCode(WIDE, (byte)i, B0, B0, B0, B0, B0);
				assertNotNull(mb.toString());
			} catch (final RuntimeException e) {
			}

			try {
				final MethodBuilder mb = new MethodBuilder();
				final Label label = mb.newLabel();
				assertNotNull(mb.addBranch((byte)i, label).updateLabel(label).toString());
			} catch (final RuntimeException e) {
			}
		}
	}

	@Test
	public void allPrimitiveConversions() throws ReflectiveOperationException {
		final List<Class<?>> allPrimitives = Arrays.asList(Number.class, Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class);

		for (final Class<?> from : allPrimitives) {
			for (final Class<?> to : allPrimitives) {
				MethodBuilder mb = new MethodBuilder().addPrimitiveConversion(from, to);
				assertNotNull(mb.toString());

				if (from.isPrimitive()) {
					mb = new MethodBuilder().addPrimitiveConversion(from, Object.class);
					assertNotNull(mb.toString());
				}
			}
		}
		// TODO: Number.class to Boolean.class

		for (final Class<?> to : allPrimitives) {
			try {
				final MethodBuilder mb = new MethodBuilder().addPrimitiveConversion(Object.class, to);
				fail();
				assertNotNull(mb.toString());
			} catch (final RuntimeException e) {
			}
		}
	}

}
