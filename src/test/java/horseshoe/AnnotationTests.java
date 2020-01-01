package horseshoe;

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

	private static final String LS = System.lineSeparator();

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
		final Template template = loader.load("AnnotationsTests", "ab{{#@test(\"value123\")}}456{{^}}789{{/}}cd");
		final MapAnnotation mapAnnotation = new MapAnnotation();
		template.render(settings, new HashMap<>(), writer, Collections.singletonMap("test", mapAnnotation));
		Assert.assertEquals("456", mapAnnotation.map.get("value123"));
		Assert.assertEquals("ab456cd", writer.toString());
	}

	@Test
	public void testMissingAnnotation() throws IOException, LoadException {
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Missing Annotation", "{{#@missing(\"blah\")}}\nGood things are happening!\nMore good things!\n{{^}}\n{{#@test}}\nEngine does not support @missing.\n{{/}}\n{{/@missing}}\n");
		final horseshoe.Settings settings = new horseshoe.Settings();
		final StringWriter writer = new StringWriter();
		final MapAnnotation mapAnnotation = new MapAnnotation();

		template.render(settings, new java.util.HashMap<>(), writer, Collections.singletonMap("test", mapAnnotation));
		Assert.assertEquals("Engine does not support @missing." + LS, mapAnnotation.map.get(null));
		Assert.assertEquals("Engine does not support @missing." + LS, writer.toString());
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
		Assert.assertEquals("789", innerMapAnnotation.map.get(null));
		Assert.assertEquals("789456", testMapAnnotation.map.get("value123"));
		Assert.assertEquals("ab789456cd", writer.toString());
	}

	@Test
	public void testNullWriter() throws IOException, LoadException {
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Null Writer", "a{{#@test}}b{{^}}d{{/}}c");
		final horseshoe.Settings settings = new horseshoe.Settings();
		final StringWriter writer = new StringWriter();

		template.render(settings, new java.util.HashMap<>(), writer, Collections.singletonMap("test", new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				return null;
			}
		}));
		Assert.assertEquals("abc", writer.toString());
	}

	@Test
	public void testOutputMapping() throws IOException, LoadException {
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Output Mapping", "Good things are happening!\n{{#@test}}\nThis should output to map annotation.\n{{/}}\nGood things are happening again!\n");
		final horseshoe.Settings settings = new horseshoe.Settings();
		final StringWriter writer = new StringWriter();
		final MapAnnotation mapAnnotation = new MapAnnotation();

		template.render(settings, new java.util.HashMap<>(), writer, Collections.singletonMap("test", mapAnnotation));

		Assert.assertEquals("This should output to map annotation." + LS, mapAnnotation.map.get(null));
		Assert.assertEquals("Good things are happening!" + LS + "This should output to map annotation." + LS + "Good things are happening again!" + LS, writer.toString());
	}

	@Test
	public void testOutputRemapping() throws IOException, LoadException {
		final String filename = "DELETE_ME.test";
		final horseshoe.Template template = new horseshoe.TemplateLoader().load("Output Remapping", "{{#@file({\"name\":\"" + filename + "\"})}}\nGood things are happening!\nMore good things!\n{{/@file}}\n");
		final horseshoe.Settings settings = new horseshoe.Settings();
		final Writer writer = new StringWriter();

		try {
			template.render(settings, Collections.emptyMap(), writer);
		} finally {
			Assert.assertEquals("Good things are happening!" + LS + "More good things!" + LS, String.join(LS, new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8)));
			Files.delete(Paths.get(filename));
		}
	}

}
