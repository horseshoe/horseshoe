package horseshoe;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Settings class allows configuring different properties that are used when rendering a {@link Template}.
 */
public class Settings {

	/**
	 * The error logger that sends error messages to the {@link Template} class logger.
	 */
	public static final ErrorLogger DEFAULT_ERROR_LOGGER = new ErrorLogger() {
		private final Logger logger = Logger.getLogger(Template.class.getName());

		@Override
		public void log(final String expression, final String location, final Throwable error) {
			if (error.getMessage() == null) {
				logger.log(Level.WARNING, "Encountered {0} while evaluating expression \"{1}\" ({2})", new Object[] { error.getClass().getName(), expression, location });
			} else {
				logger.log(Level.WARNING, "Encountered {0} while evaluating expression \"{1}\" ({2}): {3}", new Object[] { error.getClass().getName(), expression, location, error.getMessage() });
			}
		}
	};

	/**
	 * The error logger that consumes any error messages.
	 */
	public static final ErrorLogger EMPTY_ERROR_LOGGER = null;

	/**
	 * The escape function that does not escape any characters, returning the same string that was passed to it.
	 */
	public static final EscapeFunction EMPTY_ESCAPE_FUNCTION = null;

	/**
	 * The escape function that escapes a string as HTML, specifically the &amp;, &lt;, &gt;, ", and ' characters.
	 */
	public static final EscapeFunction HTML_ESCAPE_FUNCTION = new EscapeFunction() {
		@Override
		public String escape(final String raw) {
			final StringBuilder sb = new StringBuilder(raw.length() + 16);
			int start = 0;

			for (int i = 0; i < raw.length(); i++) {
				switch (raw.charAt(i)) {
				case '&':  sb.append(raw, start, i).append("&amp;");  start = i + 1; break;
				case '<':  sb.append(raw, start, i).append("&lt;");   start = i + 1; break;
				case '>':  sb.append(raw, start, i).append("&gt;");   start = i + 1; break;
				case '"':  sb.append(raw, start, i).append("&quot;"); start = i + 1; break;
				case '\'': sb.append(raw, start, i).append("&#39;");  start = i + 1; break;
				default: break;
				}
			}

			return start == 0 ? raw : sb.append(raw, start, raw.length()).toString();
		}
	};

	/**
	 * The system default line endings.
	 */
	public static final String DEFAULT_LINE_ENDINGS = System.lineSeparator();

	/**
	 * The line endings as they exist in the template.
	 */
	public static final String TEMPLATE_LINE_ENDINGS = null;

	private ContextAccess contextAccess = ContextAccess.CURRENT_AND_ROOT;
	private ErrorLogger errorLogger = DEFAULT_ERROR_LOGGER;
	private EscapeFunction escapeFunction = EMPTY_ESCAPE_FUNCTION;
	private String lineEndings = DEFAULT_LINE_ENDINGS;

	/**
	 * An enumeration used to control access when checking identifiers in expressions.
	 */
	public static enum ContextAccess {
		/**
		 * The context access that allows access to all section scopes when resolving an identifier in an expression.
		 */
		FULL,

		/**
		 * The context access that allows access to the current section scope and the root-level section scope when resolving an identifier in an expression.
		 */
		CURRENT_AND_ROOT,

		/**
		 * The context access that allows access to only the current section scope when resolving an identifier in an expression.
		 */
		CURRENT
	}

	/**
	 * An ErrorLogger is used to log errors during the evaluation of expressions.
	 */
	public static interface ErrorLogger {
		/**
		 * Logs the specified error.
		 *
		 * @param expression the expression that threw the error
		 * @param location the location of the expression that threw the error
		 * @param error the error to log
		 */
		public void log(final String expression, final String location, final Throwable error);
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
		return new Settings().setContextAccess(ContextAccess.FULL).setEscapeFunction(HTML_ESCAPE_FUNCTION).setLineEndings(TEMPLATE_LINE_ENDINGS);
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
	 * Gets the expression error logger used by the rendering process.
	 *
	 * @return the expression error logger used by the rendering process. If null, the rendering process will not log expression errors.
	 */
	public ErrorLogger getErrorLogger() {
		return errorLogger;
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
	 * Gets the line endings used by the rendering process.
	 *
	 * @return the line endings used by the rendering process. If null, the rendering process will use the line endings in the template.
	 */
	public String getLineEndings() {
		return lineEndings;
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
	 * Sets the expression error logger used by the rendering process.
	 *
	 * @param errorLogger the expression error logger used by the rendering process. If null, the rendering process will not log expression errors.
	 * @return this object
	 */
	public Settings setErrorLogger(final ErrorLogger errorLogger) {
		this.errorLogger = errorLogger;
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
	 * Sets the line endings used by the rendering process.
	 *
	 * @param lineEndings the line endings used by the rendering process. If null, the rendering process will use the line endings in the template.
	 * @return this object
	 */
	public Settings setLineEndings(final String lineEndings) {
		this.lineEndings = lineEndings;
		return this;
	}

}
