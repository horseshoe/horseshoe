package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class LoadExceptionTests {

	private static final String LS = System.lineSeparator();

	@Test
	void testAnnotationError() throws IOException {
		try {
			new TemplateLoader().load("Annotation", "{{#@Good}}\n test\n{{/}}\n{{#@Bad!}}\n{{/}}").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(4, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	void testInvalidAnonymousTag() {
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Invalid Anonymous Partial", "{{#>}}"));
	}

	@Test
	void testInvertedSectionError() throws IOException {
		try {
			new TemplateLoader().load("Inverted Section", "{{#Test}}\n test\n{{^}}\n{{/}}\n{{^}}\n{{/}}").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(5, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	void testLoadUnclosedTag() throws IOException {
		try {
			new TemplateLoader().load("Unclosed Tag", "This is a {{#Test}} test template {{/}").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(1, e.getLoaders().get(0).getLine());
			assertEquals(37, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	void testLoadUnrecognizedTag() throws IOException {
		try {
			new TemplateLoader().load("Unrecognized Tag", "This is a" + LS + " {{#Test}} " + LS + "test template" + LS + " {{" + LS + LS + "/}").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(4, e.getLoaders().get(0).getLine());
			assertEquals(4, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	void testRepeatSectionError() throws IOException {
		try {
			new TemplateLoader().load("Repeat Section", "{{#Test}}\n test\n{{/}}\n{{#}}\n  {{#}}\n  {{/}}\n{{/}}").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(5, e.getLoaders().get(0).getLine());
			assertEquals(5, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	void testSectionCloseError() throws IOException {
		try {
			new TemplateLoader().setExtensions(EnumSet.complementOf(EnumSet.of(Extension.SMART_END_TAGS))).load("Close Section", "{{#Test}}\n test\n{{/Bad}}\n{{#}}\n{{/}}").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(3, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	void testSectionCloseError2() throws IOException {
		try {
			new TemplateLoader().load("Close Section", " test\n{{/}}\n{{#Test}}\n{{/}}").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(2, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	void testSetDelimiterError() throws IOException {
		try {
			new TemplateLoader().load("Set Delimiter", " test\n{{=^^^ ^^^}}\n^^^#Test^^^\n^^^/^^^").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(2, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	void testTagError() throws IOException {
		try {
			new TemplateLoader().load("Bad Tag", " test\n{{%}}\n{{#Test}}\n{{/}}").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
			assertEquals(2, e.getLoaders().get(0).getLine());
			assertEquals(3, e.getLoaders().get(0).getColumn());
		}
	}

	@Test
	void testUnmatchedSection() throws IOException {
		try {
			new TemplateLoader().load("Unmatched Section", "{{#Test}} test\n{{#Test}}\n{{/}}").render(Collections.emptyMap(), new java.io.StringWriter());
			fail();
		} catch (final LoadException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	void testUnmatchedElseTag() {
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Documentative Else Test", "{{ IsTrue() -> true }}{{ IsFalse() -> false }}{{# IsTrue() }}correct{{^^ IsFalse() }}wrong{{/ IsTrue() }}"));
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Documentative Else Test", "{{ IsFalse() -> false }}{{^^ IsFalse() }}wrong{{/ IsFalse() }}"));
	}

}
