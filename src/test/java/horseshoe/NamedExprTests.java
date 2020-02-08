package horseshoe;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class NamedExprTests {

	@Test
	public void testNamedExprMethodCall() throws IOException, LoadException {
		final Settings settings = new Settings().setContextAccess(Settings.ContextAccess.CURRENT);
		final Template template = new TemplateLoader()
				.load("Test", "{{returnArg -> .}}{{returnArg(\"123\").replace(\"1\", \"2\")}}");
		final StringWriter writer = new StringWriter();
		template.render(settings, Collections.emptyMap(), writer);
		Assert.assertEquals("223", writer.toString());
	}

}