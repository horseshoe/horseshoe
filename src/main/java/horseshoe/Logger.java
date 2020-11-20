package horseshoe;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * The Logger interface is used to log messages during the {@link Template} rendering process.
 */
public abstract class Logger {

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
