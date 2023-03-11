package horseshoe;

abstract class TagRenderer extends Renderer {

	private boolean isStandalone = false;

	/**
	 * Checks the standalone property of the renderer. This property can affect how indentation and trailing newlines are rendered.
	 *
	 * @return true if the standalone property of the renderer is enabled, otherwise false
	 */
	boolean isStandalone() {
		return isStandalone;
	}

	/**
	 * Sets the standalone property of the renderer. This property can affect how indentation and trailing newlines are rendered.
	 *
	 * @param isStandalone true to set the renderer in standalone render mode, false to disable standalone render mode
	 */
	void setStandalone(final boolean isStandalone) {
		this.isStandalone = isStandalone;
	}

}
