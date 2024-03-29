package horseshoe;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import horseshoe.util.SectionRenderData;

/**
 * A {@link RenderContext} is the context object used during the {@link Template} rendering process.
 * It is created and populated by the user prior to rendering.
 */
public final class RenderContext {

	private final Settings settings;
	private final Map<String, AnnotationHandler> annotationMap;
	private final Stack<SectionRenderData> sectionData = new Stack<>();
	@SuppressWarnings("unchecked")
	private Stack<Object[]>[] templateBindings = new Stack[] { new Stack<>(),  new Stack<>(),  new Stack<>() };
	private Object repeatedSectionData = null;
	private final Stack<String> indentation = new Stack<>();
	private final Stack<Template> sectionPartials = new Stack<>();

	/**
	 * Creates a render context.
	 *
	 * @param settings the settings that will be used as part of the render context
	 * @param globalData the global data that will be used as part of the render context
	 * @param annotationMap the map used to process annotations
	 */
	public RenderContext(final Settings settings, final Object globalData, final Map<String, AnnotationHandler> annotationMap) {
		this.settings = settings;
		this.annotationMap = annotationMap;

		sectionData.push(new SectionRenderData(globalData));
		sectionPartials.push(new Template("[Null Partial Section]", "null"));
	}

	/**
	 * Creates a render context using the default annotation handlers.
	 *
	 * @param settings the settings that will be used as part of the render context
	 * @param globalData the global data that will be used as part of the render context
	 */
	public RenderContext(final Settings settings, final Object globalData) {
		this(settings, globalData, AnnotationHandlers.DEFAULT_ANNOTATIONS);
	}

	/**
	 * Creates a default render context with no global data.
	 */
	public RenderContext() {
		this(new Settings(), Collections.<String, Object>emptyMap());
	}

	/**
	 * Gets the annotation map used by the rendering process.
	 *
	 * @return the annotation map used by the rendering process
	 */
	public Map<String, AnnotationHandler> getAnnotationMap() {
		return annotationMap;
	}

	/**
	 * Gets the class with the specified name. The class name is first verified as a class that can be loaded.
	 *
	 * @param name the name of the class to get
	 * @return the class that corresponds to the specified name
	 */
	public Class<?> getClass(final String name) {
		return settings.getLoadableClass(name);
	}

	/**
	 * Gets the indentation used by the rendering process.
	 *
	 * @return the indentation used by the rendering process
	 */
	public Stack<String> getIndentation() {
		return indentation;
	}

	/**
	 * Gets the repeated section data used by the rendering process.
	 *
	 * @return the repeated section data used by the rendering process
	 */
	Object getRepeatedSectionData() {
		return repeatedSectionData;
	}

	/**
	 * Gets the section data used by the rendering process.
	 *
	 * @return the section data used by the rendering process
	 */
	public Stack<SectionRenderData> getSectionData() {
		return sectionData;
	}

	/**
	 * Gets the section partials used by the rendering process.
	 *
	 * @return the section partials used by the rendering process
	 */
	public Stack<Template> getSectionPartials() {
		return sectionPartials;
	}

	/**
	 * Gets the settings used by the rendering process.
	 *
	 * @return the settings used by the rendering process
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Gets the template bindings for the template at the specified index.
	 *
	 * @param templateIndex the index of the template
	 * @return the template bindings for the template at the specified index
	 */
	public Stack<Object[]> getTemplateBindings(final int templateIndex) {
		int oldLength = templateBindings.length;

		if (templateIndex >= oldLength) {
			int length = oldLength;

			do {
				length = length * 2 + 1;
			} while (templateIndex >= (length & 0xFFFFFFFFL));

			templateBindings = Arrays.copyOf(templateBindings, length);

			for (int i = oldLength; i < length; i++) {
				templateBindings[i] = new Stack<>();
			}
		}

		return templateBindings[templateIndex];
	}

	/**
	 * Sets the repeated section data used by the rendering process.
	 *
	 * @param repeatedSectionData the repeated section data used by the rendering process
	 * @return this context
	 */
	RenderContext setRepeatedSectionData(final Object repeatedSectionData) {
		this.repeatedSectionData = repeatedSectionData;
		return this;
	}

}
