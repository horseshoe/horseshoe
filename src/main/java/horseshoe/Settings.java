package horseshoe;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Set;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The {@link Settings} class allows configuring different properties that are used when rendering a {@link Template}.
 */
public class Settings {

	/**
	 * The error handler that ignores all errors.
	 */
	public static final ErrorHandler EMPTY_ERROR_HANDLER = (error, messageSupplier, context) -> { /* Intentionally left empty */ };

	/**
	 * The logger that sends messages to the {@link Template} class logger.
	 */
	public static final Logger DEFAULT_LOGGER = Logger.wrap(Template.LOGGER);

	/**
	 * The logger that consumes all messages without reporting them.
	 */
	public static final Logger EMPTY_LOGGER = (level, error, message, params) -> { /* Intentionally left empty */ };

	/**
	 * The escape function that does not escape any characters, returning the same string that was passed to it.
	 */
	public static final EscapeFunction EMPTY_ESCAPE_FUNCTION = raw -> raw;

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
	private final ErrorHandler loggingErrorHandler = (error, messageSupplier, context) -> getLogger().log(java.util.logging.Level.WARNING, messageSupplier.get(), error);
	private ErrorHandler errorHandler = getLoggingErrorHandler();
	private Logger logger = DEFAULT_LOGGER;
	private EscapeFunction escapeFunction = EMPTY_ESCAPE_FUNCTION;
	private String lineEndings = DEFAULT_LINE_ENDINGS;
	private boolean allowUnqualifiedClassNames = true;
	private final ClassMap loadableClasses = new ClassMap(DEFAULT_LOADABLE_CLASSES);

	static {
		final HashSet<Class<?>> defaultLoadableClasses = new HashSet<>(Arrays.asList(
				BigDecimal.class,
				BigInteger.class,
				Boolean.class,
				Byte.class,
				Character.class,
				Double.class,
				Float.class,
				Integer.class,
				Long.class,
				Short.class,

				Enum.class,
				Math.class,
				Object.class,
				StrictMath.class,
				String.class,

				Arrays.class,
				BitSet.class,
				Calendar.class,
				Collections.class,
				Currency.class,
				Date.class,
				EnumSet.class,
				GregorianCalendar.class,
				Matcher.class,
				Objects.class,
				Pattern.class,
				Random.class,

				SecureRandom.class,

				Collector.class,
				Collectors.class,
				DoubleStream.class,
				Duration.class,
				Instant.class,
				IntStream.class,
				LongStream.class,
				Optional.class,
				OptionalDouble.class,
				OptionalInt.class,
				OptionalLong.class,
				Spliterators.class,
				Stream.class,
				StreamSupport.class));

		DEFAULT_LOADABLE_CLASSES = Collections.unmodifiableSet(defaultLoadableClasses);
	}

	private final class ClassMap extends HashSet<Class<?>> {
		private static final long serialVersionUID = 1L;

		private final ConcurrentHashMap<String, Class<?>> qualifiedNameLookup = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<String, Class<?>> unqualifiedNameLookup = new ConcurrentHashMap<>();

		ClassMap() {
		}

		ClassMap(final Collection<Class<?>> collection) {
			this();
			addAll(collection);
		}

		@Override
		public synchronized boolean add(final Class<?> type) {
			if (super.add(type)) {
				qualifiedNameLookup.put(type.getName(), type);
				unqualifiedNameLookup.put(type.getSimpleName(), type);
				return true;
			}

			return false;
		}

		@Override
		public synchronized void clear() {
			qualifiedNameLookup.clear();
			unqualifiedNameLookup.clear();
			super.clear();
		}

		Class<?> get(final String name) {
			if (allowUnqualifiedClassNames()) {
				final Class<?> type = unqualifiedNameLookup.get(name);

				if (type != null) {
					return type;
				}
			}

			return qualifiedNameLookup.get(name);
		}

		@Override
		public Iterator<Class<?>> iterator() {
			final Iterator<Class<?>> iterator = super.iterator();

			return new Iterator<Class<?>>() {
				private Class<?> currentItem;

				@Override
				public boolean hasNext() {
					return iterator.hasNext();
				}

				@Override
				public Class<?> next() {
					currentItem = iterator.next();
					return currentItem;
				}

				@Override
				public void remove() {
					synchronized (ClassMap.this) {
						iterator.remove();
						qualifiedNameLookup.remove(currentItem.getName());
						unqualifiedNameLookup.remove(currentItem.getSimpleName());
					}
				}
			};
		}

		@Override
		public synchronized boolean remove(final Object object) {
			if (super.remove(object)) {
				qualifiedNameLookup.remove(((Class<?>)object).getName());
				unqualifiedNameLookup.remove(((Class<?>)object).getSimpleName());
				return true;
			}

			return false;
		}
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
		loadableClasses.addAll(Arrays.asList(classes));
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
	 * Gets the error handler used by the rendering process.
	 *
	 * @return the error handler used by the rendering process
	 */
	public ErrorHandler getErrorHandler() {
		return errorHandler;
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
	 * Gets a loadable class by name for use during the rendering process.
	 *
	 * @param className the name of the class to be used during the rendering process
	 * @return the class with the specified name, or null if no class could be found
	 */
	public Class<?> getLoadableClass(final String className) {
		return loadableClasses.get(className);
	}

	/**
	 * Gets the set of classes that can be loaded by the template during the rendering process.
	 *
	 * @return the set of classes that can be loaded by the template during the rendering process
	 */
	public Set<Class<?>> getLoadableClasses() {
		return loadableClasses;
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
	 * Gets the error handler that logs rendering errors as warnings.
	 *
	 * @return the error handler that logs rendering errors as warnings
	 */
	public ErrorHandler getLoggingErrorHandler() {
		return loggingErrorHandler;
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
	 * Sets the error handler used by the rendering process.
	 *
	 * @param errorHandler the error handler used by the rendering process. If null, the error handler will be set to the empty error handler.
	 * @return this object
	 */
	public Settings setErrorHandler(final ErrorHandler errorHandler) {
		this.errorHandler = (errorHandler == null ? EMPTY_ERROR_HANDLER : errorHandler);
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

}
