package horseshoe.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

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

	@Test
	public void testFields() throws IOException, LoadException {
		assertEquals("", new TemplateLoader().load("Fields", "{{PrivateClassInstance.field}}").render(new Settings(), Helper.loadMap("PrivateClassInstance", new PrivateClass()), new java.io.StringWriter()).toString());
	}

	@Test
	public void testInaccessibleMethod() throws IOException, LoadException {
		assertEquals(ManagementFactory.getOperatingSystemMXBean().getArch(), new TemplateLoader().load("Inaccessible Method", "{{ManagementFactory.getOperatingSystemMXBean().getArch()}}").render(new Settings(), Helper.loadMap("ManagementFactory", ManagementFactory.class), new java.io.StringWriter()).toString());
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
