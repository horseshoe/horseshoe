package horseshoe;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExampleTest {

	@Test
	public void testExample() throws java.io.IOException, LoadException {
		final horseshoe.Context context = new horseshoe.Context();
		// final horseshoe.Context mustacheContext = horseshoe.Context.newMustacheContext();

		final horseshoe.Template template = new horseshoe.Template("Hello World", "{{{salutation}}}, {{ recipient }}!", context);

		context.put("salutation", "Hello");
		context.put("recipient", "world");

		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(context, writer);
		assertEquals("Hello, world!", writer.toString());
	}

}
