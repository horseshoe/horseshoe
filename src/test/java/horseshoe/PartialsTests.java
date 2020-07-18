package horseshoe;

import static horseshoe.Helper.loadMap;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public final class PartialsTests {

	private static final String LS = System.lineSeparator();

	@Test
	public void testIndentation() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.put("f", "With a new line!\n")
				.put("g", "A{{!}} simple {{!}}test!\n{{! Should not show up even as empty line. }}\n\t{{>f}}\nAnd another.\n")
				.load("Test", "{{#a}}\t{{>g}}{{/a}}\n{{#a}}\n\t\t{{>g}}\n{{/a}}\n");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("b", 2, "x", false)), writer);
		Assert.assertEquals("\tA simple test!" + LS + "\tWith a new line!" + LS + "And another." + LS + LS + "\t\tA simple test!" + LS + "\t\t\tWith a new line!" + LS + "\t\tAnd another." + LS, writer.toString());
	}

	@Test
	public void testIndentation2() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.put("f", "With a new line!\n")
				.put("g", "A{{!}} simp\n\nle\n {{!}}\ntest!\n{{! Should not show up even as empty line. }}\n\t{{>f}}\nAnd another.\n")
				.load("Test", "{{#a}}\t{{>g}}{{/a}}\n{{#a}}\n\t\t{{>g}}\n{{/a}} ");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("b", 2, "x", false)), writer);
		Assert.assertEquals("\tA simp" + LS + LS + "le" + LS + "test!" + LS + "\tWith a new line!" + LS + "And another." + LS + LS + "\t\tA simp" + LS + LS + "\t\tle" + LS + "\t\ttest!" + LS + "\t\t\tWith a new line!" + LS + "\t\tAnd another." + LS, writer.toString());
	}

	@Test
	public void testFilePartial() throws IOException, LoadException {
		final Path path = Paths.get("DELETE_ME.test");
		final Path path2 = Paths.get("DELETE_ME2.test");

		try {
			Files.write(path, ("{{>" + path2.toAbsolutePath() + "}}" + LS).getBytes(StandardCharsets.UTF_16BE));
			Files.write(path2, ("It Works!" + LS).getBytes(StandardCharsets.UTF_8));
			final TemplateLoader loader = new TemplateLoader();
			loader.put(loader.load(path.toAbsolutePath().normalize(), StandardCharsets.UTF_16BE));
			Assert.assertEquals("It Works!" + LS, loader.load(path, StandardCharsets.UTF_16BE).render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
		} finally {
			Files.delete(path);
			Files.delete(path2);
		}
	}

	@Test
	public void testFilePartial2() throws IOException, LoadException {
		Files.createDirectories(Paths.get("DELETE_ME"));
		final Path path = Paths.get("DELETE_ME/DELETE_ME.test");
		final Path path2 = Paths.get("DELETE_ME2.test");
		final Path pathReload = Paths.get("DELETE_ME/DELETE_ME_RELOAD.test");

		try {
			Files.write(path, ("{{>DELETE_ME2.test}}" + LS).getBytes(StandardCharsets.UTF_16LE));
			Files.write(path2, ("It Works!" + LS).getBytes(StandardCharsets.UTF_8));
			Files.write(pathReload, ("{{>DELETE_ME2.test}}" + LS).getBytes(StandardCharsets.UTF_16LE));

			final TemplateLoader loader = new TemplateLoader(Arrays.asList(Paths.get("bad-directory"), Paths.get(".")));

			Assert.assertEquals("It Works!" + LS, loader.load(path, StandardCharsets.UTF_16LE).render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
			Assert.assertEquals("It Works!" + LS, loader.load(path, StandardCharsets.UTF_16LE).render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
			Assert.assertEquals("It Works!" + LS, loader.load(pathReload, StandardCharsets.UTF_16LE).render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());

			final TemplateLoader loader2 = new TemplateLoader(Arrays.asList(Paths.get("bad-directory"), Paths.get(".")))
					.put(path2);

			Assert.assertEquals("It Works!" + LS, loader2.load(path, StandardCharsets.UTF_16LE).render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());

			Assert.assertEquals("It Works!" + LS, new TemplateLoader(Arrays.asList(Paths.get("bad-directory"), Paths.get("."))).load("Inline Test", "{{>DELETE_ME2.test}}" + LS).render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
		} finally {
			Files.delete(path);
			Files.delete(path2);
			Files.delete(pathReload);
			Files.delete(Paths.get("DELETE_ME"));
		}
	}

	@Test (expected = LoadException.class)
	public void testFilePartialBad() throws IOException, LoadException {
		Files.createDirectories(Paths.get("DELETE_ME"));
		final Path path = Paths.get("DELETE_ME/DELETE_ME.test");
		final Path path2 = Paths.get("DELETE_ME2.test");

		try {
			Files.write(path, ("{{>../DELETE_ME2.test}}" + LS).getBytes(StandardCharsets.UTF_16LE));
			Files.write(path2, ("It Doesn't Work" + LS).getBytes(StandardCharsets.UTF_8));
			Assert.assertEquals("It Works!" + LS, new TemplateLoader().load(path, StandardCharsets.UTF_16LE).render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
		} finally {
			Files.delete(path);
			Files.delete(path2);
			Files.delete(Paths.get("DELETE_ME"));
		}
	}

	@Test
	public void testNestedPartials() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("f", "{{# a }}{{> g }}{{ b }}{{/ a }}{{^ a }}{{ x }}{{/ a }}",
					"g", new StringReader("{{# x }}{{> f }}{{ x }}{{/ x }}"),
					"{{> g }}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("b", 2, "x", false), "x", true), writer);
		Assert.assertEquals("2true", writer.toString());
	}

	@Test
	public void testRecursivePartial() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("f", "{{ b }}{{# a }}{{> f }}{{/ a }}",
					new StringReader("{{> f }}"));
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("b", 4), "b", 2), "b", 3), writer);
		Assert.assertEquals("324", writer.toString());
	}

	@Test
	public void testSectionPartial() throws IOException, LoadException {
		Assert.assertEquals("-a--b--c-", new TemplateLoader().load("f", "-{{>}}-", "Test", "{{#> f }}a{{/ f }}{{#> f }}b{{/}}{{#> f }}c{{/}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	public void testSectionPartial2() throws IOException, LoadException {
		Assert.assertEquals("<p>" + LS + "\tTest" + LS + "paragraph </p>", new TemplateLoader().load("p", "<p>\n\t{{>>}}\n</p>", "{{#> p }}\nTest\nparagraph {{/ p }}\n").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	public void testSectionPartial3() throws IOException, LoadException {
		Assert.assertEquals("<p>" + LS + "\tTest" + LS + "\tparagraph" + LS + "</p><ul><li>Item</li></ul>" + LS, new TemplateLoader().load("p", "<p>\n\t{{>}}\n</p>", "{{#> p }}\nTest\nparagraph\n {{/ p }}<ul><li>Item</li></ul>\n").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	public void testSectionPartial4() throws IOException, LoadException {
		Assert.assertEquals("<p>" + LS + "\tTest" + LS + "paragraph" + LS + "1" + LS + "</p><ul><li>Item</li></ul>" + LS, new TemplateLoader().load("p", "<p>\n\t{{>}}1\n</p>", "{{#> p }}\nTest\nparagraph\n {{/ p }}<ul><li>Item</li></ul>\n").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test(expected = LoadException.class)
	public void testBadRecursivePartial() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.put("f", "{{ b }}{{> Test }}")
				.load("Test", "{{> f }}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("b", 4), "b", 2), "b", 3), writer);
	}

	@Test
	public void testInlinePartials() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("Test", "{{< g }}{{# a }}{{ b }}{{ x }}{{/ a }}{{/ g }}{{> g }}{{# a }}{{> g }}{{/ a }}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("b", 2, "x", false), "x", true), writer);
		Assert.assertEquals("2false", writer.toString());
	}

	@Test
	public void testInlinePartials2() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("Test", "{{< a }}{{ b }}{{ x }}{{/ a }}{{# b }}{{< a }}bad{{/ a }}{{/ b }}{{< g }}{{# a }}{{> a }}{{/ a }}{{/ g }}{{> g }}{{# a }}{{> g }}{{/ a }}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("b", 2, "x", false), "x", true), writer);
		Assert.assertEquals("2false", writer.toString());
	}

	@Test
	public void testInlinePartials3() throws IOException, LoadException {
		Assert.assertEquals("Allow an inline partial without a newline", new TemplateLoader().load("{{< a }}\n an{{/}}\nAllow{{> a }} inline partial without a newline").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
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
	public void testInlineRecursivePartialIndentation2() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("Test", "{{<g}}\n{{x}}:\n{{#a}}\n\t{{>>g}} a\n{{/a}}\n{{/g}}\n\t{{>g}} a\n");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("a", loadMap("a", loadMap("x", 3), "x", 2), "x", 1), writer);
		Assert.assertEquals("\t1:" + LS + "\t2:" + LS + "\t3:" + LS + " a" + LS + " a" + LS + " a" + LS, writer.toString());
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
				.load("f", "bad",
					"f", "{{f.b}}",
					"Test", "{{#f}}{{<f}}{{a}}{{/f}}{{>f}}{{/f}}{{>f}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, loadMap("f", loadMap("a", "123", "b", "456")), writer);
		Assert.assertEquals("123456", writer.toString());
	}

}
