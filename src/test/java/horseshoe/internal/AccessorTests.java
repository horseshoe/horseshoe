package horseshoe.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import horseshoe.Helper;
import horseshoe.LoadException;
import horseshoe.Settings;
import horseshoe.TemplateLoader;

import org.junit.Test;

public class AccessorTests {

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

	public interface PublicInterface {
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
	public void testFields() throws IOException, LoadException {
		assertEquals("", new TemplateLoader().load("Fields", "{{PrivateClassInstance.field}}").render(new Settings(), Helper.loadMap("PrivateClassInstance", new PrivateClass()), new java.io.StringWriter()).toString());
	}

	@Test
	public void testInaccessibleMethod() throws IOException, LoadException {
		assertEquals(ManagementFactory.getOperatingSystemMXBean().getArch(), new TemplateLoader().load("Inaccessible Method", "{{ManagementFactory.getOperatingSystemMXBean().getArch()}}").render(new Settings(), Helper.loadMap("ManagementFactory", ManagementFactory.class), new java.io.StringWriter()).toString());
	}

	@Test
	public void testLookup() {
		assertEquals(Arrays.asList(2, 3), Accessor.lookup(new int[] { 4, 2, 5, 3, 1 }, Arrays.asList(1, 3)));
		assertEquals(Arrays.asList(2, 3), Accessor.lookup(Arrays.asList(4, 2, 5, 3, 1), Arrays.asList(1, 3)));
		assertEquals(asMap(2, 7, 3, 8), Accessor.lookup(asMap(14, 9, 2, 7, 5, 10, 3, 8, 1, 6), Arrays.asList(2, 3, 4)));
		assertEquals("Sam I am", Accessor.lookup("I am Sam", Arrays.asList(-3, -2, -1, -4, 0, 1, 2, 3)));
		assertEquals('I', Accessor.lookup("I am Sam", 0));
		assertEquals(asSet(2, 3), Accessor.lookup(asSet(14, 2, 5, 3, 1), Arrays.asList(2, 3, 4)));

		assertArrayEquals(new int[] {2, 5, 3}, (int[])Accessor.lookupRange(new int[] { 4, 2, 5, 3, 1 }, 1, 4));
		assertEquals(Arrays.asList(2, 5, 3), Accessor.lookupRange(Arrays.asList(4, 2, 5, 3, 1), 1, 4));
		assertEquals(asMap(2, 7, 3, 8), Accessor.lookupRange(asMap(14, 9, 2, 7, 5, 10, 3, 8, 1, 6), 2, 5));
		assertEquals("Sam", Accessor.lookupRange("I am Sam!", -4, -1));
		assertEquals(asSet(2, 3), Accessor.lookupRange(asSet(14, 2, 5, 3, 1), 2, 5));
	}

	@Test
	public void testMethod() throws IOException, LoadException {
		assertEquals(", ", new TemplateLoader().load("Method", "{{PrivateClassInstance.testBad()}}, {{PrivateClassInstance.testBad(6)}}").render(new Settings(), Helper.loadMap("PrivateClassInstance", new PrivateClass()), new java.io.StringWriter()).toString());
	}

	@Test
	public void testNonexistantMethods() throws IOException, LoadException {
		assertEquals(", , , , , , , ", new TemplateLoader().load("Nonexistant Methods", "{{Object.nonexistantMethod(5)}}, {{TestMapInstance.test(1)}}, {{TestMapInstance.test(1)}}, {{TestMapInstance.test(TestMapInstance)}}, {{TestMapInstance.test('')}}, {{TestMapInstance.nonexistantMethod(5)}}, {{PrivateClassInstance.testBad(5)}}, {{PrivateClassInstance.testBad()}}").render(new Settings(), Helper.loadMap("Object", ManagementFactory.class, "TestMapInstance", new TestMap(), "PrivateClassInstance", new PrivateClass()), new java.io.StringWriter()).toString());
	}

	@Test
	public void testStaticFields() throws IOException, LoadException {
		assertEquals("Values: (0) " + Byte.MAX_VALUE + ", 1) " + Short.MAX_VALUE + ", 2) " + Integer.MAX_VALUE + ", 3) " + Long.MAX_VALUE + ", 4) " + Float.MAX_VALUE + ", 5) " + Double.MAX_VALUE + ", 6) , , ", new TemplateLoader().load("Static Fields", "Values: {{.badInternal}}{{#Values}}{{#.isFirst}}({{/}}{{.index}}) {{.?.?MAX_VALUE}}{{#.hasNext}}, {{/}}{{/}}, {{Test.FIELD}}, {{Private.FIELD}}").render(new Settings(), Helper.loadMap("Values", Helper.loadList(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Object.class), "Test", TestMap.class, "Private", PrivateClass.class), new java.io.StringWriter()).toString());
	}

	@Test
	public void testStaticMethod() throws IOException, LoadException {
		assertEquals("Min: 0, Min: 1.0, Min: 0, Max: 0.0, Max: " + Byte.MAX_VALUE + ", Max: , Name: " + PrivateClass.class.getName(), new TemplateLoader().load("Static Methods", "Min: {{Math.`min:int,int`(Integer.MAX_VALUE, 0)}}, Min: {{Math.min(1.0d, 3.4)}}, Min: {{Math.min(Integer.MAX_VALUE, 0L)}}, Max: {{Math.`max:float,float`(Integer.MIN_VALUE, 0)}}, Max: {{Math.`max:int,int`(Integer.MIN_VALUE, Byte.MAX_VALUE)}}, Max: {{Math.max(Integer.MIN_VALUE, newObject)}}, Name: {{Private.getName()}}").render(new Settings(), Helper.loadMap("Math", Math.class, "Integer", Integer.class, "Byte", Byte.class, "newObject", new Object(), "Private", PrivateClass.class), new java.io.StringWriter()).toString());
	}

}
