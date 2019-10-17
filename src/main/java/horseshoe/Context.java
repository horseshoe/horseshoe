package horseshoe;

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
	 * @param partials the partials to use when a partial is included in a template
	 * @return a new mustache-compatible context
	 */
	public static Context newMustacheContext(final Partials partials) {
		return new Context(partials).setAllowAccessToFullContextStack(true).setEscapeFunction(HTML_ESCAPE_FUNCTION).setLineEnding(KEEP_TEMPLATE_LINE_ENDINGS).setThrowOnPartialNotFound(false);
	}

	/**
	 * Creates a new mustache-compatible context.
	 *
	 * @return a new mustache-compatible context
	 */
	public static Context newMustacheContext() {
		return newMustacheContext(new Partials());
	}

	private EscapeFunction escapeFunction = NO_ESCAPE_FUNCTION;
	private boolean allowAccessToFullContextStack = false;
	private String lineEnding = DEFAULT_LINE_ENDING;
	private boolean throwOnPartialNotFound = true;
	final Partials partials;

	/**
	 * Creates a context using the specified partials.
	 *
	 * @param partials the partials to use when a partial is included in a template
	 */
	public Context(final Partials partials) {
		this.partials = partials;
	}

	/**
	 * Creates a new default context.
	 */
	public Context() {
		this(new Partials());
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
	 * Gets whether or not an exception will be thrown when a partial is not found during loading.
	 *
	 * @return true if an exception will be thrown when a partial is not found, otherwise false
	 */
	public boolean getThrowOnPartialNotFound() {
		return throwOnPartialNotFound;
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
