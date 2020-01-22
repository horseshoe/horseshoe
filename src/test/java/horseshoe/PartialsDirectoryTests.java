package horseshoe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
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
	private final File partialFilePath;
	private final File partialNavigationPath;
	private final boolean preventPartialPathTraversal;
	private final Class<? super Exception> expectedException;

	public PartialsDirectoryTests(final String rootIncludeDir,
			final String partialFilePath,
			final String partialNavigationPath,
			final boolean preventPartialPathTraversal,
			final Class<? super Exception> expectedException) {
		this.rootIncludeDir = rootIncludeDir;
		this.partialFilePath = new File(partialFilePath);
		this.partialNavigationPath = new File(partialNavigationPath);
		this.preventPartialPathTraversal = preventPartialPathTraversal;
		this.expectedException = expectedException;
	}

	@Parameters(name = "rootIncludeDir = {0}, partialFilePath = {1}, partialNavigationPath = {2}, preventPartialPathTraversal = {3}, expectedException = {4}")
	public static Collection<Object[]> data() {
			return Arrays.asList(new Object[][] {
					{ "./", "Partial", "Partial", true, null },
					{ "./test1/test2", "Partial", "../../Partial", true, LoadException.class },
					{ "./test1/test2", "Partial", "../../Partial", false, null },
					{ "./test1/test3", "test1/test2/Partial", "../test2/Partial", true, LoadException.class },
					{ "./test1/test3", "test1/test2/Partial", "../test2/Partial", false, null },
			});
	}

	private void writeFileContents(final File file, final String contents) throws IOException {
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8.newEncoder())) {
			writer.write(contents);
		}
	}

	@Test
	public void testPartialFromOtherDirectory() throws IOException, LoadException {
		if (partialFilePath.getParentFile() != null) {
				temporaryFolder.newFolder(partialFilePath.getParentFile().toString().split("/"));
		}
		final File rootIncludeDir = new File(temporaryFolder.getRoot(), this.rootIncludeDir);
		final File tempFile = temporaryFolder.newFile(partialFilePath.toString());
		final String partialContent = "This partial renders text!";
		writeFileContents(tempFile, partialContent);
		final Settings settings = new Settings();
		final TemplateLoader loader = new TemplateLoader(Arrays.asList(rootIncludeDir.toPath()))
				.setPreventPartialPathTraversal(preventPartialPathTraversal)
				.setThrowOnPartialNotFound(true);
		Template template = null;
		try {
			template = loader.load("Test", "{{>" + partialNavigationPath + "}}");
		} catch (final LoadException e) {
			if (expectedException == null || !expectedException.isInstance(e)) {
				throw e;
			}
		}
		if (expectedException != null) {
			throw new AssertionError("Expected exception: " + expectedException);
		}
		final StringWriter writer = new StringWriter();
		template.render(settings, Collections.emptyMap(), writer);
		Assert.assertEquals(partialContent, writer.toString());
	}

}