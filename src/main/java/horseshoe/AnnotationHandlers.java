package horseshoe;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import horseshoe.internal.Expression;

/**
 * The AnnotationHandlers class is used to create common {@link AnnotationHandler}s. It also contains the default annotation handlers map if none is specified.
 */
public final class AnnotationHandlers {

	/**
	 * The map of default annotations that are made available during the rendering process. Valid default annotations include "StdOut", "StdErr", and "File".
	 */
	public static final Map<String, AnnotationHandler> DEFAULT_ANNOTATIONS;

	static {
		final Map<String, AnnotationHandler> defaultAnnotations = new HashMap<>();

		defaultAnnotations.put("StdErr", printWriter(System.err, Charset.defaultCharset()));
		defaultAnnotations.put("StdOut", printWriter(System.out, Charset.defaultCharset()));
		defaultAnnotations.put("File", fileWriter());

		DEFAULT_ANNOTATIONS = Collections.unmodifiableMap(defaultAnnotations);
	}

	static class FileUpdateOutputStream extends OutputStream {

		private OutputStream outputStream;

		private class CompareOutputStream extends OutputStream {
			private final File file;
			private final InputStream inputStream;
			private long truncateSize = 0;
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
					final FileInputStream fileStream = new FileInputStream(file);

					inputStream = new BufferedInputStream(fileStream);
					truncateSize = fileStream.getChannel().size();
					fileStream.getChannel().position(truncateSize);
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

				if (atOffset == 0) {
					outputStream = new FileOutputStream(file);
				} else {
					final FileOutputStream newOutputStream = new FileOutputStream(file, true);

					newOutputStream.getChannel().truncate(atOffset);
					outputStream = newOutputStream;
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
							updateOutputStream(truncateSize + i).write(b, off + j, len - j);
							return;
						}
					}
				}

				truncateSize += len;
			}
		}

		/**
		 * Creates a new file update output stream.
		 *
		 * @param file the file to update
		 * @param append true to append to the file, otherwise false
		 * @throws IOException if an exception occurs while opening the file
		 */
		FileUpdateOutputStream(final File file, final boolean append) throws IOException {
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

	private AnnotationHandlers() { }

	/**
	 * Creates an annotation handler that sends all output to a print stream using a specific character set.
	 *
	 * @param printStream the stream to use for rendering text
	 * @param charset the character set used when rendering text to the stream
	 * @return the new annotation handler
	 */
	public static AnnotationHandler printWriter(final PrintStream printStream, final Charset charset) {
		return new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				return new PrintWriter(new OutputStreamWriter(printStream, charset)) {
					@Override
					public void close() {
						flush();
					}
				};
			}
		};
	}

	/**
	 * Creates an annotation handler that sends all output to a file. The file can be specified by passing a string argument or a map argument with a "name" entry to the annotation in the template. If a map argument is used, then the "encoding" entry can be used to specify a specific character set, the "append" entry can be used to append to a file, and the "overwrite" entry can force a file to be overwritten.
	 *
	 * @return the new annotation handler
	 */
	public static AnnotationHandler fileWriter() {
		return new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				final File file;
				Charset charset = Charset.defaultCharset();
				boolean overwrite = false;
				boolean append = false;

				if (value instanceof Map) {
					final Object name = ((Map<?, ?>)value).get("name");
					final Object encoding = ((Map<?, ?>)value).get("encoding");

					file = new File(String.valueOf(name));

					if (encoding != null) {
						charset = Charset.forName(encoding.toString());
					}

					overwrite = Expression.convertToBoolean(((Map<?, ?>)value).get("overwrite"));
					append = Expression.convertToBoolean(((Map<?, ?>)value).get("append"));
				} else {
					file = new File(String.valueOf(value));
				}

				// Create the directory if it doesn't exist, and then return the writer
				final File directory = file.getParentFile();

				if (directory != null && !directory.isDirectory() && !directory.mkdirs()) {
					throw new IOException("Failed to create directory " + directory.toString());
				}

				if (overwrite) {
					return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), charset));
				}

				return new BufferedWriter(new OutputStreamWriter(new FileUpdateOutputStream(file, append), charset));
			}
		};
	}

}
