package horseshoe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

	private AnnotationHandlers() { }

	public static AnnotationHandler printWriter(final PrintStream printStream) {
		return new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				return new PrintWriter(printStream) {
					@Override
					public void close() {
						flush();
					}
				};
			}
		};
	}

	public static AnnotationHandler fileWriter(final Charset charset) {
		return new AnnotationHandler() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				@SuppressWarnings("unchecked")
				final File file = new File(((Map<String, Object>)value).get("name").toString());
				final File parentFile = file.getParentFile();

				if (parentFile != null) {
					parentFile.mkdirs();
				}

				return new BufferedWriter(new FileWriter(file));
			}
		};
	}

	public static final Map<String, AnnotationHandler> DEFAULT_ANNOTATIONS = Collections.unmodifiableMap(new HashMap<String, AnnotationHandler>() {
		private static final long serialVersionUID = 1L;
		{
			put("stderr", printWriter(System.err));
			put("file", fileWriter(Charset.defaultCharset()));
		}
	});

}
