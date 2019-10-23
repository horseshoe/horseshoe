package horseshoe;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExampleTest {

	@Test
	public void testExample() throws java.io.IOException, LoadException {
		final horseshoe.Settings settings = new horseshoe.Settings();
		// final horseshoe.Settings mustacheSettings = horseshoe.Settings.newMustacheSettings();
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Hello World", "{{{salutation}}}, {{ recipient }}!", settings);

		final java.util.Map<String, Object> data = new java.util.HashMap<>();
		data.put("salutation", "Hello");
		data.put("recipient", "world");

		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(settings, data, writer);
		assertEquals("Hello, world!", writer.toString());
	}

}
