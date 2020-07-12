package horseshoe;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PartialsDirectoryTests {

	public final @Rule TemporaryFolder temporaryFolder = new TemporaryFolder();
	private final String rootIncludeDir;
	private final String partialNavigationPath;
	private final PartialFile[] partials;
	private final boolean preventPartialPathTraversal;
	private final Class<? super Exception> expectedException;

	private static class PartialFile {
		public final File file;
		public final String contents;

		public PartialFile(final File file, final String contents) {
			this.file = file;
			this.contents = contents;
		}
	}

	/**
	 * Creates a new test with the specified values.
	 *
	 * @param rootIncludeDir the base include directory
	 * @param partialNavigationPath the partial include path
	 * @param partials existing partials
	 * @param preventPartialPathTraversal a flag to set the prevent partial path traversal flag in the template loader
	 * @param expectedException the expected exception, null for none
	 */
	public PartialsDirectoryTests(final String rootIncludeDir,
			final String partialNavigationPath,
			final PartialFile[] partials,
			final boolean preventPartialPathTraversal,
			final Class<? super Exception> expectedException) {
		this.rootIncludeDir = rootIncludeDir;
		this.partialNavigationPath = partialNavigationPath;
		this.partials = partials;
		this.preventPartialPathTraversal = preventPartialPathTraversal;
		this.expectedException = expectedException;
	}

	/**
	 * Gets the data for the tests.
	 *
	 * @return the data for the tests
	 */
	@Parameters(name = "rootIncludeDir = {0}, partialNavigationPath = {1}, partials = {2}, preventPartialPathTraversal = {3}, expectedException = {4}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "./", "Partial", new PartialFile[] { new PartialFile(new File("Partial"), "This partial renders text!") }, true, null },
				{ "./", "PartialThatDoesNotExist", new PartialFile[] { new PartialFile(new File("Partial"), "This partial renders text!") }, true, LoadException.class },
				{ "./test1/test2", "../../Partial", new PartialFile[] { new PartialFile(new File("Partial"), "This partial renders text!") }, true, LoadException.class },
				{ "./test1/test2", "../../Partial", new PartialFile[] { new PartialFile(new File("Partial"), "This partial renders text!") }, false, null },
				{ "./test1/test3", "../test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "This partial renders text!") }, true, LoadException.class },
				{ "./test1/test3", "../test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "This partial renders text!") }, false, null },
				{ "./test1/test2", "../test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "This partial renders text!") }, true, null },
				{ "./test1/test2", "../test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "This partial renders text!") }, false, null },
				{ "./test1/test3", "../test3/Partial", new PartialFile[] { new PartialFile(new File("test1/test3/Partial"), "{{>../test2/Partial2}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, true, LoadException.class },
				{ "./test1", "test3/Partial", new PartialFile[] { new PartialFile(new File("test1/test3/Partial"), "{{#true}}{{>../test2/Partial2}}{{/}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, true, null },
				{ "./test1", "test3/Partial", new PartialFile[] { new PartialFile(new File("test1/test3/Partial"), "{{#true}}{{>../test2/Partial2}}{{/}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, false, null },
				{ "./test1", "test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "{{#true}}{{>../test2/Partial2}}{{/}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, true, null },
				{ "./test1", "test2/Partial", new PartialFile[] { new PartialFile(new File("test1/test2/Partial"), "{{#true}}{{>../test2/Partial2}}{{/}}"), new PartialFile(new File("test1/test2/Partial2"), "This partial renders text!") }, false, null },
		});
	}

	private File writeFileContents(final File file, final String contents) throws IOException {
		Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
		return file;
	}

	@Test
	public void testPartialFromOtherDirectory() throws IOException, LoadException {
		for (final PartialFile partial : partials) {
			if (partial.file.getParentFile() != null && !temporaryFolder.getRoot().toPath().resolve(partial.file.getParentFile().toPath()).toFile().isDirectory()) {
				try {
					temporaryFolder.newFolder(partial.file.getParentFile().toString());
				} catch (final IOException e) {
					// Failures to create new directories are fine for duplicates
				}
			}
			writeFileContents(temporaryFolder.newFile(partial.file.toString()), partial.contents);
		}

		try {
			temporaryFolder.newFolder(rootIncludeDir);
		} catch (final IOException e) {
			// Failures to create new directories are fine for duplicates
		}

		final Path testFile = temporaryFolder.getRoot().toPath().resolve(rootIncludeDir).resolve("Test");
		final Settings settings = new Settings();
		final TemplateLoader loader = new TemplateLoader()
				.setPreventPartialPathTraversal(preventPartialPathTraversal);
		Template template = null;

		try {
			Files.write(testFile, ("{{>" + partialNavigationPath + "}}").getBytes(StandardCharsets.UTF_8));
			template = loader.load(testFile);
		} catch (final LoadException e) {
			if (expectedException == null || !expectedException.isInstance(e)) {
				throw e;
			}
			return;
		}

		if (expectedException != null) {
			throw new AssertionError("Expected exception: " + expectedException);
		}

		final StringWriter writer = new StringWriter();
		template.render(settings, Collections.emptyMap(), writer);
		Assert.assertEquals(partials[partials.length - 1].contents, writer.toString());

		final Path unrelatedFile = writeFileContents(temporaryFolder.newFile(), "DummyContents").toPath();
		loader.put(unrelatedFile).load(unrelatedFile, StandardCharsets.US_ASCII);
	}

}
