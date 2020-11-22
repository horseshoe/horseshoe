package horseshoe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RenderContext {

	private final Settings settings;
	private final Map<String, AnnotationHandler> annotationMap;
	private final Stack<Object> sectionData = new Stack<>();
	private Object repeatedSectionData = null;
	private final Stack<SectionRenderer> sectionRenderers = new Stack<>();
	private final Stack<String> indentation = new Stack<>();
	private final Stack<Template> sectionPartials = new Stack<>();
	private final Map<String, Class<?>> loadableClasses = new HashMap<>();

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

		sectionData.push(globalData);
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
		if (loadableClasses.isEmpty()) {
			for (final Class<?> loadableClass : settings.getLoadableClasses()) {
				loadableClasses.put(loadableClass.getName(), loadableClass);

				if (settings.allowUnqualifiedClassNames()) {
					loadableClasses.put(loadableClass.getSimpleName(), loadableClass);
				}
			}
		}

		return loadableClasses.get(name);
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
	public Stack<Object> getSectionData() {
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
	 * Gets the section renderers used by the rendering process.
	 *
	 * @return the section renderers used by the rendering process
	 */
	public Stack<SectionRenderer> getSectionRenderers() {
		return sectionRenderers;
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
