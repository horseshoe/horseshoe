package horseshoe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

public class LoadExceptionTests {

	private static final String LS = System.lineSeparator();

	@Test
	public void testLoadUnclosedTag() throws IOException {
		try {
			new TemplateLoader().load("Unclosed Tag", "This is a {{#Test}} test template {{/}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(1, e.getLoaders().get(0).getLine());
			assertEquals(37, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	public void testLoadUnrecognizedTag() throws IOException {
		try {
			new TemplateLoader().load("Unrecognized Tag", "This is a" + LS + " {{#Test}} " + LS + "test template" + LS + " {{" + LS + LS + "/}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(4, e.getLoaders().get(0).getLine());
			assertEquals(4, e.getLoaders().get(0).getColumn());
		}
	}

}
