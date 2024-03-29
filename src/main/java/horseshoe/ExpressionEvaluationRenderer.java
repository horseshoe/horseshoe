package horseshoe;

import java.io.IOException;
import java.io.Writer;

final class ExpressionEvaluationRenderer extends Renderer {

	private final Expression expression;

	/**
	 * Creates a new expression evaluation renderer. This renderer only performs the evaluation and does not render any content
	 *
	 * @param expression the expression to evaluate
	 */
	ExpressionEvaluationRenderer(final Expression expression) {
		this.expression = expression;
	}

	@Override
	void render(final RenderContext context, final Writer writer) throws IOException {
		expression.evaluate(context);
	}

}
