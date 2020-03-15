package horseshoe.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import horseshoe.Helper;
import horseshoe.LoadException;
import horseshoe.Settings;
import horseshoe.TemplateLoader;

public class AccessorTests {

	@Test
	public void testStaticFields() throws IOException, LoadException {
		assertEquals("Values: (0) " + Byte.MAX_VALUE + ", 1) " + Short.MAX_VALUE + ", 2) " + Integer.MAX_VALUE + ", 3) " + Long.MAX_VALUE + ", 4) " + Float.MAX_VALUE + ", 5) " + Double.MAX_VALUE + ", 6) ", new TemplateLoader().load("Static Fields", "Values: {{.badInternal}}{{#Values}}{{#.isFirst}}({{/}}{{.index}}) {{.?.?MAX_VALUE}}{{#.hasNext}}, {{/}}{{/}}").render(new Settings(), Helper.loadMap("Values", Helper.loadList(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Object.class)), new java.io.StringWriter()).toString());
	}

	@Test
	public void testStaticMethod() throws IOException, LoadException {
		assertEquals("Min: 0, Max: 0.0, Max: " + Byte.MAX_VALUE, new TemplateLoader().load("Static Methods", "Min: {{Math.`min:int,int`(Integer.MAX_VALUE, 0)}}, Max: {{Math.`max:float,float`(Integer.MIN_VALUE, 0)}}, Max: {{Math.`max:int,int`(Integer.MIN_VALUE, Byte.MAX_VALUE)}}").render(new Settings(), Helper.loadMap("Math", Math.class, "Integer", Integer.class, "Byte", Byte.class), new java.io.StringWriter()).toString());
	}

}
