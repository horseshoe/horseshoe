package horseshoe.internal;

public final class CharSequenceUtils {

	/**
	 * Disables creation of the character sequence utils class.
	 */
	private CharSequenceUtils() {
	}

	/**
	 * Creates a trimmed character sequence from the specified start and end.
	 *
	 * @param value the character sequence to trim
	 * @param start the starting index (inclusive) in the character sequence
	 * @param end the ending index (exclusive) in the character sequence
	 * @return the trimmed character sequence
	 */
	public static CharSequence trim(final CharSequence value, final int start, final int end) {
		for (int i = start; i < end; i++) {
			if (!Character.isWhitespace(value.charAt(i))) {
				for (int j = end; i < j; j--) {
					if (!Character.isWhitespace(value.charAt(j - 1))) {
						return value.subSequence(i, j);
					}
				}

				break;
			}
		}

		return "";
	}

	/**
	 * Checks if the sequence at the specified index in the buffer matches the specified sequence. No bounds checking is performed.
	 *
	 * @param buffer The buffer that is compared to the specified sequence
	 * @param index the index within the buffer where the comparison will start
	 * @param value the sequence to match
	 * @return true if the sequence at the specified index in the buffer matches the specified sequence, otherwise false
	 */
	public static boolean matches(final CharSequence buffer, final int index, final CharSequence value) {
		final int length = value.length();

		for (int i = index, j = 0; j < length; i++, j++) {
			if (buffer.charAt(i) != value.charAt(j)) {
				return false;
			}
		}

		return true;
	}

}
