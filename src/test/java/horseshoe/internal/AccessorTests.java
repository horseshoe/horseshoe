package horseshoe.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import horseshoe.Helper;
import horseshoe.LoadException;
import horseshoe.TemplateLoader;
import horseshoe.internal.Accessor.PatternMatcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AccessorTests {

	@SuppressWarnings("serial")
	public static class TestMap extends LinkedHashMap<String, Object> {

		public final String field = "Good";

		public void test(final int i) {
		}

		public void test(final Map<String, Object> map) {
		}

	}

	private interface PrivateInterface {
		String testBad(final int i);

		String testBad();
	}

	interface PublicInterface {
		String test(final int i);

		String test();
	}

	@SuppressWarnings("unused")
	private static class PrivateClass implements PrivateInterface, PublicInterface {

		public static final String FIELD = "Bad";
		public final String field = "Good";

		public static String getName() {
			return FIELD;
		}

		@Override
		public String test(final int i) {
			return field;
		}

		@Override
		public String test() {
			return field;
		}

		@Override
		public String testBad(final int i) {
			return FIELD;
		}

		@Override
		public String testBad() {
			return FIELD;
		}

	}

	private static Map<Object, Object> asMap(final Object... values) {
		final Map<Object, Object> map = new LinkedHashMap<>();

		for (int i = 0; i < values.length; i += 2) {
			map.put(values[i], values[i + 1]);
		}

		return map;
	}

	private static Set<Object> asSet(final Object... values) {
		final Set<Object> set = new LinkedHashSet<>();

		for (int i = 0; i < values.length; i++) {
			set.add(values[i]);
		}

		return set;
	}

	@Test
	void testFields() throws IOException, LoadException {
		assertEquals("", new TemplateLoader().load("Fields", "{{PrivateClassInstance.field}}").render(Helper.loadMap("PrivateClassInstance", new PrivateClass()), new java.io.StringWriter()).toString());
	}

	@Test
	void testInaccessibleMethod() throws IOException, LoadException {
		assertEquals(ManagementFactory.getOperatingSystemMXBean().getArch(), new TemplateLoader().load("Inaccessible Method", "{{ManagementFactory.getOperatingSystemMXBean().getArch()}}").render(Helper.loadMap("ManagementFactory", ManagementFactory.class), new java.io.StringWriter()).toString());
	}

	private static void assertArrayEquals(final Object expected, final Object actual) {
		final Class<?> componentType = expected.getClass().getComponentType();

		if (!componentType.isPrimitive()) {
			Assertions.assertArrayEquals((Object[])expected, (Object[])actual);
		} else if (int.class.equals(componentType)) {
			Assertions.assertArrayEquals((int[])expected, (int[])actual);
		} else if (byte.class.equals(componentType)) {
			Assertions.assertArrayEquals((byte[])expected, (byte[])actual);
		} else if (double.class.equals(componentType)) {
			Assertions.assertArrayEquals((double[])expected, (double[])actual);
		} else if (boolean.class.equals(componentType)) {
			Assertions.assertArrayEquals((boolean[])expected, (boolean[])actual);
		} else if (float.class.equals(componentType)) {
			Assertions.assertArrayEquals((float[])expected, (float[])actual);
		} else if (long.class.equals(componentType)) {
			Assertions.assertArrayEquals((long[])expected, (long[])actual);
		} else if (char.class.equals(componentType)) {
			Assertions.assertArrayEquals((char[])expected, (char[])actual);
		} else {
			Assertions.assertArrayEquals((short[])expected, (short[])actual);
		}
	}

	private static class IterableOnlyCollection<T> implements Iterable<T> {
		private final Collection<T> collection;

		public IterableOnlyCollection(final Collection<T> collection) {
			this.collection = collection;
		}

		public Iterator<T> iterator() {
			return collection.iterator();
		}
	}

	@Test
	void testLookup() {
		for (final Object object : Arrays.asList(new Object[] { new Object(), null }, new int[] { 1, 2 }, new byte[] { -1, -2 }, new double[] { 1.1, 2.2 }, new boolean[] { true, false }, new float[] { 0.1f, 0.2f }, new long[] { 0x100000000L, 2 }, new char[] { '1', '2' }, new short[] { 1, 2 })) {
			final int length = Array.getLength(object);

			assertEquals(Array.get(object, 0), Accessor.lookup(object, 0, false));
			assertEquals(Array.get(object, length - 1), Accessor.lookup(object, -1, false));
			assertArrayEquals(object, Accessor.lookup(object, Arrays.asList(0, -1), false));

			for (final int newLength : new int[] { 10, 12 }) {
				final Object newArray = Array.newInstance(object.getClass().getComponentType(), newLength);

				for (int i = 0; i < newLength; i++) {
					Array.set(newArray, i, Array.get(object, i % length));
				}

				assertArrayEquals(newArray, Accessor.lookup(newArray, new Iterable<Integer>() {
						public Iterator<Integer> iterator() {
							return IntStream.range(0, newLength).boxed().iterator();
						}
					}, false));
			}
		}

		assertEquals(Arrays.asList(2, 3), Accessor.lookup(Arrays.asList(4, 2, 5, 3, 1), Arrays.asList(1, 3), false));
		assertEquals(Arrays.asList(2, 3), Accessor.lookup(Arrays.asList(4, 2, 5, 3, 1), new IterableOnlyCollection<>(Arrays.asList(1, 3)), false));
		assertEquals(asMap(2, 7, 3, 8, 4, null), Accessor.lookup(asMap(14, 9, 2, 7, 4, null, 20, 10, 3, 8, 1, 6), Arrays.asList(2, 3, 4, 5), false));
		assertEquals("Sam I am", Accessor.lookup("I am Sam", Arrays.asList(-3, -2, -1, -4, 0, 1, 2, 3), false));
		assertEquals("Sam I am", Accessor.lookup("I am Sam", new IterableOnlyCollection<>(Arrays.asList(-3, -2, -1, -4, 0, 1, 2, 3)), false));
		assertEquals('I', Accessor.lookup("I am Sam", 0, false));
		assertEquals(asSet(2, 3), Accessor.lookup(asSet(14, 2, 5, 3, 1), Arrays.asList(2, 3, 4), false));

		assertThrows(NullPointerException.class, () -> Accessor.lookup(null, Arrays.asList(2, 3, 4), false));
		assertThrows(ClassCastException.class, () -> Accessor.lookup(new Object(), Arrays.asList(2, 3, 4), false));
		assertThrows(ClassCastException.class, () -> Accessor.lookup("I am Sam", Arrays.asList("Test"), false));
		assertThrows(IndexOutOfBoundsException.class, () -> Accessor.lookup("I am Sam", Arrays.asList(100), false));
		assertThrows(IndexOutOfBoundsException.class, () -> Accessor.lookup("I am Sam", Arrays.asList(-100), false));
		assertNull(Accessor.lookup(null, Arrays.asList("Test"), true));
		assertNull(Accessor.lookup("I am Sam", Arrays.asList("Test"), true));
		assertNull(Accessor.lookup("I am Sam", Arrays.asList(100), true));
		assertNull(Accessor.lookup("I am Sam", Arrays.asList(-100), true));
	}

	@Test
	void testLookupRange() {
		assertArrayEquals(new int[] {2, 5, 3}, (int[])Accessor.lookupRange(new int[] { 4, 2, 5, 3, 1 }, 1, 4, false));
		assertArrayEquals(new int[] {1, 3, 5}, (int[])Accessor.lookupRange(new int[] { 4, 2, 5, 3, 1 }, 4, 1, false));
		assertArrayEquals(new int[] {2, 5, 3, 1}, (int[])Accessor.lookupRange(new int[] { 4, 2, 5, 3, 1 }, 1, null, false));
		assertArrayEquals(new int[] {2, 4}, (int[])Accessor.lookupRange(new int[] { 4, 2, 5, 3, 1 }, 1, Accessor.TO_BEGINNING, false));
		assertEquals(Arrays.asList(2, 5, 3), Accessor.lookupRange(Arrays.asList(4, 2, 5, 3, 1), 1, 4, false));
		assertEquals(Arrays.asList(1, 3, 5), Accessor.lookupRange(Arrays.asList(4, 2, 5, 3, 1), 4, 1, false));
		assertEquals(asMap(2, 7, 3, 8, 4, null), Accessor.lookupRange(asMap(14, 9, 2, 7, 4, null, 20, 10, 3, 8, 1, 6), 2, 6, false));
		assertEquals(asMap(2, 7, 3, 8, 4, null), Accessor.lookupRange(asMap(14, 9, 2, 7, 4, null, 20, 10, 3, 8, 1, 6), 5, 1, false));
		assertEquals("Sam", Accessor.lookupRange("I am Sam!", -4, -1, false));
		assertEquals("ma", Accessor.lookupRange("I am Sam!", 3, 1, false));
		assertEquals(asSet(2, 3), Accessor.lookupRange(asSet(14, 2, 5, 3, 1), 2, 5, false));
		assertEquals(asSet(2, 3), Accessor.lookupRange(asSet(14, 2, 5, 3, 1), 4, 1, false));

		final Set<?> set = asSet("Alpha", "Bet", "Car", "dog", null);
		assertEquals(asSet("Car", "dog"), Accessor.lookupRange(set, "Car", null, false));
		assertEquals(asSet("Alpha", "Bet"), Accessor.lookupRange(set, "Cabin", Accessor.TO_BEGINNING, false));
		assertEquals(asSet("Car"), Accessor.lookupRange(set, "Car", "Cat", false));
		assertEquals(asSet("Car"), Accessor.lookupRange(set, "Car", "Bet", false));
		assertEquals(asSet(), Accessor.lookupRange(set, "Car", "Car", false));

		final Map<?, ?> map = asMap("Alpha", "the male", "Bet", "predicting outcome for $", "Car", "automobile", "dog", "canine", null, "undefined");
		assertEquals(asMap("Car", "automobile", "dog", "canine"), Accessor.lookupRange(map, "Car", null, false));
		assertEquals(asMap("Alpha", "the male", "Bet", "predicting outcome for $"), Accessor.lookupRange(map, "Cabin", Accessor.TO_BEGINNING, false));
		assertEquals(asMap("Car", "automobile"), Accessor.lookupRange(map, "Car", "Cat", false));
		assertEquals(asMap("Car", "automobile"), Accessor.lookupRange(map, "Car", "Bet", false));
		assertEquals(asMap(), Accessor.lookupRange(map, "Car", "Car", false));

		assertThrows(NullPointerException.class, () -> Accessor.lookupRange(null, 0, 1, false));
		assertThrows(ClassCastException.class, () -> Accessor.lookupRange(new Object(), 0, 1, false));
		assertThrows(IndexOutOfBoundsException.class, () -> Accessor.lookupRange("I am Sam!", -10, -1, false));
		assertThrows(IndexOutOfBoundsException.class, () -> Accessor.lookupRange("I am Sam!", 0, -11, false));
		assertThrows(IndexOutOfBoundsException.class, () -> Accessor.lookupRange("I am Sam!", 9, -1, false));
		assertThrows(IndexOutOfBoundsException.class, () -> Accessor.lookupRange("I am Sam!", 0, 10, false));
		assertNull(Accessor.lookupRange(null, 0, 10, true));
		assertNull(Accessor.lookupRange("I am Sam!", 0, 10, true));
		assertEquals("Sam", Accessor.lookupRange("I am Sam!", -4, -1, false));
	}

	@Test
	void testMethod() throws IOException, LoadException {
		assertEquals(", ", new TemplateLoader().load("Method", "{{PrivateClassInstance.testBad()}}, {{PrivateClassInstance.testBad(6)}}").render(Helper.loadMap("PrivateClassInstance", new PrivateClass()), new java.io.StringWriter()).toString());
	}

	@Test
	void testNonexistantMethods() throws IOException, LoadException {
		assertEquals(", , , , , , , ", new TemplateLoader().load("Nonexistant Methods", "{{Object.nonexistantMethod(5)}}, {{TestMapInstance.test(1)}}, {{TestMapInstance.test(1)}}, {{TestMapInstance.test(TestMapInstance)}}, {{TestMapInstance.test('')}}, {{TestMapInstance.nonexistantMethod(5)}}, {{PrivateClassInstance.testBad(5)}}, {{PrivateClassInstance.testBad()}}").render(Helper.loadMap("Object", ManagementFactory.class, "TestMapInstance", new TestMap(), "PrivateClassInstance", new PrivateClass()), new java.io.StringWriter()).toString());
	}

	@Test
	void testPatternMatcher() throws IOException, LoadException {
		final PatternMatcher matcher = PatternMatcher.fromInput(Pattern.compile("(?<first>[A-Z])[a-z]+"), "Test One");

		assertEquals(0, matcher.start());
		assertEquals(0, matcher.start(1));
		assertEquals(4, matcher.end());
		assertEquals(1, matcher.end(1));
		assertEquals("Test", matcher.group());
		assertEquals("T", matcher.group(1));
		assertEquals("T", matcher.group("first"));
		assertEquals(1, matcher.groupCount());

		final Iterator<PatternMatcher> it = matcher.iterator();

		while (it.hasNext()) {
			it.next();
		}

		assertThrows(NoSuchElementException.class, () -> it.next());
	}

	@Test
	void testStaticFields() throws IOException, LoadException {
		assertEquals("Values: (0) " + Byte.MAX_VALUE + ", 1) " + Short.MAX_VALUE + ", 2) " + Integer.MAX_VALUE + ", 3) " + Long.MAX_VALUE + ", 4) " + Float.MAX_VALUE + ", 5) " + Double.MAX_VALUE + ", 6) , , ", new TemplateLoader().load("Static Fields", "Values: {{.badInternal}}{{#Values}}{{#.isFirst}}({{/}}{{.index}}) {{. ?. MAX_VALUE}}{{#.hasNext}}, {{/}}{{/}}, {{Test.FIELD}}, {{Private.FIELD}}").render(Helper.loadMap("Values", Helper.loadList(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Object.class), "Test", TestMap.class, "Private", PrivateClass.class), new java.io.StringWriter()).toString());
	}

	@Test
	void testStaticMethod() throws IOException, LoadException {
		assertEquals("Min: 0, Min: 1.0, Min: 0, Max: 0.0, Max: " + Byte.MAX_VALUE + ", Max: , Name: " + PrivateClass.class.getName(), new TemplateLoader().load("Static Methods", "Min: {{Math.`min:int,int`(Integer.MAX_VALUE, 0)}}, Min: {{Math.min(1.0d, 3.4)}}, Min: {{Math.min(Integer.MAX_VALUE, 0L)}}, Max: {{Math.`max:float,float`(Integer.MIN_VALUE, 0)}}, Max: {{Math.`max:int,int`(Integer.MIN_VALUE, Byte.MAX_VALUE)}}, Max: {{Math.max(Integer.MIN_VALUE, newObject)}}, Name: {{Private.getName()}}").render(Helper.loadMap("Math", Math.class, "Integer", Integer.class, "Byte", Byte.class, "newObject", new Object(), "Private", PrivateClass.class), new java.io.StringWriter()).toString());
	}

}
