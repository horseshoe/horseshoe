package horseshoe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Settings class allows configuring different properties that are used when rendering a {@link Template}.
 */
public class Settings {

	/**
	 * Use the default (system) line endings when rendering the template.
	 */
	public static final String USE_DEFAULT_LINE_ENDINGS = System.lineSeparator();

	/**
	 * Use the line endings as they exist in the template being rendered.
	 */
	public static final String USE_TEMPLATE_LINE_ENDINGS = null;

	/**
	 * Do not escape any characters when rendering.
	 */
	public static final EscapeFunction DO_NOT_ESCAPE = null;

	private static final Pattern ESCAPE_PATTERN = Pattern.compile("[&<>\"']");

	/**
	 * Escape the text being rendered as HTML.
	 */
	public static final EscapeFunction ESCAPE_AS_HTML = new EscapeFunction() {
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
	 * An enumeration used to control access when checking identifiers in expressions.
	 */
	public static enum ContextAccess {
		/**
		 * All section scopes will be checked for an identifier in an expression.
		 */
		FULL,

		/**
		 * Only the current section scope and the root-level section scope will be checked for an identifier in an expression.
		 */
		CURRENT_AND_ROOT,

		/**
		 * Only the current section scope will be checked for an identifier in an expression.
		 */
		CURRENT_ONLY
	}

	/**
	 * An EscapeFunction is used to escape dynamic content (interpolated text) before it is rendered as output.
	 */
	public static interface EscapeFunction {
		/**
		 * Escapes the specified string.
		 *
		 * @param raw the raw string to escape
		 * @return the escaped string
		 */
		String escape(String raw);
	}

	/**
	 * Creates new mustache-compatible settings.
	 *
	 * @return new mustache-compatible settings
	 */
	public static Settings newMustacheSettings() {
		return new Settings().setContextAccess(ContextAccess.FULL).setEscapeFunction(ESCAPE_AS_HTML).setLineEnding(USE_TEMPLATE_LINE_ENDINGS);
	}

	private EscapeFunction escapeFunction = DO_NOT_ESCAPE;
	private ContextAccess contextAccess = ContextAccess.CURRENT_AND_ROOT;
	private String lineEnding = USE_DEFAULT_LINE_ENDINGS;

	/**
	 * Creates a new settings object with the default values.
	 */
	public Settings() {
	}

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
