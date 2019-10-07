package horseshoe;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class TemplateTest {

	private static String Test(final String template, final Context context) throws ParseException, UnsupportedEncodingException {
		final Template t = new Template(new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		final ByteArrayOutputStream os = new ByteArrayOutputStream();

		try (PrintStream ps = new PrintStream(os, true, StandardCharsets.UTF_8.name())) {
			t.render(context, ps);
		}

		return os.toString(StandardCharsets.UTF_8.name());
	}

	@Test
	public void testParserGood() throws ParseException, UnsupportedEncodingException {
		Test("This is a" + System.lineSeparator() + " {{#Test}} " + System.lineSeparator() + "test template" + System.lineSeparator() + " {{" + System.lineSeparator() + System.lineSeparator() + "/}}", new Context());
		Test("This is a {{#Test}} test template {{/}}!", new Context());
	}

	@Test (expected = ParseException.class)
	public void testParserBad1() throws ParseException, UnsupportedEncodingException {
		Test("This is a {{#Test}} test template {{/}", new Context());
	}

	@Test (expected = ParseException.class)
	public void testParserBad2() throws ParseException, UnsupportedEncodingException {
		Test("This is a" + System.lineSeparator() + " {{#Test}} " + System.lineSeparator() + "test template" + System.lineSeparator() + " {{" + System.lineSeparator() + System.lineSeparator() + "/}", new Context());
	}

	@Test
	public void testDelimiter() throws ParseException, UnsupportedEncodingException {
		/*
		assertEquals("(Hey!)", Test("{{=<% %>=}}(<%text%>)", new Context("text", "Hey!")));
		assertEquals("(It worked!)", Test("({{=[ ]=}}[text])", new Context("text", "It worked!")));
		assertEquals("[" + System.lineSeparator() +
				"  I got interpolated." + System.lineSeparator() +
				"  |data|" + System.lineSeparator() +
				"  {{data}}" + System.lineSeparator() +
				"  I got interpolated." + System.lineSeparator() +
				"]", Test("[\r\n" +
				"{{#section}}\r\n" +
				"  {{data}}\r\n" +
				"  |data|\r\n" +
				"{{/section}}\r\n" +
				"{{= | | =}}\r\n" +
				"|#section|\r\n" +
				"  {{data}}\r\n" +
				"  |data|\r\n" +
				"|/section|\r\n" +
				"]", new Context("section", Boolean.TRUE).put("data", "I got interpolated.")));
		assertEquals("[" + System.lineSeparator() +
				"  I got interpolated." + System.lineSeparator() +
				"  |data|" + System.lineSeparator() +
				"  {{data}}" + System.lineSeparator() +
				"  I got interpolated." + System.lineSeparator() +
				"]", Test("[\r\n" +
				"{{^section}}\r\n" +
				"  {{data}}\r\n" +
				"  |data|\r\n" +
				"{{/section}}\r\n" +
				"{{= | | =}}\r\n" +
				"|^section|\r\n" +
				"  {{data}}\r\n" +
				"  |data|\r\n" +
				"|/section|\r\n" +
				"]", new Context("section", Boolean.FALSE).put("data", "I got interpolated.")));*/
		/*
		  - name: Partial Inheritence
		    desc: Delimiters set in a parent template should not affect a partial.
		    data: { value: 'yes' }
		    partials:
		      include: '.{{value}}.'
		    template: |
		      [ {{>include}} ]
		      {{= | | =}}
		      [ |>include| ]
		    expected: |
		      [ .yes. ]
		      [ .yes. ]
		  - name: Post-Partial Behavior
		    desc: Delimiters set in a partial should not affect the parent template.
		    data: { value: 'yes' }
		    partials:
		      include: '.{{value}}. {{= | | =}} .|value|.'
		    template: |
		      [ {{>include}} ]
		      [ .{{value}}.  .|value|. ]
		    expected: |
		      [ .yes.  .yes. ]
		      [ .yes.  .|value|. ]*/

		// Whitespace Sensitivity Tests
		assertEquals("|  |", Test("| {{=@ @=}} |", new Context()));
		assertEquals(" | " + System.lineSeparator(), Test(" | {{=@ @=}}\n", new Context()));
		assertEquals("Begin." + System.lineSeparator() +
				"End.", Test("Begin.\r\n" +
				"{{=@ @=}}\r\n" +
				"End.", new Context()));
		assertEquals("Begin." + System.lineSeparator() +
				"End.", Test("Begin.\r\n" +
				"  {{=@ @=}}\r\n" +
				"End.", new Context()));
		assertEquals("|" + System.lineSeparator() + "|", Test("|\r\n{{= @ @ =}}\r\n|", new Context()));
		assertEquals("=", Test("  {{=@ @=}}\n=", new Context()));
		assertEquals("=" + System.lineSeparator(), Test("=\n  {{=@ @=}}", new Context()));

		// Whitespace Insensitivity Tests
		assertEquals("||", Test("|{{= @   @ =}}|", new Context()));
	}
}
