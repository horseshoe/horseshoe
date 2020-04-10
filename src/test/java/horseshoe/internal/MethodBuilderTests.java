package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import horseshoe.internal.MethodBuilder.Label;

public class MethodBuilderTests {

	private static final byte B0 = 0;
	private static final AtomicInteger classCounter = new AtomicInteger(10);

	public static class SwitchClass {
		public String run(final int a) {
			return "invalid";
		}
	}

	@Test
	public void switchTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();
		final SortedMap<Integer, Label> labels = new TreeMap<>();

		labels.put(1, mb.newLabel());
		labels.put(2, mb.newLabel());
		labels.put(5, mb.newLabel());
		labels.put(10, mb.newLabel());

		mb.addCode(ILOAD_1).addSwitch(labels, labels.get(2))
				.updateLabel(labels.get(1)).pushNewObject(boolean.class, 1).pushNewObject(char.class, 1).pushNewObject(byte.class, 1).pushNewObject(short.class, 1).pushNewObject(long.class, 1).pushNewObject(int.class, 1).addInvoke(Object.class.getMethod("getClass")).addInvoke(Class.class.getMethod("getName")).addCode(ARETURN)
				.updateLabel(labels.get(2)).pushNewObject(String.class, 2, 3).addCode(DUP, DUP).pushConstant(0).addCode(AALOAD).pushConstant(1).pushConstant("01").addCode(AASTORE).pushConstant(1).addCode(AALOAD).pushConstant(0).pushConstant("10").addCode(AASTORE).pushConstant(0).addCode(AALOAD).pushConstant(1).addCode(AALOAD).addInvoke(Object.class.getMethod("toString")).addCode(ARETURN)
				.updateLabel(labels.get(5)).pushNewObject(double.class, 1).addCode(DUP).pushConstant(0).pushConstant(2.0).addCode(DASTORE).pushConstant(0).addCode(DALOAD).addPrimitiveConversion(double.class, Double.class).addInvoke(Object.class.getMethod("toString")).addCode(ARETURN)
				.updateLabel(labels.get(10)).pushNewObject(float.class, 1).pushConstant(10.0f).addPrimitiveConversion(float.class, Integer.class).addInvoke(Object.class.getMethod("toString")).addCode(ARETURN);
		assertNotNull(mb.toString());

		final SwitchClass switchTest = mb.build(name, SwitchClass.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();

		assertEquals("[I", switchTest.run(1));
		assertEquals("01", switchTest.run(2));
		assertEquals("2.0", switchTest.run(5));
		assertEquals("10", switchTest.run(10));
		assertEquals("01", switchTest.run(11));
	}

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

		final SimpleInterface instance = mb.build(name, SimpleInterface.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();
		assertEquals("Hello, world!", instance.run());
	}

	@Test
	public void methodTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		// Test method calls
		mb.addCode(ALOAD_0).addInvoke(Object.class.getMethod("getClass")).addInvoke(Class.class.getMethod("getName")).addCode(ARETURN);
		assertNotNull(mb.toString());

		final SimpleInterface instance = mb.build(name, SimpleInterface.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();
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

		final ComplexInterface instance = mb.build(name, ComplexInterface.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();

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

		final FieldClass instance = mb.build(name, FieldClass.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();

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
				final MethodBuilder mb = new MethodBuilder().addCode((byte)i);
				assertNotNull(mb.toString());
			} catch (final RuntimeException e) {
			}

			try {
				final MethodBuilder mb = new MethodBuilder().addCode(WIDE, (byte)i, B0, B0, B0, B0, B0);
				assertNotNull(mb.toString());
				mb.addCode(WIDE, (byte)i);
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

		for (final Class<?> to : allPrimitives) {
			try {
				final MethodBuilder mb = new MethodBuilder().addPrimitiveConversion(Object.class, to);
				fail();
				assertNotNull(mb.toString());
			} catch (final RuntimeException e) {
			}
		}

		// Test Number.class to Boolean.class
		final String name = getClass().getName() + "$" + classCounter.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();
		final Label fail = mb.newLabel();

		mb.pushConstant(Long.MIN_VALUE).addPrimitiveConversion(long.class, Long.class).addPrimitiveConversion(Number.class, boolean.class).addBranch(IFEQ, fail)
				.pushConstant(0.8f).addPrimitiveConversion(float.class, Number.class).addPrimitiveConversion(Number.class, Boolean.class).addPrimitiveConversion(Boolean.class, boolean.class).addBranch(IFEQ, fail)
				.pushConstant(-2000).addPrimitiveConversion(int.class, Object.class).addPrimitiveConversion(Number.class, Boolean.class).addPrimitiveConversion(Boolean.class, boolean.class).addBranch(IFEQ, fail)
				.pushConstant(6.7).addPrimitiveConversion(double.class, Number.class).addPrimitiveConversion(Number.class, Boolean.class).addPrimitiveConversion(Boolean.class, boolean.class).addBranch(IFEQ, fail)
				.pushConstant(0.0f).addPrimitiveConversion(float.class, Number.class).addPrimitiveConversion(Number.class, Boolean.class).addPrimitiveConversion(Boolean.class, boolean.class).addBranch(IFNE, fail)
				.pushConstant(0.0).addPrimitiveConversion(double.class, Number.class).addPrimitiveConversion(Number.class, boolean.class).addBranch(IFNE, fail)
				.pushConstant(0).addPrimitiveConversion(int.class, Number.class).addPrimitiveConversion(Number.class, Boolean.class).addPrimitiveConversion(Boolean.class, boolean.class).addBranch(IFNE, fail)
				.pushConstant(0L).addPrimitiveConversion(long.class, Number.class).addPrimitiveConversion(Number.class, Boolean.class).addPrimitiveConversion(Boolean.class, boolean.class).addBranch(IFNE, fail)
				.pushConstant("success").addCode(ARETURN).updateLabel(fail).addThrow(RuntimeException.class, null);
		final SimpleInterface instance = mb.build(name, SimpleInterface.class, MethodBuilderTests.class.getClassLoader()).getConstructor().newInstance();
		assertEquals("success", instance.run());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testAppendSelf() {
		final MethodBuilder mb = new MethodBuilder();
		mb.append(mb);
	}

	public static interface MultipleMethodInterface {
		public static void staticMethod() { /* Empty test method */ }
		public void method1();
		public void method2();
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadBase() throws ReflectiveOperationException {
		new MethodBuilder().addCode(RETURN).build("BadBase", MultipleMethodInterface.class, MethodBuilderTests.class.getClassLoader());
	}

	public static abstract class MultipleAbstractMethodClass {
		public abstract void method1();
		public abstract void method2();
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadBase2() throws ReflectiveOperationException {
		new MethodBuilder().addCode(RETURN).build("BadBase2", MultipleAbstractMethodClass.class, MethodBuilderTests.class.getClassLoader());
	}

	public static class MultipleMethodClass {
		public void method1() { /* Empty test method */ }
		public void method2() { /* Empty test method */ }
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadBase3() throws ReflectiveOperationException {
		new MethodBuilder().addCode(RETURN).build("BadBase3", MultipleMethodClass.class, MethodBuilderTests.class.getClassLoader());
	}

	public static class NoMethodClass {
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadBase4() throws ReflectiveOperationException {
		new MethodBuilder().addCode(RETURN).build("BadBase4", NoMethodClass.class, MethodBuilderTests.class.getClassLoader());
	}

	public static final class FinalClass {
		public void method1() { /* Empty test method */ }
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadBase5() throws ReflectiveOperationException {
		new MethodBuilder().addCode(RETURN).build("BadBase5", FinalClass.class, MethodBuilderTests.class.getClassLoader());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadIndex() {
		new MethodBuilder().addAccess(ALOAD, 65536);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadIndex2() {
		new MethodBuilder().addAccess(ALOAD, -1);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadNewObject() {
		new MethodBuilder().pushNewObject(Object.class, new int[256]);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBadNewObject2() {
		new MethodBuilder().pushNewObject(int.class);
	}

}
