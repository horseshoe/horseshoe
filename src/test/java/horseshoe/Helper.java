package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

public class Helper {

	public static Object loadLambda(final Supplier<Object> value) {
		return value;
	}

	public static Object loadLambda(final Function<String, Object> value) {
		return value;
	}

	public static List<Object> loadList(final Object... values) {
		return Arrays.asList(values);
	}

	public static Map<String, Object> loadMap(final Object... values) {
		final Map<String, Object> map = new LinkedHashMap<>();

		for (int i = 0; i < values.length; i += 2) {
			map.put(values[i].toString(), values[i + 1]);
		}

		return map;
	}

	public static void executeTest(final Template template, final Settings settings, final Map<String, Object> data, final String expected) throws LoadException, IOException {
		try (final StringWriter writer = new StringWriter()) {
			template.render(settings, data, writer);
			assertEquals(expected, writer.toString());
		}
	}

	public static void executeTest(final String template, final Map<String, String> partials, final Settings settings, final Map<String, Object> data, final String expected) throws LoadException, IOException {
		executeTest(new TemplateLoader().add(partials).load("Test", template), settings, data, expected);
	}

	public static void executeMustacheTest(final String template, final Map<String, Object> data, final Map<String, Object> partialMap, final String expected) throws LoadException, IOException {
		final Map<String, String> partials = new LinkedHashMap<>();

		for (final Entry<String, Object> partial : partialMap.entrySet()) {
			partials.put(partial.getKey(), partial.getValue().toString());
		}

		executeTest(TemplateLoader.newMustacheLoader().add(partials).load("Mustache Test", template), Settings.newMustacheSettings(), data, expected);
	}

}
