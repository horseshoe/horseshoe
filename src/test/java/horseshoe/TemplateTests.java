package horseshoe;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

public class TemplateTests {

	private static final String LS = System.lineSeparator();

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
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Map Test", "{{#[\"Bob\": 45, \"Alice\": 31]}}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}");
		final horseshoe.Settings settings = new horseshoe.Settings();
		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(settings, Collections.emptyMap(), writer);

		assertEquals("Bob is 45 years old." + System.lineSeparator() + "Alice is 31 years old." + System.lineSeparator(), writer.toString());
	}

}
