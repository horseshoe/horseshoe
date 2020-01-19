package horseshoe;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;

import horseshoe.Settings.ContextAccess;

public class ContextAccessTests {

	public static class FieldClass {
		public String testField = "test";

		@Override
		public String toString() {
			return testField;
		}
	}

	@Test
	public void testMapWithContextAccessFull() throws LoadException, IOException {
		final Settings settings = new Settings()
				.setContextAccess(ContextAccess.FULL);
		final Template template = new TemplateLoader().load("Context Access Full", "{{Objects.toString(\"123\")}}" +
				"{{#map.entrySet()}}{{Objects.toString(getValue())}}{{/}}{{#789}}{{#toString()}}{{#charAt(0)}}{{nonExistantMethod()}}{{longValue()}}{{get(\"field\")}}{{#field}}{{nonExistantField}}{{testField}}{{/}}{{/}}{{/}}{{/}}");
		final Map<String, Object> data = new LinkedHashMap<>();
		data.put("Objects", Objects.class);
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("key", "value");
		data.put("map", map);
		data.put("field", new FieldClass());
		final StringWriter writer = new StringWriter();
		template.render(settings, data, writer);
		Assert.assertEquals("123value789" + data.get("field") + "test", writer.toString());
	}

	@Test
	public void testMapWithContextAccessRoot() throws LoadException, IOException {
		final Settings settings = new Settings()
				.setContextAccess(ContextAccess.CURRENT_AND_ROOT);
		final Template template = new TemplateLoader().load("Context Access Root", "{{Objects.toString(\"123\")}}" +
				"{{#map.entrySet()}}{{Objects.toString(getValue())}}{{/}}{{#789}}{{#toString()}}{{#charAt(0)}}{{longValue()}}{{entrySet()}}{{get(\"field\")}}{{#field}}{{nonExistantField}}{{testField}}{{/}}{{/}}{{/}}{{/}}");
		final Map<String, Object> data = new LinkedHashMap<>();
		data.put("Objects", Objects.class);
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("key", "value");
		data.put("map", map);
		data.put("field", new FieldClass());
		final StringWriter writer = new StringWriter();
		template.render(settings, data, writer);
		Assert.assertEquals("123value" + data.entrySet() + data.get("field") + "test", writer.toString());
	}

}
