package horseshoe.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import horseshoe.AnnotationHandler;
import horseshoe.AnnotationHandlers;
import horseshoe.Settings;

public final class RenderContext {

	private final Settings settings;
	private final Map<String, Object> globalData;
	private final Map<String, AnnotationHandler> annotationMap;
	private final PersistentStack<Object> sectionData = new PersistentStack<>();
	private Object repeatedSectionData = null;
	private final PersistentStack<Expression.Indexed> indexedData = new PersistentStack<>();
	private final PersistentStack<String> indentation = new PersistentStack<>();
	private final Map<String, Class<?>> loadableClasses;

	/**
	 * Creates a render context.
	 *
	 * @param settings the settings that will be used as part of the render context
	 * @param globalData the global data that will be used as part of the render context
	 * @param annotationMap the map used to process annotations
	 */
	public RenderContext(final Settings settings, final Map<String, Object> globalData, final Map<String, AnnotationHandler> annotationMap) {
		this.settings = settings;
		this.globalData = new LinkedHashMap<>(globalData);
		this.annotationMap = annotationMap;
		this.loadableClasses = new HashMap<>(Math.max(16, (settings.getLoadableClasses().size() * 4 + 2) / 3));

		sectionData.push(this.globalData);

		for (final String className : settings.getLoadableClasses()) {
			loadableClasses.put(className, null);
		}
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
	 * @throws ClassNotFoundException if the class could not be found
	 */
	public Class<?> getClass(final String name) throws ClassNotFoundException {
		final Class<?> theClass = loadableClasses.get(name);

		if (theClass != null) {
			return theClass;
		}

		if (!loadableClasses.containsKey(name)) {
			throw new IllegalArgumentException("Attempt to load invalid class: " + name);
		}

		final Class<?> newClass = Class.forName(name.indexOf('.') >= 0 ? name : "java.lang." + name);

		loadableClasses.put(name, newClass);
		return newClass;
	}

	/**
	 * Gets the global data used by the rendering process.
	 *
	 * @return the global data used by the rendering process
	 */
	public Map<String, Object> getGlobalData() {
		return globalData;
	}

	/**
	 * Gets the indentation used by the rendering process.
	 *
	 * @return the indentation used by the rendering process
	 */
	public PersistentStack<String> getIndentation() {
		return indentation;
	}

	/**
	 * Gets the indexed data used by the rendering process.
	 *
	 * @return the indexed data used by the rendering process
	 */
	public PersistentStack<Expression.Indexed> getIndexedData() {
		return indexedData;
	}

	/**
	 * Gets the repeated section data used by the rendering process.
	 *
	 * @return the repeated section data used by the rendering process
	 */
	public Object getRepeatedSectionData() {
		return repeatedSectionData;
	}

	/**
	 * Gets the section data used by the rendering process.
	 *
	 * @return the section data used by the rendering process
	 */
	public PersistentStack<Object> getSectionData() {
		return sectionData;
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
	public RenderContext setRepeatedSectionData(final Object repeatedSectionData) {
		this.repeatedSectionData = repeatedSectionData;
		return this;
	}

}
