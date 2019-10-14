package horseshoe;

import java.io.PrintStream;

class RenderDynamicContent implements Action {

	private final Expression resolver;
	private final boolean escaped;

	/**
	 * Creates a new render dynamic content action.
	 *
	 * @param resolver the resolver for the expression
	 * @param escaped true if the rendered content will be escaped, otherwise false
	 */
	RenderDynamicContent(final Expression resolver, final boolean escaped) {
		this.resolver = resolver;
		this.escaped = escaped;
	}

	@Override
	public void perform(final RenderContext context, final PrintStream stream) {
		final Object obj = resolver.evaluate(context);

		if (obj != null) {
			final String value = obj.toString();
			stream.append(escaped && context.getEscapeFunction() != null ? context.getEscapeFunction().escape(value) : value);
		}
	}

}
