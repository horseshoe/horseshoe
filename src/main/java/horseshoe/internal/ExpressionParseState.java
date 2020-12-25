package horseshoe.internal;

import java.util.Map;
import java.util.regex.Matcher;

import horseshoe.Stack;

/**
 * Stores the state of an expression being parsed.
 */
public final class ExpressionParseState {

	private final int startIndex;
	private final String expressionString;
	private boolean returnsValue = true;
	private final Map<String, Expression> namedExpressions;
	private final Map<Identifier, Identifier> allIdentifiers;
	private final CacheList<String> templateBindings;
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
	public ExpressionParseState(final int startIndex, final String expressionString, final Map<String, Expression> namedExpressions, final Map<Identifier, Identifier> allIdentifiers, final CacheList<String> templateBindings) {
		this.startIndex = startIndex;
		this.expressionString = expressionString;
		this.namedExpressions = namedExpressions;
		this.allIdentifiers = allIdentifiers;
		this.templateBindings = templateBindings;
	}

	/**
	 * Returns if the expression returns a value (named expression, template binding do not return values).
	 *
	 * @return true if the expression returns a value, otherwise false
	 */
	public boolean returnsValue() {
		return returnsValue;
	}

	/**
	 * Gets the matching cached identifier from the set of all identifiers.
	 *
	 * @return the matching cached identifier
	 */
	public Identifier getCachedIdentifier(final Identifier identifier) {
		return Utilities.getOrAddMapValue(allIdentifiers, identifier, identifier);
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
	 * Gets the named expressions map.
	 *
	 * @return the named expressions map
	 */
	public Map<String, Expression> getNamedExpressions() {
		return namedExpressions;
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
	 * Gets the identifier cache list.
	 *
	 * @return the identifier cache list
	 */
	public CacheList<Identifier> getIdentifiers() {
		return identifiers;
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
	 * Gets the template bindings cache list.
	 *
	 * @return the template bindings cache list
	 */
	public CacheList<String> getTemplateBindings() {
		return templateBindings;
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
	 * Sets if the expression returns a value (named expression, template binding do not return values).
	 *
	 * @param returnsValue true if the expression returns a value, otherwise false
	 * @return this parse state
	 */
	public ExpressionParseState setReturnsValue(final boolean returnsValue) {
		this.returnsValue = returnsValue;
		return this;
	}

}
