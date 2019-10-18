package horseshoe;

import java.util.HashMap;
import java.util.Map;

import horseshoe.internal.PersistentStack;

final class RenderContext {

	private final Context userContext;
	private final Map<String, Object> globalData;
	private final PersistentStack<Object> sectionData = new PersistentStack<>();
	private final PersistentStack<String> indentation = new PersistentStack<>();

	/**
	 * Creates a default render context.
	 */
	public RenderContext(final Context userContext, final Map<String, Object> globalData) {
		this.userContext = userContext;
		this.globalData = new HashMap<>(globalData);
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
	 * Gets the section data used by the rendering process.
	 *
	 * @return the section data used by the rendering process
	 */
	PersistentStack<Object> getSectionData() {
		return sectionData;
	}

	/**
	 * Gets the user context used by the rendering process.
	 *
	 * @return the user context used by the rendering process
	 */
	Context getUserContext() {
		return userContext;
	}

	/**
	 * Resets the context, so it can be reused
	 */
	void reset() {
		sectionData.clear();
		indentation.clear();
	}

}
