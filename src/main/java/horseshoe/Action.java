package horseshoe;

import java.io.IOException;
import java.io.Writer;

import horseshoe.internal.RenderContext;

interface Action {

	/**
	 * Performs a specific action using the render context and the writer.
	 *
	 * @param context the render context associated with the action
	 * @param writer the writer to use for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	void perform(final RenderContext context, final Writer writer) throws IOException;

}
