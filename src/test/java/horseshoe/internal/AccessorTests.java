package horseshoe.internal;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import horseshoe.Helper;
import horseshoe.LoadException;
import horseshoe.Settings;

public class AccessorTests {

	@Test
	public void testStaticFieldAllMAX_VALUEs() throws IOException, LoadException {
		Helper.executeTest("Values: {{#Values}}{{.index}}) {{MAX_VALUE}}{{#.hasNext}}, {{/}}{{/}}", Collections.emptyMap(), new Settings(), Helper.loadMap("Values", Helper.loadList(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class)), "Values: 0) " + Byte.MAX_VALUE + ", 1) " + Short.MAX_VALUE + ", 2) " + Integer.MAX_VALUE + ", 3) " + Long.MAX_VALUE + ", 4) " + Float.MAX_VALUE + ", 5) " + Double.MAX_VALUE);
	}

	@Test
	public void testStaticMethod() throws IOException, LoadException {
		Helper.executeTest("Min: {{Math.`min:int,int`(Integer.MAX_VALUE, 0)}}, Max: {{Math.`max:float,float`(Integer.MIN_VALUE, 0)}}, Max: {{Math.`max:int,int`(Integer.MIN_VALUE, Byte.MAX_VALUE)}}", Collections.emptyMap(), new Settings(), Helper.loadMap("Math", Math.class, "Integer", Integer.class, "Byte", Byte.class), "Min: 0, Max: 0.0, Max: " + Byte.MAX_VALUE);
	}

}
