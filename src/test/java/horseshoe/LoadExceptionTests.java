package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

public class LoadExceptionTests {

	private static final String LS = System.lineSeparator();
	private static final String INVALID = "The template should throw an exception when loading is attempted.";

	@Test
	public void testLoadUnclosedTag() throws IOException {
		try {
			Helper.executeTest("This is a {{#Test}} test template {{/}", Collections.emptyMap(), new Settings(), Collections.emptyMap(), INVALID);
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(1, e.getLoaders().get(0).getLine());
			assertEquals(37, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	public void testLoadUnrecognizedTag() throws IOException {
		try {
			Helper.executeTest("This is a" + LS + " {{#Test}} " + LS + "test template" + LS + " {{" + LS + LS + "/}", Collections.emptyMap(), new Settings(), Collections.emptyMap(), INVALID);
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(4, e.getLoaders().get(0).getLine());
			assertEquals(4, e.getLoaders().get(0).getColumn());
		}
	}

}
