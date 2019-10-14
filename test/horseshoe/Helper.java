package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Helper {

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

	public static void executeTest(final String template, final LoadContext parseContext, final RenderContext renderContext, final String expected) throws LoadException, IOException {
		final Template t = new Template("Test", template, parseContext);
		final ByteArrayOutputStream os = new ByteArrayOutputStream();

		try (PrintStream ps = new PrintStream(os, true, StandardCharsets.UTF_16.name())) {
			t.render(renderContext, ps);
		}

		assertEquals(expected, os.toString(StandardCharsets.UTF_16.name()));
	}

	public static void executeMustacheTest(final String template, final Map<String, Object> data, final Map<String, Object> partialMap, final String expected) throws LoadException, IOException {
		final Map<String, String> partials = new LinkedHashMap<>();

		for (final Entry<String, Object> partial : partialMap.entrySet()) {
			partials.put(partial.getKey(), partial.getValue().toString());
		}

		executeTest(template, LoadContext.newMustacheLoadContext(partials), RenderContext.newMustacheRenderContext().put(data), expected);
	}

}
