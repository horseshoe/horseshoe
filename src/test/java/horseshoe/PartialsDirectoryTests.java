package horseshoe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PartialsDirectoryTests {

	private static class PartialFile {
		public final File file;
		public final String contents;

		public PartialFile(final File file, final String contents) {
			this.file = file;
			this.contents = contents;
		}
	}

	static class TestSet {
		private final String rootIncludeDir;
		private final String partialNavigationPath;
		private final PartialFile[] partials;
		private final boolean preventPartialPathTraversal;
		private final Class<? extends Exception> expectedException;

		/**
		 * Creates a new test with the specified values.
		 *
		 * @param rootIncludeDir the base include directory
		 * @param partialNavigationPath the partial include path
		 * @param partials existing partials
		 * @param preventPartialPathTraversal a flag to set the prevent partial path traversal flag in the template loader
		 * @param expectedException the expected exception, null for none
		 */
		public TestSet(final String rootIncludeDir,
				final String partialNavigationPath,
				final PartialFile[] partials,
				final boolean preventPartialPathTraversal,
				final Class<? extends Exception> expectedException) {
			this.rootIncludeDir = rootIncludeDir;
			this.partialNavigationPath = partialNavigationPath;
			this.partials = partials;
			this.preventPartialPathTraversal = preventPartialPathTraversal;
			this.expectedException = expectedException;
		}

		private File writeFileContents(final File file, final String contents) throws IOException {
			Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
			return file;
		}
	}

	@Test
	void testDataSets(@TempDir Path baseDirectory) throws IOException, LoadException {
		final TestSet[] testSets = new TestSet[] {
			new TestSet(".", "Partial", new PartialFile[] { new PartialFile(new File("Partial"), "This partial renders text!") }, true, null),
			new TestSet(".", "PartialThatDoesNotExist", new PartialFile[] { new PartialFile(new File("Partial"), "This partial renders text!") }, true, LoadException.class),
			new TestSet("./test1/test2", "../../Partial", new PartialFile[] { new PartialFile(new File("Partial"), "This partial renders text!") }, true, LoadException.class),
			new TestSet("./test1/test2", "../../Partial", new PartialFile[] { new PartialFile(new File("Partial"), "This partial renders text!") }, false, null),
			new TestSet("./test1/test3", "../test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "This partial renders text!") }, true, LoadException.class),
			new TestSet("./test1/test3", "../test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "This partial renders text!") }, false, null),
			new TestSet("./test1/test2", "../test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "This partial renders text!") }, true, null),
			new TestSet("./test1/test2", "../test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "This partial renders text!") }, false, null),
			new TestSet("./test1/test3", "../test3/Partial", new PartialFile[] { new PartialFile(new File("test1/test3/Partial"), "{{>../test2/Partial2}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, true, LoadException.class),
			new TestSet("./test1", "test3/Partial", new PartialFile[] { new PartialFile(new File("test1/test3/Partial"), "{{#true}}{{>../test2/Partial2}}{{/}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, true, null),
			new TestSet("./test1", "test3/Partial", new PartialFile[] { new PartialFile(new File("test1/test3/Partial"), "{{#true}}{{>../test2/Partial2}}{{/}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, false, null),
			new TestSet("./test1", "test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "{{#true}}{{>../test2/Partial2}}{{/}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, true, null),
			new TestSet("./test1", "test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "{{#true}}{{>../test2/Partial2}}{{/}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, false, null),
		};

		for (int i = 0; i < testSets.length; i++) {
			final Path temporaryDirectory = baseDirectory.resolve("test_" + i);
			final TestSet testSet = testSets[i];

			temporaryDirectory.toFile().mkdirs();
			assertDoesNotThrow(() -> testPartialFromOtherDirectory(temporaryDirectory, testSet));
		}
	}

	private static void testPartialFromOtherDirectory(final Path temporaryDirectory, final TestSet set) throws IOException, LoadException {
		for (final PartialFile partial : set.partials) {
			if (partial.file.getParentFile() != null && !temporaryDirectory.resolve(partial.file.getParentFile().toPath()).toFile().isDirectory()) {
				temporaryDirectory.resolve(partial.file.getParentFile().toString()).toFile().mkdirs();
			}
			set.writeFileContents(temporaryDirectory.resolve(partial.file.toString()).toFile(), partial.contents);
		}

		temporaryDirectory.resolve(set.rootIncludeDir).toFile().mkdirs();

		final Path testFile = temporaryDirectory.resolve(set.rootIncludeDir).resolve("Test");
		final Settings settings = new Settings();
		final TemplateLoader loader = new TemplateLoader()
				.setPreventPartialPathTraversal(set.preventPartialPathTraversal);
		Template template = null;

		try {
			Files.write(testFile, ("{{>" + set.partialNavigationPath + "}}").getBytes(StandardCharsets.UTF_8));
			template = loader.load(testFile);
		} catch (final LoadException e) {
			if (set.expectedException == null || !set.expectedException.isInstance(e)) {
				throw e;
			}
			return;
		}

		if (set.expectedException != null) {
			throw new AssertionError("Expected exception: " + set.expectedException);
		}

		final StringWriter writer = new StringWriter();
		template.render(settings, Collections.emptyMap(), writer);
		assertEquals(set.partials[set.partials.length - 1].contents, writer.toString());

		final Path unrelatedFile = set.writeFileContents(File.createTempFile("XXXXXXXX", null, temporaryDirectory.toFile()), "DummyContents").toPath();
		loader.put(unrelatedFile).load(unrelatedFile, StandardCharsets.US_ASCII);
	}

}
