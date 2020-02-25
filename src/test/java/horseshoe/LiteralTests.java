package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Test;

public class LiteralTests {

	@Test
	public void testArrayLength() throws IOException, LoadException {
		assertEquals("5", new TemplateLoader().load("Test", "{{[1,2,3,4,5].length}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	public void testArrayLengthNamedExpr() throws IOException, LoadException {
		assertEquals("5", new TemplateLoader().load("Test", "{{a -> a.length}}{{a([\"a\": [1,2,3,4,5]])}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

	@Test
	public void testArrayLengthNamedExprScope() throws IOException, LoadException {
		assertEquals("5", new TemplateLoader().load("Test", "{{a -> length}}{{a([1,2,3,4,5])}}").render(new Settings(), Collections.emptyMap(), new StringWriter()).toString());
	}

}
