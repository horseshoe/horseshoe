package horseshoe;

import java.io.IOException;
import java.io.Writer;

final class DynamicContentRenderer implements Renderer {

	private final Expression expression;
	private final boolean escaped;

	/**
	 * Creates a new render dynamic content action.
	 *
	 * @param expression the expression for the dynamic content
	 * @param escaped true if the rendered content will be escaped, otherwise false
	 */
	public DynamicContentRenderer(final Expression expression, final boolean escaped) {
		this.expression = expression;
		this.escaped = escaped;
	}

	@Override
	public void render(final RenderContext context, final Writer writer) throws IOException {
		final Object value = expression.evaluate(context);

		if (value != null) {
			final String string = value.toString();
			writer.write(escaped ? context.getSettings().getEscapeFunction().escape(string) : string);
		}
	}

}
