package horseshoe;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import horseshoe.BufferedFileUpdateStream.Update;
import horseshoe.internal.Operands;

/**
 * The AnnotationHandlers class is used to create common {@link AnnotationHandler}s. It also contains the default annotation handlers map if none is specified.
 */
public final class AnnotationHandlers {

	/**
	 * An annotation suppling a writer that writes to a file.
	 */
	public static final AnnotationHandler FILE_HANDLER = fileHandler(Paths.get("."), StandardCharsets.UTF_8);

	/**
	 * An annotation suppling a writer that discards all output.
	 */
	public static final AnnotationHandler NULL_HANDLER = (writer, value) -> new Writer() {
		@Override
		public void close() { // Closing does nothing
		}

		@Override
		public void write(final char[] cbuf, final int off, final int len) { // Writing does nothing
		}

		@Override
		public void flush() { // Flushing does nothing
		}
	};

	/**
	 * An annotation suppling a writer that writes to stdout.
	 */
	public static final AnnotationHandler STDOUT_HANDLER = printStreamHandler(System.out, Charset.defaultCharset());

	/**
	 * An annotation suppling a writer that writes to stderr.
	 */
	public static final AnnotationHandler STDERR_HANDLER = printStreamHandler(System.err, Charset.defaultCharset());

	/**
	 * The map of default annotations that are made available during the rendering process. Valid default annotations include "Null", "StdOut", "StdErr", and "File".
	 */
	public static final Map<String, AnnotationHandler> DEFAULT_ANNOTATIONS;

	static {
		final Map<String, AnnotationHandler> defaultAnnotations = new HashMap<>();

		defaultAnnotations.put("StdErr", STDERR_HANDLER);
		defaultAnnotations.put("StdOut", STDOUT_HANDLER);
		defaultAnnotations.put("Null", NULL_HANDLER);
		defaultAnnotations.put("File", FILE_HANDLER);

		DEFAULT_ANNOTATIONS = Collections.unmodifiableMap(defaultAnnotations);
	}

	private static final class FileHandler implements AnnotationHandler {

		private final Path rootDir;
		private final Charset defaultCharset;

		private FileHandler(final Path rootDir, final Charset defaultCharset) {
			this.rootDir = rootDir;
			this.defaultCharset = defaultCharset;
		}

		@Override
		public Writer getWriter(final Writer writer, final Object[] args) throws IOException {
			final Map<?, ?> properties;
			final File file;

			if (args.length == 0) {
				throw new IllegalArgumentException("No filename specified for \"File\" annotation");
			} else if (args.length > 1) {
				properties = args[1] instanceof Map ? (Map<?, ?>) args[1] : Collections.emptyMap();
				file = rootDir.resolve(Paths.get(String.valueOf(args[0]))).toFile();
			} else if (args[0] instanceof Map) {
				properties = (Map<?, ?>) args[0];
				file = rootDir.resolve(Paths.get(String.valueOf(properties.get("name")))).toFile();
			} else {
				properties = Collections.emptyMap();
				file = rootDir.resolve(Paths.get(String.valueOf(args[0]))).toFile();
			}

			// Load properties
			Charset charset = defaultCharset;
			Update update = Update.UPDATE;

			final Object encoding = properties.get("encoding");

			if (encoding != null) {
				charset = Charset.forName(encoding.toString());
			}

			if (Operands.convertToBoolean(properties.get("overwrite"))) {
				update = Update.OVERWRITE;
			} else if (Operands.convertToBoolean(properties.get("append"))) {
				update = Update.APPEND;
			}

			// Create the directory if it doesn't exist, and then return the writer
			final File directory = file.getParentFile();

			if (directory != null && !directory.isDirectory() && !directory.mkdirs()) {
				throw new IOException("Failed to create directory " + directory.toString());
			}

			return new OutputStreamWriter(new BufferedFileUpdateStream(file, update), charset);
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
	private static AnnotationHandler printStreamHandler(final PrintStream printStream, final Charset charset) {
		return (final Writer writer, final Object[] args) -> new PrintWriter(new OutputStreamWriter(printStream, charset)) {
			@Override
			public void close() {
				flush();
			}
		};
	}

	/**
	 * Creates an annotation handler that sends all output to a file. The file can be specified by passing a string argument or a map argument with a "name" entry to the annotation in the template. If a map argument is used, then the "encoding" entry can be used to specify a specific character set, the "append" entry can be used to append to a file, and the "overwrite" entry can force a file to be overwritten.
	 *
	 * @param rootDir the root directory for the pathnames provided
	 * @param defaultCharset the {@link Charset} to use when not specified otherwise
	 * @return the new annotation handler
	 */
	public static AnnotationHandler fileHandler(final Path rootDir, final Charset defaultCharset) {
		return new FileHandler(rootDir, defaultCharset);
	}

}
