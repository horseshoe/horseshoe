package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import horseshoe.internal.HaltRenderingException;
import horseshoe.internal.RenderContext;

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

	static final Logger LOGGER = Logger.getLogger(Template.class.getName());

	private final String name;
	private final Section section;
	private final List<Action> actions = new ArrayList<>();

	/**
	 * Creates an empty template with the specified name.
	 *
	 * @param name the name of the template
	 */
	Template(final String name) {
		this.name = name;
		this.section = new Section(null, name, name, null, null, true);

		section.getLocalPartials().put(name, this);
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
	 * Gets the section associated with the template.
	 *
	 * @return the section associated with the template
	 */
	Section getSection() {
		return section;
	}

	/**
	 * Renders the template to the specified writer using the specified context and global data. The global data is copied and will not be modified while rendering the template. The writer is not closed, so that chaining can be used, if desired.
	 *
	 * @param settings the settings used while rendering
	 * @param globalData the global data used while rendering
	 * @param writer the writer used to render the template
	 * @param annotationMap the map used to process annotations
	 * @return the writer passed to the render method
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public Writer render(final Settings settings, final Map<String, Object> globalData, final Writer writer, final Map<String, AnnotationHandler> annotationMap) throws IOException {
		try {
			final RenderContext renderContext = new RenderContext(settings, globalData, annotationMap);

			renderContext.getIndentation().push("");

			for (final Action action : actions) {
				action.perform(renderContext, writer);
			}
		} catch (final HaltRenderingException e) {
			settings.getLogger().log(Level.SEVERE, e.getMessage());
		}

		return writer;
	}

	/**
	 * Renders the template to the specified writer using the specified context and global data. The global data is copied and will not be modified while rendering the template. The writer is not closed, so that chaining can be used, if desired.
	 *
	 * @param settings the settings used while rendering
	 * @param globalData the global data used while rendering
	 * @param writer the writer used to render the template
	 * @return the writer passed to the render method
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public Writer render(final Settings settings, final Map<String, Object> globalData, final Writer writer) throws IOException {
		return render(settings, globalData, writer, AnnotationHandlers.DEFAULT_ANNOTATIONS);
	}

}
