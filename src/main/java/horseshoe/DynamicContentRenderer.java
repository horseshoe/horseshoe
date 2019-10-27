package horseshoe;

import java.io.IOException;
import java.io.Writer;

final class DynamicContentRenderer implements Action {

	private final Expression resolver;
	private final boolean escaped;

	/**
	 * Creates a new render dynamic content action.
	 *
	 * @param resolver the resolver for the expression
	 * @param escaped true if the rendered content will be escaped, otherwise false
	 */
	public DynamicContentRenderer(final Expression resolver, final boolean escaped) {
		this.resolver = resolver;
		this.escaped = escaped;
	}

	@Override
	public void perform(final RenderContext context, final Writer writer) throws IOException {
		final Object obj = resolver.evaluate(context);

		if (obj != null) {
			final String value = obj.toString();
			writer.write(escaped && context.getSettings().getEscapeFunction() != null ? context.getSettings().getEscapeFunction().escape(value) : value);
		}
	}

}
