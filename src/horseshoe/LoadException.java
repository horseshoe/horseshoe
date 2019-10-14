package horseshoe;

import java.util.Iterator;
import java.util.List;

import horseshoe.internal.Loader;

public class LoadException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates the exception message based on the load items and an error string.
	 *
	 * @param loaders the items being loaded with the exception occurred
	 * @param error the error associated with the exception
	 * @return an exception message containing the error and a trace of where the exception occurred
	 */
	private static String createMessage(final List<Loader> loaders, final String error) {
		final StringBuilder sb = new StringBuilder(error);
		final Iterator<Loader> it = loaders.iterator();

		if (it.hasNext()) {
			Loader frame = it.next();
			sb.append(" at ").append(frame.getName()).append(":").append(frame.getLine()).append(",").append(frame.getColumn());

			while (it.hasNext()) {
				frame = it.next();
				sb.append(System.lineSeparator()).append(" included from ").append(frame.getName()).append(":").append(frame.getLine()).append(",").append(frame.getColumn());
			}
		}

		return sb.toString();
	}

	private final List<Loader> loaders;

	/**
	 * Creates a new load exception
	 *
	 * @param loaders the items being loaded with the exception occurred
	 * @param error the error associated with the exception
	 */
	public LoadException(final List<Loader> loaders, final String error) {
		super(createMessage(loaders, error));
		this.loaders = loaders;
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
