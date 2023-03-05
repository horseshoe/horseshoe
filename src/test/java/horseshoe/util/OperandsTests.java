package horseshoe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class OperandsTests {

	private final Number[] numbers = { (byte)1, (short)2, 3, 4L, 5.0f, 6.0, BigDecimal.valueOf(7.0), BigInteger.valueOf(8), new AtomicInteger(33), new AtomicLong(34L) };

	@Test
	void testAdd() {
		final Object object = new Object();

		for (int i = 0; i < numbers.length; i++) {
			final Number testNumber = numbers[i];

			for (int j = 0; j < numbers.length; j++) {
				assertEquals(testNumber.doubleValue() + numbers[j].doubleValue(), ((Number)Operands.add(testNumber, numbers[j])).doubleValue(), 0.0001);
			}

			assertEquals(testNumber.doubleValue() + ' ', ((Number)Operands.add(testNumber, ' ')).doubleValue(), 0.0001);
			assertEquals(' ' + testNumber.doubleValue(), ((Number)Operands.add(' ', testNumber)).doubleValue(), 0.0001);

			assertEquals(testNumber + " ", Operands.add(testNumber, " ").toString());
			assertEquals(" " + testNumber, Operands.add(" ", testNumber).toString());

			assertThrows(IllegalArgumentException.class, () -> Operands.add(testNumber, object));
			assertThrows(IllegalArgumentException.class, () -> Operands.add(object, testNumber));

			assertThrows(IllegalArgumentException.class, () -> Operands.add(testNumber, null));
			assertThrows(IllegalArgumentException.class, () -> Operands.add(null, testNumber));
		}

		assertEquals(' ' + ' ', ((Number)Operands.add(' ', ' ')).intValue());
		assertEquals(" " + ' ', Operands.add(" ", ' ').toString());
		assertEquals(' ' + " ", Operands.add(' ', " ").toString());

		final Map<Integer, Integer> singletonMap23 = Collections.singletonMap(2, 3);
		final List<Integer> list1 = Arrays.asList(1);
		final LinkedHashSet<Integer> set1 = new LinkedHashSet<>(list1);
		final LinkedHashMap<Integer, Integer> expectedMap = new LinkedHashMap<>();

		expectedMap.put(1, 1);
		expectedMap.put(2, 3);

		assertEquals(Arrays.asList(1, 2, 3), Operands.add(list1, Arrays.asList(2, 3)));
		assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), Operands.add(set1, Arrays.asList(2, 3)));
		assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), Operands.add(list1, new LinkedHashSet<>(Arrays.asList(2, 3, 1))));
		assertEquals(expectedMap, Operands.add(Collections.singletonMap(1, 1), singletonMap23));

		assertThrows(IllegalArgumentException.class, () -> Operands.add(set1, singletonMap23));
		assertThrows(IllegalArgumentException.class, () -> Operands.add(list1, singletonMap23));
		assertThrows(IllegalArgumentException.class, () -> Operands.add(singletonMap23, list1));

		final List<Integer> testList = Arrays.asList(1, 2, 3);
		assertThrows(IllegalArgumentException.class, () -> Operands.add(testList, null));
		assertThrows(IllegalArgumentException.class, () -> Operands.add(null, testList));

		final Collection<Integer> testCollection = Collections.singleton(1);
		assertThrows(IllegalArgumentException.class, () -> Operands.add(testCollection, null));
		assertThrows(IllegalArgumentException.class, () -> Operands.add(null, testCollection));

		final Map<Integer, Integer> testMap = Collections.singletonMap(1, 2);
		assertThrows(IllegalArgumentException.class, () -> Operands.add(testMap, null));
		assertThrows(IllegalArgumentException.class, () -> Operands.add(null, testMap));
	}

	@Test
	void testAddIterables() {
		class Iter<T> implements Iterable<T> {

			private final Iterable<T> iter;

			Iter(final Iterable<T> iter) {
				this.iter = iter;
			}

			@Override
			public Iterator<T> iterator() {
				return iter.iterator();
			}

		}

		// Iterable + Iterable = List
		assertEquals(Arrays.asList(1, 2, 3, 4),
				Operands.add(new Iter<>(Arrays.asList(1, 2)), new Iter<>(Arrays.asList(3, 4))));
		// Iterable + Collection = List
		assertEquals(Arrays.asList(1, 2, 3, 4),
				Operands.add(new Iter<>(Arrays.asList(1, 2)), Arrays.asList(3, 4)));
		// Collection + Iterable = List
		assertEquals(Arrays.asList(1, 2, 3, 4),
				Operands.add(Arrays.asList(1, 2), new Iter<>(Arrays.asList(3, 4))));
		// Iterable + Set = Set
		assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4)),
				Operands.add(new Iter<>(Arrays.asList(1, 2)), new LinkedHashSet<>(Arrays.asList(3, 4))));
		// Set + Iterable = Set
		assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4)),
				Operands.add(new LinkedHashSet<>(Arrays.asList(1, 2)), new Iter<>(Arrays.asList(3, 4))));

		final Map<Integer, Integer> singletonMap12 = Collections.singletonMap(1, 2);
		final Iter<Integer> iter34 = new Iter<>(Arrays.asList(3, 4));

		// Map + Iterable -> exception
		assertThrows(IllegalArgumentException.class, () ->
				Operands.add(singletonMap12, iter34));
		// Iterable + Map -> exception
		assertThrows(IllegalArgumentException.class, () ->
				Operands.add(iter34, singletonMap12));
	}

	@Test
	void testBadCompare() throws ReflectiveOperationException {
		assertThrows(IllegalArgumentException.class, () -> Operands.compare(false, 5, "5"));
	}

	@Test
	void testBadCompare2() throws ReflectiveOperationException {
		assertThrows(ClassCastException.class, () -> Operands.compare(false, "5", 5));
	}

	@Test
	void testBadCompare3() throws ReflectiveOperationException {
		final Object object1 = new Object();
		final Object object2 = new Object();
		assertThrows(IllegalArgumentException.class, () -> Operands.compare(false, object1, object2));
	}

	@Test
	void testBadNumbers() throws ReflectiveOperationException {
		final Number badNumber = new Number() {
			@Override
			public int intValue() {
				return 0;
			}

			@Override
			public long longValue() {
				return 0;
			}

			@Override
			public float floatValue() {
				return 0;
			}

			@Override
			public double doubleValue() {
				return 0;
			}
		};

		assertThrows(IllegalArgumentException.class, () -> Operands.toNumeric(null));
		assertThrows(IllegalArgumentException.class, () -> Operands.toNumeric("Bad"));
		assertThrows(IllegalArgumentException.class, () -> Operands.toNumeric(badNumber));
		assertThrows(IllegalArgumentException.class, () -> Operands.toIntegral(null));
		assertThrows(IllegalArgumentException.class, () -> Operands.toIntegral(5.6));
	}

	@Test
	void testCompare() {
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

		final Object[] stringEquivalents = { "5", '5' };

		for (int i = 0; i < stringEquivalents.length; i++) {
			for (int j = 0; j < stringEquivalents.length; j++) {
				assertEquals(0, Operands.compare(true, stringEquivalents[i], stringEquivalents[j]));
			}
		}
	}

	enum TestCompareEnum {
		Apple,
		Blue
	}

	@Test
	void testCompareEnum() {
		assertEquals(0, Operands.compare(true, TestCompareEnum.Apple, "Apple"));
		assertEquals(0, Operands.compare(true, "Apple", TestCompareEnum.Apple));
		assertNotEquals(0, Operands.compare(true, TestCompareEnum.Blue, "Apple"));
		assertNotEquals(0, Operands.compare(true, "Apple", TestCompareEnum.Blue));
	}

	@Test
	void testConvertToBoolean() {
		for (final Object object : new Object[] { true, (byte)1, (short)2, 3, 4L, 5.0f, 6.0, BigDecimal.valueOf(7.0), BigInteger.valueOf(8), ' ', new AtomicInteger(33), new AtomicLong(34L), "a", new int[1], Collections.singletonList(0), Collections.singletonMap("key", "value"), new Object() }) {
			assertTrue(Operands.convertToBoolean(object));
		}

		for (final Object object : new Object[] { false, (byte)0, (short)0, 0, 0L, 0.0f, 0.0, BigDecimal.valueOf(0.0), BigInteger.valueOf(0), '\0', new AtomicInteger(0), new AtomicLong(0L), "", new int[0], Collections.emptyList(), Collections.emptyMap(), null }) {
			assertFalse(Operands.convertToBoolean(object));
		}
	}

	@Test
	void testDivide() {
		for (int i = 0; i < numbers.length; i++) {
			final Number testNumber = numbers[i];

			for (int j = 0; j < numbers.length; j++) {
				assertEquals(testNumber.doubleValue() / numbers[j].doubleValue(), Operands.divide(Operands.toNumeric(testNumber), Operands.toNumeric(numbers[j])).doubleValue(), Math.abs(testNumber.doubleValue() / numbers[j].doubleValue() - testNumber.longValue() / numbers[j].longValue()) + 0.0001);
			}

			assertEquals(testNumber.doubleValue() / ' ', Operands.divide(Operands.toNumeric(testNumber), Operands.toNumeric(' ')).doubleValue(), Math.abs((testNumber.doubleValue() / ' ') % 1.0) + 0.0001);
			assertEquals(' ' / testNumber.doubleValue(), Operands.divide(Operands.toNumeric(' '), Operands.toNumeric(testNumber)).doubleValue(), Math.abs((' ' / testNumber.doubleValue()) % 1.0) + 0.0001);
		}

		assertEquals(' ' / ' ', Operands.divide(Operands.toNumeric(' '), Operands.toNumeric(' ')).intValue());
	}

	@Test
	void testFloatingPointUnaryOperations() {
		for (int i = 0; i < numbers.length; i++) {
			assertEquals(-numbers[i].doubleValue(), Operands.negate(Operands.toNumeric(numbers[i])).doubleValue(), 0.0);
		}

		assertTrue(Operands.convertToBoolean(Operands.toNumeric(5.6)));
		assertFalse(Operands.convertToBoolean(Operands.toNumeric(0)));
		assertFalse(Operands.convertToBoolean(Operands.toNumeric(Double.NaN)));
	}

	@Test
	void testIntegralOperations() {
		for (final Number number : new Number[] { (byte)1, (short)2, 3, 4L, BigInteger.valueOf(8), new AtomicInteger(33), new AtomicLong(34L) }) {
			assertEquals(number.intValue(), Operands.toIntegral(number).longValue());
			assertEquals(~number.intValue(), Operands.not(Operands.toIntegral(number)).intValue());
		}

		assertEquals(' ', Operands.toIntegral(' ').longValue());
		assertEquals(-5, Operands.toIntegral(-5).intValue());
		assertEquals(-5, Operands.toIntegral(-5L).intValue());
		assertEquals(-5f, Operands.toIntegral(-5).floatValue(), 0);
		assertEquals(-5f, Operands.toIntegral(-5L).floatValue(), 0);
		assertEquals(1, Operands.and(Operands.toIntegral(3), Operands.toIntegral(-3)).intValue());
		assertEquals(1, Operands.and(Operands.toIntegral(3), Operands.toIntegral(-3)).longValue());
		assertEquals(1, Operands.and(Operands.toIntegral(3), Operands.toIntegral(-3L)).intValue());
		assertEquals(1, Operands.and(Operands.toIntegral(3), Operands.toIntegral(-3L)).longValue());
		assertEquals(-3, Operands.or(Operands.toIntegral(-4), Operands.toIntegral(1)).intValue());
		assertEquals(-3, Operands.or(Operands.toIntegral(-4L), Operands.toIntegral(1)).longValue());
		assertEquals(-3, Operands.or(Operands.toIntegral(-4), Operands.toIntegral(1L)).intValue());
		assertEquals(-3, Operands.or(Operands.toIntegral(-4), Operands.toIntegral(1L)).longValue());
		assertEquals(1 << 31, Operands.shiftLeft(Operands.toIntegral(3), Operands.toIntegral(31L)).longValue());
		assertEquals(3L << 31, Operands.shiftLeft(Operands.toIntegral(3L), Operands.toIntegral(31)).longValue());
		assertEquals(-1 >>> 31, Operands.shiftRightZero(Operands.toIntegral(-1), Operands.toIntegral(31L)).longValue());
		assertEquals(-1L >>> 31, Operands.shiftRightZero(Operands.toIntegral(-1L), Operands.toIntegral(31)).longValue());
		assertEquals(2, Operands.xor(Operands.toIntegral(3), Operands.toIntegral(1)).intValue());
		assertEquals(2, Operands.xor(Operands.toIntegral(3L), Operands.toIntegral(1)).longValue());
		assertEquals(2, Operands.xor(Operands.toIntegral(3), Operands.toIntegral(1L)).intValue());
		assertEquals(2, Operands.xor(Operands.toIntegral(3), Operands.toIntegral(1L)).longValue());
		assertEquals(Operands.toIntegral(5).hashCode(), Operands.toIntegral(5).hashCode());
		assertEquals(Operands.toIntegral(5), Operands.toIntegral(5));
		assertNotEquals(Operands.toIntegral(5), Operands.toIntegral(4));
		assertEquals(Operands.toIntegral(5), Operands.toNumeric(5));
		assertNotEquals(Operands.toIntegral(5), Operands.toNumeric(5.6));
		assertNotEquals(Operands.toIntegral(5), Operands.toNumeric(4));
		assertTrue(Operands.convertToBoolean(Operands.toIntegral(1L)));
		assertFalse(Operands.convertToBoolean(Operands.toIntegral(0)));
	}

	@Test
	void testIsIn() {
		assertTrue(Operands.isIn(3, new Iterable<Integer>() {

			@Override
			public Iterator<Integer> iterator() {
				return Arrays.asList(1, 2, 3, 4).iterator();
			}

		}));
		assertFalse(Operands.isIn(5, new Iterable<Integer>() {

			@Override
			public Iterator<Integer> iterator() {
				return Arrays.asList(1, 2, 3, 4).iterator();
			}

		}));
		assertTrue(Operands.isIn(4, Arrays.asList(1, 2, 3, 4)));
		assertFalse(Operands.isIn(4, Arrays.asList(1, 2, 3, 5)));
		assertTrue(Operands.isIn("key", Collections.singletonMap("key", "value")));
		assertFalse(Operands.isIn("value", Collections.singletonMap("key", "value")));
		assertFalse(Operands.isIn(4, new Object()));
	}

	@Test
	void testMultiply() {
		for (int i = 0; i < numbers.length; i++) {
			final Number testNumber = numbers[i];

			for (int j = 0; j < numbers.length; j++) {
				assertEquals(testNumber.doubleValue() * numbers[j].doubleValue(), Operands.multiply(Operands.toNumeric(testNumber), Operands.toNumeric(numbers[j])).doubleValue(), 0.0001);
			}

			assertEquals(testNumber.doubleValue() * ' ', Operands.multiply(Operands.toNumeric(testNumber), Operands.toNumeric(' ')).doubleValue(), 0.0001);
			assertEquals(' ' * testNumber.doubleValue(), Operands.multiply(Operands.toNumeric(' '), Operands.toNumeric(testNumber)).doubleValue(), 0.0001);
		}

		assertEquals(' ' * ' ', Operands.multiply(Operands.toNumeric(' '), Operands.toNumeric(' ')).intValue());
	}

	@Test
	void testModulo() {
		for (int i = 0; i < numbers.length; i++) {
			final Number testNumber = numbers[i];

			for (int j = 0; j < numbers.length; j++) {
				assertEquals(testNumber.doubleValue() % numbers[j].doubleValue(), Operands.modulo(Operands.toNumeric(testNumber), Operands.toNumeric(numbers[j])).doubleValue(), 0.0001);
			}

			assertEquals(testNumber.doubleValue() % ' ', Operands.modulo(Operands.toNumeric(testNumber), Operands.toNumeric(' ')).doubleValue(), 0.0001);
			assertEquals(' ' % testNumber.doubleValue(), Operands.modulo(Operands.toNumeric(' '), Operands.toNumeric(testNumber)).doubleValue(), 0.0001);
		}

		assertEquals(' ' % ' ', Operands.modulo(Operands.toNumeric(' '), Operands.toNumeric(' ')).intValue());
	}

	@Test
	void testSubtract() {
		final Object testObject = new Object();

		for (int i = 0; i < numbers.length; i++) {
			final Number testNumber = numbers[i];

			for (int j = 0; j < numbers.length; j++) {
				assertEquals(testNumber.doubleValue() - numbers[j].doubleValue(), ((Number)Operands.subtract(testNumber, numbers[j])).doubleValue(), 0.0001);
			}

			assertEquals(testNumber.doubleValue() - ' ', ((Number)Operands.subtract(testNumber, ' ')).doubleValue(), 0.0001);
			assertEquals(' ' - testNumber.doubleValue(), ((Number)Operands.subtract(' ', testNumber)).doubleValue(), 0.0001);

			assertThrows(IllegalArgumentException.class, () -> Operands.subtract(testNumber, testObject));
			assertThrows(IllegalArgumentException.class, () -> Operands.subtract(testObject, testNumber));

			assertThrows(IllegalArgumentException.class, () -> Operands.subtract(testNumber, null));
			assertThrows(IllegalArgumentException.class, () -> Operands.subtract(null, testNumber));
		}

		assertEquals(' ' - ' ', ((Number)Operands.subtract(' ', ' ')).intValue());

		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(' ', testObject));

		final LinkedHashMap<Integer, Integer> startingMap = new LinkedHashMap<>();

		startingMap.put(1, 1);
		startingMap.put(2, 3);

		assertEquals(Arrays.asList(1, 3), Operands.subtract(Arrays.asList(1, 2, 3), Arrays.asList(2)));
		assertEquals(Arrays.asList(1, 3), Operands.subtract(Arrays.asList(1, 2, 3), Collections.singletonMap(2, 3)));
		assertEquals(new LinkedHashSet<>(Arrays.asList(1, 3)), Operands.subtract(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), Arrays.asList(2)));
		assertEquals(new LinkedHashSet<>(Arrays.asList(1, 3)), Operands.subtract(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), Collections.singletonMap(2, 3)));
		assertEquals(Collections.singletonMap(1, 1), Operands.subtract(startingMap, Collections.singletonMap(2, 5)));
		assertEquals(Collections.singletonMap(2, 3), Operands.subtract(startingMap, Arrays.asList(1, 3)));

		final List<Integer> testList = Arrays.asList(1, 2, 3);
		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(testList, null));
		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(null, testList));

		final Collection<Integer> testCollection = Collections.singleton(1);
		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(testCollection, null));
		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(null, testCollection));

		final Map<Integer, Integer> testMap = Collections.singletonMap(1, 2);
		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(testMap, null));
		assertThrows(IllegalArgumentException.class, () -> Operands.subtract(null, testMap));
	}

}
