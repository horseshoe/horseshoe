package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

public class TemplateTests {

	private static final String LS = System.lineSeparator();

	@Test
	public void testBackreach() throws IOException, LoadException {
		assertEquals("Original String" + LS, new TemplateLoader().load("Backreach", "{{#\"Original String\"}}\n{{#charAt(1)}}\n{{..}}\n{{/}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("Original String" + LS, new TemplateLoader().load("Backreach", "{{#\"Original String\"}}\n{{#charAt(1)}}\n{{.././toString()}}\n{{/}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	public void testDuplicateSection() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Duplicate Section", "Names:\n{{#people}}\n - {{lastName}}, {{firstName}}\n{{/}}\n\nMailing Labels:\n{{#}}\n{{firstName}} {{lastName}}\n{{address}}\n{{city}}, {{state}} {{zip}}\n{{#.hasNext}}\n\n{{/}}\n{{/}}\n");

		final Settings settings = new Settings();
		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(settings, Helper.loadMap("people", Helper.loadList(Helper.loadMap("firstName", "John", "lastName", "Doe", "address", "101 1st St", "city", "Seattle", "state", "WA", "zip", 98101), Helper.loadMap("firstName", "Jane", "lastName", "Doey", "address", "202 2nd St", "city", "Miami", "state", "FL", "zip", 33255))), writer);

		assertEquals("Names:" + LS + " - Doe, John" + LS + " - Doey, Jane" + LS + LS + "Mailing Labels:" + LS + "John Doe" + LS + "101 1st St" + LS + "Seattle, WA 98101" + LS + LS + "Jane Doey" + LS + "202 2nd St" + LS + "Miami, FL 33255" + LS, writer.toString());
	}

	/**
	 * This test evaluates the example code given in the {@link Template} javadoc and the README markdown file. Any changes to this code should be updated in those locations as well.
	 */
	@Test
	public void testExample() throws IOException, LoadException {
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
	public void testMapLiteral() throws IOException, LoadException {
		assertEquals("Bob is 45 years old." + LS + "Alice is 31 years old." + LS + "Jim is 80 years old." + LS, new TemplateLoader().load("Map Test", "{{#{\"Bob\": 45, \"Alice\": 31, \"Jim\": 80} }}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("Bob is 45 years old." + LS + "Alice is 31 years old." + LS + "Jim is 80 years old." + LS, new TemplateLoader().load("Map Test", "{{#\"Bob\": 45, \"Alice\": 31, \"Jim\": 80}}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("Bob is 45 years old." + LS + "Alice is 31 years old." + LS + "Jim is 80 years old." + LS, new TemplateLoader().load("Map Test", new StringReader("{{#(\"Bob\": 45, \"Alice\": 31, \"Jim\": 80)}}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}")).render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	public void testNamedExpressions() throws IOException, LoadException {
		assertEquals("ORIGINAL STRING-Original string" + LS, new TemplateLoader().load("Upper", "{{upper->toUpperCase()}}\n{{capitalize=>substring(0, 1).toUpperCase() + substring(1).toLowerCase()}}\n{{#\"orIgInal StrIng\"}}\n{{#charAt(1)}}\n{{upper(..)}}-{{capitalize(..)}}\n{{/}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
		assertEquals(LS + "  ORIGINAL STRING-original string" + LS, new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{>a}}\n  {{upper() + \"-\" + lower()}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test(expected = IOException.class)
	public void testRenderException() throws IOException, LoadException {
		new TemplateLoader().load("Exception Test", " ").render(new Settings(), Collections.emptyMap(), new Writer() {
			@Override
			public void close() {
			}

			@Override
			public void flush() {
			}

			@Override
			public void write(final char[] arg0, final int arg1, final int arg2) throws IOException {
				throw new IOException();
			}
		});
	}

	@Test
	public void testSections() throws IOException, LoadException {
		assertEquals("nil, String, String2, true, false, -128, 127, -, :, -32768, 32767, -2147483648, 2147483647, -9223372036854775808, 9223372036854775807, 1.2, -1.2, 5.6, -5.6", new TemplateLoader().load("Array Test", "{{#arrays}}{{#.}}{{.}}{{#.hasNext}}, {{/}}{{^}}nil{{/}}{{#.hasNext}}, {{/}}{{/}}").render(new Settings(), Collections.singletonMap("arrays", new Object[] { new Object[0], new Object[] { "String", "String2" }, new boolean[] { true, false }, new byte[] { -128, 127 }, new char[] { '-', ':' }, new short[] { -32768, 32767 }, new int[] { -2147483648, 2147483647 }, new long[] { -9223372036854775808L, 9223372036854775807L }, new float[] { 1.2f, -1.2f }, new double[] { 5.6, -5.6 } }), new java.io.StringWriter()).toString());
		assertEquals("String, 1.6, 1234 -> String, 1.6, 1234", new TemplateLoader().load("Stream Test", "{{#stream}}{{.}}{{#.hasNext}}, {{/}}{{/}} -> {{#}}{{.}}{{#.hasNext}}, {{/}}{{/}}").render(new Settings(), Collections.singletonMap("stream", Arrays.asList("String", 1.6, 1234).stream()), new java.io.StringWriter()).toString());
		assertEquals("String, nil -> String, nil", new TemplateLoader().load("Optional Test", "{{#optionals}}{{#.}}{{.}}{{^}}nil{{/}}{{#.hasNext}}, {{/}}{{/}} -> {{#}}{{#.}}{{.}}{{^}}nil{{/}}{{#.hasNext}}, {{/}}{{/}}").render(new Settings(), Collections.singletonMap("optionals", new Object[] { Optional.of("String"), Optional.empty() }), new java.io.StringWriter()).toString());
	}

	@Test
	public void testTemplateLoader() throws IOException, LoadException {
		assertEquals(StandardCharsets.UTF_8, new TemplateLoader().setCharset(StandardCharsets.UTF_8).getCharset());
		assertEquals(StandardCharsets.UTF_16, new TemplateLoader().setCharset(StandardCharsets.UTF_16).getCharset());
		assertEquals(true, new TemplateLoader().setPreventPartialPathTraversal(true).getPreventPartialPathTraversal());
		assertEquals(false, new TemplateLoader().setPreventPartialPathTraversal(false).getPreventPartialPathTraversal());
		assertEquals(true, new TemplateLoader().setThrowOnPartialNotFound(true).getThrowOnPartialNotFound());
		assertEquals(false, new TemplateLoader().setThrowOnPartialNotFound(false).getThrowOnPartialNotFound());
		assertEquals(true, new TemplateLoader().setUseSimpleExpressions(true).getUseSimpleExpressions());
		assertEquals(false, new TemplateLoader().setUseSimpleExpressions(false).getUseSimpleExpressions());

		assertEquals(" ", new TemplateLoader().load("Simple Test", " ").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());

		final TemplateLoader tl1 = new TemplateLoader().add("Dup", new StringReader("1")).add("Dup", "2");
		tl1.load("Dup", new StringReader("3"));
		tl1.load("Dup", "55");
		tl1.load("Dup", Paths.get("fakeFile"), StandardCharsets.UTF_16BE);
		assertEquals("3", tl1.load("Dup", new StringReader("4")).render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());
		new TemplateLoader().add("Dup", new StringReader("1")).close();

		assertEquals("a", new TemplateLoader().add(new TemplateLoader().add("a", "a").load("a")).load("b", "{{>a}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter()).toString());

		new TemplateLoader().getIncludeDirectories().add(Paths.get("."));
	}

	@Test(expected = LoadException.class)
	public void testTemplateLoaderException() throws IOException, LoadException {
		new TemplateLoader().load("Exception Test", new StringReader(" ") {
			@Override
			public int read(final char[] cbuf, final int off, final int len) throws IOException {
				throw new IOException();
			}
		}).render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
	}

}
