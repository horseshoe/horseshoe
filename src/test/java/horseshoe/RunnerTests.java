package horseshoe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class RunnerTests {

	private static class IntCollector implements java.util.function.IntConsumer {
		Integer value;

		@Override
		public void accept(int value) {
			this.value = value;
		}

		static <T> IntCollector runWith(final java.util.function.Consumer<IntCollector> runable) {
			final IntCollector errorCode = new IntCollector();

			runable.accept(errorCode);
			return errorCode;
		}
	}

	@Test
	void generateExampleResults() throws IOException {
		Files.walk(Paths.get("samples/data"), 1)
				.filter(path -> path.toString().endsWith(".U"))
				.forEach(path -> {
					final Path dataFile = path.resolveSibling(path.getFileName().toString().replace(".U", ".data"));
					final ArrayList<String> args = new ArrayList<>();

					args.add("--output");
					args.add(path.resolveSibling(Paths.get("results", path.getFileName().toString().replaceFirst("([.].*)[.]U$", "$1").replace(".U", ".txt"))).toString());
					args.add(path.toString());

					if (dataFile.toFile().exists()) {
						args.add("--data-file");
						args.add(dataFile.toString());
					}

					assertDoesNotThrow(() -> Runner.main(args.toArray(new String[0])));
				});
	}

	@Test
	void testBadArgument() {
		assertEquals(Runner.ERROR_EXIT_CODE, IntCollector.runWith(onError -> Runner.run(new String[] { "arg1", "arg2", "arg3", "arg4", "arg5", "arg6", "arg7", "arg8", "arg9" }, onError)).value);
	}

	@Test
	void testBadDataMap1() {
		Runner.DataParser parser = new Runner.DataParser("    ");
		assertThrows(RuntimeException.class, () -> parser.parseAsMap());
	}

	@Test
	void testBadDataMap2() {
		Runner.DataParser parser = new Runner.DataParser(" ] ");
		assertThrows(RuntimeException.class, () -> parser.parseAsMap());
	}

	@Test
	void testBadOption() {
		assertEquals(Runner.ERROR_EXIT_CODE, IntCollector.runWith(onError -> Runner.run(new String[] { "--bad-option" }, onError)).value);
	}

	@Test
	void testBadOption2() {
		assertEquals(Runner.ERROR_EXIT_CODE, IntCollector.runWith(onError -> Runner.run(new String[] { "-9" }, onError)).value);
	}

	@Test
	void testBadOptionArgument() {
		assertEquals(Runner.ERROR_EXIT_CODE, IntCollector.runWith(onError -> Runner.run(new String[] { "--help=yes" }, onError)).value);
	}

	@Test
	void testBadOptionArgument2() {
		assertEquals(Runner.ERROR_EXIT_CODE, IntCollector.runWith(onError -> Runner.run(new String[] { "--log-level" }, onError)).value);
	}

	@Test
	void testBadOptionArgument3() {
		assertEquals(Runner.ERROR_EXIT_CODE, IntCollector.runWith(onError -> Runner.run(new String[] { "-l" }, onError)).value);
	}

	@Test
	void testBadOutputArgument() throws IOException {
		final InputStream originalIn = System.in;

		try (final ByteArrayInputStream in = new ByteArrayInputStream("{{ test }}\n".getBytes(StandardCharsets.UTF_8))) {
			System.setIn(in);
			assertEquals(Runner.ERROR_EXIT_CODE, IntCollector.runWith(onError -> Runner.run(new String[] { "-oa/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z.out" }, onError)).value);
		} finally {
			System.setIn(originalIn);
		}
	}

	@Test
	void testCurrentContext() throws IOException {
		final InputStream originalIn = System.in;

		try (final ByteArrayInputStream in = new ByteArrayInputStream("{{# ['blue', 'red'] }}\n{{.}}{{# a }}: {{ blah }}{{/}}{{# .hasNext }},{{/}}\n{{/}}\n".getBytes(StandardCharsets.UTF_8))) {
			System.setIn(in);
			Runner.main(new String[] { "--output", "out2.test", "-Da=true", "-Dblah=blah", "--output-charset=UTF-16BE", "--access=CURRENT", "--disable-extensions=" + Extension.ANNOTATIONS + "," + Extension.SMART_END_TAGS, "-" });
		} finally {
			System.setIn(originalIn);
		}

		final Path path = Paths.get("out2.test");
		final String renderedContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_16BE);
		Files.delete(path);
		assertEquals("blue," + System.lineSeparator() + "red" + System.lineSeparator(), renderedContent);
	}

	@Test
	void testCurrentAndRootContext() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final Path path = Paths.get("in.test");
		Files.write(path, ("{{# a }}\n" +
				"<html{{# ['<red>', '<blue>'] }} attr=\"{{.}}\"{{/}}/>\n" +
				"{{ ~@UUID.fromString('01234567-89AB-CDEF-fedc-ba9876543210') }}\n" +
				"{{# ['a': false, 'b': 'c'] }}\n" +
				"{{# ['blah': 'override'] }}\n" +
				"{{ a }}{{ ~@'System'.lineSeparator() }}" +
				"{{ blah }}\n" +
				"{{/}}\n" +
				"{{/}}\n" +
				"{{/}}").getBytes(StandardCharsets.UTF_16LE));

		final PrintStream originalOut = System.out;
		System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8.name()));
		Runner.main(new String[] { "-I", "includeDir", "--html", "--input-charset=UTF-16LE", "-Da=true", "-Dblah=blah", "--add-class=System", "--add-class=java.util.UUID", "--access=CURRENT_AND_ROOT", "in.test" });
		System.setOut(originalOut);

		Files.delete(path);
		assertEquals("<html attr=\"&lt;red&gt;\" attr=\"&lt;blue&gt;\"/>" + System.lineSeparator() +
				"01234567-89ab-cdef-fedc-ba9876543210" + System.lineSeparator() +
				"true" + System.lineSeparator() +
				"override" + System.lineSeparator(), out.toString(StandardCharsets.UTF_8.name()));
	}

	@Test
	void testFullContext() throws IOException {
		final InputStream originalIn = System.in;

		try (final ByteArrayInputStream in = new ByteArrayInputStream("{{# ['<blue>', '<red>'] }}\n{{.}}{{# a }}: {{ blah }}{{/}}{{# .hasNext }},{{/}}\n{{/}}\n".getBytes(StandardCharsets.UTF_8))) {
			System.setIn(in);
			Runner.main(new String[] { "-oout.test", "-lOFF", "-Da=true", "-Dblah=2 words ", "--access=FULL", "--", "-" });
		} finally {
			System.setIn(originalIn);
		}

		final Path path = Paths.get("out.test");
		final String renderedContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		Files.delete(path);
		assertEquals("<blue>: 2 words ," + System.lineSeparator() + "<red>: 2 words " + System.lineSeparator(), renderedContent);
	}

	@Test
	void testHelp() throws IOException {
		assertDoesNotThrow(() -> Runner.main(new String[] { "--help" }));
	}

	@Test
	void testJson() throws IOException {
		assertEquals(0L, Files.walk(Paths.get("samples/data/json"))
				.filter(path -> path.toString().endsWith(".json"))
				.filter(path -> {
					final String fileName = path.getFileName().toString();

					try {
						final String data = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
						new Runner.DataParser(data, 0, false).parseAsValue();

						if (fileName.startsWith("y_") || fileName.startsWith("i_")) {
							return false;
						}

						System.err.println(fileName + " - accepted");
					} catch (final RuntimeException | StackOverflowError e) {
						if (fileName.startsWith("n_") || fileName.startsWith("i_")) {
							return false;
						}

						System.err.println(fileName + " - rejected: " + e.getMessage());
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}

					return true;
				}).count());
	}

	@Test
	void testValues() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final InputStream originalIn = System.in;
		final PrintStream originalOut = System.out;

		try (final ByteArrayInputStream in = new ByteArrayInputStream("{{ t }}, {{ f }}, {{ d }}, {{ d+1 }}, {{ l }}, {{ i }}, {{ n }}\n".getBytes(StandardCharsets.UTF_8))) {
			System.setIn(in);
			System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8.name()));
			Runner.main(new String[] { "--disable-extensions", "-Dt", "-Df=false", "-Dd=1.5", "-Dd+1=-0.5", "-Dl=12345678901234", "-Di=123456789", "-Dn=null" });
		} finally {
			System.setIn(originalIn);
			System.setOut(originalOut);
		}

		assertEquals("true, false, 1.5, -0.5, 12345678901234, 123456789, " + System.lineSeparator(), out.toString(StandardCharsets.UTF_8.name()));
	}

	@Test
	void testVersion() throws IOException {
		assertDoesNotThrow(() -> Runner.main(new String[] { "--version" }));
	}

}
