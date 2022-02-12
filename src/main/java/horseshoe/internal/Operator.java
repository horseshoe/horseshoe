package horseshoe.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class Operator {

	/**
	 * This represents all allowed ASCII characters that can be used as unary operators.
	 *
	 * <p>Notes:
	 *  - ! probably shouldn't be allowed as it conflicts with Mustache comments ({@code {{! Mustache comment }}}), but it is the most logical "not" operator.
	 *  - ", ', and ` aren't allowed because they are used in string and identifier literals.
	 *  - #, &amp;, /, &lt;, &gt;, @, ^ aren't allowed because they conflict with Mustache or Horseshoe tags.
	 *  - $, _ aren't allowed because they are allowed to start identifier literals.
	 *  - %, *, :, =, ?, | aren't allowed because they could cause ambiguity in multi-character expressions.
	 *  - ), ], } aren't allowed because they close other operators.
	 *  - comma, ; aren't allowed because they are separators.
	 *  - \ isn't allowed because it is reserved for root scoping.
	 */
	private static final String UNARY_OPERATOR_CHARACTERS = "!(+-.[{~";

	/**
	 * This represents all allowed ASCII characters that can be used as binary operators.
	 *
	 * <p>Notes:
	 *  - ", ', and ` aren't allowed because they are used in string and identifier literals.
	 *  - $, _ aren't allowed because they are allowed in identifier literals.
	 *  - ), ], } aren't allowed because they close other operators.
	 *  - / probably shouldn't be allowed as it can be used as a path separator, but it is the most logical "divide" operator.
	 *  - \ isn't allowed because it is reserved as a path separator.
	 */
	private static final String BINARY_OPERATOR_CHARACTERS = "!#%&(*+,-./:;<=>?@[^{|~";

	private static final String OPERATOR_GE_128_PATTERN = "[\\u0080-\\uFFFF&&[\\p{P}\\p{S}]&&" + Identifier.NEGATED_CHARACTER_CLASS + "]";
	private static final String REMAINING_OPERATOR_CHARACTERS = ")]}" + BINARY_OPERATOR_CHARACTERS.replaceAll("[" + Pattern.quote(UNARY_OPERATOR_CHARACTERS) + "]+", "");

	private static final Pattern UNARY_OPERATOR_CHARACTER_PATTERN =  Pattern.compile(OPERATOR_GE_128_PATTERN + // Allow a single character >= 128...
			"|[" + Pattern.quote(UNARY_OPERATOR_CHARACTERS) + "][" + Pattern.quote(REMAINING_OPERATOR_CHARACTERS) + "]*"); // ...or allow additional non-unary operator characters after initial unary operator character
	private static final Pattern BINARY_OPERATOR_CHARACTER_PATTERN = Pattern.compile(OPERATOR_GE_128_PATTERN + // Allow a single character >= 128...
			"|[" + Pattern.quote(BINARY_OPERATOR_CHARACTERS) + "][" + Pattern.quote(REMAINING_OPERATOR_CHARACTERS) + "]*"); // ...or allow additional non-unary operator characters after initial binary operator character

	// Operator Properties
	public static final int LEFT_EXPRESSION     = 0x00000001; // Has an expression on the left
	public static final int RIGHT_EXPRESSION    = 0x00000002; // Has an expression on the right
	public static final int X_RIGHT_EXPRESSIONS = 0x00000004; // Has 0 or more comma-separated expressions on the right

	public static final int METHOD_CALL         = 0x00000010; // Is a method call (starts with '.', ends with '(')
	public static final int KNOWN_OBJECT        = 0x00000020; // Has an associated known object
	public static final int RIGHT_ASSOCIATIVITY = 0x00000040; // Is evaluated right to left
	public static final int ALLOW_PAIRS         = 0x00000080; // Can contain pairs
	public static final int NAVIGATION          = 0x00000100; // Is a navigation operator
	public static final int IGNORE_FAILURES     = 0x00000200; // Failures should be ignored
	public static final int SAFE                = 0x00000400; // Is a safe operator
	public static final int IGNORE_TRAILING     = 0x00000800; // Trailing operators should be ignored
	public static final int ASSIGNMENT          = 0x00001000; // Is an assignment operator
	public static final int TRAILING_IDENTIFIER = 0x00002000; // Requires a trailing identifier

	private static final List<Operator> OPERATORS;
	private static final Map<String, Operator> OPERATOR_LOOKUP = new LinkedHashMap<>();

	private final String string;
	private final int precedence; // 0 is the highest
	private final int properties;
	private final String description;
	private final String closingString;
	private final int localBindingIndex;
	private final int rightExpressions;
	private Operator next = null;

	static {
		final List<Operator> operators = new ArrayList<>();

		operators.add(new Operator("{",      0,  X_RIGHT_EXPRESSIONS | ALLOW_PAIRS, "Set / Map Literal", "}", 0, 0));
		operators.add(new Operator("[",      0,  X_RIGHT_EXPRESSIONS | ALLOW_PAIRS, "List / Map Literal", "]", 0, 0));
		operators.add(new Operator("[:]",    0,  0, "Empty Map"));
		operators.add(new Operator("[",      0,  LEFT_EXPRESSION | RIGHT_EXPRESSION | ALLOW_PAIRS, "Lookup", "]", 0, 1));
		operators.add(new Operator("?[",     0,  LEFT_EXPRESSION | RIGHT_EXPRESSION | ALLOW_PAIRS | SAFE, "Safe Lookup", "]", 0, 1));
		operators.add(new Operator("[?",     0,  LEFT_EXPRESSION | RIGHT_EXPRESSION | ALLOW_PAIRS | IGNORE_FAILURES, "Nullable Lookup", "]", 0, 1));
		operators.add(createMethod("(", true));
		operators.add(new Operator("(",      0,  RIGHT_EXPRESSION, "Parentheses", ")", 0, 1));
		operators.add(new Operator("~@",     0,  RIGHT_EXPRESSION, "Get Class"));
		operators.add(new Operator(".",      0,  LEFT_EXPRESSION | RIGHT_EXPRESSION | NAVIGATION, "Navigate"));
		operators.add(new Operator("?.",     0,  LEFT_EXPRESSION | RIGHT_EXPRESSION | NAVIGATION | SAFE, "Safe Navigate"));
		operators.add(new Operator(".?",     0,  LEFT_EXPRESSION | RIGHT_EXPRESSION | NAVIGATION | IGNORE_FAILURES, "Nullable Navigate"));
		operators.add(new Operator("**",     1,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Exponentiate"));
		operators.add(new Operator("+",      2,  RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Unary Plus"));
		operators.add(new Operator("-",      2,  RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Unary Minus"));
		operators.add(new Operator("~",      2,  RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Bitwise Negate"));
		operators.add(new Operator("!",      2,  RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Logical Negate"));
		operators.add(new Operator("..",     3,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Integer Range"));
		operators.add(new Operator("..<",    3,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Exclusive Integer Range"));
		operators.add(new Operator("*",      4,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Multiply"));
		operators.add(new Operator("/",      4,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Divide"));
		operators.add(new Operator("%",      4,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Modulus"));
		operators.add(new Operator("+",      5,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Add"));
		operators.add(new Operator("-",      5,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Subtract"));
		operators.add(new Operator("<<",     6,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Bitwise Shift Left"));
		operators.add(new Operator(">>",     6,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Bitwise Shift Right Sign Extend"));
		operators.add(new Operator(">>>",    6,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Bitwise Shift Right Zero Extend"));
		operators.add(new Operator("<=>",    7,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Three-way Comparison"));
		operators.add(new Operator("<=",     8,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Less Than or Equal"));
		operators.add(new Operator(">=",     8,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Greater Than or Equal"));
		operators.add(new Operator("<",      8,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Less Than"));
		operators.add(new Operator(">",      8,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Greater Than"));
		operators.add(new Operator("in",     8,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Is In"));
		operators.add(new Operator("==",     9,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Equal"));
		operators.add(new Operator("!=",     9,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Not Equal"));
		operators.add(new Operator("=~",     9,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Find Pattern"));
		operators.add(new Operator("==~",    9,  LEFT_EXPRESSION | RIGHT_EXPRESSION, "Match Pattern"));
		operators.add(new Operator("&",      10, LEFT_EXPRESSION | RIGHT_EXPRESSION, "Bitwise And"));
		operators.add(new Operator("^",      11, LEFT_EXPRESSION | RIGHT_EXPRESSION, "Bitwise Xor"));
		operators.add(new Operator("|",      12, LEFT_EXPRESSION | RIGHT_EXPRESSION, "Bitwise Or"));
		operators.add(new Operator("&&",     13, LEFT_EXPRESSION | RIGHT_EXPRESSION, "Logical And"));
		operators.add(new Operator("||",     14, LEFT_EXPRESSION | RIGHT_EXPRESSION, "Logical Or"));
		operators.add(new Operator("?:",     15, LEFT_EXPRESSION | RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Elvis"));
		operators.add(new Operator("??",     15, LEFT_EXPRESSION | RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Null Coalescing"));
		operators.add(new Operator("!:",     15, LEFT_EXPRESSION | RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Inverted Elvis"));
		operators.add(new Operator("!?",     15, LEFT_EXPRESSION | RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Non-null Coalescing"));
		operators.add(new Operator("?",      15, LEFT_EXPRESSION | RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Ternary"));
		operators.add(new Operator(":",      15, LEFT_EXPRESSION | RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Pair / Range"));
		operators.add(new Operator(":<",     15, LEFT_EXPRESSION | RIGHT_ASSOCIATIVITY, "Backward Range"));
		operators.add(new Operator("\u2620", 16, RIGHT_EXPRESSION, "Die")); // Skull and crossbones
		operators.add(new Operator("~:<",    16, RIGHT_EXPRESSION, "Die - Alternate"));
		operators.add(new Operator("#^",     16, RIGHT_EXPRESSION, "Return"));
		operators.add(new Operator("#>",     17, LEFT_EXPRESSION | RIGHT_EXPRESSION | TRAILING_IDENTIFIER, "Streaming Remap"));
		operators.add(new Operator("#.",     17, LEFT_EXPRESSION | RIGHT_EXPRESSION | TRAILING_IDENTIFIER, "Streaming Remap - Alternate"));
		operators.add(new Operator("#|",     17, LEFT_EXPRESSION | RIGHT_EXPRESSION | TRAILING_IDENTIFIER, "Streaming Flatten Remap"));
		operators.add(new Operator("#?",     17, LEFT_EXPRESSION | RIGHT_EXPRESSION | TRAILING_IDENTIFIER, "Streaming Filter"));
		operators.add(new Operator("#<",     17, LEFT_EXPRESSION | RIGHT_EXPRESSION | TRAILING_IDENTIFIER, "Streaming Reduction"));
		operators.add(new Operator("=",      18, LEFT_EXPRESSION | RIGHT_EXPRESSION | RIGHT_ASSOCIATIVITY | ASSIGNMENT, "Bind Local Name"));
		operators.add(new Operator(",",      19, LEFT_EXPRESSION | X_RIGHT_EXPRESSIONS | ALLOW_PAIRS | IGNORE_TRAILING, "Item Separator"));
		operators.add(new Operator(";",      20, LEFT_EXPRESSION | RIGHT_EXPRESSION | IGNORE_TRAILING, "Statement Separator"));

		// These operators have known assertion failures and may contain ambiguities with other operators or Horseshoe features. These ambiguities have been thoroughly analyzed and deemed acceptable.
		final Set<String> ignoreFailuresIn = new HashSet<>(Arrays.asList("?[" /* ambiguous with "?" and "[" operators; allowed, spaces must be used to disambiguate safe array lookup and ternary with list literal (e.g. "a ? [1..4] : null") */,
				"?." /* ambiguous with "?" and "." operators; allowed, spaces must be used to disambiguate safe navigation and ternary with internal variable (e.g. "a ? .isFirst : d") */,
				".." /* ambiguous with "." unary operator; allowed, because the "\" separator must be used when applying "." to the current object (".") */,
				"..<" /* see line above */,
				"in" /* doesn't use valid characters */,
				"=~" /* ambiguous with "~" operator if "=" were an operator; allowed, spaces must be used to disambiguate (e.g. "a = ~1") */,
				"==~" /* ambiguous with "==" and "~" operators; allowed, spaces must be used to disambiguate (e.g. "0 == ~1") */,
				"#^" /* ambiguous with section tag; allowed, because the operator would never be used at start of content tag */,
				"#." /* ambiguous with "." unary operator; allowed, because there is no binary operator ending with "#" */));

		for (final Operator operator : operators) {
			final String string = operator.string;

			if (!ignoreFailuresIn.contains(string)) { // Sanity and ambiguity checks for the operators
				assert string != null && !string.isEmpty() : "Operators cannot consist of a null or empty string";

				if (operator.has(LEFT_EXPRESSION)) { // All binary operators must start with a binary operator character
					assert BINARY_OPERATOR_CHARACTER_PATTERN.matcher(string).matches() : "Invalid binary operator \"" + string + "\", must match the following pattern: \"" + BINARY_OPERATOR_CHARACTER_PATTERN + "\"";
				} else { // All unary operators must start with a unary operator character
					assert UNARY_OPERATOR_CHARACTER_PATTERN.matcher(string).matches() : "Invalid unary operator \"" + string + "\", must match the following pattern: \"" + UNARY_OPERATOR_CHARACTER_PATTERN + "\"";
				}
			}

			operator.next = OPERATOR_LOOKUP.put(string, operator);
		}

		OPERATORS = Collections.unmodifiableList(operators);
	}

	/**
	 * Gets the operator for the specified method name.
	 *
	 * @param name the name of the method
	 * @param hasObject true if the method has an identified object, false if the object cannot be determined based on the context
	 * @return the operator for the specified method name
	 */
	public static Operator createMethod(final String name, final boolean hasObject) {
		return new Operator(name, 0, METHOD_CALL | X_RIGHT_EXPRESSIONS | (hasObject ? KNOWN_OBJECT : 0), "Call Method", ")", 0, 0);
	}

	/**
	 * Gets the operator of the specified string representation left expression property.
	 *
	 * @param operator the string representation of the operator
	 * @param hasLeftExpression true if the operator has a left expression, otherwise false
	 * @return the matching operator if it exists, otherwise null
	 */
	public static Operator get(final String operator, final boolean hasLeftExpression) {
		Operator possibleOperator = OPERATOR_LOOKUP.get(operator);

		while (possibleOperator != null && (possibleOperator.properties & LEFT_EXPRESSION) == (hasLeftExpression ? 0 : LEFT_EXPRESSION)) {
			possibleOperator = possibleOperator.next;
		}

		return possibleOperator;
	}

	/**
	 * Gets a list of all supported operators.
	 *
	 * @return a list of all supported operators
	 */
	public static List<Operator> getAll() {
		return OPERATORS;
	}

	private Operator(final String string, final int precedence, final int properties, final String description, final String closingString, final int localBindingIndex, final int rightExpressions) {
		this.string = string;
		this.precedence = precedence;
		this.properties = properties;
		this.description = description;
		this.closingString = closingString;
		this.localBindingIndex = localBindingIndex;
		this.rightExpressions = rightExpressions;
	}

	private Operator(final String string, final int precedence, final int properties, final String description) {
		this(string, precedence, properties, description, null, 0, (properties & RIGHT_EXPRESSION) != 0 ? 1 : 0);
	}

	/**
	 * Adds a right expression to the operator.
	 *
	 * @return the new operator
	 */
	public Operator addRightExpression() {
		return withRightExpressions(rightExpressions + 1);
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
	 * Gets the local binding index associated with the operator.
	 *
	 * @return the local binding index associated with the operator
	 */
	public int getLocalBindingIndex() {
		return localBindingIndex;
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
	 * @param property the property or set of properties to test for
	 * @return true if the operator has the given property or at least one of a set of properties, otherwise false
	 */
	public boolean has(final int property) {
		return (properties & property) != 0;
	}

	/**
	 * Gets if the operator is a container operator.
	 *
	 * @return true if the operator is a container operator, otherwise false
	 */
	public boolean isContainer() {
		return has(X_RIGHT_EXPRESSIONS) || closingString != null;
	}

	/**
	 * Gets if the operator is left associative.
	 *
	 * @return true if the operator is left associative, otherwise false
	 */
	public boolean isLeftAssociative() {
		return !has(RIGHT_ASSOCIATIVITY);
	}

	@Override
	public String toString() {
		if (has(X_RIGHT_EXPRESSIONS)) {
			return string + ": " + rightExpressions;
		}

		return string;
	}

	/**
	 * Gets the operator with the specified localBindingIndex.
	 *
	 * @param localBindingIndex the local binding index
	 * @return the operator for the specified identifier
	 */
	public Operator withLocalBindingIndex(final int localBindingIndex) {
		if (!has(TRAILING_IDENTIFIER)) {
			throw new UnsupportedOperationException();
		}

		return new Operator(string, precedence, properties, description, closingString, localBindingIndex, 0);
	}

	/**
	 * Sets the number of right expressions for the operator.
	 *
	 * @param expressions the number of right expressions
	 * @return the new operator
	 */
	public Operator withRightExpressions(final int expressions) {
		if (!has(X_RIGHT_EXPRESSIONS)) {
			throw new UnsupportedOperationException();
		}

		return new Operator(string, precedence, properties, description, closingString, 0, expressions);
	}

}
