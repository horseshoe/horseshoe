package horseshoe;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A {@link Logger} is used to log messages during the {@link Template} rendering process.
 */
public interface Logger {

	/**
	 * Creates a {@link Logger} that only logs each error once.
	 *
	 * @param logger the logger to wrap
	 * @return the new filtering logger
	 */
	static Logger filterDuplicateErrors(final Logger logger) {
		final HashSet<ThrowableComparator> errors = new HashSet<>();

		return (Level level, Throwable error, String message, Object... params) -> {
			if (error == null || errors.add(new ThrowableComparator(error))) {
				logger.log(level, error, message, params);
			}
		};
	}

	/**
	 * Creates a {@link Logger} that filters out {@link java.lang.NullPointerException}s when rendering Horseshoe {@link Template}s.
	 * This allows {@link Template}s that would otherwise need to make significant use of safe operators (`?.`, `?[]`) to ignore null dereferences.
	 * Note that this will ignore all {@link java.lang.NullPointerException}s including those thrown in called methods.
	 *
	 * @param logger the logger to wrap
	 * @return the new filtering logger
	 */
	static Logger filterNullErrors(final Logger logger) {
		return (Level level, Throwable error, String message, Object... params) -> {
			if (!(error instanceof NullPointerException)) {
				logger.log(level, error, message, params);
			}
		};
	}

	/**
	 * Wraps a {@link java.util.logging.Logger} in a Horseshoe {@link Logger}.
	 *
	 * @param logger the logger to wrap
	 * @return the wrapped logger
	 */
	static Logger wrap(final java.util.logging.Logger logger) {
		return (Level level, Throwable error, String message, Object... params) -> {
			final LogRecord event = new LogRecord(level, message);

			event.setThrown(error);
			event.setParameters(params);
			logger.log(event);
		};
	}

	/**
	 * Log a message, with no arguments.
	 *
	 * @param level the level at which the message should be logged
	 * @param message the message to log
	 */
	default void log(final Level level, final String message) {
		log(level, message, (Object[])null);
	}

	/**
	 * Log a message, with multiple arguments.
	 *
	 * @param level the level at which the message should be logged
	 * @param message the message to log
	 * @param params the parameters that will be applied to the message
	 */
	default void log(final Level level, final String message, final Object... params) {
		log(level, null, message, params);
	}

	/**
	 * Log a message, with a thrown error that caused the logged message.
	 *
	 * @param level the level at which the message should be logged
	 * @param message the message to log
	 * @param error the error that caused this message to be logged
	 */
	default void log(final Level level, final String message, final Throwable error) {
		log(level, error, message, (Object[])null);
	}

	/**
	 * Log a message, with a thrown error that caused the logged message.
	 *
	 * @param level the level at which the message should be logged
	 * @param error the error that caused this message to be logged
	 * @param message the message to log
	 * @param params the parameters that will be applied to the message
	 */
	void log(final Level level, final Throwable error, final String message, final Object... params);

}
