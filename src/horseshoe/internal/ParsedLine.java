package horseshoe.internal;

public class ParsedLine {

	private final String line;
	private final String ending;

	/**
	 * Creates a new parsed line from the line's content and ending.
	 *
	 * @param line the text of the parsed line
	 * @param ending the line ending of the parsed line
	 */
	public ParsedLine(final String line, final String ending) {
		this.line = line;
		this.ending = ending;
	}

	/**
	 * Gets the ending of the parsed line.
	 *
	 * @return the ending of the parsed line
	 */
	public String getEnding() {
		return ending;
	}

	/**
	 * Gets the text of the parsed line.
	 *
	 * @return the text of the parsed line
	 */
	public String getLine() {
		return line;
	}

}
