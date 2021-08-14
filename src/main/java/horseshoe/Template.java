package horseshoe;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import horseshoe.internal.Expression;
import horseshoe.internal.TemplateBinding;

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
	private final int index;
	private final List<String> parameters;
	private final Map<String, TemplateBinding> bindings = new LinkedHashMap<>();
	private final Map<String, Expression> rootExpressions = new LinkedHashMap<>();

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
	 * Creates an empty template with the specified name and index.
	 *
	 * @param name the name of the template
	 * @param identifier the identifier of the template
	 * @param index the index of the template partial, or 0 to indicate top-level template
	 * @param parameters the names of the parameters for the template, or null if the template is not a local partial
	 */
	Template(final String name, final Object identifier, final int index, final List<String> parameters) {
		this.identifier = identifier;
		this.section = new Section(null, name == null ? "[Anonymous]" : name, identifier, null, null, true, parameters != null);
		this.index = index;

		if (parameters == null) {
			this.parameters = Collections.emptyList();
		} else {
			this.parameters = parameters;

			for (final String parameterName : parameters) {
				bindings.put(parameterName, new TemplateBinding(parameterName, index, bindings.size()));
			}
		}
	}

	/**
	 * Creates an empty template with the specified name and index.
	 *
	 * @param name the name of the template
	 * @param identifier the identifier of the template
	 * @param index the index of the template partial, or 0 to indicate top-level template
	 */
	Template(final String name, final Object identifier, final int index) {
		this(name, identifier, index, null);
	}

	/**
	 * Creates an empty template with the specified name.
	 *
	 * @param name the name of the template
	 * @param identifier the identifier of the template
	 */
	protected Template(final String name, final Object identifier) {
		this(name, identifier, 0);
	}

	/**
	 * Gets the bindings associated with the template.
	 *
	 * @return the bindings associated with the template
	 */
	final Map<String, TemplateBinding> getBindings() {
		return bindings;
	}

	/**
	 * Gets the index associated with the template.
	 *
	 * @return the index associated with the template
	 */
	final int getIndex() {
		return index;
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
	 * Gets the list of parameters associated with the template.
	 *
	 * @return the list of parameters associated with the template
	 */
	final List<String> getParameters() {
		return parameters;
	}

	/**
	 * Gets the root expressions associated with the template.
	 *
	 * @return the root expressions associated with the template
	 */
	final Map<String, Expression> getRootExpressions() {
		return rootExpressions;
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
			renderUnprotected(settings, globalData, writer, annotations);
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

	/**
	 * Renders the template to the specified writer using the specified settings, global data, and annotation handlers. The writer is not closed, so that chaining can be used, if desired.
	 *
	 * @param settings the settings used while rendering
	 * @param globalData the global data used while rendering
	 * @param writer the writer used to render the template
	 * @param annotations the map of annotation handlers used to process annotations
	 * @return the writer passed to the render method
	 * @throws HaltRenderingException if rendering the template results in a die operation
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public Writer renderUnprotected(final Settings settings, final Object globalData, final Writer writer, final Map<String, AnnotationHandler> annotations) throws HaltRenderingException, IOException {
		new TemplateRenderer(this, null).render(new RenderContext(settings, globalData, annotations), writer);
		return writer;
	}

}
