package horseshoe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.List;
import java.util.Map;

import horseshoe.internal.Operands;

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
		defaultAnnotations.put("File", fileWriter(Paths.get("."), StandardCharsets.UTF_8));

		DEFAULT_ANNOTATIONS = Collections.unmodifiableMap(defaultAnnotations);
	}

	private static final class FileWriterHandler implements AnnotationHandler {

		private final Path rootDir;
		private final Charset defaultCharset;

		private FileWriterHandler(final Path rootDir, final Charset defaultCharset) {
			this.rootDir = rootDir;
			this.defaultCharset = defaultCharset;
		}

		@Override
		public Writer getWriter(final Writer writer, final Object value) throws IOException {
			final Map<?, ?> properties;
			final File file;

			if (value instanceof Map) {
				properties = (Map<?, ?>)value;
				file = rootDir.resolve(Paths.get(String.valueOf(properties.get("name")))).toFile();
			} else if (value instanceof List) {
				final List<?> list = (List<?>)value;
				final int size = list.size();

				properties = size > 1 && list.get(1) instanceof Map ? (Map<?, ?>)list.get(1) : Collections.emptyMap();
				file = rootDir.resolve(Paths.get(String.valueOf(size == 0 ? null : list.get(0)))).toFile();
			} else {
				properties = Collections.emptyMap();
				file = rootDir.resolve(Paths.get(String.valueOf(value))).toFile();
			}

			// Load properties
			Charset charset = defaultCharset;
			boolean overwrite = false;
			boolean append = false;

			final Object encoding = properties.get("encoding");

			if (encoding != null) {
				charset = Charset.forName(encoding.toString());
			}

			overwrite = Operands.convertToBoolean(properties.get("overwrite"));
			append = Operands.convertToBoolean(properties.get("append"));

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
	 * @param rootDir the root directory for the pathnames provided
	 * @param defaultCharset the {@link Charset} to use when not specified otherwise
	 * @return the new annotation handler
	 */
	public static AnnotationHandler fileWriter(final Path rootDir, final Charset defaultCharset) {
		return new FileWriterHandler(rootDir, defaultCharset);
	}

}
