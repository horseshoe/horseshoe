package horseshoe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;

class SettingsTests {

	public static class FieldClass {
		public String testField = "test";

		@Override
		public String toString() {
			return testField;
		}
	}

	@Test
	void testMapWithContextAccessFull() throws LoadException, IOException {
		final Settings settings = new Settings().setContextAccess(ContextAccess.FULL);
		final Template template = new TemplateLoader().load("Context Access Full", "{{ Objects.toString(\"123\") }}" +
				"{{# map.entrySet() }}{{ Objects.toString(getValue()) }}{{/}}{{# 789 }}{{# toString() }}{{# charAt(0) }}{{ nonExistantMethod() }}{{ longValue() }}{{ get(\"field\") }}{{# field }}{{ nonExistantField }}{{ testField }}{{/}}{{/}}{{/}}{{/}}");
		final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("Objects", Objects.class);
		final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("key", "value");
		data.put("map", map);
		data.put("field", new FieldClass());
		final StringWriter writer = new StringWriter();
		template.render(settings, data, writer);
		assertEquals("123value789" + data.get("field") + "test", writer.toString());
	}

	@Test
	void testMapWithContextAccessRoot() throws LoadException, IOException {
		final Settings settings = new Settings().setContextAccess(ContextAccess.CURRENT_AND_ROOT);
		final Template template = new TemplateLoader().load("Context Access Root", "{{ Objects.toString(\"123\") }}" +
				"{{# map.entrySet() }}{{ Objects.toString(getValue()) }}{{/}}{{# 789 }}{{# toString() }}{{# charAt(0) }}{{ longValue() }}{{ entrySet() }}{{ get(\"field\") }}{{# field }}{{ nonExistantField }}{{ testField }}{{/}}{{/}}{{/}}{{/}}");
		final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("Objects", Objects.class);
		final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("key", "value");
		data.put("map", map);
		data.put("field", new FieldClass());
		final StringWriter writer = new StringWriter();
		template.render(settings, data, writer);
		assertEquals("123value" + data.entrySet() + data.get("field") + "test", writer.toString());
	}

	@Test
	void testErrorHandlerAccess() {
		final Settings settings = new Settings();
		assertEquals(settings.getLoggingErrorHandler(), settings.getErrorHandler());
		assertEquals(settings.getLoggingErrorHandler(), settings.setErrorHandler(settings.getLoggingErrorHandler()).getErrorHandler());
		assertEquals(Settings.EMPTY_ERROR_HANDLER, new Settings().setErrorHandler(Settings.EMPTY_ERROR_HANDLER).getErrorHandler());
		assertEquals(Settings.EMPTY_ERROR_HANDLER, new Settings().setErrorHandler(null).getErrorHandler());
	}

	@Test
	void testEscapeFunctionAccess() {
		assertEquals(Settings.EMPTY_ESCAPE_FUNCTION, new Settings().getEscapeFunction());
		assertEquals(Settings.HTML_ESCAPE_FUNCTION, new Settings().setEscapeFunction(Settings.HTML_ESCAPE_FUNCTION).getEscapeFunction());
		assertEquals(Settings.EMPTY_ESCAPE_FUNCTION, new Settings().setEscapeFunction(Settings.EMPTY_ESCAPE_FUNCTION).getEscapeFunction());
		assertEquals(Settings.EMPTY_ESCAPE_FUNCTION, new Settings().setEscapeFunction(null).getEscapeFunction());
	}

	@Test
	void testHTMLEscaping() {
		assertEquals("&amp;&lt;&gt;&quot;&#39;", Settings.HTML_ESCAPE_FUNCTION.escape("&<>\"'"));
		assertEquals("&lt;&gt;&quot;&#39;&amp;", Settings.HTML_ESCAPE_FUNCTION.escape("<>\"'&"));
		assertEquals("&gt;&quot;&#39;&amp;&lt;", Settings.HTML_ESCAPE_FUNCTION.escape(">\"'&<"));
		assertEquals("&quot;&#39;&amp;&lt;&gt;", Settings.HTML_ESCAPE_FUNCTION.escape("\"'&<>"));
		assertEquals("&#39;&amp;&lt;&gt;&quot;", Settings.HTML_ESCAPE_FUNCTION.escape("'&<>\""));
		assertEquals("Hello, World!", Settings.HTML_ESCAPE_FUNCTION.escape("Hello, World!"));
	}

	@Test
	void testNoEscaping() {
		assertEquals("&<>\"'", Settings.EMPTY_ESCAPE_FUNCTION.escape("&<>\"'"));
	}

	@Test
	void testLoggerAccess() {
		assertEquals(Settings.DEFAULT_LOGGER, new Settings().getLogger());
		assertEquals(Settings.DEFAULT_LOGGER, new Settings().setLogger(Settings.DEFAULT_LOGGER).getLogger());
		assertEquals(Settings.EMPTY_LOGGER, new Settings().setLogger(Settings.EMPTY_LOGGER).getLogger());
		assertEquals(Settings.EMPTY_LOGGER, new Settings().setLogger(null).getLogger());
	}

	@Test
	void testLoggers() {
		for (final Logger logger : new Logger[] { Settings.DEFAULT_LOGGER, Settings.EMPTY_LOGGER }) {
			assertDoesNotThrow(() -> logger.log(Level.INFO, "Info log for " + logger.toString()));
			assertDoesNotThrow(() -> logger.log(Level.WARNING, "Warning log for {0}, with {1}", logger.toString(), "argument"));
			assertDoesNotThrow(() -> logger.log(Level.SEVERE, "Severe log for " + logger.toString(), new IllegalArgumentException("Test exception for logger")));
		}
	}

}
