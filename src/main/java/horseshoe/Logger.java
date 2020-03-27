package horseshoe;

import java.util.logging.Level;

/**
 * The Logger interface is used to log messages during the {@link Template} rendering process.
 */
public interface Logger {

	/**
	 * Log a message, with no arguments.
	 *
	 * @param level the level at which the message should be logged
	 * @param message the message to log
	 */
	public void log(final Level level, final String message);

	/**
	 * Log a message, with multiple arguments.
	 *
	 * @param level the level at which the message should be logged
	 * @param message the message to log
	 * @param params the parameters that will be applied to the message
	 */
	public void log(final Level level, final String message, final Object... params);

	/**
	 * Log a message, with a thrown error that caused the logged message.
	 *
	 * @param level the level at which the message should be logged
	 * @param message the message to log
	 * @param error the error that caused this message to be logged
	 */
	public void log(final Level level, final String message, final Throwable error);

}
