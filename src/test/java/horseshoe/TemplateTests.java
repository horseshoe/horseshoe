package horseshoe;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

public class TemplateTests {

	private static final String LS = System.lineSeparator();

	@Test
	public void testBackreach() throws java.io.IOException, LoadException {
		assertEquals("Original String" + LS, new horseshoe.TemplateLoader().load("Backreach", "{{#\"Original String\"}}\n{{#charAt(1)}}\n{{..}}\n{{/}}\n{{/}}").render(new horseshoe.Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("Original String" + LS, new horseshoe.TemplateLoader().load("Backreach", "{{#\"Original String\"}}\n{{#charAt(1)}}\n{{.././toString()}}\n{{/}}\n{{/}}").render(new horseshoe.Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	public void testDuplicateSection() throws java.io.IOException, LoadException {
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Duplicate Section", "Names:\n{{#people}}\n - {{lastName}}, {{firstName}}\n{{/}}\n\nMailing Labels:\n{{#}}\n{{firstName}} {{lastName}}\n{{address}}\n{{city}}, {{state}} {{zip}}\n{{#.hasNext}}\n\n{{/}}\n{{/}}\n");

		final horseshoe.Settings settings = new horseshoe.Settings();
		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(settings, Helper.loadMap("people", Helper.loadList(Helper.loadMap("firstName", "John", "lastName", "Doe", "address", "101 1st St", "city", "Seattle", "state", "WA", "zip", 98101), Helper.loadMap("firstName", "Jane", "lastName", "Doey", "address", "202 2nd St", "city", "Miami", "state", "FL", "zip", 33255))), writer);

		assertEquals("Names:" + LS + " - Doe, John" + LS + " - Doey, Jane" + LS + LS + "Mailing Labels:" + LS + "John Doe" + LS + "101 1st St" + LS + "Seattle, WA 98101" + LS + LS + "Jane Doey" + LS + "202 2nd St" + LS + "Miami, FL 33255" + LS, writer.toString());
	}

	/**
	 * This test evaluates the example code given in the {@link Template} javadoc and the README markdown file. Any changes to this code should be updated in those locations as well.
	 */
	@Test
	public void testExample() throws java.io.IOException, LoadException {
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Hello World", "{{{salutation}}}, {{ recipient }}!");
		// final horseshoe.Template mustacheTemplate = horseshoe.TemplateLoader.newMustacheLoader().load("Hello World", "{{{salutation}}}, {{ recipient }}!");

		final java.util.Map<String, Object> data = new java.util.HashMap<>();
		data.put("salutation", "Hello");
		data.put("recipient", "world");

		final horseshoe.Settings settings = new horseshoe.Settings();
		// final horseshoe.Settings mustacheSettings = horseshoe.Settings.newMustacheSettings();
		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(settings, data, writer);

		assertEquals("Hello, world!", writer.toString());
	}

	@Test
	public void testMapLiteral() throws java.io.IOException, LoadException {
		assertEquals("Bob is 45 years old." + LS + "Alice is 31 years old." + LS + "Jim is 80 years old." + LS, new horseshoe.TemplateLoader().load("Map Test", "{{#{\"Bob\": 45, \"Alice\": 31, \"Jim\": 80} }}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}").render(new horseshoe.Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("Bob is 45 years old." + LS + "Alice is 31 years old." + LS + "Jim is 80 years old." + LS, new horseshoe.TemplateLoader().load("Map Test", "{{#\"Bob\": 45, \"Alice\": 31, \"Jim\": 80}}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}").render(new horseshoe.Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("Bob is 45 years old." + LS + "Alice is 31 years old." + LS + "Jim is 80 years old." + LS, new horseshoe.TemplateLoader().load("Map Test", "{{#(\"Bob\": 45, \"Alice\": 31, \"Jim\": 80)}}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}").render(new horseshoe.Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	public void testNamedExpressions() throws java.io.IOException, LoadException {
		assertEquals("ORIGINAL STRING" + LS, new horseshoe.TemplateLoader().load("Upper", "{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n{{#charAt(1)}}\n{{upper(..)}}\n{{/}}\n{{/}}").render(new horseshoe.Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
		assertEquals(LS + "ORIGINAL STRING-original string" + LS, new horseshoe.TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n{{>a}}\n{{upper() + \"-\" + lower()}}\n{{/}}").render(new horseshoe.Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
	}

}
