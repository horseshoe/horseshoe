package horseshoe;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A {@code FileUpdateOutputStream} only modifies the underlying file if the contents written differs from the current file contents.
 *
 * <p>The recommended usage is to wrap the output stream in a buffered stream or writer. For example:
 *
 * <pre>
 * Writer out
 *   = new BufferedWriter(new OutputStreamWriter(new FileUpdateOutputStream(file, append), charset));
 * </pre>
 */
public class FileUpdateOutputStream extends OutputStream {

	private OutputStream outputStream;

	private class CompareOutputStream extends OutputStream {
		private final File file;
		private final InputStream inputStream;
		private long truncateSize = 0; // -1 indicates append to end
		private byte[] buffer = { };

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
					public int read() {
						return -1;
					}

					@Override
					public int read(final byte[] b, final int off, final int len) {
						return -1;
					}
				};
				truncateSize = -1;
			} else {
				inputStream = new BufferedInputStream(new FileInputStream(file));
			}
		}

		@Override
		public void close() throws IOException {
			final boolean endOfFile = (inputStream.read() < 0);

			inputStream.close();

			// Check to see if the file should be truncated
			if (!endOfFile) {
				updateOutputStream(truncateSize).close();
			}
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
			if (inputStream.read() != b) {
				updateOutputStream(truncateSize).write(b);
			}

			truncateSize++;
		}

		@Override
		public void write(final byte[] b, final int off, final int len) throws IOException {
			if (buffer.length < len) {
				buffer = new byte[Math.max(len, 4096)];
			}

			int read;

			for (int i = 0; i < len; i += read) {
				read = inputStream.read(buffer, i, len - i);

				if (read < 0) {
					updateOutputStream(truncateSize + i).write(b, off + i, len - i);
					return;
				}

				for (int j = i; j < i + read; j++) {
					if (b[off + j] != buffer[j]) {
						updateOutputStream(truncateSize + j).write(b, off + j, len - j);
						return;
					}
				}
			}

			truncateSize += len;
		}
	}

	/**
	 * Creates a new file update output stream. This stream only modifies the underlying file if the contents written differs from the current file contents.
	 *
	 * @param file the file to update
	 * @param append true to append to the file, otherwise false
	 * @throws IOException if an exception occurs while opening the file
	 */
	public FileUpdateOutputStream(final File file, final boolean append) throws IOException {
		try {
			if (file.canRead()) {
				outputStream = new CompareOutputStream(file, append);
				return;
			}
		} catch (final SecurityException e) { // Ignore any security error, although it will probably just get thrown when the output stream is created
		}

		// We weren't able to open the file for reading, so just try to overwrite it.
		outputStream = new FileOutputStream(file);
	}

	@Override
	public void close() throws IOException {
		outputStream.close();
	}

	@Override
	public void flush() throws IOException {
		outputStream.flush();
	}

	@Override
	public void write(final int b) throws IOException {
		outputStream.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		outputStream.write(b, off, len);
	}

}
