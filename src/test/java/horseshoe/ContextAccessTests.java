package horseshoe;

import horseshoe.Settings.ContextAccess;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;

public class ContextAccessTests {

	@Test
	public void testMapWithContextAccessFull() throws LoadException, IOException {
		final Settings settings = new Settings()
				.setContextAccess(ContextAccess.FULL);
		final Template template = new TemplateLoader().load(null, "{{Objects.toString(\"123\")}}" +
				"{{#map.entrySet()}}{{Objects.toString(getValue())}}{{/}}");
		final Map<String, Object> data = new HashMap<>();
		data.put("Objects", Objects.class);
		final Map<String, Object> map = new HashMap<>();
		map.put("key", "value");
		data.put("map", map);
		final StringWriter writer = new StringWriter();
		template.render(settings, data, writer);
		Assert.assertEquals("123value", writer.toString());
	}

}