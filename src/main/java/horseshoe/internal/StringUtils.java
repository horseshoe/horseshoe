package horseshoe.internal;

public final class StringUtils {

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

	public static final class Range {

		public final int start;
		public final int end;

		public Range(final int start, final int end) {
			this.start = start;
			this.end = end;
		}

	}

	/**
	 * Disables creation of the character sequence utils class.
	 */
	private StringUtils() {
	}

	/**
	 * Finds the next end of line sequence. An end of line sequence is equivalent to the following pattern: \r\n|[\n\x0B\x0C\r\u0085\u2028\u2029]
	 *
	 * @param value the character sequence to search for the next end of line sequence
	 * @param start the starting index where the search will be performed
	 * @param end the ending index where the search will be performed
	 * @return the character range of the next end of line sequence, or null if none could be found
	 */
	public static Range findNewLine(final char[] value, final int start, final int end) {
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
	public static Range findNewLine(final String value, final int start, final int end) {
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
	public static boolean isWhitespace(final String value) {
		final int length = value.length();

		for (int index = 0; index < length; index++) {
			if (!isWhitespace(value.charAt(index))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates a trimmed character sequence from the specified start and end.
	 *
	 * @param value the character sequence to trim
	 * @param start the starting index (inclusive) in the character sequence
	 * @param end the ending index (exclusive) in the character sequence
	 * @return the trimmed character sequence
	 */
	public static String trim(final String value, final int start, final int end) {
		for (int i = start; i < end; i++) {
			if (!isWhitespace(value.charAt(i))) {
				int j;

				for (j = end; isWhitespace(value.charAt(j - 1)); j--);

				return value.substring(i, j);
			}
		}

		return "";
	}

}
