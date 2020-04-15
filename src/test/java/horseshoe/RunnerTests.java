package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;

public class RunnerTests {

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();

	@Rule
	public final TextFromStandardInputStream systemInMock = TextFromStandardInputStream.emptyStandardInputStream();

	@Rule
	public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

	@Test
	public void testBadArgument() {
		exit.expectSystemExitWithStatus(Runner.ERROR_EXIT_CODE);
		Runner.main(new String[] { "arg1", "arg2", "arg3", "arg4", "arg5", "arg6", "arg7", "arg8", "arg9" });
	}

	@Test
	public void testBadOption() {
		exit.expectSystemExitWithStatus(Runner.ERROR_EXIT_CODE);
		Runner.main(new String[] { "--bad-option" });
	}

	@Test
	public void testBadOption2() {
		exit.expectSystemExitWithStatus(Runner.ERROR_EXIT_CODE);
		Runner.main(new String[] { "-9" });
	}

	@Test
	public void testBadOptionArgument() {
		exit.expectSystemExitWithStatus(Runner.ERROR_EXIT_CODE);
		Runner.main(new String[] { "--help=yes" });
	}

	@Test
	public void testBadOptionArgument2() {
		exit.expectSystemExitWithStatus(Runner.ERROR_EXIT_CODE);
		Runner.main(new String[] { "--log-level" });
	}

	@Test
	public void testBadOptionArgument3() {
		exit.expectSystemExitWithStatus(Runner.ERROR_EXIT_CODE);
		Runner.main(new String[] { "-l" });
	}

	@Test
	public void testCurrentContext() throws IOException {
		systemInMock.provideLines("{{#['blue', 'red']}}", "{{.}}{{#a}}: {{blah}}{{/}}{{#.hasNext}},{{/}}", "{{/}}");
		Runner.main(new String[] { "--output", "out2.test", "-Da=true", "-Dblah=blah", "--output-charset=UTF-16BE", "--access=CURRENT", "-" });

		final Path path = Paths.get("out2.test");
		final String renderedContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_16BE);
		Files.delete(path);
		assertEquals("blue," + System.lineSeparator() + "red" + System.lineSeparator(), renderedContent);
	}

	@Test
	public void testCurrentAndRootContext() throws IOException {
		final Path path = Paths.get("in.test");
		Files.write(path, ("{{#a}}\n" +
				"<html{{#['<red>', '<blue>']}} attr=\"{{.}}\"{{/}}/>\n" +
				"{{#['a': false, 'b': 'c']}}\n" +
				"{{#['blah': 'override']}}\n" +
				"{{a}}{{~@'System'.lineSeparator()}}" +
				"{{blah}}\n" +
				"{{/}}\n" +
				"{{/}}\n" +
				"{{/}}").getBytes(StandardCharsets.UTF_16LE));

		Runner.main(new String[] { "-I", "includeDir", "--html", "--input-charset=UTF-16LE", "-Da=true", "-Dblah=blah", "--add-class=System", "--access=CURRENT_AND_ROOT", "in.test" });

		Files.delete(path);
		assertEquals("<html attr=\"&lt;red&gt;\" attr=\"&lt;blue&gt;\"/>" + System.lineSeparator() +
				"true" + System.lineSeparator() +
				"override" + System.lineSeparator(), systemOutRule.getLog());
	}

	@Test
	public void testFullContext() throws IOException {
		systemInMock.provideLines("{{#['<blue>', '<red>']}}", "{{.}}{{#a}}: {{blah}}{{/}}{{#.hasNext}},{{/}}", "{{/}}");
		Runner.main(new String[] { "-oout.test", "-lOFF", "-Da=true", "-Dblah=blah", "--access=FULL", "--", "-" });

		final Path path = Paths.get("out.test");
		final String renderedContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		Files.delete(path);
		assertEquals("<blue>: blah," + System.lineSeparator() + "<red>: blah" + System.lineSeparator(), renderedContent);
	}

	@Test (expected = Test.None.class) // No exception expected
	public void testHelp() throws IOException {
		Runner.main(new String[] { "--help" });
	}

	@Test
	public void testValues() throws IOException {
		systemInMock.provideLines("{{t}}, {{f}}, {{d}}, {{d+1}}, {{l}}, {{i}}, {{n}}");
		Runner.main(new String[] { "--disable-extensions", "-Dt", "-Df=false", "-Dd=1.5", "-Dd+1=-0.5", "-Dl=12345678901234", "-Di=123456789", "-Dn=null" });

		assertEquals("true, false, 1.5, -0.5, 12345678901234, 123456789, " + System.lineSeparator(), systemOutRule.getLog());
	}

	@Test (expected = Test.None.class) // No exception expected
	public void testVersion() throws IOException {
		Runner.main(new String[] { "--version" });
	}

}
