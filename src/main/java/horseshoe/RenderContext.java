package horseshoe;

import java.util.HashMap;
import java.util.Map;

import horseshoe.internal.PersistentStack;

final class RenderContext {

	private final Settings settings;
	private final Map<String, Object> globalData;
	private final Template.WriterMap writerMap;
	private final PersistentStack<Object> sectionData = new PersistentStack<>();
	private final PersistentStack<String> indentation = new PersistentStack<>();

	/**
	 * Creates a render context.
	 *
	 * @param settings the settings that will be used as part of the render context
	 * @param globalData the global data that will be used as part of the render context
	 */
	public RenderContext(final Settings settings, final Map<String, Object> globalData, final Template.WriterMap writerMap) {
		this.settings = settings;
		this.globalData = new HashMap<>(globalData);
		this.writerMap = writerMap;
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
	 * Gets the settings used by the rendering process.
	 *
	 * @return the settings used by the rendering process
	 */
	Settings getSettings() {
		return settings;
	}

	/**
	 * Gets the writer map used by the rendering process.
	 *
	 * @return the writer map used by the rendering process
	 */
	Template.WriterMap getWriterMap() {
		return writerMap;
	}

	/**
	 * Resets the context, so it can be reused
	 */
	void reset() {
		sectionData.clear();
		indentation.clear();
	}

}
