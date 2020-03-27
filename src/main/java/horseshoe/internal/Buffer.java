package horseshoe.internal;

/**
 * A simple buffer for loading in character data. The provided subsequence implementation wraps the data provided by the buffer class. Changes to the underlying buffer will result in changes to the subsequence object.
 */
public final class Buffer implements CharSequence {

	private final char[] data;
	private int length;

	/**
	 * A character sequence that wraps a buffer without copying data. Note that the wrapper references the underlying buffer, so any changes to the buffer will be reflected in the wrapper object.
	 */
	public static final class Wrapper implements CharSequence {
		private final char[] data;
		private final int start;
		private final int end;

		/**
		 * Creates a new wrapper for the buffer.
		 *
		 * @param data the data to wrap
		 * @param start the start index (inclusive) of the wrapped buffer
		 * @param end the end index (exclusive) of the wrapped buffer
		 */
		public Wrapper(final char[] data, final int start, final int end) {
			this.data = data;
			this.start = start;
			this.end = end;
		}

		@Override
		public char charAt(final int index) {
			return data[start + index];
		}

		@Override
		public int length() {
			return end - start;
		}

		@Override
		public Wrapper subSequence(final int start, final int end) {
			return new Wrapper(data, this.start + start, this.start + end);
		}

		@Override
		public String toString() {
			return new String(data, start, end - start);
		}
	}

	/**
	 * Creates a buffer of the specified capacity and length. This assumes the user will fill the buffer with useful data up to the specified length.
	 *
	 * @param capacity the capacity of the buffer
	 * @param length the length of useful data in the buffer
	 */
	public Buffer(final int capacity, final int length) {
		this.data = new char[capacity];
		this.length = length;
	}

	/**
	 * Creates a buffer of the specified capacity. The length will be set to 0.
	 *
	 * @param capacity the capacity of the buffer
	 */
	public Buffer(final int capacity) {
		this(capacity, 0);
	}

	/**
	 * Gets the capacity of the buffer
	 *
	 * @return the capacity of the buffer
	 */
	public int capacity() {
		return data.length;
	}

	@Override
	public char charAt(final int index) {
		return data[index];
	}

	/**
	 * Gets the underlying character data used as the buffer
	 *
	 * @return the underlying character data used as the buffer
	 */
	public char[] getData() {
		return data;
	}

	@Override
	public int length() {
		return length;
	}

	/**
	 * Sets the length of useful data in the buffer
	 *
	 * @param length the length of useful data in the buffer
	 * @return this buffer
	 */
	public Buffer setLength(final int length) {
		this.length = length;
		return this;
	}

	@Override
	public Wrapper subSequence(final int start, final int end) {
		return new Wrapper(data, start, end);
	}

	@Override
	public String toString() {
		return new String(data, 0, length);
	}

	/**
	 * Creates a string from the specified start and end.
	 *
	 * @param start the starting index (inclusive) in the buffer
	 * @param end the ending index (exclusive) in the buffer
	 * @return the created string
	 */
	public String toString(final int start, final int end) {
		return new String(data, start, end - start);
	}

}
