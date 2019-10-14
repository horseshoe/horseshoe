package horseshoe;

import java.io.PrintStream;

interface Action {

	/**
	 * Performs a specific action using the render context and the rendering stream.
	 *
	 * @param context the render context associated with the action
	 * @param stream the rendering stream associated with the action
	 */
	void perform(final RenderContext context, final PrintStream stream);

}
