package horseshoe;

/**
 * An {@link EscapeFunction} is used to escape dynamic content (interpolated text) before it is rendered as output.
 */
public interface EscapeFunction {

	/**
	 * Escapes the specified string.
	 *
	 * @param raw the raw string to escape
	 * @return the escaped string
	 */
	String escape(String raw);

}
