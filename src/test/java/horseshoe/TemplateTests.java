package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class TemplateTests {

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
	public void testOutputMapping() throws java.io.IOException, LoadException {
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Output Mapping", "Good things are happening!\n{{#>&2}}\nThis should output to stderr.\n{{/}}\nGood things are happening again!\n");

		final horseshoe.Settings settings = new horseshoe.Settings();
		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(settings, new java.util.HashMap<>(), writer);

		assertEquals("Good things are happening!" + System.lineSeparator() + "Good things are happening again!" + System.lineSeparator(), writer.toString());
	}

	@Test
	public void testOutputRemapping() throws java.io.IOException, LoadException {
		final String filename = "DELETE_ME.test";
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Output Remapping", filename + "\n{{#>OutputToFile}}\nGood things are happening!\nMore good things!\n{{/}}\n");

		final horseshoe.Settings settings = new horseshoe.Settings();
		final java.io.Writer writers[] = new java.io.Writer[] { new java.io.StringWriter(), null };

		try {
			template.render(settings, new java.util.HashMap<>(), writers[0], new Template.WriterMap() {
				@Override
				public Writer getWriter(final String name) {
					if ("OutputToFile".equals(name)) {
						try {
							return writers[1] = new java.io.FileWriter(writers[0].toString().trim());
						} catch (final IOException e) {
						}
					}

					return null;
				}
			});
		} finally {
			for (final java.io.Writer writer : writers) {
				writer.close();
			}

			assertEquals("Good things are happening!" + System.lineSeparator() + "More good things!" + System.lineSeparator(), String.join(System.lineSeparator(), new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8)));
			Files.delete(Paths.get(filename));
		}
	}

}
