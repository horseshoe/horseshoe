package horseshoe;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

final class ExpressionSegment {

	private final Map<ResolverContext, Resolver> resolverDatabase = new LinkedHashMap<>();
	private final String identifier;
	private final Expression args[];

	public ExpressionSegment(final String identifier, final Expression args[]) {
		this.identifier = identifier;
		this.args = args;
	}

	@Override
	public boolean equals(final Object object) {
		if (this == object) {
			return true;
		} else if (object instanceof ExpressionSegment) {
			return identifier.equals(((ExpressionSegment)object).identifier) && Arrays.equals(args, ((ExpressionSegment)object).args);
		} else if (object instanceof CharSequence) {
			return false; // TODO
		}

		return false;
	}

	/**
	 * Evaluates the expression segment given the render context and the result of the previous segment
	 *
	 * @param context the render context to use for evaluation
	 * @param object the result of the previous segment
	 * @return the result of the current expression
	 */
	public Object evaluate(final RenderContext context, final Object object) {
		// Evaluate arguments
		Object evaluatedArgs[] = null;
		Class<?> evaluatedClasses[] = null;

		if (args != null) {
			evaluatedArgs = new Object[args.length];
			evaluatedClasses = new Class<?>[args.length];

			for (int i = 0; i < args.length; i++) {
				evaluatedArgs[i] = args[i].evaluate(context);

				if (evaluatedArgs[i] != null) {
					evaluatedClasses[i] = evaluatedArgs[i].getClass();
				}
			}
		}

		// Get resolver
		final ResolverContext expressionContext = new ResolverContext(object.getClass(), evaluatedClasses);
		Resolver resolver = resolverDatabase.get(expressionContext);

		if (resolver == null) {
			resolver = Resolver.FACTORY.create(expressionContext, identifier);
			resolverDatabase.put(expressionContext, resolver);
		}

		return resolver.resolve(object, evaluatedArgs);
	}

	@Override
	public int hashCode() {
		int hash = identifier.hashCode();

		for (final Expression arg : args) {
			hash = hash * 31 + arg.hashCode();
		}

		return hash;
	}

	@Override
	public String toString() {
		if (args.length == 0) {
			return identifier;
		}

		final StringBuilder sb = new StringBuilder(identifier).append("(").append(args[0].toString());

		for (int i = 1; i < args.length; i++) {
			sb.append(", ").append(args[i].toString());
		}

		return sb.append(")").toString();
	}

}
