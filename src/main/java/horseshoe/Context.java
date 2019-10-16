package horseshoe;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Context {

	public static interface EscapeFunction {
		/**
		 * Escapes the specified string.
		 *
		 * @param raw the raw string to escape
		 * @return the escaped string
		 */
		String escape(String raw);
	}

	public static final String DEFAULT_LINE_ENDING = System.lineSeparator();
	public static final String KEEP_TEMPLATE_LINE_ENDINGS = null;

	public static final EscapeFunction NO_ESCAPE_FUNCTION = null;

	private static final Pattern ESCAPE_PATTERN = Pattern.compile("[&<>\"']");
	public static final EscapeFunction HTML_ESCAPE_FUNCTION = new EscapeFunction() {
		@Override
		public String escape(final String raw) {
			final StringBuilder sb = new StringBuilder();
			final Matcher matcher = ESCAPE_PATTERN.matcher(raw);
			int start = 0;

			while (matcher.find()) {
				final int found = matcher.start();
				sb.append(raw, start, found);

				switch (raw.charAt(found)) {
				case '&':  sb.append("&amp;");  break;
				case '<':  sb.append("&lt;");   break;
				case '>':  sb.append("&gt;");   break;
				case '"':  sb.append("&quot;"); break;
				case '\'': sb.append("&#39;");  break;
				default: throw new IllegalArgumentException("");
				}

				start = matcher.end();
			}

			return sb.append(raw, start, raw.length()).toString();
		}
	};

	/**
	 * Creates a new mustache-compatible context using the specified string partials.
	 *
	 * @param stringPartials the string partials to use when a partial is included in a template
	 * @return a new mustache-compatible context
	 */
	public static Context newMustacheContext(final Map<String, String> stringPartials) {
		return new Context(stringPartials).setAllowAccessToFullContextStack(true).setEscapeFunction(HTML_ESCAPE_FUNCTION).setLineEnding(KEEP_TEMPLATE_LINE_ENDINGS).setThrowOnPartialNotFound(false);
	}

	/**
	 * Creates a new mustache-compatible context.
	 *
	 * @return a new mustache-compatible context
	 */
	public static Context newMustacheContext() {
		return newMustacheContext(new HashMap<String, String>());
	}

	private Charset charset = StandardCharsets.UTF_8;
	private EscapeFunction escapeFunction = NO_ESCAPE_FUNCTION;
	private final Map<String, Object> globalData = new LinkedHashMap<>();

	private boolean allowAccessToFullContextStack = false;
	private String lineEnding = DEFAULT_LINE_ENDING;

	final Map<String, Template> partials = new LinkedHashMap<>();
	private final Map<String, String> stringPartials = new LinkedHashMap<>();
	private final List<Path> includeDirectories = new ArrayList<>();
	private boolean throwOnPartialNotFound = true;

	/**
	 * Creates a context using the specified string partials and include directories.
	 *
	 * @param stringPartials the string partials to use when a partial is included in a template
	 * @param includeDirectories the list of directories used to locate partial files included in a template
	 */
	public Context(final Map<String, String> stringPartials, final Iterable<? extends Path> partialDirectories) {
		this.stringPartials.putAll(stringPartials);

		for (final Path path : partialDirectories) {
			this.includeDirectories.add(path);
		}
	}

	/**
	 * Creates a context using the specified string partials. The default list of include directories contains only the current directory.
	 *
	 * @param stringPartials the string partials to use when a partial is included in a template
	 */
	public Context(final Map<String, String> stringPartials) {
		this(stringPartials, Collections.singletonList(Paths.get(".")));
	}

	/**
	 * Creates a context using the specified include directories.
	 *
	 * @param includeDirectories the list of directories used to locate partial files included in a template
	 */
	public Context(final Iterable<? extends Path> includeDirectories) {
		this(new HashMap<String, String>(), includeDirectories);
	}

	/**
	 * Creates a default context. The default list of include directories contains only the current directory.
	 */
	public Context() {
		this(new HashMap<String, String>());
	}

	/**
	 * Gets the line endings used by the rendering process.
	 *
	 * @return the line endings used by the rendering process. If null, the rendering process will use the line endings in the template.
	 */
	public Object get(final String key) {
		return globalData.get(key);
	}

	/**
	 * Gets whether or not expressions have full access to the context stack during rendering.
	 *
	 * @return true if expressions have full access to the context stack, otherwise false
	 */
	public boolean getAllowAccessToFullContextStack() {
		return allowAccessToFullContextStack;
	}

	/**
	 * Gets the character set used for loading templates.
	 *
	 * @return the character set used for loading templates
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Gets the escape function used by the rendering process.
	 *
	 * @return the escape function used by the rendering process. If null, the rendering process will not escape the text.
	 */
	public EscapeFunction getEscapeFunction() {
		return escapeFunction;
	}

	/**
	 * Gets the global data used by the rendering process.
	 *
	 * @return the global data used by the rendering process
	 */
	Map<String, Object> getGlobalData() {
		return globalData;
	}

	/**
	 * Gets the list of directories used to locate partial files included in a template. The list of string partials is always searched first.
	 *
	 * @return the list of directories used to locate partial files included in a template
	 */
	public List<Path> getIncludeDirectories() {
		return includeDirectories;
	}

	/**
	 * Gets the line ending used by the rendering process.
	 *
	 * @return the line ending used by the rendering process. If null, the rendering process will use the line endings in the template.
	 */
	public String getLineEnding() {
		return lineEnding;
	}

	/**
	 * Gets the map of string partials. This map is used to locate partials included in a template. It is searched before files in the include directories.
	 *
	 * @return the map of string partials
	 */
	public Map<String, String> getStringPartials() {
		return stringPartials;
	}

	/**
	 * Gets whether or not an exception will be thrown when a partial is not found during loading.
	 *
	 * @return true if an exception will be thrown when a partial is not found, otherwise false
	 */
	public boolean getThrowOnPartialNotFound() {
		return throwOnPartialNotFound;
	}

	/**
	 * Puts the specified key and value into the global data.
	 *
	 * @param key the key to use when inserting the data
	 * @param value the value to insert
	 * @return this context
	 */
	public Context put(final String key, final Object value) {
		globalData.put(key, value);
		return this;
	}

	/**
	 * Puts all items in the map into the global data.
	 *
	 * @param map the items to put into the global data
	 * @return this context
	 */
	public Context put(final Map<String, Object> map) {
		globalData.putAll(map);
		return this;
	}

	/**
	 * Sets whether or not expressions have full access to the context stack during rendering.
	 *
	 * @param allowAccessToFullContextStack true to allow expressions have full access to the context stack, otherwise false
	 * @return this context
	 */
	public Context setAllowAccessToFullContextStack(final boolean allowAccessToFullContextStack) {
		this.allowAccessToFullContextStack = allowAccessToFullContextStack;
		return this;
	}

	/**
	 * Sets the character set used for loading templates
	 *
	 * @param charset the character set used for loading templates
	 * @return this context
	 */
	public Context setCharset(final Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * Sets the escape function used by the rendering process.
	 *
	 * @param escapeFunction the escape function used by the rendering process. If null, the rendering process will not escape the text.
	 * @return this object
	 */
	public Context setEscapeFunction(final EscapeFunction escapeFunction) {
		this.escapeFunction = escapeFunction;
		return this;
	}

	/**
	 * Sets the line ending used by the rendering process.
	 *
	 * @param lineEnding the line ending used by the rendering process. If null, the rendering process will use the line endings in the template.
	 * @return this object
	 */
	public Context setLineEnding(final String lineEnding) {
		this.lineEnding = lineEnding;
		return this;
	}

	/**
	 * Sets whether or not an exception will be thrown when a partial is not found during loading.
	 *
	 * @param throwOnPartialNotFound true to throw an exception when a partial is not found, otherwise false
	 * @return this context
	 */
	public Context setThrowOnPartialNotFound(final boolean throwOnPartialNotFound) {
		this.throwOnPartialNotFound = throwOnPartialNotFound;
		return this;
	}

}
