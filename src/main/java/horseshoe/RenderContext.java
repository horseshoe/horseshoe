package horseshoe;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RenderContext {

	private static final Set<Package> UNQUALIFIED_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			System.class.getPackage())));

	private final Settings settings;
	private final Map<String, AnnotationHandler> annotationMap;
	private final Stack<Object> sectionData = new Stack<>();
	private Object repeatedSectionData = null;
	private final Stack<SectionRenderer> sectionRenderers = new Stack<>();
	private final Stack<String> indentation = new Stack<>();
	private final Map<String, Class<?>> loadableClasses = new HashMap<>();

	/**
	 * Creates a render context.
	 *
	 * @param settings the settings that will be used as part of the render context
	 * @param globalData the global data that will be used as part of the render context
	 * @param annotationMap the map used to process annotations
	 */
	public RenderContext(final Settings settings, final Map<String, Object> globalData, final Map<String, AnnotationHandler> annotationMap) {
		this.settings = settings;
		this.annotationMap = annotationMap;

		sectionData.push(new LinkedHashMap<>(globalData));
	}

	/**
	 * Creates a render context using the default annotation handlers.
	 *
	 * @param settings the settings that will be used as part of the render context
	 * @param globalData the global data that will be used as part of the render context
	 */
	public RenderContext(final Settings settings, final Map<String, Object> globalData) {
		this(settings, globalData, AnnotationHandlers.DEFAULT_ANNOTATIONS);
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

				if (UNQUALIFIED_PACKAGES.contains(loadableClass.getPackage())) {
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
