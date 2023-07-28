package horseshoe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.junit.jupiter.api.Test;

class AnnotationTests {

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
		final LinkedHashMap<String, String> map = new LinkedHashMap<>();

		@Override
		public Writer getWriter(final Writer writer, final Object[] args) throws IOException {
			return new StringWriter() {
				@Override
				public void close() throws IOException {
					map.put(args == null ? null : Objects.toString(args[0]), toString());
					super.close();
				}

				@Override
				public void write(final char[] cbuf, final int off, final int len) {
					super.write(cbuf, off, len);
					try {
						writer.write(cbuf, off, len);
					} catch (final IOException e) {
						// Ignore exceptions (should never occur)
					}
				}

				@Override
				public void write(final int c) {
					final char[] chars = Character.toChars(c);
					write(chars, 0, chars.length);
				}

				@Override
				public void write(final String str, final int off, final int len) {
					super.write(str, off, len);
					try {
						writer.write(str, off, len);
					} catch (final IOException e) {
						// Ignore exceptions (should never occur)
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
	void testAnnotationEmptySection() throws LoadException, IOException, InterruptedException {
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final TemplateLoader loader = new TemplateLoader();
		final Template template = loader.load("AnnotationsTests", "ab{{# @Test(\"value123\") }}{{/ @Test }}cd");
		final MapAnnotation mapAnnotation = new MapAnnotation();
		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", mapAnnotation));
		assertEquals("value123", mapAnnotation.map.keySet().stream().findFirst().get());
		assertEquals("", mapAnnotation.map.values().stream().findFirst().get());
		assertEquals("abcd", writer.toString());
	}

	@Test
	void testAnnotationSection() throws LoadException, IOException {
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final TemplateLoader loader = new TemplateLoader();
		final Template template = loader.load("AnnotationsTests", "ab{{# @Test(\"value123\") }}456{{^}}789{{/}}cd");
		final MapAnnotation mapAnnotation = new MapAnnotation();
		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", mapAnnotation));
		assertEquals("456", mapAnnotation.map.get("value123"));
		assertEquals("ab456cd", writer.toString());
	}

	@Test
	void testCloseException() throws IOException, LoadException {
		assertDoesNotThrow(() -> new TemplateLoader().load("Exception Writer", "a{{# @Test }}b{{^}}d{{/}}c").render(new Settings(), Collections.emptyMap(), new StringWriter(), Collections.singletonMap("Test", new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object[] args) throws IOException {
				return new TestWriter() {
					@Override
					public void close() throws IOException {
						throw new IOException("Logged-only close IOException");
					}
				};
			}
		})));
	}

	@Test
	void testException() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Exception Writer", "a{{# @Test }}{{# true }}b{{/}}{{^}}d{{/}}c");
		final AnnotationHandler handler = new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object[] args) throws IOException {
				return new TestWriter() {
					@Override
					public void write(final char[] cbuf, final int off, final int len) throws IOException {
						throw new IOException("Test write IOException");
					}
				};
			}
		};

		assertThrows(IOException.class, () -> template.render(new Settings(), Collections.emptyMap(), new StringWriter(), Collections.singletonMap("Test", handler)));
	}

	@Test
	void testExceptionWithCloseException() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Exception Writer", "a{{# true }}{{# @Test }}b{{^}}d{{/}}{{/}}c");
		final AnnotationHandler handler = new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object[] args) throws IOException {
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
		};

		assertThrows(IOException.class, () -> template.render(new Settings(), Collections.emptyMap(), new StringWriter(), Collections.singletonMap("Test", handler)));
	}

	private static final class FileTest {
		public final String initial;
		public final String write;
		public final FileModification update;
		public final String result;

		public FileTest(final String initial, final String write, final FileModification update, final String result) {
			this.initial = initial;
			this.write = write;
			this.update = update;
			this.result = result;
		}

		public FileTest(final String initial, final String write) {
			this(initial, write, FileModification.UPDATE, write);
		}
	}

	@Test
	void testFileUpdate() throws IOException, LoadException {
		final Path path = Paths.get("DELETE_ME.test");

		try {
			final String bigBuf = new String(new byte[4096], StandardCharsets.US_ASCII).replace('\0', ' ');

			Files.write(path, ("Test 1" + LS + "Test 3" + LS).getBytes(StandardCharsets.UTF_8));

			try (final OutputStream os = new BufferedFileUpdateStream(path.toFile(), FileModification.UPDATE, bigBuf.length() - 1)) {
				os.flush();
				os.write(("Test 1" + LS).getBytes(StandardCharsets.UTF_8));
				os.flush();
				os.write("Test".getBytes(StandardCharsets.UTF_8));
				os.write(' ');
				os.write('2');
				os.write(bigBuf.getBytes(StandardCharsets.UTF_8));
			}

			assertEquals("Test 1" + LS + "Test 2" + bigBuf, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));

			try (final OutputStream os = new BufferedFileUpdateStream(path.toFile(), FileModification.OVERWRITE, bigBuf.length())) {
				os.write(("Test" + LS).getBytes(StandardCharsets.UTF_8));
				os.write(bigBuf.getBytes(StandardCharsets.UTF_8));
			}

			assertEquals("Test" + LS + bigBuf, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));

			try (final OutputStream os = new BufferedFileUpdateStream(path.toFile(), FileModification.OVERWRITE, bigBuf.length())) {
				os.write(bigBuf.getBytes(StandardCharsets.UTF_8));
				os.write('1');
			}

			assertEquals(bigBuf + "1", new String(Files.readAllBytes(path), StandardCharsets.UTF_8));

			for (final FileTest test : new FileTest[] {
					new FileTest("Test" + LS, ""),
					new FileTest("Test" + LS, "", FileModification.APPEND, "Test" + LS),
					new FileTest("Test" + LS, "Test 2", FileModification.APPEND, "Test" + LS + "Test 2"),
					new FileTest("Test" + LS, "Test 2", FileModification.OVERWRITE, "Test 2"),
					new FileTest("Test" + LS, new String(new byte[65536], StandardCharsets.US_ASCII).replace('\0', ' ')),
			}) {
				Files.write(path, test.initial.getBytes(StandardCharsets.UTF_8));

				try (final OutputStream os = new BufferedFileUpdateStream(path.toFile(), test.update, 4096)) {
					if (!test.write.isEmpty()) {
						os.write(test.write.getBytes(StandardCharsets.UTF_8));
					}
				}

				assertEquals(test.result, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
			}
		} finally {
			Files.delete(path);
		}
	}

	@Test
	void testFileUpdateAnnotation() throws IOException, LoadException, InterruptedException {
		final Path path = Paths.get("DELETE_ME.test");
		final Path path2 = Paths.get("TEMP_DIR", "1", "2", "DELETE_ME.test");
		final Path path3 = Paths.get("3");

		try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
			Files.write(path, "T ".getBytes(StandardCharsets.UTF_8));
			new TemplateLoader().load("File Update", "{{# @File('" + path.toAbsolutePath() + "', { })}}\nTest 1\n{{/ @File }}\n").render(Collections.emptyMap(), new StringWriter());
			assertEquals("Test 1" + LS, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));

			Files.write(path, "Test".getBytes(StandardCharsets.UTF_8));
			new TemplateLoader().load("File Update", "{{# @File(\"" + path + "\", { 'append': false })}}\nTest 1\n{{/ @File }}\n").render(Collections.emptyMap(), new StringWriter());
			assertEquals("Test 1" + LS, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));

			new TemplateLoader().load("File Update", "{{# @File('" + path2 + "', 'Bad Option') }}\nTest 1\n{{/ @File }}\n").render(Collections.emptyMap(), new StringWriter());
			assertEquals("Test 1" + LS, new String(Files.readAllBytes(path2), StandardCharsets.UTF_8));

			new TemplateLoader().load("File Update", "{{# @File('" + path2 + "', 'Bad Option') }}\nTest 1\n{{/ @File }}\n").render(Collections.emptyMap(), new StringWriter());
			assertEquals("Test 1" + LS, new String(Files.readAllBytes(path2), StandardCharsets.UTF_8));

			new TemplateLoader().load("File Update", "{{# @File(" + path3 + ", 'Bad Option') }}\nTest 3\n{{/ @File }}\n").render(Collections.emptyMap(), new StringWriter());
			assertEquals("Test 3" + LS, new String(Files.readAllBytes(path3), StandardCharsets.UTF_8));

			assertEquals("NoFile", Template.load("{{# @File(null) }}Bad{{^^ @File }}NoFile{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());
			assertEquals("NoFile", Template.load("{{# @`File`() }}Bad{{^}}NoFile{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());
			assertEquals("NoFile", Template.load("{{# @File }}Bad{{^}}NoFile{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());

			assertEquals("Good", Template.load("{{# true ?: @File }}Good{{^}}NoFile{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());
			assertEquals("NoFile", Template.load("{{# true !: @File }}Bad{{^}}NoFile{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());
			assertEquals("NoFile", Template.load("{{# @File('Test') + '1' }}{{.}}Bad{{^}}NoFile{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());

			Files.write(path, ("Test 1" + LS).getBytes(StandardCharsets.UTF_8));
			final WatchKey watchKey1 = Paths.get(".").register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
			new TemplateLoader().load("File Update", "{{# @File({\"name\":\"" + path + "\", 'append': false})}}\nTest 1\n{{/ @File }}\n").render(Collections.emptyMap(), new StringWriter());
			assertEquals("Test 1" + LS, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
			assertFalse(watchKey1.pollEvents().stream().anyMatch(event -> event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY) && path.equals(event.context())));

			Files.write(path, ("Test 1" + LS).getBytes(StandardCharsets.UTF_8));
			final WatchKey watchKey2 = Paths.get(".").register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
			new TemplateLoader().load("File Update", "{{# @File({\"name\":\"" + path + "\", 'overwrite': true})}}\nTest 1\n{{/ @File }}\n").render(Collections.emptyMap(), new StringWriter());
			assertEquals("Test 1" + LS, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
			assertTrue(watchKey2.pollEvents().stream().anyMatch(event -> event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY) && path.equals(event.context())));

			Files.write(path, ("Test 1" + LS + "Test 2").getBytes(StandardCharsets.UTF_8));
			new TemplateLoader().load("File Update", "{{# @File({\"name\":\"" + path + "\", 'append': false})}}\nTest 1\n{{/ @File }}\n").render(Collections.emptyMap(), new StringWriter());
			assertEquals("Test 1" + LS, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
		} finally {
			Files.deleteIfExists(path);
			Files.deleteIfExists(path2);
			Files.deleteIfExists(path2.getParent());
			Files.deleteIfExists(path2.getParent().getParent());
			Files.deleteIfExists(path2.getParent().getParent().getParent());
			Files.deleteIfExists(path3);
		}
	}

	@Test
	void testMissingAnnotation() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Missing Annotation", "{{# @Missing(\"blah\") }}\nGood things are happening!\nMore good things!\n{{^}}\n{{# @Test }}\nEngine does not support @missing.\n{{/}}\n{{/ @Missing }}\n");
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final MapAnnotation mapAnnotation = new MapAnnotation();

		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", mapAnnotation));
		assertEquals("Engine does not support @missing." + LS, mapAnnotation.map.get(null));
		assertEquals("Engine does not support @missing." + LS, writer.toString());
	}

	@Test
	void testNestedAnnotations() throws LoadException, IOException {
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final TemplateLoader loader = new TemplateLoader();
		final Template template = loader.load("AnnotationsTests", "ab{{# @Test(\"value123\") }}{{# @Inner }}789{{/}}456{{/ @Test(\"value123\") }}cd");
		final MapAnnotation testMapAnnotation = new MapAnnotation();
		final MapAnnotation innerMapAnnotation = new MapAnnotation();
		final LinkedHashMap<String, AnnotationHandler> annotations = new LinkedHashMap<>();
		annotations.put("Test", testMapAnnotation);
		annotations.put("Inner", innerMapAnnotation);
		template.render(settings, Collections.emptyMap(), writer, annotations);
		assertEquals("789", innerMapAnnotation.map.get(null));
		assertEquals("789456", testMapAnnotation.map.get("value123"));
		assertEquals("ab789456cd", writer.toString());
	}

	@Test
	void testNullWriter() throws IOException, LoadException {
		assertEquals("1", Template.load("{{# @Null }}{{ a := 1 }}Bad{{/ @Null }}{{ a }}").render(Collections.emptyMap(), new StringWriter()).toString());
		assertDoesNotThrow(() -> {
			try (final Writer writer = AnnotationHandlers.NULL_HANDLER.getWriter(null, null)) {
				writer.flush();
			}
		});
	}

	@Test
	void testOutputMapping() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Output Mapping", "Good things are happening!\n{{# @Test }}\nThis should output to map annotation.\n{{/}}\nGood things are happening again!\n");
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();
		final MapAnnotation mapAnnotation = new MapAnnotation();

		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", mapAnnotation));

		assertEquals("This should output to map annotation." + LS, mapAnnotation.map.get(null));
		assertEquals("Good things are happening!" + LS + "This should output to map annotation." + LS + "Good things are happening again!" + LS, writer.toString());
	}

	@Test
	void testOutputRemapping() throws IOException, LoadException {
		final Path path = Paths.get("DELETE_ME.test");

		try {
			final Template template = new TemplateLoader().load("Output Remapping", "{{# @File(\"" + path + "\") }}\nTest 1\n{{/}}\n{{# @File({\"name\":\"" + path + "\", \"encoding\": \"ASCII\", 'append': true}) }}\nGood things are happening!\nMore good things!\n{{/ @File }}\n{{# @StdErr }}\nThis should print to stderr\n{{/}}\n");
			template.render(Collections.emptyMap(), new StringWriter());
			assertEquals("Test 1" + LS + "Good things are happening!" + LS + "More good things!" + LS, new String(Files.readAllBytes(path), StandardCharsets.US_ASCII));
		} finally {
			Files.delete(path);
		}
	}

	@Test
	void testSameWriter() throws IOException, LoadException {
		final Template template = new TemplateLoader().load("Same Writer", "a{{# @Test }}b{{^}}d{{/}}c");
		final Settings settings = new Settings();
		final StringWriter writer = new StringWriter();

		template.render(settings, Collections.emptyMap(), writer, Collections.singletonMap("Test", new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object[] args) throws IOException {
				return writer;
			}
		}));
		assertEquals("abc", writer.toString());
	}

}
