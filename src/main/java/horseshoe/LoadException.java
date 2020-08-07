package horseshoe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * LoadExceptions can occur if an error is encountered while loading a {@link Template}. A LoadException can be used to determine the exact location and scope in a template that an error occurred.
 */
public class LoadException extends Exception {

	private static final long serialVersionUID = 1L;

	private final List<Loader> loaders = new ArrayList<>();

	/**
	 * Creates the exception message based on the load items and an error string.
	 *
	 * @param loaders the items being loaded with the exception occurred
	 * @param error the error associated with the exception
	 * @return an exception message containing the error and a trace of where the exception occurred
	 */
	private static String createMessage(final Iterable<Loader> loaders, final String error) {
		final StringBuilder sb = new StringBuilder(error);
		final Iterator<Loader> it = loaders.iterator();

		if (it.hasNext()) {
			Loader frame = it.next();
			sb.append(", at character ").append(frame.getColumn()).append(" of (").append(frame.getName()).append(':').append(frame.getLine()).append(')');

			while (it.hasNext()) {
				frame = it.next();
				sb.append(System.lineSeparator()).append(", included from (").append(frame.getName()).append(':').append(frame.getLine()).append(')');
			}
		}

		return sb.toString();
	}

	/**
	 * Creates a new load exception.
	 *
	 * @param loaders the items being loaded with the exception occurred
	 * @param error the error associated with the exception
	 */
	LoadException(final Iterable<Loader> loaders, final String error) {
		super(createMessage(loaders, error));

		for (final Loader loader : loaders) {
			this.loaders.add(loader);
		}
	}

	/**
	 * Creates a new load exception.
	 *
	 * @param loaders the items being loaded with the exception occurred
	 * @param error the error associated with the exception
	 * @param throwable the associated exception
	 */
	LoadException(final Iterable<Loader> loaders, final String error, final Throwable throwable) {
		super(createMessage(loaders, error), throwable);

		for (final Loader loader : loaders) {
			this.loaders.add(loader);
		}
	}

	/**
	 * Gets the list of active loaders when the exception occurred.
	 *
	 * @return the list of active loaders when the exception occurred
	 */
	public List<Loader> getLoaders() {
		return loaders;
	}

}
