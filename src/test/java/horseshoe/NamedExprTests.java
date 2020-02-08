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
		final Template template = new TemplateLoader()
				.load("Test", "{{returnArg -> .}}{{(returnArg(\"123\") + \"4\").`replace:CharSequence`(\"1\", \"2\")}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, Collections.emptyMap(), writer);
		assertEquals("2234", writer.toString());
	}

	@Test
	public void testNamedExpressions() throws IOException, LoadException {
		assertEquals("ORIGINAL STRING-Original string" + LS, new TemplateLoader().load("Upper", "{{upper->toUpperCase()}}\n{{capitalize=>substring(0, 1).toUpperCase() + substring(1).toLowerCase()}}\n{{#\"orIgInal StrIng\"}}\n{{#charAt(1)}}\n{{upper(..)}}-{{capitalize(..)}}\n{{/}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
		assertEquals(LS + "  ORIGINAL STRING-original string" + LS, new TemplateLoader().load("Upper-Lower", "{{<a}}{{lower->toLowerCase()}}{{/}}\n{{lower->toString()}}\n{{upper->toUpperCase()}}\n{{#\"Original String\"}}\n  {{>a}}\n  {{upper() + \"-\" + lower()}}\n{{/}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

}