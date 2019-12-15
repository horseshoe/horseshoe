package horseshoe.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Operator {

	// Operator Properties
	public static final int LEFT_EXPRESSION     = 0x00000001; // Has an expression on the left
	public static final int LEFT_ASSIGNABLE     = 0x00000002; // Has an assignable on the left
	public static final int RIGHT_EXPRESSION    = 0x00000004; // Has an expression on the right
	public static final int RIGHT_ASSIGNABLE    = 0x00000008; // Has an assignable on the right
	public static final int X_RIGHT_EXPRESSIONS = 0x00000010; // Has 0 or more comma-separated expressions on the right

	public static final int METHOD_CALL         = 0x00000020; // Is a method call (starts with '.', ends with '(')
	public static final int RIGHT_ASSOCIATIVITY = 0x00000040; // Is evaluated right to left
	public static final int DEFERRED_EVALUATION = 0x00000080; // The right side may not be evaluated depending on the left side

	public static final int RIGHT_TYPE_RETURN  = 0x00002000;
	public static final int RESOLVABLE_RETURN  = 0x00100000;

	private static final List<Operator> OPERATORS;
	private static final Map<String, Operator> OPERATOR_LOOKUP = new LinkedHashMap<>();

	static {
		final List<Operator> operators = new ArrayList<>();

		operators.add(createMethod("("));
		operators.add(new Operator("(",    0,  RIGHT_EXPRESSION | RIGHT_TYPE_RETURN, "Parentheses", ")"));
		operators.add(new Operator(".",    0,  LEFT_EXPRESSION | RIGHT_EXPRESSION | RESOLVABLE_RETURN, "Navigate"));
		operators.add(new Operator("+",    5,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Add"));
		operators.add(new Operator(",",    16, LEFT_EXPRESSION | RIGHT_EXPRESSION | RIGHT_TYPE_RETURN, "Comma Operator"));

		for (final Operator operator : operators) {
			operator.next = OPERATOR_LOOKUP.put(operator.string, operator);
		}

		OPERATORS = Collections.unmodifiableList(operators);
	}

	/**
	 * Gets the operator for the specified method name.
	 *
	 * @param name the name of the method
	 * @return the operator for the specified method name
	 */
	public static Operator createMethod(final String name) {
		return new Operator(name, 0, METHOD_CALL | X_RIGHT_EXPRESSIONS, "Call Method", ")");
	}

	/**
	 * Gets the operator of the specified string representation.
	 *
	 * @param operator the string representation of the operator
	 * @return the matching operator if it exists, otherwise null
	 */
	public static Operator get(final String operator) {
		return OPERATOR_LOOKUP.get(operator);
	}

	/**
	 * Gets a list of all supported operators.
	 *
	 * @return a list of all supported operators
	 */
	public static List<Operator> getAll() {
		return OPERATORS;
	}

	private final String string;
	private final int precedence; // 0 is the highest
	private final int properties;
	private final String description;
	private final String closingString;

	private int rightExpressions;
	private Operator next = null;

	private Operator(final String string, final int precedence, final int properties, final String description, final String closingString) {
		this.string = string;
		this.precedence = precedence;
		this.properties = properties;
		this.description = description;
		this.closingString = closingString;
		this.rightExpressions = has(RIGHT_EXPRESSION) ? 1 : 0;
	}

	private Operator(final String string, final int precedence, final int properties, final String description) {
		this(string, precedence, properties, description, null);
	}

	/**
	 * Adds a right expression to the operator.
	 *
	 * @return this operator
	 */
	public Operator addRightExpression() {
		if (has(X_RIGHT_EXPRESSIONS)) {
			rightExpressions++;
		}

		return this;
	}

	/**
	 * Duplicates the operator.
	 *
	 * @return the duplicate of the operator
	 */
	public Operator duplicate() {
		return new Operator(string, precedence, properties, closingString);
	}

	/**
	 * Gets the closing operator.
	 *
	 * @return the closing operator
	 */
	public Operator getClosingOperator() {
		return new Operator(closingString, precedence, LEFT_EXPRESSION, null);
	}

	/**
	 * Gets the closing string for the operator.
	 *
	 * @return the closing string for the operator, or null if none exists
	 */
	public String getClosingString() {
		return closingString;
	}

	/**
	 * Gets the description for the operator.
	 *
	 * @return the description for the operator
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the string representation of the operator.
	 *
	 * @return the string representation of the operator
	 */
	public String getString() {
		return string;
	}

	/**
	 * Gets the precedence of the operator.
	 *
	 * @return the precedence of the operator
	 */
	public int getPrecedence() {
		return precedence;
	}

	/**
	 * Gets the number of right expressions for the operator.
	 *
	 * @return the number of right expressions for the operator
	 */
	public int getRightExpressions() {
		return rightExpressions;
	}

	/**
	 * Gets if the operator has the given property.
	 *
	 * @return true if the operator has the given property, otherwise false
	 */
	public boolean has(final int property) {
		return (properties & property) != 0;
	}

	/**
	 * Gets if the operator is left associative.
	 *
	 * @return true if the operator is left associative, otherwise false
	 */
	public boolean isLeftAssociative() {
		return !has(RIGHT_ASSOCIATIVITY);
	}

	/**
	 * Gets the next operator with the same string representation.
	 *
	 * @return the next operator with the same string representation, or null if none exists
	 */
	public Operator next() {
		return next;
	}

	@Override
	public String toString() {
		if (has(X_RIGHT_EXPRESSIONS)) {
			return string + ": " + rightExpressions;
		}

		return string;
	}

}
