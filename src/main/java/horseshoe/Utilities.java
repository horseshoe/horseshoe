package horseshoe;

final class Utilities {

	private static final byte WHITESPACE_PROPERTY = 0x01;
	private static final byte END_OF_LINE_PROPERTY = 0x02;

	private static final byte[] ASCII_PROPERTIES = new byte[] {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 3, 3, 3, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	};

	static final class Range {

		final int start;
		final int end;

		Range(final int start, final int end) {
			this.start = start;
			this.end = end;
		}

	}

	static final class TrimmedString {

		final int start;
		final String string;

		TrimmedString(final int start, final String string) {
			this.start = start;
			this.string = string;
		}

	}

	/**
	 * Finds the next end of line sequence. An end of line sequence is equivalent to the following pattern: \r\n|[\n\x0B\x0C\r\u0085\u2028\u2029]
	 *
	 * @param value the character sequence to search for the next end of line sequence
	 * @param start the starting index where the search will be performed
	 * @param end the ending index where the search will be performed
	 * @return the character range of the next end of line sequence, or null if none could be found
	 */
	static Range findNewLine(final char[] value, final int start, final int end) {
		for (int index = start; index < end; index++) {
			final char c = value[index];

			if (c >= ASCII_PROPERTIES.length) {
				if (c >= 0x2028 && c <= 0x2029) {
					return new Range(index, index + 1);
				}
			} else if ((ASCII_PROPERTIES[c] & END_OF_LINE_PROPERTY) != 0) {
				if (c == '\r' && index + 1 < end && value[index + 1] == '\n') {
					return new Range(index, index + 2);
				}

				return new Range(index, index + 1);
			}
		}

		return null;
	}

	/**
	 * Finds the next end of line sequence. An end of line sequence is equivalent to the following pattern: \r\n|[\n\x0B\x0C\r\u0085\u2028\u2029]
	 *
	 * @param value the character sequence to search for the next end of line sequence
	 * @param start the starting index where the search will be performed
	 * @param end the ending index where the search will be performed
	 * @return the character range of the next end of line sequence, or null if none could be found
	 */
	static Range findNewLine(final String value, final int start, final int end) {
		return findNewLine(value.toCharArray(), start, end);
	}

	/**
	 * Gets whether or not the character is a whitespace character.
	 *
	 * @param c the character to test for a whitespace character
	 * @return true if the character is a whitespace character, otherwise false
	 */
	private static boolean isWhitespace(final char c) {
		if (c >= ASCII_PROPERTIES.length) {
			return Character.isSpaceChar(c);
		}

		return (ASCII_PROPERTIES[c] & WHITESPACE_PROPERTY) != 0;
	}

	/**
	 * Gets whether or not all characters are whitespace characters.
	 *
	 * @param value the string to test for all whitespace characters
	 * @return true if all characters are whitespace characters, otherwise false
	 */
	static boolean isWhitespace(final String value) {
		final int length = value.length();

		for (int index = 0; index < length; index++) {
			if (!isWhitespace(value.charAt(index))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Gets the trimmed string from the specified starting index.
	 *
	 * @param value the string to trim
	 * @param start the starting index (inclusive) in the string
	 * @return the trimmed string
	 */
	static TrimmedString trim(final String value, final int start) {
		for (int i = start; i < value.length(); i++) {
			if (!isWhitespace(value.charAt(i))) {
				int j;

				for (j = value.length(); isWhitespace(value.charAt(j - 1)); j--);

				return new TrimmedString(i, value.substring(i, j));
			}
		}

		return new TrimmedString(start, "");
	}

	/**
	 * Disables creation of the utilities class.
	 */
	private Utilities() {
	}

}
