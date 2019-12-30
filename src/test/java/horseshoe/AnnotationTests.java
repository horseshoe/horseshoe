package horseshoe;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class AnnotationTests {

	static class MapAnnotation implements AnnotationHandler {
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
					super.write(c);
					try {
						writer.write(c);
					} catch (final IOException e) {
					}
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
					super.write(str);
					try {
						writer.write(str);
					} catch (final IOException e) {
					}
				}
			};
		}
	}

	@Test
	public void testAnnotationEmptySection() throws LoadException, IOException, InterruptedException {
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final TemplateLoader loader = new TemplateLoader();
		final Template template = loader.load("AnnotationsTests", "ab{{#@test(\"value123\")}}{{/@test}}cd");
		final MapAnnotation mapAnnotation = new MapAnnotation();
		template.render(settings, new HashMap<>(), writer, Collections.singletonMap("test", mapAnnotation));
		Assert.assertEquals("value123", mapAnnotation.map.keySet().stream().findFirst().get());
		Assert.assertEquals("", mapAnnotation.map.values().stream().findFirst().get());
		Assert.assertEquals("abcd", writer.toString());
	}

	@Test
	public void testAnnotationSection() throws LoadException, IOException {
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final TemplateLoader loader = new TemplateLoader();
		final Template template = loader.load("AnnotationsTests", "ab{{#@test(\"value123\")}}456{{/}}cd");
		final MapAnnotation mapAnnotation = new MapAnnotation();
		template.render(settings, new HashMap<>(), writer, Collections.singletonMap("test", mapAnnotation));
		Assert.assertEquals("456", mapAnnotation.map.get("value123"));
		Assert.assertEquals("ab456cd", writer.toString());
	}

	@Test
	public void testNestedAnnotations() throws LoadException, IOException {
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final TemplateLoader loader = new TemplateLoader();
		final Template template = loader.load("AnnotationsTests", "ab{{#@test(\"value123\")}}{{#@inner}}789{{/}}456{{/}}cd");
		final MapAnnotation testMapAnnotation = new MapAnnotation();
		final MapAnnotation innerMapAnnotation = new MapAnnotation();
		final Map<String, AnnotationHandler> annotations = new LinkedHashMap<>();
		annotations.put("test", testMapAnnotation);
		annotations.put("inner", innerMapAnnotation);
		template.render(settings, new HashMap<>(), writer, annotations);
		Assert.assertEquals("789456", testMapAnnotation.map.get("value123"));
		Assert.assertEquals("789", innerMapAnnotation.map.get(null));
		Assert.assertEquals("ab789456cd", writer.toString());
	}

	@Test
	public void testOutputRemapping() throws java.io.IOException, LoadException {
		final String filename = "DELETE_ME.test";
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("AnnotationTest", "{{#@OutputToFile(\"" + filename + "\")}}\nGood things are happening!\nMore good things!\n{{/}}\n");

		final horseshoe.Settings settings = new horseshoe.Settings();
		final java.io.Writer writer = new java.io.StringWriter();

		try {
			template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("OutputToFile", new AnnotationHandler() {
				@Override
				public Writer getWriter(final Writer writer, final Object value) throws IOException {
					return new BufferedWriter(new FileWriter(value.toString())) {
						@Override
						public void write(final char[] cbuf, final int off, final int len) throws IOException {
							super.write(cbuf, off, len);
							writer.write(cbuf, off, len);
						}

					};
				}
			}));
		} finally {
			Assert.assertEquals("Good things are happening!" + System.lineSeparator() + "More good things!" + System.lineSeparator(), String.join(System.lineSeparator(), new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8)));
			Files.delete(Paths.get(filename));
		}
	}

}