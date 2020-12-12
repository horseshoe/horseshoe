package horseshoe.internal;

import java.util.Map;
import java.util.regex.Matcher;

import horseshoe.Stack;

public final class ExpressionParseState {

	private final int startIndex;
	private final String trimmedString;
	private final Map<String, Expression> namedExpressions;
	private final CacheList<Expression> expressions = new CacheList<>();
	private final CacheList<Identifier> identifiers = new CacheList<>();
	private final CacheList<String> localBindings = new CacheList<>();
	private final Stack<Operand> operands = new Stack<>();
	private final Stack<Operator> operators = new Stack<>();

	/**
	 * Creates a new expression parse state.
	 *
	 * @param startIndex the starting index of the trimmed string within the tag
	 * @param trimmedString the trimmed string of the expression
	 * @param namedExpressions the set of named expressions that can be used in the expression
	 */
	public ExpressionParseState(final int startIndex, final String trimmedString, final Map<String, Expression> namedExpressions) {
		this.startIndex = startIndex;
		this.trimmedString = trimmedString;
		this.namedExpressions = namedExpressions;
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
	 * Gets the trimmed expression string.
	 *
	 * @return the trimmed expression string
	 */
	public String getTrimmedString() {
		return trimmedString;
	}

}
