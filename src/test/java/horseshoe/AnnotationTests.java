package horseshoe;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class AnnotationTests {

	private static final String LS = System.lineSeparator();

	private static class TestWriter extends Writer {
		private final Writer writer = new StringWriter();

		@Override
		public void write(final char[] cbuf, final int off, final int len) throws IOException {
			writer.write(cbuf, off, len);
		}

		@Override
		public void flush() throws IOException {
			writer.flush();
		}

		@Override
		public void close() throws IOException {
			writer.close();
		}

		@Override
		public String toString() {
			return writer.toString();
		}
	}

	private static final class MapAnnotation implements AnnotationHandler {
		final Map<String, String> map = new LinkedHashMap<>();
		@Override
		public Writer getWriter(final Writer writer, final Object value) throws IOException {
			return new StringWriter() {
				@Override
				public void close() throws IOException {
					map.put(value == null ? null : value.toString(), toString());
					super.close();
				}

				@Override
				public void write(final char[] cbuf, final int off, final int len) {
					super.write(cbuf, off, len);
					try {
						writer.write(cbuf, off, len);
					} catch (final IOException e) {
					}
				}

				@Override
				public void write(final int c) {
					final char chars[] = Character.toChars(c);
					write(chars, 0, chars.length);
				}

				@Override
				public void write(final String str, final int off, final int len) {
					super.write(str, off, len);
					try {
						writer.write(str, off, len);
					} catch (final IOException e) {
					}
				}

				@Override
				public void write(final String str) {
					write(str, 0, str.length());
				}
			};
		}
	}

	@Test
	public void testAnnotationEmptySection() throws LoadException, IOException, InterruptedException {
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final TemplateLoader loader = new TemplateLoader();
		final Template template = loader.load("AnnotationsTests", "ab{{#@Test(\"value123\")}}{{/@Test}}cd");
		final MapAnnotation mapAnnotation = new MapAnnotation();
		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", mapAnnotation));
		Assert.assertEquals("value123", mapAnnotation.map.keySet().stream().findFirst().get());
		Assert.assertEquals("", mapAnnotation.map.values().stream().findFirst().get());
		Assert.assertEquals("abcd", writer.toString());
	}

	@Test
	public void testAnnotationSection() throws LoadException, IOException {
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final TemplateLoader loader = new TemplateLoader();
		final Template template = loader.load("AnnotationsTests", "ab{{#@Test(\"value123\")}}456{{^}}789{{/}}cd");
		final MapAnnotation mapAnnotation = new MapAnnotation();
		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", mapAnnotation));
		Assert.assertEquals("456", mapAnnotation.map.get("value123"));
		Assert.assertEquals("ab456cd", writer.toString());
	}

	@Test (expected = Test.None.class) // No exception expected
	public void testCloseException() throws IOException, LoadException {
		new TemplateLoader().load("Exception Writer", "a{{#@Test}}b{{^}}d{{/}}c").render(new Settings(), Collections.emptyMap(), new StringWriter(), Collections.singletonMap("Test", new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				return new TestWriter() {
					@Override
					public void close() throws IOException {
						throw new IOException("Logged-only close IOException");
					}
				};
			}
		}));
	}

	@Test (expected = IOException.class)
	public void testException() throws IOException, LoadException {
		new TemplateLoader().load("Exception Writer", "a{{#@Test}}{{#true}}b{{/}}{{^}}d{{/}}c").render(new Settings(), Collections.emptyMap(), new StringWriter(), Collections.singletonMap("Test", new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				return new TestWriter() {
					@Override
					public void write(final char[] cbuf, final int off, final int len) throws IOException {
						throw new IOException("Test write IOException");
					}
				};
			}
		}));
	}

	@Test (expected = IOException.class)
	public void testExceptionWithCloseException() throws IOException, LoadException {
		new TemplateLoader().load("Exception Writer", "a{{#true}}{{#@Test}}b{{^}}d{{/}}{{/}}c").render(new Settings(), Collections.emptyMap(), new StringWriter(), Collections.singletonMap("Test", new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				return new TestWriter() {
					@Override
					public void close() throws IOException {
						throw new IOException("Logged-only, suppressed close IOException");
					}

					@Override
					public void write(final char[] cbuf, final int off, final int len) throws IOException {
						throw new IOException("Test write IOException");
					}
				};
			}
		}));
	}

	@Test
	public void testMissingAnnotation() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Missing Annotation", "{{#@Missing(\"blah\")}}\nGood things are happening!\nMore good things!\n{{^}}\n{{#@Test}}\nEngine does not support @missing.\n{{/}}\n{{/@Missing}}\n");
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final MapAnnotation mapAnnotation = new MapAnnotation();

		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", mapAnnotation));
		Assert.assertEquals("Engine does not support @missing." + LS, mapAnnotation.map.get(null));
		Assert.assertEquals("Engine does not support @missing." + LS, writer.toString());
	}

	@Test
	public void testNestedAnnotations() throws LoadException, IOException {
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final TemplateLoader loader = new TemplateLoader();
		final Template template = loader.load("AnnotationsTests", "ab{{#@Test(\"value123\")}}{{#@Inner}}789{{/}}456{{/}}cd");
		final MapAnnotation testMapAnnotation = new MapAnnotation();
		final MapAnnotation innerMapAnnotation = new MapAnnotation();
		final Map<String, AnnotationHandler> annotations = new LinkedHashMap<>();
		annotations.put("Test", testMapAnnotation);
		annotations.put("Inner", innerMapAnnotation);
		template.render(settings, Collections.emptyMap(), writer, annotations);
		Assert.assertEquals("789", innerMapAnnotation.map.get(null));
		Assert.assertEquals("789456", testMapAnnotation.map.get("value123"));
		Assert.assertEquals("ab789456cd", writer.toString());
	}

	@Test
	public void testNullWriter() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Null Writer", "a{{#@Test}}b{{^}}d{{/}}c");
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();

		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				return null;
			}
		}));
		Assert.assertEquals("abc", writer.toString());
	}

	@Test
	public void testOutputMapping() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Output Mapping", "Good things are happening!\n{{#@Test}}\nThis should output to map annotation.\n{{/}}\nGood things are happening again!\n");
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final MapAnnotation mapAnnotation = new MapAnnotation();

		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", mapAnnotation));

		Assert.assertEquals("This should output to map annotation." + LS, mapAnnotation.map.get(null));
		Assert.assertEquals("Good things are happening!" + LS + "This should output to map annotation." + LS + "Good things are happening again!" + LS, writer.toString());
	}

	@Test
	public void testOutputRemapping() throws IOException, LoadException {
		final String filename = "DELETE_ME.test";
		final Template template = new TemplateLoader().load("Output Remapping", "{{#@File(\"" + filename + "\")}}Test 1{{/}}\n{{#@File({\"name\":\"" + filename + "\", \"encoding\": \"UTF-8\"})}}\nGood things are happening!\nMore good things!\n{{/@File}}\n{{#@StdErr}}\nThis should print to stderr\n{{/}}\n");
		final Settings settings = new Settings();
		final Writer writer = new StringWriter();

		try {
			template.render(settings, Collections.emptyMap(), writer);
		} finally {
			Assert.assertEquals("Good things are happening!" + LS + "More good things!" + LS, String.join(LS, new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8)));
			Files.delete(Paths.get(filename));
		}
	}

	@Test
	public void testSameWriter() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Same Writer", "a{{#@Test}}b{{^}}d{{/}}c");
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();

		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				return writer;
			}
		}));
		Assert.assertEquals("abc", writer.toString());
	}

}
