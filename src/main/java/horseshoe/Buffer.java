package horseshoe;

/**
 * A simple buffer for loading in character data. The provided subsequence implementation wraps the data provided by the buffer class. Changes to the underlying buffer will result in changes to the subsequence object.
 */
final class Buffer {

	private final char[] data;
	private int length;

	/**
	 * Creates a buffer of the specified capacity and length. This assumes the user will fill the buffer with useful data up to the specified length.
	 *
	 * @param capacity the capacity of the buffer
	 * @param length the length of useful data in the buffer
	 */
	Buffer(final int capacity, final int length) {
		this.data = new char[capacity];
		this.length = length;
	}

	/**
	 * Creates a buffer of the specified capacity. The length will be set to 0.
	 *
	 * @param capacity the capacity of the buffer
	 */
	Buffer(final int capacity) {
		this(capacity, 0);
	}

	/**
	 * Gets the capacity of the buffer.
	 *
	 * @return the capacity of the buffer
	 */
	int capacity() {
		return data.length;
	}

	/**
	 * Gets the underlying character data used as the buffer.
	 *
	 * @return the underlying character data used as the buffer
	 */
	char[] getData() {
		return data;
	}

	/**
	 * Gets the next index of the specified string in the buffer, beginning at the specified index.
	 *
	 * @param value the string to search for in the buffer
	 * @param index the index in the buffer to begin the search
	 * @return the next index of the specified string in the buffer, or -1 if the string could not be found
	 */
	int indexOf(final String value, final int index) {
		if (value.length() == 0) {
			return index;
		}

		final char first = value.charAt(0);
		final int end = length - value.length();

		for (int i = index; i <= end; i++) {
			// Find first character
			if (data[i] == first) {
				final int matchEnd = i + value.length();
				int j = i + 1;

				// Match remaining characters
				for (int k = 1; j < matchEnd && data[j] == value.charAt(k); j++, k++);

				if (j == matchEnd) {
					return i;
				}
			}
		}

		return -1;
	}

	/**
	 * Gets the length of useful data in the buffer.
	 *
	 * @return the length of useful data in the buffer
	 */
	int length() {
		return length;
	}

	/**
	 * Sets the length of useful data in the buffer.
	 *
	 * @param length the length of useful data in the buffer
	 * @return this buffer
	 */
	Buffer setLength(final int length) {
		this.length = length;
		return this;
	}

	/**
	 * Creates a string from the specified start and end.
	 *
	 * @param start the starting index (inclusive) in the buffer
	 * @param end the ending index (exclusive) in the buffer
	 * @return the created string
	 */
	String substring(final int start, final int end) {
		return new String(data, start, end - start);
	}

	@Override
	public String toString() {
		return new String(data, 0, length);
	}

}
