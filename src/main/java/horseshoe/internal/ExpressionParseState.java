package horseshoe.internal;

import java.util.Map;
import java.util.regex.Matcher;

import horseshoe.Stack;

/**
 * Stores the state of an expression being parsed.
 */
public final class ExpressionParseState {

	public enum Evaluation {
		EVALUATE_AND_RENDER,
		EVALUATE,
		NO_EVALUATION
	}

	private final int startIndex;
	private final String expressionString;
	private Evaluation evaluation = Evaluation.EVALUATE_AND_RENDER;
	private final Map<String, Expression> namedExpressions;
	private final Map<Identifier, Identifier> allIdentifiers;
	private final Stack<Map<String, TemplateBinding>> templateBindings;
	private final CacheList<Expression> expressions = new CacheList<>();
	private final CacheList<Identifier> identifiers = new CacheList<>();
	private final CacheList<String> localBindings = new CacheList<>();
	private final Stack<Operand> operands = new Stack<>();
	private final Stack<Operator> operators = new Stack<>();

	/**
	 * Creates a new expression parse state.
	 *
	 * @param startIndex the starting index of the trimmed string within the tag
	 * @param expressionString the string representation of the expression
	 * @param namedExpressions the set of named expressions that can be used in the expression
	 * @param allIdentifiers the set of all identifiers that can be used as a cache in the expression
	 * @param templateBindings the set of all bindings used in the template
	 */
	public ExpressionParseState(final int startIndex, final String expressionString, final Map<String, Expression> namedExpressions, final Map<Identifier, Identifier> allIdentifiers, final Stack<Map<String, TemplateBinding>> templateBindings) {
		this.startIndex = startIndex;
		this.expressionString = expressionString;
		this.namedExpressions = namedExpressions;
		this.allIdentifiers = allIdentifiers;
		this.templateBindings = templateBindings;
	}

	/**
	 * Gets the matching cached identifier from the set of all identifiers.
	 *
	 * @return the matching cached identifier
	 */
	public Identifier getCachedIdentifier(final Identifier identifier) {
		allIdentifiers.putIfAbsent(identifier, identifier);
		return identifier;
	}

	/**
	 * Gets the evaluation of the expression (template binding assignments only evaluate, named expressions don't evaluate).
	 *
	 * @return the evaluation of the expression
	 */
	public Evaluation getEvaluation() {
		return evaluation;
	}

	/**
	 * Gets the expression cache list.
	 *
	 * @return the expression cache list
	 */
	public CacheList<Expression> getExpressions() {
		return expressions;
	}

	/**
	 * Gets the expression string.
	 *
	 * @return the expression string
	 */
	public String getExpressionString() {
		return expressionString;
	}

	/**
	 * Gets the identifier cache list.
	 *
	 * @return the identifier cache list
	 */
	public CacheList<Identifier> getIdentifiers() {
		return identifiers;
	}

	/**
	 * Gets the index of the matcher within the tag.
	 *
	 * @return the index of the matcher within the tag
	 */
	public int getIndex(final Matcher matcher) {
		return startIndex + matcher.regionStart();
	}

	/**
	 * Gets the identifier cache list.
	 *
	 * @return the identifier cache list
	 */
	public CacheList<String> getLocalBindings() {
		return localBindings;
	}

	/**
	 * Gets the named expressions map.
	 *
	 * @return the named expressions map
	 */
	public Map<String, Expression> getNamedExpressions() {
		return namedExpressions;
	}

	/**
	 * Gets the stack of operands currently being parsed.
	 *
	 * @return the stack of operands currently being parsed
	 */
	public Stack<Operand> getOperands() {
		return operands;
	}

	/**
	 * Gets the stack of operators currently being parsed.
	 *
	 * @return the stack of operators currently being parsed
	 */
	public Stack<Operator> getOperators() {
		return operators;
	}

	/**
	 * Gets or adds the template binding for the specified name in the current template.
	 *
	 * @param name the name of the template binding to get or add
	 * @return the template binding with the specified name
	 */
	public TemplateBinding getOrAddTemplateBinding(final String name) {
		final Map<String, TemplateBinding> bindings = templateBindings.peek();
		final TemplateBinding existingBinding = bindings.get(name);

		if (existingBinding != null) {
			return existingBinding;
		}

		final TemplateBinding binding = new TemplateBinding(name, templateBindings.size() - 1, bindings.size());

		bindings.put(name, binding);
		return binding;
	}

	/**
	 * Gets the template binding for the specified name.
	 *
	 * @param name the name of the template binding to get
	 * @return the template binding with the specified name, or null if none exists
	 */
	public TemplateBinding getTemplateBinding(final String name) {
		for (final Map<String, TemplateBinding> bindings : templateBindings) {
			final TemplateBinding binding = bindings.get(name);

			// If the binding is found, return it
			if (binding != null) {
				return binding;
			}
		}

		return null;
	}

	/**
	 * Sets the evaluation of the expression (template binding assignments only evaluate, named expressions don't evaluate).
	 *
	 * @param evaluation the evaluation of the expression
	 * @return this parse state
	 */
	public ExpressionParseState setEvaluation(final Evaluation evaluation) {
		this.evaluation = evaluation;
		return this;
	}

}
