package horseshoe;

/**
 * A {@link HaltRenderingException} is thrown when the die operator is encountered while rendering a {@link Template}.
 * It is the only exception besides {@link java.io.IOException} allowed to escape the rendering process.
 */
public class HaltRenderingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HaltRenderingException(final String message) {
		super(message);
	}

}
