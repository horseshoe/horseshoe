package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import horseshoe.internal.MethodBuilder.Label;

import org.junit.jupiter.api.Test;

class MethodBuilderTests {

	private static final byte B0 = 0;
	private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(10);
	private static final ClassLoader classLoader = MethodBuilderTests.class.getClassLoader();

	public static class SwitchClass {
		public String run(final int a) {
			return "invalid";
		}
	}

	@Test
	void switchTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + CLASS_COUNTER.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();
		final SortedMap<Integer, Label> labels = new TreeMap<>();

		labels.put(1, mb.newLabel());
		labels.put(2, mb.newLabel());
		labels.put(5, mb.newLabel());
		labels.put(10, mb.newLabel());

		mb.addCode(ILOAD_1).addSwitch(labels, labels.get(2))
				.updateLabel(labels.get(1)).pushNewObject(boolean.class, 1).pushNewObject(char.class, 1).pushNewObject(byte.class, 1).pushNewObject(short.class, 1).pushNewObject(long.class, 1).pushNewObject(int.class, 1).addInvoke(getMethod(Object.class, "getClass")).addInvoke(getMethod(Class.class, "getName")).addFlowBreakingCode(ARETURN, 6)
				.updateLabel(labels.get(2)).pushNewObject(String.class, 2, 3).addCode(DUP, DUP).pushConstant(0).addCode(AALOAD).pushConstant(1).pushConstant("01").addCode(AASTORE).pushConstant(1).addCode(AALOAD).pushConstant(0).pushConstant("10").addCode(AASTORE).pushConstant(0).addCode(AALOAD).pushConstant(1).addCode(AALOAD).addInvoke(getMethod(Object.class, "toString")).addFlowBreakingCode(ARETURN, 0)
				.updateLabel(labels.get(5)).pushNewObject(double.class, 1).addCode(DUP).pushConstant(0).pushConstant(2.0).addCode(DASTORE).pushConstant(0).addCode(DALOAD).addPrimitiveConversion(double.class, Double.class).addInvoke(getMethod(Object.class, "toString")).addFlowBreakingCode(ARETURN, 0)
				.updateLabel(labels.get(10)).pushNewObject(float.class, 1).pushConstant(10.0f).addPrimitiveConversion(float.class, Integer.class).addInvoke(getMethod(Object.class, "toString")).addFlowBreakingCode(ARETURN, 1);
		assertNotNull(mb.toString());

		final SwitchClass switchTest = new MethodBuilder().addCode(NOP).append(mb).build(name, SwitchClass.class, classLoader).getConstructor().newInstance();

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
	void simpleTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + CLASS_COUNTER.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		// Hello, world!
		mb.pushConstant("Hello, world!").addFlowBreakingCode(ARETURN, 0);
		assertNotNull(mb.toString());
		System.out.println(mb);
		assertEquals("", new MethodBuilder().toString());

		final SimpleInterface instance = mb.build(name, SimpleInterface.class, classLoader).getConstructor().newInstance();
		assertEquals("Hello, world!", instance.run());
	}

	@Test
	void methodTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + CLASS_COUNTER.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		// Test method calls
		mb.addCode(ALOAD_0).addInvoke(getMethod(Object.class, "getClass")).addInvoke(getMethod(Class.class, "getName")).addFlowBreakingCode(ARETURN, 0);
		assertNotNull(mb.toString());

		final SimpleInterface instance = mb.build(name, SimpleInterface.class, classLoader).getConstructor().newInstance();
		assertEquals(name, instance.run());
	}

	public abstract static class ComplexInterface {
		public abstract double calculate(final SimpleInterface getter, final int[] extras, final List<Double> more, final double last);
	}

	@Test
	void complexTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + CLASS_COUNTER.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		mb.addCode(ALOAD_1)
				.addInvoke(SimpleInterface.class.getDeclaredMethod("run"))              // Call getter.run() -> String
				.addInvoke(Double.class.getDeclaredMethod("parseDouble", String.class)) // Convert String -> double
				.addCode(ALOAD_2, ICONST_0, IALOAD, I2D, DADD)                          // double + (double)extra[0] -> double
				.addCode(ALOAD_2, ICONST_1, IALOAD, I2D, DADD)                          // double + (double)extra[1] -> double
				.addCode(ALOAD_3, ICONST_0).addInvoke(List.class.getDeclaredMethod("get", int.class))
				.addCast(Double.class)
				.addInvoke(getMethod(Double.class, "doubleValue"))
				.addCode(DADD, DLOAD, (byte)4, DADD).addFlowBreakingCode(DRETURN, 0);
		assertNotNull(mb.toString());
		System.out.println(mb);

		final ComplexInterface instance = mb.build(name, ComplexInterface.class, classLoader).getConstructor().newInstance();

		assertEquals(3.14159 + 5 + 6 + 15.4 + 1.0, instance.calculate(new SimpleInterface() {
			@Override
			public String run() {
				return "3.14159";
			}
		}, new int[] { 5, 6 }, Arrays.asList(15.4), 1.0), 0.0001);
	}

	public abstract static class FieldClass {
		public static int testB = 5;
		public double testA = 10.5;

		protected void doNothing() {
		}

		public abstract double calculate();
	}

	@Test
	void fieldTest() throws ReflectiveOperationException {
		final String name = getClass().getName() + "$" + CLASS_COUNTER.getAndIncrement();
		final MethodBuilder mb = new MethodBuilder();

		mb.pushConstant(-1).pushConstant(0).pushConstant(1).pushConstant(2).pushConstant(3).pushConstant(4).pushConstant(5).pushConstant(127).pushConstant(128).pushConstant(Short.MIN_VALUE).pushConstant(Short.MAX_VALUE).pushConstant(0x80000000).addCode(ISTORE, (byte)100, IINC, (byte)100, (byte)2)
				.pushConstant(0L).pushConstant(1L).pushConstant(2L).pushConstant(0x80000000L).pushConstant(0x8000000000000000L)
				.pushConstant(0.0f).pushConstant(1.0f).pushConstant(2.0f).pushConstant(5.0f)
				.pushConstant(0.0).pushConstant(1.0).pushConstant(2.0).pushConstant(999.666)
				.addCode(ALOAD_0).pushConstant(3.2).addFieldAccess(FieldClass.class.getDeclaredField("testA"), false)
				.addCode(ALOAD_0).addFieldAccess(FieldClass.class.getDeclaredField("testA"), true)
				.addCode(ALOAD_0).addInvoke(FieldClass.class.getDeclaredMethod("doNothing"), true)
				.addCode(DUP2, DADD)
				.addFieldAccess(FieldClass.class.getDeclaredField("testB"), true)
				.addCode(I2D, DADD, DUP2, D2I)
				.addFieldAccess(FieldClass.class.getDeclaredField("testB"), false)
				.addFlowBreakingCode(DRETURN, 33);
		assertNotNull(mb.toString());
		System.out.println(mb);

		final FieldClass instance = mb.build(name, FieldClass.class, classLoader).getConstructor().newInstance();

		assertEquals(3.2 + 3.2 + 5, instance.calculate(), 0.0001);
		assertEquals(11, FieldClass.testB);
	}

	@Test
	void allOpcodes() throws ReflectiveOperationException {
		for (int i = 0; i < 256; i++) {
			try {
				final MethodBuilder mb = new MethodBuilder().addCode((byte)i, B0, B0, B0, B0, B0);
				assertNotNull(mb.toString());
			} catch (final RuntimeException e) {
				// Many failures expected
			}

			try {
				final MethodBuilder mb = new MethodBuilder().addCode((byte)i);
				assertNotNull(mb.toString());
			} catch (final RuntimeException e) {
				// Many failures expected
			}

			try {
				final MethodBuilder mb = new MethodBuilder().addCode(WIDE, (byte)i, B0, B0, B0, B0, B0);
				assertNotNull(mb.toString());
				mb.addCode(WIDE, (byte)i);
				assertNotNull(mb.toString());
			} catch (final RuntimeException e) {
				// Many failures expected
			}

			try {
				final MethodBuilder mb = new MethodBuilder().addFlowBreakingCode((byte)i, 0);
				assertNotNull(mb.toString());
			} catch (final RuntimeException e) {
				// Many failures expected
			}

			try {
				final MethodBuilder mb = new MethodBuilder();
				final Label label = mb.newLabel();
				assertNotNull(mb.addBranch((byte)i, label).updateLabel(label).toString());
			} catch (final RuntimeException e) {
				// Many failures expected
			}
		}
	}

	@Test
	void allPrimitiveConversions() throws ReflectiveOperationException {
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
			final MethodBuilder mb = new MethodBuilder();
			assertThrows(IllegalArgumentException.class, () -> mb.addPrimitiveConversion(Object.class, to));
		}

		// Test Number.class to Boolean.class
		final String name = getClass().getName() + "$" + CLASS_COUNTER.getAndIncrement();
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
				.pushConstant("success").addFlowBreakingCode(ARETURN, 0).updateLabel(fail).addThrow(RuntimeException.class, null, 0);
		final SimpleInterface instance = mb.build(name, SimpleInterface.class, classLoader).getConstructor().newInstance();
		assertEquals("success", instance.run());
	}

	@Test
	void testAppendSelf() {
		final MethodBuilder mb = new MethodBuilder();
		assertThrows(IllegalArgumentException.class, () -> mb.append(mb));
	}

	public interface MultipleMethodInterface {
		public void method1();

		public void method2();
	}

	@Test
	void testBadBase() throws ReflectiveOperationException {
		final MethodBuilder mb = new MethodBuilder().addFlowBreakingCode(RETURN, 0);
		assertThrows(IllegalArgumentException.class, () -> mb.build("BadBase", MultipleMethodInterface.class, classLoader));
	}

	public abstract static class MultipleAbstractMethodClass {
		public abstract void method1();

		public abstract void method2();
	}

	@Test
	void testBadBase2() throws ReflectiveOperationException {
		final MethodBuilder mb = new MethodBuilder().addFlowBreakingCode(RETURN, 0);
		assertThrows(IllegalArgumentException.class, () -> mb.build("BadBase2", MultipleAbstractMethodClass.class, classLoader));
	}

	public static class MultipleMethodClass {
		public void method1() {
			/* Empty test method */
		}

		public void method2() {
			/* Empty test method */
		}
	}

	@Test
	void testBadBase3() throws ReflectiveOperationException {
		final MethodBuilder mb = new MethodBuilder().addFlowBreakingCode(RETURN, 0);
		assertThrows(IllegalArgumentException.class, () -> mb.build("BadBase3", MultipleMethodClass.class, classLoader));
	}

	public static class NoMethodClass {
	}

	@Test
	void testBadBase4() throws ReflectiveOperationException {
		final MethodBuilder mb = new MethodBuilder().addFlowBreakingCode(RETURN, 0);
		assertThrows(IllegalArgumentException.class, () -> mb.build("BadBase4", NoMethodClass.class, classLoader));
	}

	public static final class FinalClass {
		public void method1() {
			/* Empty test method */
		}
	}

	@Test
	void testBadBase5() throws ReflectiveOperationException {
		final MethodBuilder mb = new MethodBuilder().addFlowBreakingCode(RETURN, 0);
		assertThrows(IllegalArgumentException.class, () -> mb.build("BadBase5", FinalClass.class, classLoader));
	}

	@Test
	void testBadIndex() {
		final MethodBuilder mb = new MethodBuilder();
		assertThrows(IllegalArgumentException.class, () -> mb.addAccess(ALOAD, 65536));
	}

	@Test
	void testBadIndex2() {
		final MethodBuilder mb = new MethodBuilder();
		assertThrows(IllegalArgumentException.class, () -> mb.addAccess(ALOAD, -1));
	}

	@Test
	void testBadNewObject() {
		final MethodBuilder mb = new MethodBuilder();
		assertThrows(IllegalArgumentException.class, () -> mb.pushNewObject(Object.class, new int[256]));
	}

	@Test
	void testBadNewObject2() {
		final MethodBuilder mb = new MethodBuilder();
		assertThrows(IllegalArgumentException.class, () -> mb.pushNewObject(int.class));
	}

	@Test
	void testBadStack() throws ReflectiveOperationException {
		final MethodBuilder mb = new MethodBuilder().addCode(ACONST_NULL);
		assertThrows(IllegalStateException.class, () -> mb.build("BadStack", SimpleInterface.class, classLoader));
	}

	@Test
	void testStaticAdd() throws ReflectiveOperationException {
		assertEquals(5, ((Integer)new MethodBuilder().addCode(ILOAD_0, ILOAD_1, IADD).addFlowBreakingCode(IRETURN, 0).build("StaticAdd", "add", MethodType.methodType(int.class, int.class, int.class), classLoader).invoke(null, 2, 3)).intValue());
	}

	@Test
	void testNonexistantMembers() {
		assertThrows(NoSuchMethodError.class, () -> getConstructor(MethodBuilder.class, NoSuchMethodError.class));
		assertThrows(NoSuchFieldError.class, () -> getField(MethodBuilder.class, "NON_EXISTANT_FIELD"));
		assertThrows(NoSuchMethodError.class, () -> getMethod(MethodBuilder.class, "nonexistantMethod", NoSuchMethodError.class));
	}

}
