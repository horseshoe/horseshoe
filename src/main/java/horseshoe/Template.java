package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Template {

	public static interface WriterMap {
		/**
		 * Gets the writer based on the specified name.
		 *
		 * @param name the name of the writer to get
		 * @return the writer associated with the specified name, or null to indicate that the writer should not be changed
		 */
		public Writer getWriter(final String name);
	}

	private static final WriterMap DEFAULT_WRITER_MAP = new WriterMap() {

		private final Writer ERROR = new Writer() {
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

		@Override
		public Writer getWriter(final String name) {
			switch (name) {
			case "&2":
			case "stderr":
				return ERROR;

			default:
				break;
			}

			return null;
		}

	};

	private final String name;
	private final List<Action> actions = new ArrayList<>();
	int recursionLevel;

	/**
	 * Creates an empty template with the specified name.
	 *
	 * @param name the name of the template
	 * @param recursionLevel the recursion level of the template
	 */
	Template(final String name, final int recursionLevel) {
		this.name = name;
		this.recursionLevel = recursionLevel;
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
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public void render(final Settings settings, final Map<String, Object> globalData, final Writer writer, final WriterMap writerMap) throws IOException {
		// TODO: Linking of variables
		final RenderContext renderContext = new RenderContext(settings, globalData, writerMap);

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
		render(settings, globalData, writer, DEFAULT_WRITER_MAP);
	}

}
