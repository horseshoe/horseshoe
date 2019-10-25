package horseshoe;

import static horseshoe.Helper.loadMap;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

public final class PartialsTests {

	@Test
	public void testNestedPartials() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT_ONLY);
		final Template template = new TemplateLoader()
				.add("f", "{{#a}}{{>g}}{{b}}{{/a}}{{^a}}{{x}}{{/a}}")
				.add("g", "{{#x}}{{>f}}{{x}}{{/x}}")
				.load("Test", "{{>g}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("b", 2, "x", false), "x", true), writer);
		Assert.assertEquals("2true", writer.toString());
	}

	@Test
	public void testRecursivePartial() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT_ONLY);
		final Template template = new TemplateLoader()
				.add("f", "{{b}}{{#a}}{{>f}}{{/a}}")
				.load("Test", "{{>f}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("b", 4), "b", 2), "b", 3), writer);
		Assert.assertEquals("324", writer.toString());
	}

}