package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class NamedExpressionTests {

	private static final String LS = System.lineSeparator();

	/**
	 * Splits the target string using the given pattern.
	 *
	 * @param pattern the pattern that will match a split of the string
	 * @param target the target string to split
	 * @return the list of strings from the target that match the pattern
	 */
	public static List<String> split(final Pattern pattern, final String target) {
		final List<String> list = new ArrayList<>();

		for (final Matcher matcher = pattern.matcher(target); matcher.find(); matcher.region(matcher.end(), target.length())) {
			list.add(matcher.group());
		}

		return list;
	}

	@Test
	void testIndentation() throws IOException, LoadException {
		assertEquals("	// This is a test comment that is super long, so the regular expression" + LS +
				"	// splitter can be tested to determine whether or not it works." + LS +
				"	// ThisIsAReallyLongSingleWordThatWillHelpTestIfSplittingASingleWordAcrossMult" + LS +
				"	// ipleLinesWorks." + LS +
				"	void myFunc()" + LS +
				"	{" + LS +
				"		printf(\"Hello, World!\\n\");" + LS +
				"	}" + LS,
				new TemplateLoader().load("Test", "{{< makeFunction }}\n{{ Comment -> '// ' + ~@'String'.join(\"" + LS + "\" + .indentation + '// ', ~@'" + NamedExpressionTests.class.getName() + "'.split(~/\\S(?:.{0,73}\\S)?(?=\\s|$)|\\S.{49}\\S{25}/, .)) }}\n" +
					"{{ Comment('This is a test comment that is super long, so the regular expression splitter can be tested to determine whether or not it works. ThisIsAReallyLongSingleWordThatWillHelpTestIfSplittingASingleWordAcrossMultipleLinesWorks.') }}\nvoid myFunc()\n{\n\tprintf(\"Hello, World!\\n\");\n}\n{{/}}\n\t{{> makeFunction }}\n").render(new Settings().addLoadableClasses(NamedExpressionTests.class), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	void testNamedExprCache() throws IOException, LoadException {
		assertEquals("Good, Good, Bad, ", new TemplateLoader().load("Test", "{{# true }}{{ Good() -> 'Good' }}{{ Good() }}, {{ Good() }}, {{ Good() -> 'Bad' }}{{ Good() }}{{/}}, {{ Good() }}").render(new Settings().setContextAccess(Settings.ContextAccess.CURRENT), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	void testNamedExprMethodCall() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader().load("Test", "{{ returnArg -> . }}{{ (returnArg('123') + '4').replace('1', '2') }}");
		final StringWriter writer = new StringWriter();
		template.render(settings, Collections.emptyMap(), writer);
		assertEquals("2234", writer.toString());
	}

	@Test
	void testNamedExprMethodCall2() throws IOException, LoadException {
		assertEquals("1231, 1BCD", new TemplateLoader().load("Test", "{{upper -> ./substring(0, 1) == \"A\" ? ./toUpperCase() + \"D\" : .}}{{upper(\"123a\").replaceAll(\"[aA]\", \"1\") + \", \" + upper(\"Abc\").replaceAll(\"[aA]\", \"1\")}}").render(new Settings().setContextAccess(Settings.ContextAccess.CURRENT), Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("1231, 1BCD", new TemplateLoader().load("Test", "{{upper -> ./substring(0, 1) == \"A\" ? ./toUpperCase() + \"D\" : .}}{{(upper(\"123a\") + \", \" + upper(\"Abc\")).replaceAll(\"[aA]\", \"1\")}}").render(new Settings().setContextAccess(Settings.ContextAccess.CURRENT), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	void testNamedExpressions() throws IOException, LoadException {
		assertEquals("  " + LS, new TemplateLoader().load("Upper", "{{func()->\"Hello!\"}}" + LS + "{{<a}}" + LS + "  {{func()}}" + LS + "{{/a}}" + LS + "{{>a}}").render(Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("ORIGINAL STRING-Original string" + LS, new TemplateLoader().load("Upper", "{{upper->toUpperCase()}}\n{{capitalize=>substring(0, 1).toUpperCase() + substring(1).toLowerCase()}}\n{{#\"orIgInal StrIng\"}}\n{{#charAt(1)}}\n{{upper(..)}}-{{capitalize(..)}}\n{{/}}\n{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("    ORIGINAL STRING-Original String" + LS, new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{>a}}\n  {{upper() + \"-\" + lower()}}\n{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("    ORIGINAL STRING-original string" + LS, new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{> a | * }}\n  {{upper() + \"-\" + lower()}}\n{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("    ORIGINAL STRING-original string" + LS, new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{> a | lower }}\n  {{upper() + \"-\" + lower()}}\n{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("    ORIGINAL STRING-original string" + LS, new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{> a | lower , lower ( ) }}\n  {{upper() + \"-\" + lower()}}\n{{/}}").render(Collections.emptyMap(), new StringWriter()).toString());
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{> a | badNamedExpression }}\n  {{upper() + \"-\" + lower()}}\n{{/}}"));
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{> a | lower ( ) , badNamedExpression }}\n  {{upper() + \"-\" + lower()}}\n{{/}}"));
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{> a | lower ( ) lower }}\n  {{upper() + \"-\" + lower()}}\n{{/}}"));
	}

	@Test
	void testNamedExpressionsWithArgs() throws IOException, LoadException {
		assertEquals(", , , c, c", new TemplateLoader().load("Multi-Arguments", "{{func(., /*no-arg*/ ,c)->c}}" + LS + "{{func()}}, {{func('a')}}, {{func('a','b')}}, {{func('a','b','c')}}, {{func('a','b','c','d')}}").render(Collections.emptyMap(), new StringWriter()).toString());
		assertEquals("abc:abc, abc:abc, a:abc, a:abc", new TemplateLoader().load("Multi-Arguments", "{{func(,,)->.+':'+..}}" + LS + "{{func2(a,b,c)->a+':'+..}}" + LS + "{{#a}}{{func()}}, {{func2()}}, {{func('a')}}, {{func2('a')}}{{/}}").render(Collections.singletonMap("a", "abc"), new StringWriter()).toString());
	}

	@Test
	void testNamedExpressionDupParam() throws IOException, LoadException {
		assertThrows(LoadException.class, () -> new TemplateLoader().load("Duplicate Parameter", "{{func(c,c)->c}}" + LS + "{{func()}}"));
	}

	@Test
	void testRecursiveFibonacci() throws IOException, LoadException {
		assertEquals("011235", new TemplateLoader().load("Test", "{{fib -> . > 1 ? fib(. - 1) + fib(. - 2) : .}}{{fib(0)}}{{fib(1)}}{{fib(2)}}{{fib(3)}}{{fib(4)}}{{fib(5)}}").render(Collections.emptyMap(), new StringWriter()).toString());
	}

}
