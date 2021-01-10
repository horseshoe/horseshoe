package horseshoe;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import horseshoe.internal.Expression;

/**
 * The Logger class is used to log messages during the {@link Template} rendering process.
 */
public abstract class Logger {

	static final class StoredError {
		private final Throwable throwable;
		private final StackTraceElement[] trace;

		StoredError(final Throwable throwable) {
			this.throwable = throwable;
			this.trace = throwable.getStackTrace();
		}

		@Override
		public boolean equals(Object object) {
			if (!(object instanceof StoredError) || !throwable.getClass().equals(((StoredError)object).throwable.getClass()) ||
					!Objects.equals(throwable.getMessage(), ((StoredError)object).throwable.getMessage())) {
				return false;
			}

			// Compare stack traces up to the last expression element
			final StackTraceElement[] otherTrace = ((StoredError)object).trace;
			final int length = Math.min(trace.length, otherTrace.length);
			int i;

			for (i = 0; i < length && trace[i].equals(otherTrace[i]); i++);

			for (int j = i; j < trace.length; j++) {
				if (Expression.class.getName().equals(trace[j].getClassName())) {
					return false;
				}
			}

			for (int j = i; j < otherTrace.length; j++) {
				if (Expression.class.getName().equals(otherTrace[j].getClassName())) {
					return false;
				}
			}

			return true;
		}

		@Override
		public int hashCode() {
			return Objects.hash(throwable.getClass(), throwable.getMessage());
		}
	}

	/**
	 * Creates a {@link Logger} that only logs each error once.
	 *
	 * @param logger the logger to wrap
	 * @return the new filtering logger
	 */
	public static Logger filterDuplicateErrors(final Logger logger) {
		return new Logger() {
			private final Set<StoredError> errors = new HashSet<>();

			@Override
			public void log(Level level, Throwable error, String message, Object... params) {
				if (error == null || errors.add(new StoredError(error))) {
					logger.log(level, error, message, params);
				}
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
	public static Logger filterNullErrors(final Logger logger) {
		return new Logger() {
			@Override
			public void log(Level level, Throwable error, String message, Object... params) {
				if (!(error instanceof NullPointerException)) {
					logger.log(level, error, message, params);
				}
			}
		};
	}

	/**
	 * Wraps a {@link java.util.logging.Logger} in a Horseshoe {@link Logger}.
	 *
	 * @param logger the logger to wrap
	 * @return the wrapped logger
	 */
	public static Logger wrap(final java.util.logging.Logger logger) {
		return new Logger() {
			@Override
			public void log(Level level, Throwable error, String message, Object... params) {
				final LogRecord record = new LogRecord(level, message);

				record.setThrown(error);
				record.setParameters(params);
				logger.log(record);
			}
		};
	}

	/**
	 * Log a message, with no arguments.
	 *
	 * @param level the level at which the message should be logged
	 * @param message the message to log
	 */
	public void log(final Level level, final String message) {
		log(level, message, (Object[])null);
	}

	/**
	 * Log a message, with multiple arguments.
	 *
	 * @param level the level at which the message should be logged
	 * @param message the message to log
	 * @param params the parameters that will be applied to the message
	 */
	public void log(final Level level, final String message, final Object... params) {
		log(level, null, message, params);
	}

	/**
	 * Log a message, with a thrown error that caused the logged message.
	 *
	 * @param level the level at which the message should be logged
	 * @param message the message to log
	 * @param error the error that caused this message to be logged
	 */
	public void log(final Level level, final String message, final Throwable error) {
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
	public abstract void log(final Level level, final Throwable error, final String message, final Object... params);

}
