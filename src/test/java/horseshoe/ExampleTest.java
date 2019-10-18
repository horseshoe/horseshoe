package horseshoe;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ExampleTest {

	@Test
	public void testExample() throws java.io.IOException, LoadException {
		// final horseshoe.Context mustacheContext = horseshoe.Context.newMustacheContext();

		final horseshoe.Template template = new horseshoe.Template("Hello World", "{{{salutation}}}, {{ recipient }}!", new horseshoe.Context());

		final Map<String, Object> data = new HashMap<>();
		data.put("salutation", "Hello");
		data.put("recipient", "world");

		final java.io.StringWriter writer = new java.io.StringWriter();
		template.render(data, writer);
		assertEquals("Hello, world!", writer.toString());
	}

}
