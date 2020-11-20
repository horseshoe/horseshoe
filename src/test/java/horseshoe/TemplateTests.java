package horseshoe;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TemplateTests {

	private static final String LS = System.lineSeparator();

	@Test
	void testBackreach() throws IOException, LoadException {
		assertEquals("Original String" + LS, new TemplateLoader().load("Backreach", "{{#\"Original String\"}}\n{{#charAt(1)}}\n{{..}}\n{{/}}\n{{/}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("Original String" + LS, new TemplateLoader().load("Backreach", "{{#\"Original String\"}}\n{{#charAt(1)}}\n{{../toString()}}\n{{/}}\n{{/}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testClassLoading() throws IOException, LoadException {
		assertEquals("Good, Good, 73.7", new TemplateLoader().load("ClassLoading", "{{#classes}}{{^~@./getName()}}Bad, {{/}}{{/}}{{#~@'Blah'}}Bad, {{/}}{{#~@'NonExistantClass'}}Bad, {{/}}{{#~@'java.lang.System'}}Bad, {{/}}{{#~@'System'}}Bad, {{/}}{{#~@'ClassLoader'}}Good, {{/}}{{#~@'" + TemplateTests.class.getName() + "'}}Good, {{/}}{{~@'Integer'.parseInt('67') + ~@'Double'.parseDouble('6.7')}}").render(new Settings().addLoadableClasses(TemplateTests.class).addLoadableClasses(Arrays.asList(ClassLoader.class)), Collections.<String, Object>singletonMap("classes", new Settings().getLoadableClasses()), new java.io.StringWriter()).toString());
	}

	@Test
	void testCloseException() throws LoadException, IOException {
		final String templateName = "Duplicate Template Close Exception";
		final TemplateLoader loader = new TemplateLoader().put(templateName, new StringReader(""));
		final Reader reader = new Reader() {
			@Override
			public int read(final char[] cbuf, final int off, final int len) {
				return -1;
			}

			@Override
			public void close() throws IOException {
				throw new IOException("Logged-only close IOException");
			}
		};

		assertAll(() -> loader.load(templateName), () -> loader.load(templateName, reader));
	}

	@Test
	void testDie() throws IOException, LoadException {
		assertEquals("String 1" + LS + "String 2" + LS, new TemplateLoader().load("Die", "{{#'String 1', 'String 2', \"String 3\"}}\n{{^.hasNext}}\n{{☠\"Should print out as a severe log statement\"; 'Did not die'}}\n{{/}}\n{{.}}\n{{/}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testEmpty() throws IOException, LoadException {
		assertEquals("", new TemplateLoader().load("Empty", "{{}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	/**
	 * This test evaluates the example code given in the {@link Template} javadoc and the README markdown file. Any changes to this code should be updated in those locations as well.
	 */
	@Test
	void testExample() throws IOException, LoadException {
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
	void testMapLiteral() throws IOException, LoadException {
		assertEquals("Bob is 45 years old." + LS + "Alice is 31 years old." + LS + "Jim is 80 years old." + LS, new TemplateLoader().load("Map Test", "{{#{\"Bob\": 45, \"Alice\": 31, \"Jim\": 80}.entrySet() }}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("Bob is 45 years old." + LS + "Alice is 31 years old." + LS + "Jim is 80 years old." + LS, new TemplateLoader().load("Map Test", "{{#\"Bob\": 45, \"Alice\": 31, \"Jim\": 80}}\n{{#./entrySet()}}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}\n{{/}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("Bob is 45 years old." + LS + "Alice is 31 years old." + LS + "Jim is 80 years old." + LS, new TemplateLoader().load("Map Test", new StringReader("{{#(\"Bob\": 45, \"Alice\": 31, \"Jim\": 80)}}\n{{#./entrySet()}}\n{{./getKey()}} is {{./getValue()}} years old.\n{{/}}\n{{/}}")).render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testPatternLookup() throws IOException, LoadException {
		assertEquals("One, Two, Three, One", new TemplateLoader().load("Pattern Lookup Test", "{{# 'One Two Three'[~/[A-Z][a-z]+/] }}{{.}}{{# .hasNext }}, {{/}}{{/}}, {{ 'One Two Three'[~/[A-Z][a-z]+/] }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals(", ", new TemplateLoader().load("Pattern Lookup Test", "{{# 'One Two Three'[~/[A-Z][a-z]+[A-Z]/] }}{{.}}{{# .hasNext }}, {{/}}{{/}}, {{ 'One Two Three'[~/[A-Z][a-z]+[A-Z]/] }}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testRenderException() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Exception Test", " ");
		final Writer writer = new Writer() {
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
		};

		assertThrows(IOException.class, () -> template.render(Collections.<String, Object>emptyMap(), writer));
	}

	@Test
	void testRepeatedSection() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Repeated Section", "Names:\n{{#people}}\n - {{lastName}}, {{firstName}}{{#deceased}} (deceased){{/}}\n{{/}}\n\nMailing Labels:\n{{#}}\n{{#}}Family of {{/}}{{firstName}} {{lastName}}\n{{address}}\n{{city}}, {{state}} {{zip}}\n{{#.hasNext}}\n\n{{/}}\n{{/}}\n");

		final Settings settings = new Settings();
		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(settings, Helper.loadMap("people", Helper.loadList(Helper.loadMap("firstName", "John", "lastName", "Doe", "address", "101 1st St", "city", "Seattle", "state", "WA", "zip", 98101, "deceased", true), Helper.loadMap("firstName", "Jane", "lastName", "Doey", "address", "202 2nd St", "city", "Miami", "state", "FL", "zip", 33255, "deceased", false))), writer);

		assertEquals("Names:" + LS + " - Doe, John (deceased)" + LS + " - Doey, Jane" + LS + LS + "Mailing Labels:" + LS + "Family of John Doe" + LS + "101 1st St" + LS + "Seattle, WA 98101" + LS + LS + "Jane Doey" + LS + "202 2nd St" + LS + "Miami, FL 33255" + LS, writer.toString());
	}

	@Test
	void testRepeatedSection2() throws IOException, LoadException {
		assertEquals("Names:" + LS + " - John Doe" + LS + " - Jane Doey" + LS + LS + "All:" + LS + " - John Doe, Jane Doey" + LS, new TemplateLoader().load("Repeated Section", "Names:\n{{#people}}\n{{#['first':firstName, 'last':lastname, 'full':firstName + ' ' + lastName]}}\n - {{full}}\n{{/}}\n{{/}}\n\nAll:\n - {{#}}{{#}}{{full}}{{/}}{{#.hasNext}}, {{/}}{{/}}\n").render(Helper.loadMap("people", Helper.loadList(Helper.loadMap("firstName", "John", "lastName", "Doe"), Helper.loadMap("firstName", "Jane", "lastName", "Doey"))), new java.io.StringWriter()).toString());
	}

	@Test
	void testRootIdentifiers() throws IOException, LoadException {
		assertEquals("Root Local Root Root", new TemplateLoader().load("Root", "{{#[\"Value\":\"Local\"]}}{{\\Value}} {{Value}} {{/Value}}{{#/.}} {{Value}}{{/}}{{/}}").render(Collections.<String, Object>singletonMap("Value", "Root"), new java.io.StringWriter()).toString());
	}

	@Test
	void testSections() throws IOException, LoadException {
		assertEquals("nil, String, String2, nil, true, false, nil, -128, 127, nil, -, :, nil, -32768, 32767, nil, -2147483648, 2147483647, nil, -9223372036854775808, 9223372036854775807, nil, 1.2, -1.2, nil, 5.6, -5.6", new TemplateLoader().load("Array Test", "{{#arrays}}{{#.}}{{.}}{{#.hasNext}}, {{/}}{{^}}nil{{/}}{{#.hasNext}}, {{/}}{{/}}").render(Collections.<String, Object>singletonMap("arrays", new Object[] { new Object[0], new Object[] { "String", "String2" }, new boolean[0], new boolean[] { true, false }, new byte[0], new byte[] { -128, 127 }, new char[0], new char[] { '-', ':' }, new short[0], new short[] { -32768, 32767 }, new int[0], new int[] { -2147483648, 2147483647 }, new long[0], new long[] { -9223372036854775808L, 9223372036854775807L }, new float[0], new float[] { 1.2f, -1.2f }, new double[0], new double[] { 5.6, -5.6 } }), new java.io.StringWriter()).toString());
		assertEquals("String, 1.6, 1234", new TemplateLoader().load("Stream Test", "{{#stream}}{{.}}{{#.hasNext}}, {{/}}{{^}}nil{{/}}").render(Collections.<String, Object>singletonMap("stream", Arrays.asList("String", 1.6, 1234).stream()), new java.io.StringWriter()).toString());
		assertEquals("nil", new TemplateLoader().load("Empty Stream Test", "{{#stream}}{{.}}{{#.hasNext}}, {{/}}{{^}}nil{{/}}").render(Collections.<String, Object>singletonMap("stream", Arrays.asList().stream()), new java.io.StringWriter()).toString());
		assertEquals("String, 1.6, 1234 -> String, 1.6, 1234", new TemplateLoader().load("Repeat Stream Test", "{{#stream}}{{.}}{{#.hasNext}}, {{/}}{{^}}nil{{/}} -> {{#}}{{.}}{{#.hasNext}}, {{/}}{{^}}nil{{/}}").render(Collections.<String, Object>singletonMap("stream", Arrays.asList("String", 1.6, 1234).stream()), new java.io.StringWriter()).toString());
		assertEquals("String, nil -> String, nil", new TemplateLoader().load("Optional Test", "{{#optionals}}{{#.}}{{.}}{{^}}nil{{/}}{{#.hasNext}}, {{/}}{{/}} -> {{#}}{{#.}}{{.}}{{^}}nil{{/}}{{#.hasNext}}, {{/}}{{/}}").render(Collections.<String, Object>singletonMap("optionals", new Object[] { Optional.of("String"), Optional.empty() }), new java.io.StringWriter()).toString());
		assertEquals("X, X, 1, X, 1, X, 2.0, X, -1.0, X, 1, X, 10, X, 1, X, 1, X", new TemplateLoader().load("Section Test", "{{#values}}{{#.index}}, {{/}}{{#'X'}}{{#..}}{{.}}{{^}}{{.}}{{/}}{{/}}{{/}}").render(Collections.<String, Object>singletonMap("values", new Object[] { true, false, 1, 0, 1L, 0L, 2.0, 0.0, -1.0f, 0.0f, BigDecimal.ONE, BigDecimal.ZERO, BigInteger.TEN, BigInteger.ZERO, '1', '\0', "1", null }), new java.io.StringWriter()).toString());
		assertEquals("Big, Medium, Small, Zero, ", new TemplateLoader().load("Else If Test", "{{# [1_000_000, 1_000, 1, 0, -1] }}{{# .index }}, {{/}}{{# . > 65_536 }}Big{{^# . > 64 }}Medium{{^# . > 0 }}Small{{^# . == 0 }}Zero{{/}}{{/}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testSectionError1() throws IOException, LoadException {
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Bad Repeat Test", "{{#}}{{/}}"));
	}

	@Test
	void testSectionError2() throws IOException, LoadException {
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Optional Test", "{{#optionals}}{{#.}}{{.}}{{^}}nil{{/}}{{#.hasNext}}, {{/}}{{/}} -> {{#}}{{#}}{{.}}{{^}}nil{{/}}{{#.hasNext}}, {{/}}{{/}}"));
	}

	@Test
	void testSectionError3() throws IOException, LoadException {
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Optional Test", "{{#optionals}}{{/}} -> {{#}}{{#}}{{.}}{{^}}nil{{/}}{{#.hasNext}}, {{/}}{{/}}"));
	}

	@Test
	void testStreamingSections() throws IOException, LoadException {
		assertEquals("3, 4, 5", new TemplateLoader().load("Remap Test", "{{#[1,2,3] #> i -> i + 2}}{{.}}{{#.hasNext}}, {{/}}{{/}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("-1", new TemplateLoader().load("Min Test", "{{ min = ~@'Integer'.MAX_VALUE; [1,-1,1000] #< i -> min=i<min?i:min}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("2", new TemplateLoader().load("Filter Test", "{{min = ~@'Integer'.MAX_VALUE;[1,-1,1000] #. v-> v+1 #? j-> j>0 #< j => min=j<min?j:min}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("1", new TemplateLoader().load("First Test", "{{[1,-1,1000] #< v-> #^ v}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("1000", new TemplateLoader().load("Last Test", "{{[1,-1,1000] #< i=>i}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("10", new TemplateLoader().load("Sum Test", "{{sum = 0; [[1, 2], [3, 4].toArray(), null] #? i -> i #| i -> i #< i -> sum = sum + i}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("", new TemplateLoader().load("Sum Test", "{{sum = 0; [[1, 2], [3, 4].toArray(), null, 5] #| i -> i #< i -> sum = sum + i}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Bad Identifier Test", "{{sum = 0; [[1, 2], [3, 4].toArray(), null] #| + -> i #< i -> sum = sum + i}}"));
	}

	@Test
	void testTemplateLoader() throws IOException, LoadException {
		assertEquals(StandardCharsets.UTF_8, new TemplateLoader().setCharset(StandardCharsets.UTF_8).getCharset());
		assertEquals(StandardCharsets.UTF_16, new TemplateLoader().setCharset(StandardCharsets.UTF_16).getCharset());
		assertEquals(true, new TemplateLoader().setPreventPartialPathTraversal(true).getPreventPartialPathTraversal());
		assertEquals(false, new TemplateLoader().setPreventPartialPathTraversal(false).getPreventPartialPathTraversal());
		assertEquals(EnumSet.of(Extension.ELSE_TAGS, Extension.INLINE_PARTIALS), new TemplateLoader().setExtensions(EnumSet.of(Extension.ELSE_TAGS, Extension.INLINE_PARTIALS)).getExtensions());
		assertEquals(EnumSet.noneOf(Extension.class), new TemplateLoader().setExtensions(EnumSet.noneOf(Extension.class)).getExtensions());
		assertEquals(EnumSet.allOf(Extension.class), new TemplateLoader().setExtensions(EnumSet.allOf(Extension.class)).getExtensions());

		assertEquals(" ", new TemplateLoader().load("Simple Test", " ").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());

		final TemplateLoader tl1 = new TemplateLoader().put("Dup", new StringReader("1")).put("Dup", "2");
		tl1.load("Dup", new StringReader("3"));
		tl1.load("Dup", "55");
		assertEquals("4", tl1.load("Dup", new StringReader("4")).render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		new TemplateLoader().put("Dup", new StringReader("1"));

		assertEquals("a", new TemplateLoader().put(new TemplateLoader().load("a", "a")).load("b", "{{>a}}").render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());

		new TemplateLoader().getIncludeDirectories().add(Paths.get("."));

		final Template reloadTemplate = new TemplateLoader().load("Test 1", "It works!" + LS);
		final Path fakePath = Paths.get("fakePath").toAbsolutePath();

		assertEquals("It works!" + LS, new TemplateLoader().put("Test 2", reloadTemplate).load("Test 3", "{{>Test 2}}" + LS).render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
		assertEquals("It works!" + LS, new TemplateLoader().put(fakePath, reloadTemplate).load("Test 3", "{{>" + fakePath + "}}" + LS).render(Collections.<String, Object>emptyMap(), new java.io.StringWriter()).toString());
	}

	@Test
	void testTemplateLoaderException1() throws LoadException, IOException {
		final StringReader reader = new StringReader(" ") {
			@Override
			public int read(final char[] cbuf, final int off, final int len) throws IOException {
				throw new IOException();
			}
		};
		assertThrows(IOException.class, () -> new TemplateLoader().load("Exception Test", reader));
	}

	@Test
	void testTemplateLoaderException2() throws LoadException, IOException {
		final StringReader reader = new StringReader("{{>" + Paths.get("non-existant-file").toAbsolutePath() + "}}");
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Exception Test", reader));
	}

	@Test
	void testTemplateLoaderException3() throws LoadException, IOException {
		final TemplateLoader loader = new TemplateLoader();
		final Object object = new Object();

		assertThrows(IllegalArgumentException.class, () -> loader.load());
		assertThrows(IllegalArgumentException.class, () -> loader.load(null, object));
		assertThrows(IllegalArgumentException.class, () -> loader.load(object, object));
		assertThrows(IllegalArgumentException.class, () -> loader.load(new Object[] { null, null }));
		assertThrows(IllegalArgumentException.class, () -> loader.load(object));
		assertThrows(IllegalArgumentException.class, () -> loader.load(new Object[] { null }));
	}

}
