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

	private static int filesCount = 0;

	private AnnotationHandlers() { }

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

	public static AnnotationHandler fileWriter() {
		return new AnnotationHandler() {
			private String getDefaultFilename() {
				return "Horseshoe_" + filesCount + ".out";
			}

			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				final File file;
				Charset charset = Charset.defaultCharset();

				if (value instanceof Map) {
					final Object name = ((Map<?, ?>)value).get("name");
					final Object encoding = ((Map<?, ?>)value).get("encoding");

					file = new File(name == null ? getDefaultFilename() : name.toString());

					if (encoding != null) {
						charset = Charset.forName(encoding.toString());
					}
				} else {
					file = new File(value == null ? getDefaultFilename() : value.toString());
				}

				final File directory = file.getParentFile();

				if (directory != null && !directory.isDirectory() && !directory.mkdirs()) {
					throw new IOException("Failed to create directory " + directory.toString());
				}

				filesCount++;
				return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
			}
		};
	}

	public static final Map<String, AnnotationHandler> DEFAULT_ANNOTATIONS = Collections.unmodifiableMap(new HashMap<String, AnnotationHandler>() {
		private static final long serialVersionUID = 1L;
		{
			put("StdErr", printWriter(System.err, Charset.defaultCharset()));
			put("StdOut", printWriter(System.out, Charset.defaultCharset()));
			put("File", fileWriter());
		}
	});

}
