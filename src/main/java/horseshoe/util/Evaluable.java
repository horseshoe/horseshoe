package horseshoe.util;

import horseshoe.Expression;
import horseshoe.RenderContext;

public abstract class Evaluable {

	/**
	 * Evaluates the object using the specified parameters.
	 *
	 * @param expressions the expressions used to evaluate the object
	 * @param identifiers the identifiers used to evaluate the object
	 * @param context the context used to evaluate the object
	 * @param arguments the arguments used to evaluate the object
	 * @return the result of evaluating the object
	 * @throws Exception if an error occurs while evaluating the expression
	 */
	public abstract Object evaluate(final Expression[] expressions, final Identifier[] identifiers, final RenderContext context, final Object[] arguments) throws Exception;

}