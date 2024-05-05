package horseshoe;

import java.util.function.Supplier;

/**
 * An {@link ErrorHandler} is used to handle errors that occur during the {@link Template} rendering process.
 * All errors, excluding fatal JVM errors, fatal {@link java.io.IOException}s, and {@link HaltRenderingException}s, are reported via the {@link #onError(Throwable, Supplier, Object)} method before the rendering process continues.
 * (Unreported errors are thrown up the call stack and should be handled from the {@link Template} rendering methods.)
 * The default {@link ErrorHandler} logs reported errors as warning messages.
 */
public interface ErrorHandler {

	/**
	 * Handles an error that occurs during the rendering process.
	 *
	 * @param error the error that occurred
	 * @param messageSupplier a supplier of an error message
	 * @param context the object that was being evaluated when the error occurred
	 */
	void onError(final Throwable error, final Supplier<String> messageSupplier, final Object context);

}
