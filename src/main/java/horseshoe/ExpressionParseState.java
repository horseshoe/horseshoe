package horseshoe;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;

import horseshoe.util.Identifier;

/**
 * Stores the state of an expression being parsed.
 */
final class ExpressionParseState {

	enum Evaluation {
		EVALUATE_AND_RENDER,
		EVALUATE,
		NO_EVALUATION
	}

	private final int startIndex;
	private final String expressionString;
	private final EnumSet<Extension> extensions;
	private final boolean isCall;
	private Evaluation evaluation = Evaluation.EVALUATE_AND_RENDER;
	private String bindingName = null;
	private String callName = null;
	private final HashMap<String, Expression> namedExpressions;
	private final HashMap<Identifier, Identifier> allIdentifiers;
	private final Stack<LinkedHashMap<String, TemplateBinding>> templateBindings;
	private final CacheList<Expression> expressions;
	private final CacheList<Identifier> identifiers;
	private final CacheList<String> localBindings;
	private final Stack<Operand> operands = new Stack<>();
	private final Stack<Operator> operators = new Stack<>();

	/**
	 * Creates a new expression parse state.
	 *
	 * @param startIndex the starting index of the trimmed string within the tag
	 * @param expressionString the string representation of the expression
	 * @param extensions the set of extensions currently in use
	 * @param isCall true if the expression should be parsed as a call invocation (no starting "(", ends with ")", returns array object), otherwise false
	 * @param namedExpressions the set of named expressions that can be used in the expression
	 * @param allIdentifiers the set of all identifiers that can be used as a cache in the expression
	 * @param templateBindings the set of all bindings used in the template
	 * @param expressions the cache of all expressions used in the template
	 * @param identifiers the cache of all identifiers used in the template
	 * @param localBindings the cache of all local bindings used in the template
	 */
	private ExpressionParseState(final int startIndex, final String expressionString, final EnumSet<Extension> extensions, final boolean isCall, final HashMap<String, Expression> namedExpressions, final HashMap<Identifier, Identifier> allIdentifiers, final Stack<LinkedHashMap<String, TemplateBinding>> templateBindings, final CacheList<Expression> expressions, final CacheList<Identifier> identifiers, final CacheList<String> localBindings) {
		this.startIndex = startIndex;
		this.expressionString = expressionString;
		this.extensions = extensions;
		this.isCall = isCall;
		this.namedExpressions = namedExpressions;
		this.allIdentifiers = allIdentifiers;
		this.templateBindings = templateBindings;
		this.expressions = expressions;
		this.identifiers = identifiers;
		this.localBindings = localBindings;
	}

	/**
	 * Creates a new expression parse state.
	 *
	 * @param startIndex the starting index of the trimmed string within the tag
	 * @param expressionString the string representation of the expression
	 * @param extensions the set of extensions currently in use
	 * @param isCall true if the expression should be parsed as a call invocation (no starting "(", ends with ")", returns array object), otherwise false
	 * @param namedExpressions the set of named expressions that can be used in the expression
	 * @param allIdentifiers the set of all identifiers that can be used as a cache in the expression
	 * @param templateBindings the set of all bindings used in the template
	 */
	ExpressionParseState(final int startIndex, final String expressionString, final EnumSet<Extension> extensions, final boolean isCall, final HashMap<String, Expression> namedExpressions, final HashMap<Identifier, Identifier> allIdentifiers, final Stack<LinkedHashMap<String, TemplateBinding>> templateBindings) {
		this(startIndex, expressionString, extensions, isCall, namedExpressions, allIdentifiers, templateBindings, new CacheList<>(), new CacheList<>(), new CacheList<>());
	}

	/**
	 * Creates a new nested expression parse state.
	 *
	 * @param startIndex the starting index of the trimmed string within the tag
	 * @param expressionString the string representation of the expression
	 * @return the empty nested expression parse state
	 */
	ExpressionParseState createNestedState(final int startIndex, final String expressionString) {
		return new ExpressionParseState(startIndex, expressionString, extensions, false, namedExpressions, allIdentifiers, templateBindings, expressions, identifiers, localBindings);
	}

	/**
	 * Gets the binding name associated with the expression.
	 *
	 * @return the binding name associated with the expression
	 */
	String getBindingName() {
		return bindingName;
	}

	/**
	 * Gets the matching cached identifier from the set of all identifiers.
	 *
	 * @return the matching cached identifier
	 */
	Identifier getCachedIdentifier(final Identifier identifier) {
		allIdentifiers.putIfAbsent(identifier, identifier);
		return identifier;
	}

	/**
	 * Gets the call name associated with the expression.
	 *
	 * @return the call name associated with the expression
	 */
	String getCallName() {
		return callName;
	}

	/**
	 * Gets the evaluation of the expression (template binding assignments only evaluate, named expressions don't evaluate).
	 *
	 * @return the evaluation of the expression
	 */
	Evaluation getEvaluation() {
		return evaluation;
	}

	/**
	 * Gets the expression cache list.
	 *
	 * @return the expression cache list
	 */
	CacheList<Expression> getExpressions() {
		return expressions;
	}

	/**
	 * Gets the expression string.
	 *
	 * @return the expression string
	 */
	String getExpressionString() {
		return expressionString;
	}

	/**
	 * Gets the set of extensions currently in use.
	 *
	 * @return the set of extensions currently in use
	 */
	EnumSet<Extension> getExtensions() {
		return extensions;
	}

	/**
	 * Gets the identifier cache list.
	 *
	 * @return the identifier cache list
	 */
	CacheList<Identifier> getIdentifiers() {
		return identifiers;
	}

	/**
	 * Gets the index of the matcher within the tag.
	 *
	 * @return the index of the matcher within the tag
	 */
	int getIndex(final Matcher matcher) {
		return startIndex + matcher.regionStart();
	}

	/**
	 * Gets the identifier cache list.
	 *
	 * @return the identifier cache list
	 */
	CacheList<String> getLocalBindings() {
		return localBindings;
	}

	/**
	 * Gets the named expressions map.
	 *
	 * @return the named expressions map
	 */
	HashMap<String, Expression> getNamedExpressions() {
		return namedExpressions;
	}

	/**
	 * Gets the stack of operands currently being parsed.
	 *
	 * @return the stack of operands currently being parsed
	 */
	Stack<Operand> getOperands() {
		return operands;
	}

	/**
	 * Gets the stack of operators currently being parsed.
	 *
	 * @return the stack of operators currently being parsed
	 */
	Stack<Operator> getOperators() {
		return operators;
	}

	/**
	 * Gets or adds the template binding for the specified name in the current template.
	 *
	 * @param name the name of the template binding to get or add
	 * @return the template binding with the specified name
	 */
	TemplateBinding getOrAddTemplateBinding(final String name) {
		final TemplateBinding existingBinding = getTemplateBinding(name);

		if (existingBinding != null) {
			return existingBinding;
		}

		final LinkedHashMap<String, TemplateBinding> bindings = templateBindings.peek();
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
	TemplateBinding getTemplateBinding(final String name) {
		for (final LinkedHashMap<String, TemplateBinding> bindings : templateBindings) {
			final TemplateBinding binding = bindings.get(name);

			// If the binding is found, return it
			if (binding != null) {
				return binding;
			}
		}

		return null;
	}

	/**
	 * Checks if the expression should be parsed as a call invocation (starts "(", ends with ")", returns array object).
	 *
	 * @return true if the expression should be parsed as a call invocation
	 */
	boolean isCall() {
		return isCall;
	}

	/**
	 * Sets the binding name associated with the expression.
	 *
	 * @param bindingName the binding name associated with the expression
	 * @return this parse state
	 */
	ExpressionParseState setBindingName(final String bindingName) {
		this.bindingName = bindingName;
		return this;
	}

	/**
	 * Sets the call name associated with the expression.
	 *
	 * @param callName the call name associated with the expression
	 * @return this parse state
	 */
	ExpressionParseState setCallName(final String callName) {
		this.callName = callName;
		return this;
	}

	/**
	 * Sets the evaluation of the expression (template binding assignments only evaluate, named expressions don't evaluate).
	 *
	 * @param evaluation the evaluation of the expression
	 * @return this parse state
	 */
	ExpressionParseState setEvaluation(final Evaluation evaluation) {
		this.evaluation = evaluation;
		return this;
	}

}
