package horseshoe;

/**
 * Section render data are used to track details about the state of the current section being rendered.
 */
public final class SectionRenderData {

	public Object data = null;
	public int index = 0;
	public boolean hasNext = false;

	/**
	 * Creates an empty section render data.
	 */
	public SectionRenderData() {
	}

	/**
	 * Creates a section render data with the specified data object.
	 *
	 * @param data the data object used to render the section
	 */
	public SectionRenderData(final Object data) {
		this.data = data;
	}

	/**
	 * Updates all the data in the section render data.
	 *
	 * @param data the data object used to render the section
	 * @param index the index of the data object within the section
	 * @param hasNext true if at least one more data object will be rendered using the section, otherwise false
	 */
	public void update(final Object data, final int index, final boolean hasNext) {
		this.data = data;
		this.index = index;
		this.hasNext = hasNext;
	}

}
