package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Template {

	public static interface AnnotationProcessor {
		/**
		 * Gets the writer based on the result of the expression.
		 *
		 * @param writer the writer from the enveloping section
		 * @param value the resulting value of the evaluated expression, or null if no expression was given
		 * @return the writer to use for the annotated section, or null to indicate that the writer should not be changed
		 */
		public Writer getWriter(final Writer writer, final Object value) throws IOException;

		/**
		 * Returns the writer. This method should close or flush the writer as appropriate.
		 *
		 * @param writer the writer to return
		 */
		public void returnWriter(final Writer writer) throws IOException;
	}

	private static final Map<String, AnnotationProcessor> DEFAULT_ANNOTATION_MAP = new HashMap<>();

	static {
		DEFAULT_ANNOTATION_MAP.put("StdErr", new AnnotationProcessor() {
			@Override
			public Writer getWriter(final Writer writer, final Object value) throws IOException {
				return new Writer() {
					@Override
					public void close() {
					}

					@Override
					public void flush() {
						System.err.flush();
					}

					@Override
					public void write(final char[] cbuf, final int off, final int len) throws IOException {
						System.err.print(off == 0 && len == cbuf.length ? cbuf : Arrays.copyOfRange(cbuf, off, off + len));
					}
				};
			}

			@Override
			public void returnWriter(final Writer writer) throws IOException {
				System.err.flush();
			}
		});
	}

	private final String name;
	private final List<Action> actions = new ArrayList<>();

	/**
	 * Creates an empty template with the specified name.
	 *
	 * @param name the name of the template
	 */
	Template(final String name) {
		this.name = name;
	}

	/**
	 * Gets the actions associated with the template.
	 *
	 * @return the actions associated with the template
	 */
	List<Action> getActions() {
		return actions;
	}

	/**
	 * Gets the name of the template.
	 *
	 * @return the name of the template
	 */
	public String getName() {
		return name;
	}

	/**
	 * Renders the template to the specified writer using the specified context and global data. The global data is copied and will not be modified while rendering the template.
	 *
	 * @param settings the settings used while rendering
	 * @param globalData the global data used while rendering
	 * @param writer the writer used to render the template
	 * @param annotationMap the map used to process annotations
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public void render(final Settings settings, final Map<String, Object> globalData, final Writer writer, final Map<String, AnnotationProcessor> annotationMap) throws IOException {
		final RenderContext renderContext = new RenderContext(settings, globalData, annotationMap);

		renderContext.getIndentation().push("");
		renderContext.getSectionData().push(renderContext.getGlobalData());

		for (final Action action : actions) {
			action.perform(renderContext, writer);
		}
	}

	/**
	 * Renders the template to the specified writer using the specified context and global data. The global data is copied and will not be modified while rendering the template.
	 *
	 * @param settings the settings used while rendering
	 * @param globalData the global data used while rendering
	 * @param writer the writer used to render the template
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public void render(final Settings settings, final Map<String, Object> globalData, final Writer writer) throws IOException {
		render(settings, globalData, writer, DEFAULT_ANNOTATION_MAP);
	}

}
