package horseshoe;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

public class ResolverTests {

	@Test
	public void testStaticFieldAllMAX_VALUEs() throws IOException, LoadException {
		Helper.executeTest("Values: {{#Values}}{{#[]}}, {{/}}{{MAX_VALUE}}{{/}}", Collections.emptyMap(), new Settings(), Helper.loadMap("Values", Helper.loadList(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class)), "Values: " + Byte.MAX_VALUE + ", " + Short.MAX_VALUE + ", " + Integer.MAX_VALUE + ", " + Long.MAX_VALUE + ", " + Float.MAX_VALUE + ", " + Double.MAX_VALUE);
	}

	@Test
	public void testStaticMethod() throws IOException, LoadException {
		Helper.executeTest("Min: {{Math.min(Integer.MAX_VALUE, 0)}}, Max: {{Math.max(Integer.MIN_VALUE, 0)}}, Max: {{Math.max(Integer.MIN_VALUE, Byte.MAX_VALUE)}}", Collections.emptyMap(), new Settings(), Helper.loadMap("Math", Math.class, "Integer", Integer.class, "Byte", Byte.class), "Min: 0, Max: 0, Max: " + Byte.MAX_VALUE);
	}

}
