package horseshoe;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import horseshoe.internal.HaltRenderingException;

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
public class Template {

	private static final String IMPLEMENTATION_VERSION = Template.class.getPackage().getImplementationVersion();

	static final Logger LOGGER = Logger.getLogger(Template.class.getName());
	static final String VERSION = IMPLEMENTATION_VERSION == null ? "unspecified" : IMPLEMENTATION_VERSION;

	private final Object identifier;
	private final Section section;
	private final List<Renderer> renderList = new ArrayList<>();

	/**
	 * Loads a template from a file.
	 *
	 * @param file the file to load as a template
	 * @return the loaded template
	 * @throws IOException if an I/O exception is encountered while opening or reading the file
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public static Template load(final Path file) throws IOException, LoadException {
		return new TemplateLoader().load(file);
	}

	/**
	 * Loads a template from a string.
	 *
	 * @param value the string value to load as a template, not a filename
	 * @return the loaded template
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public static Template load(final String value) throws LoadException {
		return new TemplateLoader().load(value);
	}

	/**
	 * Loads a template using a reader.
	 *
	 * @param reader the reader to use to load as a template
	 * @return the loaded template
	 * @throws IOException if an I/O exception is encountered while reading from the reader
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public static Template load(final Reader reader) throws IOException, LoadException {
		return new TemplateLoader().load(reader);
	}

	/**
	 * Creates an empty template with the specified name.
	 *
	 * @param name the name of the template
	 * @param identifier the identifier of the template
	 */
	protected Template(final String name, final Object identifier) {
		this.identifier = identifier;
		this.section = new Section(null, name == null ? "[Anonymous]" : name, identifier, null, null, true);
	}

	/**
	 * Gets the render actions associated with the template.
	 *
	 * @return the render actions associated with the template
	 */
	protected final List<Renderer> getRenderList() {
		return renderList;
	}

	/**
	 * Gets the identifier associated with the template.
	 *
	 * @return the identifier associated with the template
	 */
	public final Object getIdentifier() {
		return identifier;
	}

	/**
	 * Gets the section associated with the template.
	 *
	 * @return the section associated with the template
	 */
	final Section getSection() {
		return section;
	}

	/**
	 * Renders the template to the specified writer using the specified settings, global data, and annotation handlers. The writer is not closed, so that chaining can be used, if desired.
	 *
	 * @param settings the settings used while rendering
	 * @param globalData the global data used while rendering
	 * @param writer the writer used to render the template
	 * @param annotations the map of annotation handlers used to process annotations
	 * @return the writer passed to the render method
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public Writer render(final Settings settings, final Object globalData, final Writer writer, final Map<String, AnnotationHandler> annotations) throws IOException {
		try {
			final RenderContext renderContext = new RenderContext(settings, globalData, annotations);

			renderContext.getIndentation().push("");

			for (final Renderer renderer : renderList) {
				renderer.render(renderContext, writer);
			}
		} catch (final HaltRenderingException e) {
			settings.getLogger().log(Level.SEVERE, e.getMessage());
		}

		return writer;
	}

	/**
	 * Renders the template to the specified writer using the specified settings and global data. The default annotation handlers will be used. The writer is not closed, so that chaining can be used, if desired.
	 *
	 * @param settings the settings used while rendering
	 * @param globalData the global data used while rendering
	 * @param writer the writer used to render the template
	 * @return the writer passed to the render method
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public Writer render(final Settings settings, final Object globalData, final Writer writer) throws IOException {
		return render(settings, globalData, writer, AnnotationHandlers.DEFAULT_ANNOTATIONS);
	}

	/**
	 * Renders the template to the specified writer using the specified global data. The default settings and annotation handlers will be used. The writer is not closed, so that chaining can be used, if desired.
	 *
	 * @param globalData the global data used while rendering
	 * @param writer the writer used to render the template
	 * @return the writer passed to the render method
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public Writer render(final Object globalData, final Writer writer) throws IOException {
		return render(new Settings(), globalData, writer);
	}

}
