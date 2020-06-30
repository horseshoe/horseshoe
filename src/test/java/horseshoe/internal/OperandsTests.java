package horseshoe.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class OperandsTests {

	private final Number[] numbers = { (byte)1, (short)2, 3, 4L, 5.0f, 6.0, BigDecimal.valueOf(7.0), BigInteger.valueOf(8), new AtomicInteger(33), new AtomicLong(34L) };

	@Test
	public void testAdd() {
		for (int i = 0; i < numbers.length; i++) {
			for (int j = 0; j < numbers.length; j++) {
				assertEquals(numbers[i].doubleValue() + numbers[j].doubleValue(), ((Number)Operands.add(numbers[i], numbers[j])).doubleValue(), 0.0001);
			}

			assertEquals(numbers[i].doubleValue() + ' ', ((Number)Operands.add(numbers[i], ' ')).doubleValue(), 0.0001);
			assertEquals(' ' + numbers[i].doubleValue(), ((Number)Operands.add(' ', numbers[i])).doubleValue(), 0.0001);

			assertEquals(numbers[i] + " ", Operands.add(numbers[i], " ").toString());
			assertEquals(" " + numbers[i], Operands.add(" ", numbers[i]).toString());

			assertEquals(numbers[i] + " ", Operands.add(numbers[i], new StringBuilder(" ")).toString());
			assertEquals(" " + numbers[i], Operands.add(new StringBuilder(" "), numbers[i]).toString());

			final int k = i;

			assertThrows(IllegalArgumentException.class, () -> Operands.add(numbers[k], new Object()));
			assertThrows(IllegalArgumentException.class, () -> Operands.add(new Object(), numbers[k]));

			assertThrows(IllegalArgumentException.class, () -> Operands.add(numbers[k], null));
			assertThrows(IllegalArgumentException.class, () -> Operands.add(null, numbers[k]));
		}

		assertEquals(' ' + ' ', ((Number)Operands.add(' ', ' ')).intValue());
		assertEquals(" " + ' ', Operands.add(" ", ' ').toString());
		assertEquals(' ' + " ", Operands.add(' ', " ").toString());

		final Map<Integer, Integer> expectedMap = new LinkedHashMap<>();

		expectedMap.put(1, 1);
		expectedMap.put(2, 3);

		assertEquals(Arrays.asList(1, 2, 3), Operands.add(Arrays.asList(1), Arrays.asList(2, 3)));
		assertEquals(expectedMap, Operands.add(Arrays.asList(1), Collections.singletonMap(2, 3)));
		assertEquals(expectedMap, Operands.add(Collections.singletonMap(1, 1), Collections.singletonMap(2, 3)));
		assertEquals(expectedMap, Operands.add(Collections.singletonMap(2, 3), Arrays.asList(1)));

		assertThrows(IllegalArgumentException.class, () -> Operands.add(Arrays.asList(1, 2, 3), null));
		assertThrows(IllegalArgumentException.class, () -> Operands.add(null, Arrays.asList(1, 2, 3)));

		assertThrows(IllegalArgumentException.class, () -> Operands.add(Collections.singletonMap(1, 2), null));
		assertThrows(IllegalArgumentException.class, () -> Operands.add(null, Collections.singletonMap(1, 2)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadCompare() throws ReflectiveOperationException {
		assertNotEquals(0, Operands.compare(false, 5, "5"));
	}

	@Test(expected = ClassCastException.class)
	public void testBadCompare2() throws ReflectiveOperationException {
		assertNotEquals(0, Operands.compare(false, "5", 5));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadCompare3() throws ReflectiveOperationException {
		assertNotEquals(0, Operands.compare(false, new Object(), new Object()));
	}

	@Test
	public void testBadHorseshoeNumber() throws ReflectiveOperationException {
		assertThrows(IllegalArgumentException.class, () -> HorseshoeNumber.ofUnknown(null));
		assertThrows(IllegalArgumentException.class, () -> HorseshoeNumber.ofUnknown("Bad"));
	}

	@Test
	public void testCompare() {
		final Object[] notEqual = { (byte)1, (short)2, 3, 4L, 5.0f, 6.0, BigDecimal.valueOf(7.0), BigInteger.valueOf(8), ' ', new AtomicInteger(33), new AtomicLong(34L) };

		for (int i = 0; i < notEqual.length; i++) {
			for (int j = 0; j < notEqual.length; j++) {
				if (i == j) {
					assertEquals(0, Operands.compare(true, notEqual[i], notEqual[j]));
				} else if (i < j) {
					assertTrue(Operands.compare(true, notEqual[i], notEqual[j]) < 0);
				} else {
					assertTrue(Operands.compare(true, notEqual[i], notEqual[j]) > 0);
				}
			}

			assertNotEquals(0, Operands.compare(true, notEqual[i], new Date(0)));
			assertNotEquals(0, Operands.compare(true, notEqual[i], null));
			assertNotEquals(0, Operands.compare(true, null, notEqual[i]));
		}

		final Object[] equal = { (byte)32, (short)32, 32, 32L, 32.0f, 32.0, BigDecimal.valueOf(32.0), BigInteger.valueOf(32), ' ', new AtomicInteger(32), new AtomicLong(32L) };

		for (int i = 0; i < equal.length; i++) {
			for (int j = 0; j < equal.length; j++) {
				assertEquals(0, Operands.compare(true, equal[i], equal[j]));
			}
		}

		assertNotEquals(0, Operands.compare(true, 5, "5"));
		assertNotEquals(0, Operands.compare(true, "5", 5));
		assertTrue(Operands.compare(false, "a", "b") < 0);
		assertTrue(Operands.compare(false, "2", "1") > 0);
		assertEquals(0, Operands.compare(true, new Date(0), new Date(0)));
		assertNotEquals(0, Operands.compare(true, new Date(0), new Date(1)));
		assertTrue(Operands.compare(false, new Date(0), new Date(1)) < 0);
		assertTrue(Operands.compare(false, new Date(1), new Date(0)) > 0);

		final Object[] stringEquivalents = { new StringBuilder().append("5"), "5", '5' };

		for (int i = 0; i < stringEquivalents.length; i++) {
			for (int j = 0; j < stringEquivalents.length; j++) {
				assertEquals(0, Operands.compare(true, stringEquivalents[i], stringEquivalents[j]));
			}
		}
	}

	@Test
	public void testConvertToBoolean() {
		for (final Object object : new Object[] { true, (byte)1, (short)2, 3, 4L, 5.0f, 6.0, BigDecimal.valueOf(7.0), BigInteger.valueOf(8), ' ', new AtomicInteger(33), new AtomicLong(34L), "", "a" }) {
			assertTrue(Operands.convertToBoolean(object));
		}

		for (final Object object : new Object[] { false, (byte)0, (short)0, 0, 0L, 0.0f, 0.0, BigDecimal.valueOf(0.0), BigInteger.valueOf(0), '\0', new AtomicInteger(0), new AtomicLong(0L), null }) {
			assertFalse(Operands.convertToBoolean(object));
		}
	}

	@Test
	public void testDivide() {
		for (int i = 0; i < numbers.length; i++) {
			for (int j = 0; j < numbers.length; j++) {
				assertEquals(numbers[i].doubleValue() / numbers[j].doubleValue(), HorseshoeNumber.ofUnknown(numbers[i]).divide(HorseshoeNumber.ofUnknown(numbers[j])).doubleValue(), Math.abs(numbers[i].doubleValue() / numbers[j].doubleValue() - numbers[i].longValue() / numbers[j].longValue()) + 0.0001);
			}

			assertEquals(numbers[i].doubleValue() / ' ', HorseshoeNumber.ofUnknown(numbers[i]).divide(HorseshoeNumber.ofUnknown(' ')).doubleValue(), Math.abs((numbers[i].doubleValue() / ' ') % 1.0) + 0.0001);
			assertEquals(' ' / numbers[i].doubleValue(), HorseshoeNumber.ofUnknown(' ').divide(HorseshoeNumber.ofUnknown(numbers[i])).doubleValue(), Math.abs((' ' / numbers[i].doubleValue()) % 1.0) + 0.0001);
		}

		assertEquals(' ' / ' ', HorseshoeNumber.ofUnknown(' ').divide(HorseshoeNumber.ofUnknown(' ')).intValue());
	}

	@Test
	public void testHorseshoeFloat() {
		assertEquals(5, HorseshoeFloat.of(5.6).intValue());
		assertEquals(5, HorseshoeFloat.of(5.6).longValue());
		assertEquals(5.6f, HorseshoeFloat.of(5.6).floatValue(), 0);
		assertEquals(-5.6f, HorseshoeFloat.of(5.6).negate().floatValue(), 0);
		assertEquals(HorseshoeFloat.of(5.6), HorseshoeFloat.of(5.6));
		assertNotEquals(HorseshoeFloat.of(5.7), HorseshoeFloat.of(5.6));
		assertNotEquals(HorseshoeFloat.of(5.6), "5.6");
		assertEquals(HorseshoeInt.of(5), HorseshoeFloat.of(5));
		assertEquals(HorseshoeFloat.of(5).hashCode(), HorseshoeFloat.of(5).hashCode());
		assertTrue(HorseshoeFloat.of(5.6).toBoolean());
		assertFalse(HorseshoeFloat.of(0).toBoolean());
		assertFalse(HorseshoeFloat.of(Double.NaN).toBoolean());
	}

	@Test
	public void testHorseshoeInt() {
		for (final Number number : new Number[] { (byte)1, (short)2, 3, 4L, BigInteger.valueOf(8), new AtomicInteger(33), new AtomicLong(34L) }) {
			assertEquals(number.intValue(), HorseshoeInt.ofUnknown(number).longValue());
		}

		assertEquals(' ', HorseshoeInt.ofUnknown(' ').longValue());
		assertThrows(IllegalArgumentException.class, () -> HorseshoeInt.ofUnknown(null));
		assertThrows(IllegalArgumentException.class, () -> HorseshoeInt.ofUnknown(5.6));

		assertEquals(-5, HorseshoeInt.of(-5).intValue());
		assertEquals(-5, HorseshoeInt.of(-5L).intValue());
		assertEquals(-5f, HorseshoeInt.of(-5).floatValue(), 0);
		assertEquals(-5f, HorseshoeInt.of(-5L).floatValue(), 0);
		assertEquals(1, HorseshoeInt.of(3).and(HorseshoeInt.of(-3)).intValue());
		assertEquals(1, HorseshoeInt.of(3).and(HorseshoeInt.of(-3)).longValue());
		assertEquals(1, HorseshoeInt.of(3).and(HorseshoeInt.of(-3L)).intValue());
		assertEquals(1, HorseshoeInt.of(3).and(HorseshoeInt.of(-3L)).longValue());
		assertEquals(-3, HorseshoeInt.of(-4).or(HorseshoeInt.of(1)).intValue());
		assertEquals(-3, HorseshoeInt.of(-4).or(HorseshoeInt.of(1)).longValue());
		assertEquals(-3, HorseshoeInt.of(-4).or(HorseshoeInt.of(1L)).intValue());
		assertEquals(-3, HorseshoeInt.of(-4).or(HorseshoeInt.of(1L)).longValue());
		assertEquals(1 << 31, HorseshoeInt.of(3).shiftLeft(HorseshoeInt.of(31L)).longValue());
		assertEquals(3L << 31, HorseshoeInt.of(3L).shiftLeft(HorseshoeInt.of(31)).longValue());
		assertEquals(-1 >>> 31, HorseshoeInt.of(-1).shiftRightZero(HorseshoeInt.of(31L)).longValue());
		assertEquals(-1L >>> 31, HorseshoeInt.of(-1L).shiftRightZero(HorseshoeInt.of(31)).longValue());
		assertEquals(2, HorseshoeInt.of(3).xor(HorseshoeInt.of(1)).intValue());
		assertEquals(2, HorseshoeInt.of(3).xor(HorseshoeInt.of(1)).longValue());
		assertEquals(2, HorseshoeInt.of(3).xor(HorseshoeInt.of(1L)).intValue());
		assertEquals(2, HorseshoeInt.of(3).xor(HorseshoeInt.of(1L)).longValue());
		assertEquals(HorseshoeInt.of(5).hashCode(), HorseshoeInt.of(5).hashCode());
		assertEquals(HorseshoeInt.of(5), HorseshoeInt.of(5));
		assertNotEquals(HorseshoeInt.of(5), HorseshoeInt.of(4));
		assertEquals(HorseshoeInt.of(5), HorseshoeFloat.of(5));
		assertNotEquals(HorseshoeInt.of(5), HorseshoeFloat.of(5.6));
		assertNotEquals(HorseshoeInt.of(5), HorseshoeFloat.of(4));
		assertNotEquals(HorseshoeInt.of(5), "5");
		assertTrue(HorseshoeInt.of(1L).toBoolean());
		assertFalse(HorseshoeInt.of(0).toBoolean());
	}

	@Test
	public void testMultiply() {
		for (int i = 0; i < numbers.length; i++) {
			for (int j = 0; j < numbers.length; j++) {
				assertEquals(numbers[i].doubleValue() * numbers[j].doubleValue(), HorseshoeNumber.ofUnknown(numbers[i]).multiply(HorseshoeNumber.ofUnknown(numbers[j])).doubleValue(), 0.0001);
			}

			assertEquals(numbers[i].doubleValue() * ' ', HorseshoeNumber.ofUnknown(numbers[i]).multiply(HorseshoeNumber.ofUnknown(' ')).doubleValue(), 0.0001);
			assertEquals(' ' * numbers[i].doubleValue(), HorseshoeNumber.ofUnknown(' ').multiply(HorseshoeNumber.ofUnknown(numbers[i])).doubleValue(), 0.0001);
		}

		assertEquals(' ' * ' ', HorseshoeNumber.ofUnknown(' ').multiply(HorseshoeNumber.ofUnknown(' ')).intValue());
	}

	@Test
	public void testModulo() {
		for (int i = 0; i < numbers.length; i++) {
			for (int j = 0; j < numbers.length; j++) {
				assertEquals(numbers[i].doubleValue() % numbers[j].doubleValue(), HorseshoeNumber.ofUnknown(numbers[i]).modulo(HorseshoeNumber.ofUnknown(numbers[j])).doubleValue(), 0.0001);
			}

			assertEquals(numbers[i].doubleValue() % ' ', HorseshoeNumber.ofUnknown(numbers[i]).modulo(HorseshoeNumber.ofUnknown(' ')).doubleValue(), 0.0001);
			assertEquals(' ' % numbers[i].doubleValue(), HorseshoeNumber.ofUnknown(' ').modulo(HorseshoeNumber.ofUnknown(numbers[i])).doubleValue(), 0.0001);
		}

		assertEquals(' ' % ' ', HorseshoeNumber.ofUnknown(' ').modulo(HorseshoeNumber.ofUnknown(' ')).intValue());
	}

	@Test
	public void testSubtract() {
		for (int i = 0; i < numbers.length; i++) {
			for (int j = 0; j < numbers.length; j++) {
				assertEquals(numbers[i].doubleValue() - numbers[j].doubleValue(), ((Number)Operands.subtract(numbers[i], numbers[j])).doubleValue(), 0.0001);
			}

			assertEquals(numbers[i].doubleValue() - ' ', ((Number)Operands.subtract(numbers[i], ' ')).doubleValue(), 0.0001);
			assertEquals(' ' - numbers[i].doubleValue(), ((Number)Operands.subtract(' ', numbers[i])).doubleValue(), 0.0001);

			final int k = i;

			assertThrows(IllegalArgumentException.class, () -> Operands.subtract(numbers[k], new Object()));
			assertThrows(IllegalArgumentException.class, () -> Operands.subtract(new Object(), numbers[k]));

			assertThrows(IllegalArgumentException.class, () -> Operands.subtract(numbers[k], null));
			assertThrows(IllegalArgumentException.class, () -> Operands.subtract(null, numbers[k]));
		}

		assertEquals(' ' - ' ', ((Number)Operands.subtract(' ', ' ')).intValue());

		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(' ', new Object()));

		final Map<Integer, Integer> startingMap = new LinkedHashMap<>();

		startingMap.put(1, 1);
		startingMap.put(2, 3);

		assertEquals(Arrays.asList(1, 3), Operands.subtract(Arrays.asList(1, 2, 3), Arrays.asList(2)));
		assertEquals(Arrays.asList(1, 3), Operands.subtract(Arrays.asList(1, 2, 3), Collections.singletonMap(2, 3)));
		assertEquals(Collections.singletonMap(1, 1), Operands.subtract(startingMap, Collections.singletonMap(2, 5)));
		assertEquals(Collections.singletonMap(2, 3), Operands.subtract(startingMap, Arrays.asList(1, 3)));

		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(Arrays.asList(1, 2, 3), null));
		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(null, Arrays.asList(1, 2, 3)));

		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(Collections.singletonMap(1, 2), null));
		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(null, Collections.singletonMap(1, 2)));
	}

}
