package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.Test;

public class TemplateTests {

	private static final String LS = System.lineSeparator();

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
	public void testDuplicateSection() throws java.io.IOException, LoadException {
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Duplicate Section", "Names:\n{{#people}}\n - {{lastName}}, {{firstName}}\n{{/}}\n\nMailing Labels:\n{{#}}\n{{firstName}} {{lastName}}\n{{address}}\n{{city}}, {{state}} {{zip}}\n{{#.hasNext}}\n\n{{/}}\n{{/}}\n");

		final horseshoe.Settings settings = new horseshoe.Settings();
		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(settings, Helper.loadMap("people", Helper.loadList(Helper.loadMap("firstName", "John", "lastName", "Doe", "address", "101 1st St", "city", "Seattle", "state", "WA", "zip", 98101), Helper.loadMap("firstName", "Jane", "lastName", "Doey", "address", "202 2nd St", "city", "Miami", "state", "FL", "zip", 33255))), writer);

		assertEquals("Names:" + LS + " - Doe, John" + LS + " - Doey, Jane" + LS + LS + "Mailing Labels:" + LS + "John Doe" + LS + "101 1st St" + LS + "Seattle, WA 98101" + LS + LS + "Jane Doey" + LS + "202 2nd St" + LS + "Miami, FL 33255" + LS, writer.toString());
	}

	@Test
	public void testOutputMapping() throws java.io.IOException, LoadException {
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Output Mapping", "Good things are happening!\n{{#@StdErr}}\nThis should output to stderr.\n{{/}}\nGood things are happening again!\n");
		final horseshoe.Settings settings = new horseshoe.Settings();
		final java.io.StringWriter writer = new java.io.StringWriter();

		template.render(settings, new java.util.HashMap<>(), writer);

		assertEquals("Good things are happening!" + LS + "Good things are happening again!" + LS, writer.toString());
	}

	@Test
	public void testOutputRemapping() throws java.io.IOException, LoadException {
		final String filename = "DELETE_ME.test";
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Output Remapping", "{{#@File=\"" + filename + "\"}}\nGood things are happening!\nMore good things!\n{{/}}\n");
		final horseshoe.Settings settings = new horseshoe.Settings();
		final java.io.StringWriter writer = new java.io.StringWriter();

		try {
			template.render(settings, new java.util.HashMap<>(), writer, new HashMap<String, Template.AnnotationProcessor>() {
				private static final long serialVersionUID = 1L;

				{
					put("File", new Template.AnnotationProcessor() {
						@Override
						public Writer getWriter(final Writer writer, final Object value) throws IOException {
							return new FileWriter(value.toString());
						}

						@Override
						public void returnWriter(final Writer writer) throws IOException {
							writer.close();
						}
					});
				}
			});
		} finally {
			assertEquals("Good things are happening!" + LS + "More good things!" + LS, String.join(LS, new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8)));
			Files.delete(Paths.get(filename));
		}
	}

	@Test
	public void testBadAnnotation() throws java.io.IOException, LoadException {
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Bad Annotation", "{{#@BadAnnotation=\"blah\"}}\nGood things are happening!\nMore good things!\n{{^}}\n{{#@StdErr}}\nEngine does not support @BadAnnotation.\n{{/}}\n{{/@BadAnnotation}}\n");
		final horseshoe.Settings settings = new horseshoe.Settings();
		final java.io.StringWriter writer = new java.io.StringWriter();

		template.render(settings, new java.util.HashMap<>(), writer);
	}

}
