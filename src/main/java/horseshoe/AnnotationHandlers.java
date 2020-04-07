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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
	 * Creates an annotation handler that sends all output to a file. The file can be specified by passing a string argument or a map argument with a "name" entry to the annotation in the template. If a map argument is used, then a specific character set can be specified using an "encoding" entry.
	 *
	 * @return the new annotation handler
	 */
	public static AnnotationHandler fileWriter() {
		return new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				final File file;
				Charset charset = Charset.defaultCharset();

				if (value instanceof Map) {
					final Object name = ((Map<?, ?>)value).get("name");
					final Object encoding = ((Map<?, ?>)value).get("encoding");

					file = new File(String.valueOf(name));

					if (encoding != null) {
						charset = Charset.forName(encoding.toString());
					}
				} else {
					file = new File(String.valueOf(value));
				}

				final File directory = file.getParentFile();

				if (directory != null && !directory.isDirectory() && !directory.mkdirs()) {
					throw new IOException("Failed to create directory " + directory.toString());
				}

				return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
			}
		};
	}

}
