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

	public static void executeTest(final String template, final Context context, final Map<String, Object> data, final String expected) throws LoadException, IOException {
		final Template t = new Template("Test", template, context);
		try (StringWriter writer = new StringWriter()) {
			t.render(data, writer);
			assertEquals(expected, writer.toString());
		}
	}

	public static void executeMustacheTest(final String template, final Map<String, Object> data, final Map<String, Object> partialMap, final String expected) throws LoadException, IOException {
		final Partials partials = new Partials();
		for (final Entry<String, Object> partial : partialMap.entrySet()) {
			partials.add(partial.getKey(), partial.getValue().toString());
		}
		executeTest(template, Context.newMustacheContext(partials), data, expected);
	}

}
