package horseshoe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {

	public static enum ContextAccess {
		FULL,
		CURRENT_AND_ROOT,
		CURRENT_ONLY
	}

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
	 * Creates new mustache-compatible settings.
	 *
	 * @return new mustache-compatible settings
	 */
	public static Settings newMustacheSettings() {
		return new Settings().setContextAccess(ContextAccess.FULL).setEscapeFunction(HTML_ESCAPE_FUNCTION).setLineEnding(KEEP_TEMPLATE_LINE_ENDINGS);
	}

	private EscapeFunction escapeFunction = NO_ESCAPE_FUNCTION;
	private ContextAccess contextAccess = ContextAccess.CURRENT_AND_ROOT;
	private String lineEnding = DEFAULT_LINE_ENDING;

	/**
	 * Gets the type of access to the context stack allowed during rendering.
	 *
	 * @return true the type of access to the context stack allowed during rendering
	 */
	public ContextAccess getContextAccess() {
		return contextAccess;
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
	 * Gets the line ending used by the rendering process.
	 *
	 * @return the line ending used by the rendering process. If null, the rendering process will use the line endings in the template.
	 */
	public String getLineEnding() {
		return lineEnding;
	}

	/**
	 * Sets the type of access to the context stack allowed during rendering.
	 *
	 * @param contextAccess the type of access to the context stack allowed during rendering
	 * @return this object
	 */
	public Settings setContextAccess(final ContextAccess contextAccess) {
		this.contextAccess = contextAccess;
		return this;
	}

	/**
	 * Sets the escape function used by the rendering process.
	 *
	 * @param escapeFunction the escape function used by the rendering process. If null, the rendering process will not escape the text.
	 * @return this object
	 */
	public Settings setEscapeFunction(final EscapeFunction escapeFunction) {
		this.escapeFunction = escapeFunction;
		return this;
	}

	/**
	 * Sets the line ending used by the rendering process.
	 *
	 * @param lineEnding the line ending used by the rendering process. If null, the rendering process will use the line endings in the template.
	 * @return this object
	 */
	public Settings setLineEnding(final String lineEnding) {
		this.lineEnding = lineEnding;
		return this;
	}

}
