package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Templates represent parsed and resolved Horseshoe template files. They are loaded using the {@link TemplateLoader} class. An example of how to load and render a template is given below:
 * <pre>{@code
 * final horseshoe.Template template = new horseshoe.TemplateLoader().load("Hello World", "{{{salutation}}}, {{ recipient }}!");
 *
 * final java.util.Map<String, Object> data = new java.util.HashMap<>();
 * data.put("salutation", "Hello");
 * data.put("recipient", "world");
 *
 * final horseshoe.Settings settings = new horseshoe.Settings();
 * final java.io.StringWriter writer = new java.io.StringWriter();
 * template.render(settings, data, writer);
 *
 * return writer.toString(); // returns "Hello, world!"}
 * </pre>
 */
public final class Template {

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
	public void render(final Settings settings, final Map<String, Object> globalData, final Writer writer, final Map<String, AnnotationHandler> annotationMap) throws IOException {
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
		render(settings, globalData, writer, AnnotationHandlers.DEFAULT_ANNOTATIONS);
	}

}
