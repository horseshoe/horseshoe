package horseshoe;

import static horseshoe.Helper.loadMap;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

public final class PartialsTests {

	private static final String LS = System.lineSeparator();

	@Test
	public void testIndentation() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.add("f", "With a new line!\n")
				.add("g", "A{{!}} simple {{!}}test!\n{{! Should not show up even as empty line. }}\n\t{{>f}}\nAnd another.\n")
				.load("Test", "{{#a}}\t{{>g}}{{/a}}\n{{#a}}\n\t\t{{>g}}\n{{/a}}\n");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("b", 2, "x", false)), writer);
		Assert.assertEquals("\tA simple test!" + LS + "\tWith a new line!" + LS + "And another." + LS + LS + "\t\tA simple test!" + LS + "\t\t\tWith a new line!" + LS + "\t\tAnd another." + LS, writer.toString());
	}

	@Test
	public void testNestedPartials() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
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
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.add("f", "{{b}}{{#a}}{{>f}}{{/a}}")
				.load("Test", "{{>f}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("b", 4), "b", 2), "b", 3), writer);
		Assert.assertEquals("324", writer.toString());
	}

	@Test(expected = StackOverflowError.class)
	public void testBadRecursivePartial() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.add("f", "{{b}}{{>Test}}")
				.load("Test", "{{>f}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("b", 4), "b", 2), "b", 3), writer);
	}

	@Test
	public void testInlinePartials() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("Test", "{{<g}}{{#a}}{{b}}{{x}}{{/a}}{{/g}}{{>g}}{{#a}}{{>g}}{{/a}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("b", 2, "x", false), "x", true), writer);
		Assert.assertEquals("2false", writer.toString());
	}

	@Test
	public void testInlineRecursivePartialIndentation() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("Test", "{{<g}}\n{{x}}:\n{{#a}}\n\t{{>g}}\n{{/a}}\n{{/g}}\n\t{{>g}}\n");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("x", 3), "x", 2), "x", 1), writer);
		Assert.assertEquals("\t1:" + LS + "\t\t2:" + LS + "\t\t\t3:" + LS, writer.toString());
	}

	@Test
	public void testInlineRecursivePartial() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("Test", "{{<f}}\n{{b}}\n{{#a}}\n{{>f}}\n{{/a}}\n{{/f}}\n{{>f}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("b", 4), "b", 2), "b", 3), writer);
		Assert.assertEquals("3" + LS + "2" + LS + "4" + LS, writer.toString());
	}

	@Test(expected = StackOverflowError.class)
	public void testInlineBadRecursivePartial() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("Test", "{{<f}}\n{{b}}{{>Test}}\n{{/f}}\n{{>f}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("b", 4), "b", 2), "b", 3), writer);
	}

	@Test(expected = LoadException.class)
	public void testInlineBadPartial() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("Test", "{{#a}}{{<f}}\n{{b}}{{>Test}}\n{{/f}}{{/a}}\n{{>f}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("b", 4), "b", 2), "b", 3), writer);
	}

	@Test
	public void testClashingPartialsAndData() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.add("f", "{{f.b}}")
				.load("Test", "{{#f}}{{<f}}{{a}}{{/f}}{{>f}}{{/f}}{{>f}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("f", loadMap("a", "123", "b", "456")), writer);
		Assert.assertEquals("123456", writer.toString());
	}

}
