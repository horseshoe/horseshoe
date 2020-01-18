package horseshoe;

import java.util.LinkedHashMap;
import java.util.Map;

import horseshoe.internal.Expression;
import horseshoe.internal.PersistentStack;

final class RenderContext {

	private final Settings settings;
	private final Map<String, Object> globalData;
	private final Map<String, AnnotationHandler> annotationMap;
	private final PersistentStack<Object> sectionData = new PersistentStack<>();
	private final PersistentStack<Expression.Indexed> indexedData = new PersistentStack<>();
	private final PersistentStack<String> indentation = new PersistentStack<>();

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
	}

	/**
	 * Gets the annotation map used by the rendering process.
	 *
	 * @return the annotation map used by the rendering process
	 */
	Map<String, AnnotationHandler> getAnnotationMap() {
		return annotationMap;
	}

	/**
	 * Gets the global data used by the rendering process.
	 *
	 * @return the global data used by the rendering process
	 */
	Map<String, Object> getGlobalData() {
		return globalData;
	}

	/**
	 * Gets the indentation used by the rendering process.
	 *
	 * @return the indentation used by the rendering process
	 */
	PersistentStack<String> getIndentation() {
		return indentation;
	}

	/**
	 * Gets the indexed data used by the rendering process.
	 *
	 * @return the indexed data used by the rendering process
	 */
	PersistentStack<Expression.Indexed> getIndexedData() {
		return indexedData;
	}

	/**
	 * Gets the section data used by the rendering process.
	 *
	 * @return the section data used by the rendering process
	 */
	PersistentStack<Object> getSectionData() {
		return sectionData;
	}

	/**
	 * Gets the settings used by the rendering process.
	 *
	 * @return the settings used by the rendering process
	 */
	Settings getSettings() {
		return settings;
	}

	/**
	 * Resets the context, so it can be reused
	 */
	void reset() {
		sectionData.clear();
		indentation.clear();
	}

}
