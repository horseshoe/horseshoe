package horseshoe;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.Properties;

/**
 * The Settings class allows configuring different properties that are used when rendering a {@link Template}.
 */
public class Settings {

	/**
	 * The logger that sends messages to the {@link Template} class logger.
	 */
	public static final Logger DEFAULT_LOGGER = Logger.wrap(Template.LOGGER);

	/**
	 * The logger that consumes all messages without reporting them.
	 */
	public static final Logger EMPTY_LOGGER = new Logger() {
		@Override
		public void log(final Level level, final Throwable error, final String message, final Object... params) {
			// Intentionally left empty
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
		private String escapeStartingAt(final String raw, final String firstEscape, final int firstIndex) {
			final StringBuilder sb = new StringBuilder(raw.length() + 16);
			int start = firstIndex + 1;

			sb.append(raw, 0, firstIndex).append(firstEscape);

			for (int i = firstIndex + 1; i < raw.length(); i++) {
				switch (raw.charAt(i)) {
					case '&':
						sb.append(raw, start, i).append("&amp;");
						start = i + 1;
						break;
					case '<':
						sb.append(raw, start, i).append("&lt;");
						start = i + 1;
						break;
					case '>':
						sb.append(raw, start, i).append("&gt;");
						start = i + 1;
						break;
					case '"':
						sb.append(raw, start, i).append("&quot;");
						start = i + 1;
						break;
					case '\'':
						sb.append(raw, start, i).append("&#39;");
						start = i + 1;
						break;
					default:
						break;
				}
			}

			return sb.append(raw, start, raw.length()).toString();
		}

		@Override
		public String escape(final String raw) {
			for (int i = 0; i < raw.length(); i++) {
				switch (raw.charAt(i)) {
					case '&':
						return escapeStartingAt(raw, "&amp;", i);
					case '<':
						return escapeStartingAt(raw, "&lt;", i);
					case '>':
						return escapeStartingAt(raw, "&gt;", i);
					case '"':
						return escapeStartingAt(raw, "&quot;", i);
					case '\'':
						return escapeStartingAt(raw, "&#39;", i);
					default:
						break;
				}
			}

			return raw;
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

	/**
	 * The default loadable classes.
	 */
	public static final Set<Class<?>> DEFAULT_LOADABLE_CLASSES;

	private ContextAccess contextAccess = ContextAccess.CURRENT_AND_ROOT;
	private Logger logger = DEFAULT_LOGGER;
	private EscapeFunction escapeFunction = EMPTY_ESCAPE_FUNCTION;
	private String lineEndings = DEFAULT_LINE_ENDINGS;
	private boolean allowUnqualifiedClassNames = true;
	private final Set<Class<?>> loadableClasses = new LinkedHashSet<>(DEFAULT_LOADABLE_CLASSES);

	static {
		final Set<Class<?>> defaultLoadableClasses = new LinkedHashSet<>(Arrays.asList(
				Integer.class,
				Byte.class,
				Short.class,
				Long.class,
				BigInteger.class,
				Float.class,
				Double.class,
				BigDecimal.class,
				Character.class,
				Boolean.class,

				Math.class,
				StrictMath.class,
				Enum.class,
				String.class,

				Arrays.class,
				BitSet.class,
				Calendar.class,
				Collections.class,
				Currency.class,
				Date.class,
				EnumSet.class,
				GregorianCalendar.class,
				Objects.class,
				Matcher.class,
				Pattern.class));

		if (Properties.JAVA_VERSION >= 8) {
			for (final String className : Arrays.asList("java.time.Duration",
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
					"java.util.stream.Collectors")) {
				try {
					defaultLoadableClasses.add(Class.forName(className));
				} catch (final ClassNotFoundException e) {
					Template.LOGGER.log(Level.WARNING, "Failed to load class \"{0}\", even though Java 8 or later was detected.", className);
				}
			}
		}

		DEFAULT_LOADABLE_CLASSES = Collections.unmodifiableSet(defaultLoadableClasses);
	}

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
	 * Adds classes that can be loaded by the template during the rendering process.
	 *
	 * @param classes the classes to add that can be loaded by the template during the rendering process
	 * @return this object
	 */
	public Settings addLoadableClasses(final Class<?>... classes) {
		for (final Class<?> loadableClass : classes) {
			loadableClasses.add(loadableClass);
		}

		return this;
	}

	/**
	 * Adds classes that can be loaded by the template during the rendering process.
	 *
	 * @param classes the classes to add that can be loaded by the template during the rendering process
	 * @return this object
	 */
	public Settings addLoadableClasses(final Iterable<Class<?>> classes) {
		for (final Class<?> loadableClass : classes) {
			loadableClasses.add(loadableClass);
		}

		return this;
	}

	/**
	 * Gets whether or not unqualified class names can be used to load classes by the rendering process.
	 *
	 * @return true if unqualified class names can be used to load classes by the rendering process, otherwise false
	 */
	public boolean allowUnqualifiedClassNames() {
		return allowUnqualifiedClassNames;
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
	public Set<Class<?>> getLoadableClasses() {
		return loadableClasses;
	}

	/**
	 * Sets whether or not unqualified class names can be used to load classes by the rendering process.
	 *
	 * @param allowUnqualifiedClassNames true if unqualified class names can be used to load classes by the rendering process, otherwise false
	 * @return this object
	 */
	public Settings setAllowUnqualifiedClassNames(final boolean allowUnqualifiedClassNames) {
		this.allowUnqualifiedClassNames = allowUnqualifiedClassNames;
		return this;
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
