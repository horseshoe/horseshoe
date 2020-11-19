package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class ArrayTests {

	@Test
	void testArrayLength() throws IOException, LoadException {
		assertEquals("5", new TemplateLoader().load("Test", "{{arr.length}}").render(new Settings(), Collections.singletonMap("arr", new int[] { 1, 2, 3, 4, 5 }), new StringWriter()).toString());
	}

	@Test
	void testArrayLengthNamedExpr() throws IOException, LoadException {
		assertEquals("5, ", new TemplateLoader().load("Test", "{{a -> a.length}}{{a([\"a\": arr])}}, {{arr.Length}}").render(new Settings(), Collections.singletonMap("arr", new int[] { 1, 2, 3, 4, 5 }), new StringWriter()).toString());
	}

	@Test
	void testArrayLengthNamedExprScope() throws IOException, LoadException {
		assertEquals("5", new TemplateLoader().load("Test", "{{a -> length}}{{a(arr)}}").render(new Settings(), Collections.singletonMap("arr", new int[] { 1, 2, 3, 4, 5 }), new StringWriter()).toString());
	}

}
