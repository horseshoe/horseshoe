package horseshoe;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import horseshoe.Settings.ContextAccess;

import org.junit.Assert;
import org.junit.Test;

public class SettingsTests {

	public static class FieldClass {
		public String testField = "test";

		@Override
		public String toString() {
			return testField;
		}
	}

	@Test
	public void testMapWithContextAccessFull() throws LoadException, IOException {
		final Settings settings = new Settings().setContextAccess(ContextAccess.FULL);
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
		final Settings settings = new Settings().setContextAccess(ContextAccess.CURRENT_AND_ROOT);
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

	@Test
	public void testEscapeFunctionAccess() {
		Assert.assertEquals(Settings.EMPTY_ESCAPE_FUNCTION, new Settings().getEscapeFunction());
		Assert.assertEquals(Settings.HTML_ESCAPE_FUNCTION, new Settings().setEscapeFunction(Settings.HTML_ESCAPE_FUNCTION).getEscapeFunction());
		Assert.assertEquals(Settings.EMPTY_ESCAPE_FUNCTION, new Settings().setEscapeFunction(Settings.EMPTY_ESCAPE_FUNCTION).getEscapeFunction());
		Assert.assertEquals(Settings.EMPTY_ESCAPE_FUNCTION, new Settings().setEscapeFunction(null).getEscapeFunction());
	}

	@Test
	public void testHTMLEscaping() {
		Assert.assertEquals("&amp;&lt;&gt;&quot;&#39;", Settings.HTML_ESCAPE_FUNCTION.escape("&<>\"'"));
		Assert.assertEquals("&lt;&gt;&quot;&#39;&amp;", Settings.HTML_ESCAPE_FUNCTION.escape("<>\"'&"));
		Assert.assertEquals("&gt;&quot;&#39;&amp;&lt;", Settings.HTML_ESCAPE_FUNCTION.escape(">\"'&<"));
		Assert.assertEquals("&quot;&#39;&amp;&lt;&gt;", Settings.HTML_ESCAPE_FUNCTION.escape("\"'&<>"));
		Assert.assertEquals("&#39;&amp;&lt;&gt;&quot;", Settings.HTML_ESCAPE_FUNCTION.escape("'&<>\""));
		Assert.assertEquals("Hello, World!", Settings.HTML_ESCAPE_FUNCTION.escape("Hello, World!"));
	}

	@Test
	public void testNoEscaping() {
		Assert.assertEquals("&<>\"'", Settings.EMPTY_ESCAPE_FUNCTION.escape("&<>\"'"));
	}

	@Test
	public void testLoggerAccess() {
		Assert.assertEquals(Settings.DEFAULT_LOGGER, new Settings().getLogger());
		Assert.assertEquals(Settings.DEFAULT_LOGGER, new Settings().setLogger(Settings.DEFAULT_LOGGER).getLogger());
		Assert.assertEquals(Settings.EMPTY_LOGGER, new Settings().setLogger(Settings.EMPTY_LOGGER).getLogger());
		Assert.assertEquals(Settings.EMPTY_LOGGER, new Settings().setLogger(null).getLogger());
	}

	@Test (expected = Test.None.class) // No exception expected
	public void testLoggers() {
		for (final Logger logger : new Logger[] { Settings.DEFAULT_LOGGER, Settings.EMPTY_LOGGER }) {
			logger.log(Level.INFO, "Info log for " + logger.toString());
			logger.log(Level.WARNING, "Warning log for {0}, with {1}", logger.toString(), "argument");
			logger.log(Level.SEVERE, "Severe log for " + logger.toString(), new IllegalArgumentException("Test exception for logger"));
		}
	}

}
