package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Test;

public class NamedExprTests {

	private static final String LS = System.lineSeparator();

	@Test
	public void testNamedExprMethodCall() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader().load("Test", "{{returnArg -> .}}{{(returnArg(\"123\") + \"4\").replace(\"1\", \"2\")}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, Collections.emptyMap(), writer);
		assertEquals("2234", writer.toString());
	}

	@Test
	public void testNamedExprMethodCall2() throws IOException, LoadException {
		assertEquals("1231, 1BCD", new TemplateLoader().load("Test", "{{upper -> ./substring(0, 1) == \"A\" ? ./toUpperCase() + \"D\" : .}}{{upper(\"123a\").replaceAll(\"[aA]\", \"1\") + \", \" + upper(\"Abc\").replaceAll(\"[aA]\", \"1\")}}").render(new Settings().setContextAccess(Settings.ContextAccess.CURRENT), Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("1231, 1BCD", new TemplateLoader().load("Test", "{{upper -> ./substring(0, 1) == \"A\" ? ./toUpperCase() + \"D\" : .}}{{(upper(\"123a\") + \", \" + upper(\"Abc\")).replaceAll(\"[aA]\", \"1\")}}").render(new Settings().setContextAccess(Settings.ContextAccess.CURRENT), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	public void testNamedExpressions() throws IOException, LoadException {
		assertEquals("  " + LS, new TemplateLoader().load("Upper", "{{func()->\"Hello!\"}}" + LS + "{{<a}}" + LS + "  {{func()}}" + LS + "{{/a}}" + LS + "{{>a}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("ORIGINAL STRING-Original string" + LS, new TemplateLoader().load("Upper", "{{upper->toUpperCase()}}\n{{capitalize=>substring(0, 1).toUpperCase() + substring(1).toLowerCase()}}\n{{#\"orIgInal StrIng\"}}\n{{#charAt(1)}}\n{{upper(..)}}-{{capitalize(..)}}\n{{/}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
		assertEquals(LS + "    ORIGINAL STRING-original string" + LS, new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{>a}}\n  {{upper() + \"-\" + lower()}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	public void testNamedExpressionsWithArgs() throws IOException, LoadException {
		assertEquals(", , , c, c", new TemplateLoader().load("Multi-Arguments", "{{func(., /*no-arg*/ ,c)->c}}" + LS + "{{func()}}, {{func('a')}}, {{func('a','b')}}, {{func('a','b','c')}}, {{func('a','b','c','d')}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("abc:abc, abc:abc, a:abc, a:abc", new TemplateLoader().load("Multi-Arguments", "{{func(,,)->.+':'+..}}" + LS + "{{func2(a,b,c)->a+':'+..}}" + LS + "{{#a}}{{func()}}, {{func2()}}, {{func('a')}}, {{func2('a')}}{{/}}").render(new Settings(), Collections.singletonMap("a", "abc"), new StringWriter()).toString());
	}

	@Test (expected = LoadException.class)
	public void testNamedExpressionDupParam() throws IOException, LoadException {
		assertEquals("?", new TemplateLoader().load("Duplicate Parameter", "{{func(c,c)->c}}" + LS + "{{func()}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	public void testRecursiveFibonacci() throws IOException, LoadException {
		assertEquals("011235", new TemplateLoader().load("Test", "{{fib -> . > 1 ? fib(. - 1) + fib(. - 2) : .}}{{fib(0)}}{{fib(1)}}{{fib(2)}}{{fib(3)}}{{fib(4)}}{{fib(5)}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

}
