package horseshoe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.PersistentStack;

public class RenderContext {

	public static interface EscapeFunction {
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
	 * Creates a new mustache-compatible render context.
	 *
	 * @return a new mustache-compatible render context
	 */
	public static RenderContext newMustacheRenderContext() {
		return new RenderContext().setAllowAccessToFullContextStack(true).setEscapeFunction(HTML_ESCAPE_FUNCTION).setLineEnding(KEEP_TEMPLATE_LINE_ENDINGS);
	}

	private EscapeFunction escapeFunction = NO_ESCAPE_FUNCTION;
	private final Map<String, Object> globalData = new LinkedHashMap<>();
	private final PersistentStack<Object> sectionData = new PersistentStack<>();

	private boolean allowAccessToFullContextStack = false;
	private final PersistentStack<String> indentation = new PersistentStack<>();
	private String lineEnding = DEFAULT_LINE_ENDING;

	/**
	 * Creates a default render context.
	 */
	public RenderContext() {
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
	 * Gets whether or not expressions have full access to the context stack.
	 *
	 * @return true if expressions have full access to the context stack, otherwise false
	 */
	public boolean getAllowAccessToFullContextStack() {
		return allowAccessToFullContextStack;
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
	 * Gets the indentation used by the rendering process.
	 *
	 * @return the indentation used by the rendering process
	 */
	PersistentStack<String> getIndentation() {
		return indentation;
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
	 * Gets the section data used by the rendering process.
	 *
	 * @return the section data used by the rendering process
	 */
	PersistentStack<Object> getSectionData() {
		return sectionData;
	}

	/**
	 * Puts
	 * @param key
	 * @param value
	 * @return
	 */
	public RenderContext put(final String key, final Object value) {
		globalData.put(key, value);
		return this;
	}

	/**
	 * Puts
	 * @param key
	 * @param value
	 * @return
	 */
	public RenderContext put(final Map<String, Object> value) {
		globalData.putAll(value);
		return this;
	}

	/**
	 * Resets the context, so it can be reused
	 */
	void reset() {
		sectionData.clear();
		indentation.clear();
	}

	/**
	 * Sets whether or not expressions have full access to the context stack.
	 *
	 * @param allowAccessToFullContextStack true to allow expressions have full access to the context stack, otherwise false
	 * @return this render context
	 */
	public RenderContext setAllowAccessToFullContextStack(final boolean allowAccessToFullContextStack) {
		this.allowAccessToFullContextStack = allowAccessToFullContextStack;
		return this;
	}

	/**
	 * Sets the escape function used by the rendering process.
	 *
	 * @param escapeFunction the escape function used by the rendering process. If null, the rendering process will not escape the text.
	 * @return this object
	 */
	public RenderContext setEscapeFunction(final EscapeFunction escapeFunction) {
		this.escapeFunction = escapeFunction;
		return this;
	}

	/**
	 * Sets the line ending used by the rendering process.
	 *
	 * @param lineEnding the line ending used by the rendering process. If null, the rendering process will use the line endings in the template.
	 * @return this object
	 */
	public RenderContext setLineEnding(final String lineEnding) {
		this.lineEnding = lineEnding;
		return this;
	}

}
