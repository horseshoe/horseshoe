package horseshoe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A {@link BufferedFileUpdateStream} may not modify the underlying file if the contents written are the same as the current file contents depending on the update settings. It is a single threaded buffered stream.
 *
 * <p>It is not necessary to wrap the output stream in a buffered stream or writer, as it is already buffered. For example:
 *
 * <pre>
 * Writer out
 *   = new OutputStreamWriter(new BufferedFileUpdateStream(file, update), charset);
 * </pre>
 */
public class BufferedFileUpdateStream extends OutputStream {

	private OutputStream outputStream;

	private final byte[] buffer;
	private int count = 0;

	public enum Update {
		UPDATE,
		APPEND,
		OVERWRITE
	}

	private class CompareOutputStream extends OutputStream {
		private final File file;
		private final InputStream inputStream;
		private final byte[] buffer;
		private int count = 0;
		private int offset = 0;
		private long truncateSize = 0; // -1 indicates append to end

		/**
		 * Creates a new compare output stream.
		 *
		 * @param file the file with the contents to compare
		 * @param append true to append to the file, otherwise false
		 * @throws IOException if the file cannot be opened for reading
		 */
		private CompareOutputStream(final File file, final boolean append) throws IOException {
			this.file = file;

			if (append) {
				inputStream = new InputStream() {
					@Override
					public int read(byte[] b) {
						return -1;
					}

					@Override
					public int read() {
						return -1;
					}
				};
				buffer = null;
				truncateSize = -1;
			} else {
				inputStream = new FileInputStream(file);
				buffer = new byte[BufferedFileUpdateStream.this.buffer.length];
			}
		}

		@Override
		public void close() throws IOException {
			final boolean endOfFile = (offset == count && inputStream.read() < 0);

			inputStream.close();

			// Check to see if the file should be truncated
			if (!endOfFile) {
				updateOutputStream(truncateSize).close();
			}
		}

		/**
		 * Reads data from the input stream to fill up the buffer.
		 *
		 * @return true if the more data was read into the buffer, false if the end of the stream has been reached
		 * @throws IOException if an I/O error occurs while reading data from the input stream
		 */
		private boolean read() throws IOException {
			offset = 0;
			count = inputStream.read(buffer);
			return count >= 0;
		}

		/**
		 * Updates the output stream to actually modify the file instead of checking the current file. This should be called when a byte mismatch is detected.
		 *
		 * @param atOffset the offset of the first mismatch, at which the file will be truncated
		 * @return the updated output stream
		 * @throws IOException if an I/O error occurs while updating the output stream
		 */
		private OutputStream updateOutputStream(final long atOffset) throws IOException {
			inputStream.close();
			final FileOutputStream newOutputStream = new FileOutputStream(file, atOffset != 0);
			outputStream = newOutputStream;

			if (atOffset > 0) {
				newOutputStream.getChannel().truncate(atOffset);
			}

			return outputStream;
		}

		@Override
		public void write(final int b) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(final byte[] b, final int off, final int len) throws IOException {
			for (int read = 0; read < len; read++) {
				if ((offset == count && !read()) || b[off + read] != buffer[offset++]) {
					updateOutputStream(truncateSize + read).write(b, off + read, len - read);
					return;
				}
			}

			truncateSize += len;
		}
	}

	/**
	 * Creates a new buffered file update stream.
	 *
	 * @param file the file to update
	 * @param update the update settings to use when writing to the file
	 * @param bufferSize the size of the buffer used to write and/or read from the file
	 * @throws IOException if an exception occurs while opening the file
	 */
	public BufferedFileUpdateStream(final File file, final Update update, int bufferSize) throws IOException {
		this.buffer = new byte[bufferSize];

		try {
			if (update != Update.OVERWRITE && file.canRead()) {
				this.outputStream = new CompareOutputStream(file, update == Update.APPEND);
				return;
			}
		} catch (final SecurityException e) { // Ignore any security error, although it will probably just get thrown again when the output stream is created
		}

		// We weren't able to open the file for reading, so just try to overwrite it.
		this.outputStream = new FileOutputStream(file);
	}

	/**
	 * Creates a new buffered file update stream using the default buufer size.
	 *
	 * @param file the file to update
	 * @param update the update settings to use when writing to the file
	 * @throws IOException if an exception occurs while opening the file
	 */
	public BufferedFileUpdateStream(final File file, final Update update) throws IOException {
		this(file, update, 8192);
	}

	@Override
	public void close() throws IOException {
		if (count > 0) {
			writeBufferedBytes();
		}

		outputStream.close();
	}

	@Override
	public void flush() throws IOException {
		if (count > 0) {
			writeBufferedBytes();
		}

		outputStream.flush();
	}

	@Override
	public void write(final int b) throws IOException {
		if (count >= buffer.length) {
			writeBufferedBytes();
		}

		buffer[count++] = (byte)b;
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		if (len <= buffer.length - count) {
			System.arraycopy(b, off, buffer, count, len);
			count += len;
		} else if (len > buffer.length) {
			if (count != 0) {
				writeBufferedBytes();
			}

			outputStream.write(b, off, len);
		} else {
			final int firstCopy = buffer.length - count;

			System.arraycopy(b, off, buffer, count, firstCopy);
			outputStream.write(buffer);
			count = len - firstCopy;
			System.arraycopy(b, off + firstCopy, buffer, 0, count);
		}
	}

	/**
	 * Writes the buffered bytes to the output stream and resets the buffer.
	 */
	private void writeBufferedBytes() throws IOException {
		outputStream.write(buffer, 0, count);
		count = 0;
	}

}
