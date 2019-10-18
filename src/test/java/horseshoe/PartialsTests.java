package horseshoe;

import static horseshoe.Helper.loadMap;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

public final class PartialsTests {

	@Test
	public void testNestedPartials() throws IOException, LoadException {
		final Partials p = new Partials();
		p.add("f", "{{#a}}{{>g}}{{b}}{{/a}}{{^a}}{{x}}{{/a}}")
				.add("g", "{{#x}}{{>f}}{{x}}{{/x}}");
		final Context c = new Context(p).setAllowAccessToFullContextStack(false);
		final Template t = new Template(null, "{{>g}}", c);
		final StringWriter writer = new StringWriter();
		t.render(loadMap("a", loadMap("b", 2, "x", false), "x", true), writer);
		Assert.assertEquals("2true", writer.toString());
		// FIXME: StackOverflowError
	}

	@Test
	public void testRecursivePartial() throws IOException, LoadException {
		final Partials p = new Partials().add("f", "{{b}}{{#a}}{{>f}}{{/a}}");
		final Context c = new Context(p).setAllowAccessToFullContextStack(false);
		final Template t = new Template(null, "{{>f}}", c);
		final StringWriter writer = new StringWriter();
		t.render(loadMap("a", loadMap("a", loadMap("b", 4), "b", 2), "b", 3), writer);
		Assert.assertEquals("324", writer.toString());
		// FIXME: StackOverflowError
	}

}