package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

	/**
	 * Loads a lambda from a supplier.
	 *
	 * @param value the supplier
	 * @return the lambda
	 */
	public static Object loadLambda(final Supplier<Object> value) {
		return value;
	}

	/**
	 * Loads a lambda from a function.
	 *
	 * @param value the function
	 * @return the lambda
	 */
	public static Object loadLambda(final Function<String, Object> value) {
		return value;
	}

	/**
	 * Loads a list from a series of values.
	 *
	 * @param values the series of values
	 * @return the resulting list
	 */
	public static List<Object> loadList(final Object... values) {
		return Arrays.asList(values);
	}

	/**
	 * Loads a map from a series of values. The entries specify a repeating sequence of a key followed by a value. Each key is converted to a string.
	 *
	 * @param values the values that make up the map
	 * @return the resulting map
	 */
	public static Map<String, Object> loadMap(final Object... values) {
		final Map<String, Object> map = new LinkedHashMap<>();

		for (int i = 0; i < values.length; i += 2) {
			map.put(values[i].toString(), values[i + 1]);
		}

		return map;
	}

	/**
	 * Executes a test using the specified parameters.
	 *
	 * @param template the template
	 * @param settings the settings used when rendering the template
	 * @param data the data used when rendering the template
	 * @param expected the expected result of rendering the template
	 * @throws IOException if an exception occurs while rendering (should never happen, since rendering is done with a StringWriter)
	 */
	public static void executeTest(final Template template, final Settings settings, final Map<String, Object> data, final String expected) throws IOException {
		try (final StringWriter writer = new StringWriter()) {
			template.render(settings, data, writer);
			assertEquals(expected, writer.toString());
		}
	}

	/**
	 * Executes a Mustache test using the specified parameters.
	 *
	 * @param template the string value of the template
	 * @param data the data used when rendering the template
	 * @param partialMap the map of partials to load when loading the template
	 * @param expected the expected result of rendering the template
	 * @throws LoadException if an exception occures while loading the template
	 * @throws IOException if an exception occurs while rendering (should never happen, since rendering is done with a StringWriter)
	 */
	public static void executeMustacheTest(final String template, final Map<String, Object> data, final Map<String, Object> partialMap, final String expected) throws LoadException, IOException {
		final TemplateLoader loader = TemplateLoader.newMustacheLoader();

		for (final Entry<String, Object> partial : partialMap.entrySet()) {
			loader.put(partial.getKey(), partial.getValue().toString());
		}

		executeTest(loader.load("Mustache Test", template), Settings.newMustacheSettings(), data, expected);
	}

}
