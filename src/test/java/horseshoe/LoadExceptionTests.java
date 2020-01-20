package horseshoe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

public class LoadExceptionTests {

	private static final String LS = System.lineSeparator();

	@Test
	public void testAnnotationError() throws IOException {
		try {
			new TemplateLoader().load("Annotation", "{{#@Good}}\n test\n{{/}}\n{{#@Bad!}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(4, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	public void testInvertedSectionError() throws IOException {
		try {
			new TemplateLoader().load("Inverted Section", "{{#Test}}\n test\n{{^}}\n{{/}}\n{{^}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(5, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

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

	@Test
	public void testRepeatSectionError() throws IOException {
		try {
			new TemplateLoader().load("Repeat Section", "{{#Test}}\n test\n{{/}}\n{{#}}\n  {{#}}\n  {{/}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(5, e.getLoaders().get(0).getLine());
			assertEquals(5, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	public void testSectionCloseError() throws IOException {
		try {
			new TemplateLoader().load("Close Section", "{{#Test}}\n test\n{{/Bad}}\n{{#}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(3, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	public void testSectionCloseError2() throws IOException {
		try {
			new TemplateLoader().load("Close Section", " test\n{{/}}\n{{#Test}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(2, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	public void testSetDelimiterError() throws IOException {
		try {
			new TemplateLoader().load("Set Delimiter", " test\n{{=^^^ ^^^}}\n^^^#Test^^^\n^^^/^^^").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(2, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	public void testUnmatchedSection() throws IOException {
		try {
			new TemplateLoader().load("Unmatched Section", "{{#Test}} test\n{{#Test}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
		}
	}

}
