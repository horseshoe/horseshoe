package horseshoe;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * The Settings class allows configuring different properties that are used when rendering a {@link Template}.
 */
public class Settings {

	/**
	 * The logger that sends messages to the {@link Template} class logger.
	 */
	public static final Logger DEFAULT_LOGGER = new Logger() {
		@Override
		public void log(final Level level, final String message) {
			Template.LOGGER.log(level, message);
		}

		@Override
		public void log(final Level level, final String message, final Object... params) {
			Template.LOGGER.log(level, message, params);
		}

		@Override
		public void log(final Level level, final String message, final Throwable error) {
			Template.LOGGER.log(level, message, error);
		}
	};

	/**
	 * The logger that consumes all messages without reporting them.
	 */
	public static final Logger EMPTY_LOGGER = new Logger() {
		@Override
		public void log(final Level level, final String message) { // Intentionally left empty
		}

		@Override
		public void log(final Level level, final String message, final Object... params) { // Intentionally left empty
		}

		@Override
		public void log(final Level level, final String message, final Throwable error) { // Intentionally left empty
		}
	};

	/**
	 * The escape function that does not escape any characters, returning the same string that was passed to it.
	 */
	public static final EscapeFunction EMPTY_ESCAPE_FUNCTION = new EscapeFunction() {
		@Override
		public String escape(final String raw) {
			return raw;
		}
	};

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
	private Logger logger = DEFAULT_LOGGER;
	private EscapeFunction escapeFunction = EMPTY_ESCAPE_FUNCTION;
	private String lineEndings = DEFAULT_LINE_ENDINGS;
	private final Set<String> loadableClasses = new HashSet<>(Arrays.asList(
			"java.lang.Integer", "Integer",
			"java.lang.Byte", "Byte",
			"java.lang.Short", "Short",
			"java.lang.Long", "Long",
			"java.math.BigInteger",
			"java.lang.Float", "Float",
			"java.lang.Double", "Double",
			"java.math.BigDecimal",
			"java.lang.Character", "Character",
			"java.lang.Boolean", "Boolean",

			"java.lang.Math", "Math",
			"java.lang.Enum", "Enum",
			"java.lang.String", "String",

			"java.util.Arrays",
			"java.util.BitSet",
			"java.util.Calendar",
			"java.util.Collections",
			"java.util.Currency",
			"java.util.Date",
			"java.util.EnumSet",
			"java.util.GregorianCalendar",
			"java.util.Objects",
			"java.util.regex.Matcher",
			"java.util.regex.Pattern",

			// Java 8 classes
			"java.time.Duration",
			"java.time.Instant",
			"java.util.Optional",
			"java.util.OptionalDouble",
			"java.util.OptionalInt",
			"java.util.OptionalLong",
			"java.util.Spliterators",
			"java.util.stream.StreamSupport",
			"java.util.stream.Stream",
			"java.util.stream.DoubleStream",
			"java.util.stream.IntStream",
			"java.util.stream.LongStream",
			"java.util.stream.Collector",
			"java.util.stream.Collectors"));

	/**
	 * An enumeration used to control access when checking identifiers in expressions.
	 */
	public enum ContextAccess {
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
	 * Gets the logger used by the rendering process.
	 *
	 * @return the logger used by the rendering process
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Gets the escape function used by the rendering process.
	 *
	 * @return the escape function used by the rendering process.
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
	 * Gets the classes that can be loaded by the template during the rendering process.
	 *
	 * @return the set of classes that can be loaded by the template during the rendering process
	 */
	public Set<String> getLoadableClasses() {
		return loadableClasses;
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
	 * Sets the logger used by the rendering process.
	 *
	 * @param logger the logger used by the rendering process. If null, the logger will be set to the empty logger.
	 * @return this object
	 */
	public Settings setLogger(final Logger logger) {
		this.logger = (logger == null ? EMPTY_LOGGER : logger);
		return this;
	}

	/**
	 * Sets the escape function used by the rendering process.
	 *
	 * @param escapeFunction the escape function used by the rendering process. If null, the escape function will be set to the empty escape function.
	 * @return this object
	 */
	public Settings setEscapeFunction(final EscapeFunction escapeFunction) {
		this.escapeFunction = (escapeFunction == null ? EMPTY_ESCAPE_FUNCTION : escapeFunction);
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
