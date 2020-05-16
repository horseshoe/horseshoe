package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.MethodBuilder.Label;

public final class Expression {

	private static final AtomicInteger DYN_INDEX = new AtomicInteger();

	// Reflected Methods
	private static final Constructor<?> ARRAY_LIST_CTOR_INT;
	private static final Method ACCESSOR_LOOKUP;
	private static final Method EXPRESSION_EVALUATE;
	private static final Method EXPRESSION_GET_VALUE;
	private static final Constructor<?> HALT_EXCEPTION_CTOR_STRING;
	private static final Method IDENTIFIER_FIND_VALUE;
	private static final Method IDENTIFIER_FIND_VALUE_METHOD;
	private static final Method IDENTIFIER_GET_ROOT_VALUE;
	private static final Method IDENTIFIER_GET_VALUE;
	private static final Method IDENTIFIER_GET_VALUE_METHOD;
	private static final Method INDEXED_GET_INDEX;
	private static final Method INDEXED_HAS_NEXT;
	private static final Constructor<?> LINKED_HASH_MAP_CTOR_INT;
	private static final Constructor<?> LINKED_HASH_SET_CTOR_INT;
	private static final Method LIST_ADD;
	private static final Method MAP_PUT;
	private static final Method OBJECT_TO_STRING;
	private static final Method PATTERN_COMPILE;
	private static final Method PERSISTENT_STACK_PEEK;
	private static final Method PERSISTENT_STACK_PEEK_BASE;
	private static final Method PERSISTENT_STACK_POP;
	private static final Method PERSISTENT_STACK_PUSH_OBJECT;
	private static final Method RENDER_CONTEXT_GET_INDEXED_DATA;
	private static final Method RENDER_CONTEXT_GET_SECTION_DATA;
	private static final Method SET_ADD;
	private static final Method STRING_BUILDER_APPEND_OBJECT;
	private static final Constructor<?> STRING_BUILDER_INIT_STRING;
	private static final Method STRING_VALUE_OF;

	// The patterns used for parsing the grammar
	private static final Pattern COMMENTS_PATTERN = Pattern.compile("(?:(?s://[^\\n\\x0B\\x0C\\r\\u0085\\u2028\\u2029]*|/[*].*?[*]/)\\s*)+", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(?<identifier>(?:" + Identifier.PATTERN + "|`(?:[^`\\\\]|\\\\[`\\\\])+`|[.][.]|[.])[(]?)\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_WITH_PREFIX_PATTERN;
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("(?<double>[-+]Infinity|[-+]?(?:[0-9]+[fFdD]|(?:[0-9]+[.]?[eE][-+]?[0-9]+|[0-9]+[.][0-9]+(?:[eE][-+]?[0-9]+)?|0[xX](?:[0-9A-Fa-f]+[.]?|[0-9A-Fa-f]+[.][0-9A-Fa-f]+)[pP][-+]?[0-9]+)[fFdD]?))\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern LONG_PATTERN = Pattern.compile("(?:(?<hexsign>[-+]?)0[xX](?<hexadecimal>[0-9A-Fa-f]+)|(?<decimal>[-+]?[0-9]+))(?<isLong>[lL])?\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern REGEX_PATTERN = Pattern.compile("~/(?<regex>(?:[^/\\\\]|\\\\.)*)/\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern STRING_PATTERN = Pattern.compile("(?:\"(?<string>(?:[^\"\\\\]|\\\\[\\\\\"'btnfr{}]|\\\\0|\\\\x[0-9A-Fa-f]|\\\\u[0-9A-Fa-f]{4}|\\\\U[0-9A-Fa-f]{8})*)\"|'(?<unescapedString>[^']*)')\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern OPERATOR_PATTERN;

	private static final byte[] CHAR_VALUE = new byte[] {
			127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
			127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,   0,   1,   2,   3,   4,   5,   6,   7,   8,   9, 127, 127, 127, 127, 127, 127,
			127,  10,  11,  12,  13,  14,  15,  16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,  32,  33,  34,  35, 127, 127, 127, 127, 127,
			127,  10,  11,  12,  13,  14,  15,  16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,  32,  33,  34,  35, 127, 127, 127, 127, 127
	};

	private final String location;
	private final String originalString;
	private final Expression[] expressions;
	private final Identifier[] identifiers;
	private final Evaluable evaluable;

	public static interface Indexed {
		/**
		 * Gets the index of the current indexed object.
		 *
		 * @return The index of the current indexed object
		 */
		public int getIndex();

		/**
		 * Gets whether or not another item exists after the current item.
		 *
		 * @return true if another item exists after the current item, otherwise false
		 */
		public boolean hasNext();
	}

	public abstract static class Evaluable {
		private static final byte LOAD_EXPRESSIONS = ALOAD_1;
		private static final byte LOAD_IDENTIFIERS = ALOAD_2;
		private static final byte LOAD_CONTEXT = ALOAD_3;
		private static final int FIRST_LOCAL_INDEX = 4;

		/**
		 * Evaluates the object using the specified parameters.
		 *
		 * @param expressions the expressions used to evaluate the object
		 * @param identifiers the identifiers used to evaluate the object
		 * @param context the context used to evaluate the object
		 * @return the result of evaluating the object
		 * @throws Exception if an error occurs while evaluating the expression
		 */
		public abstract Object evaluate(final Expression[] expressions, final Identifier[] identifiers, final RenderContext context) throws Exception;
	}

	static {
		try {
			ARRAY_LIST_CTOR_INT = ArrayList.class.getConstructor(int.class);
			ACCESSOR_LOOKUP = Accessor.class.getMethod("lookup", Object.class, Object.class);
			EXPRESSION_EVALUATE = Expression.class.getMethod("evaluate", RenderContext.class);
			EXPRESSION_GET_VALUE = Expression.class.getMethod("getClass", RenderContext.class, String.class);
			HALT_EXCEPTION_CTOR_STRING = HaltRenderingException.class.getConstructor(String.class);
			IDENTIFIER_FIND_VALUE = Identifier.class.getMethod("findValue", RenderContext.class, int.class);
			IDENTIFIER_FIND_VALUE_METHOD = Identifier.class.getMethod("findValue", RenderContext.class, int.class, Object[].class);
			IDENTIFIER_GET_ROOT_VALUE = Identifier.class.getMethod("getRootValue", RenderContext.class);
			IDENTIFIER_GET_VALUE = Identifier.class.getMethod("getValue", Object.class);
			IDENTIFIER_GET_VALUE_METHOD = Identifier.class.getMethod("getValue", Object.class, Object[].class);
			INDEXED_GET_INDEX = Indexed.class.getMethod("getIndex");
			INDEXED_HAS_NEXT = Indexed.class.getMethod("hasNext");
			LINKED_HASH_MAP_CTOR_INT = LinkedHashMap.class.getConstructor(int.class);
			LINKED_HASH_SET_CTOR_INT = LinkedHashSet.class.getConstructor(int.class);
			LIST_ADD = List.class.getMethod("add", Object.class);
			MAP_PUT = Map.class.getMethod("put", Object.class, Object.class);
			OBJECT_TO_STRING = Object.class.getMethod("toString");
			PATTERN_COMPILE = Pattern.class.getMethod("compile", String.class);
			PERSISTENT_STACK_PEEK = PersistentStack.class.getMethod("peek", int.class);
			PERSISTENT_STACK_PEEK_BASE = PersistentStack.class.getMethod("peekBase");
			PERSISTENT_STACK_POP = PersistentStack.class.getMethod("pop");
			PERSISTENT_STACK_PUSH_OBJECT = PersistentStack.class.getMethod("push", Object.class);
			RENDER_CONTEXT_GET_INDEXED_DATA = RenderContext.class.getMethod("getIndexedData");
			RENDER_CONTEXT_GET_SECTION_DATA = RenderContext.class.getMethod("getSectionData");
			SET_ADD = Set.class.getMethod("add", Object.class);
			STRING_BUILDER_APPEND_OBJECT = StringBuilder.class.getMethod("append", Object.class);
			STRING_BUILDER_INIT_STRING = StringBuilder.class.getConstructor(String.class);
			STRING_VALUE_OF = String.class.getMethod("valueOf", Object.class);
		} catch (final ReflectiveOperationException e) {
			throw new ExceptionInInitializerError("Failed to get required class member: " + e.getMessage());
		}
	}

	static {
		final StringBuilder allOperators = new StringBuilder();
		final StringBuilder assignmentOperators = new StringBuilder();
		final Set<Operator> patterns = new TreeSet<>(new Comparator<Operator>() {
			@Override
			public int compare(final Operator o1, final Operator o2) {
				final int lengthDiff = o2.getString().length() - o1.getString().length();
				return lengthDiff == 0 ? o1.getString().compareTo(o2.getString()) : lengthDiff;
			}
		});

		// Get each operator, ordered by length
		for (final Operator operator : Operator.getAll()) {
			patterns.add(operator);

			if (operator.getClosingString() != null) {
				patterns.add(operator.getClosingOperator());
			}
		}

		// Create the patterns
		for (final Operator pattern : patterns) {
			allOperators.append(Pattern.quote(pattern.getString())).append('|');

			if (pattern.has(Operator.ASSIGNMENT)) {
				assignmentOperators.append('|').append(Pattern.quote(pattern.getString()));
			}
		}

		// Add comma as a separator
		IDENTIFIER_WITH_PREFIX_PATTERN = Pattern.compile("(?:(?<prefix>[&/\\\\])|(?<backreach>(?:[.][.][/\\\\])+)|(?<current>[.][/\\\\])?)(?<internal>[.](?![.]))?" + IDENTIFIER_PATTERN + "(?=(?:" + COMMENTS_PATTERN + ")?(?<assignment>" + assignmentOperators.substring(1) + ")(?:[^=]|$))?", Pattern.UNICODE_CHARACTER_CLASS);
		OPERATOR_PATTERN = Pattern.compile("(?<operator>" + allOperators.append(",)\\s*").toString(), Pattern.UNICODE_CHARACTER_CLASS);
	}

	/**
	 * Compares two objects.
	 *
	 * @param equality true to test for equality, false to perform 3-way comparison
	 * @param first the object to compare
	 * @param second the object being compared
	 * @return the result of comparing the two values where a negative value indicates the first object is less than the second, a positive value indicates the first object is greater than the second, and a value of zero indicates the objects are equivalent
	 */
	@SuppressWarnings("unchecked")
	public static int compare(final boolean equality, final Object first, final Object second) {
		if (first instanceof Number) {
			if (first instanceof Double || first instanceof Float) {
				if (second instanceof Number) {
					if (second instanceof Double || second instanceof Float) {
						return Double.compare(((Number)first).doubleValue(), ((Number)second).doubleValue());
					} else if (second instanceof BigDecimal) {
						return BigDecimal.valueOf(((Number)first).doubleValue()).compareTo((BigDecimal)second);
					} else if (second instanceof BigInteger) {
						return BigDecimal.valueOf(((Number)first).doubleValue()).compareTo(new BigDecimal((BigInteger)second));
					} else {
						return BigDecimal.valueOf(((Number)first).doubleValue()).compareTo(BigDecimal.valueOf(((Number)second).longValue()));
					}
				} else if (second instanceof Character) {
					return Double.compare(((Number)first).doubleValue(), ((Character)second).charValue());
				}
			} else if (first instanceof BigDecimal) {
				if (second instanceof Number) {
					if (second instanceof Double || second instanceof Float) {
						return ((BigDecimal)first).compareTo(BigDecimal.valueOf(((Number)second).doubleValue()));
					} else if (second instanceof BigDecimal) {
						return ((BigDecimal)first).compareTo((BigDecimal)second);
					} else if (second instanceof BigInteger) {
						return ((BigDecimal)first).compareTo(new BigDecimal((BigInteger)second));
					} else {
						return ((BigDecimal)first).compareTo(BigDecimal.valueOf(((Number)second).longValue()));
					}
				} else if (second instanceof Character) {
					return ((BigDecimal)first).compareTo(BigDecimal.valueOf(((Character)second).charValue()));
				}
			} else if (first instanceof BigInteger) {
				if (second instanceof Number) {
					if (second instanceof Double || second instanceof Float) {
						return new BigDecimal((BigInteger)first).compareTo(BigDecimal.valueOf(((Number)second).doubleValue()));
					} else if (second instanceof BigDecimal) {
						return new BigDecimal((BigInteger)first).compareTo((BigDecimal)second);
					} else if (second instanceof BigInteger) {
						return ((BigInteger)first).compareTo((BigInteger)second);
					} else {
						return ((BigInteger)first).compareTo(BigInteger.valueOf(((Number)second).longValue()));
					}
				} else if (second instanceof Character) {
					return ((BigInteger)first).compareTo(BigInteger.valueOf(((Character)second).charValue()));
				}
			} else {
				if (second instanceof Number) {
					if (second instanceof Double || second instanceof Float) {
						return BigDecimal.valueOf(((Number)first).longValue()).compareTo(BigDecimal.valueOf(((Number)second).doubleValue()));
					} else if (second instanceof BigDecimal) {
						return BigDecimal.valueOf(((Number)first).longValue()).compareTo((BigDecimal)second);
					} else if (second instanceof BigInteger) {
						return BigInteger.valueOf(((Number)first).longValue()).compareTo((BigInteger)second);
					} else {
						return Long.compare(((Number)first).longValue(), ((Number)second).longValue());
					}
				} else if (second instanceof Character) {
					return Long.compare(((Number)first).longValue(), ((Character)second).charValue());
				}
			}
		} else if (first instanceof Character) {
			if (second instanceof Number) {
				if (second instanceof Double || second instanceof Float) {
					return BigDecimal.valueOf(((Character)first).charValue()).compareTo(BigDecimal.valueOf(((Number)second).doubleValue()));
				} else if (second instanceof BigDecimal) {
					return BigDecimal.valueOf(((Character)first).charValue()).compareTo((BigDecimal)second);
				} else if (second instanceof BigInteger) {
					return BigInteger.valueOf(((Character)first).charValue()).compareTo((BigInteger)second);
				}

				return Long.compare(((Character)first).charValue(), ((Number)second).longValue());
			} else if (second instanceof StringBuilder || second instanceof String || second instanceof Character) {
				return first.toString().compareTo(second.toString());
			}
		} else if ((first instanceof StringBuilder || first instanceof String) &&
				(second instanceof StringBuilder || second instanceof String || second instanceof Character)) {
			return first.toString().compareTo(second.toString());
		} else if (equality) {
			return Objects.equals(first, second) ? 0 : 1;
		} else if (first instanceof Comparable) {
			return ((Comparable<Object>)first).compareTo(second);
		}

		if (equality) {
			return 1; // Indicate not equal
		}

		throw new IllegalArgumentException("Unexpected object, expecting comparable object");
	}

	/**
	 * Converts an object to a boolean.
	 *
	 * @param object the object to convert to a boolean
	 * @return the result of converting the object to a boolean
	 */
	public static boolean convertToBoolean(final Object object) {
		if (object instanceof Number) {
			if (object instanceof Double || object instanceof Float) {
				return ((Number)object).doubleValue() != 0.0;
			} else if (object instanceof BigDecimal) {
				return BigDecimal.ZERO.compareTo((BigDecimal)object) != 0;
			} else if (object instanceof BigInteger) {
				return !BigInteger.ZERO.equals(object);
			}

			return ((Number)object).longValue() != 0;
		} else if (object instanceof Boolean) {
			return ((Boolean)object).booleanValue();
		} else if (object instanceof Character) {
			return ((Character)object).charValue() != 0;
		}

		return object != null;
	}

	/**
	 * Gets the class of the specified name.
	 *
	 * @param context the context used to get the class
	 * @param name the name of the class to get
	 * @return the class associated with the specified name
	 * @throws ClassNotFoundException if the class could not be found
	 */
	public static Class<?> getClass(final RenderContext context, final String name) throws ClassNotFoundException {
		return context.getClass(name);
	}

	/**
	 * Gets the index for the specified identifier. If the identifier does not exist in the map, a new entry will be created and that index will be returned.
	 *
	 * @param identifiers the map of identifiers to indices
	 * @param identifier the identifier to locate
	 * @return the index of the specified identifier
	 */
	private static int getIdentifierIndex(final Map<Identifier, Integer> identifiers, final Identifier identifier) {
		Integer index = identifiers.get(identifier);

		if (index == null) {
			index = identifiers.size();
			identifiers.put(identifier, index);
		}

		return index;
	}

	/**
	 * Parse a string literal, substituting character escape sequences as necessary.
	 *
	 * @param matcher the matcher with the current string literal matched
	 * @return the parsed string representation of the string literal
	 */
	private static String parseStringLiteral(final Matcher matcher) {
		final String string = matcher.group("string");
		int backslash = 0;

		if (string == null || (backslash = string.indexOf('\\')) < 0) {
			return string == null ? matcher.group("unescapedString") : string;
		}

		// Find escape sequences and replace them with the proper character
		final StringBuilder sb = new StringBuilder(string.length());
		int start = 0;

		do {
			sb.append(string, start, backslash);
			assert backslash + 1 < string.length() : "Invalid string literal accepted with trailing \"\\\""; // According to the pattern, a backslash must be followed by a character
			start = backslash + 2;

			switch (string.charAt(backslash + 1)) {
				case 'b':
					sb.append('\b');
					break;
				case 't':
					sb.append('\t');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'f':
					sb.append('\f');
					break;
				case 'r':
					sb.append('\r');
					break;
				case '0': // Only allow zero, not the octal escape of C, C++, Java, etc.
					sb.appendCodePoint(0);
					break;
				case 'x':
					start = parseStringLiteralHex(sb, string, start);
					break;
				case 'u':
					assert backslash + 5 < string.length() : "Invalid string literal accepted with trailing \"\\u\""; // According to the pattern, \\u must be followed by 4 digits
					sb.appendCodePoint(Integer.parseInt(string.substring(backslash + 2, backslash + 6), 16));
					start = backslash + 6;
					break;
				case 'U':
					assert backslash + 9 < string.length() : "Invalid string literal accepted with trailing \"\\U\""; // According to the pattern, \\U must be followed by 8 digits
					sb.appendCodePoint(Integer.parseInt(string.substring(backslash + 2, backslash + 10), 16));
					start = backslash + 10;
					break;
				default:
					sb.append(string.charAt(backslash + 1));
					break;
			}
		} while ((backslash = string.indexOf('\\', start)) >= 0);

		return sb.append(string, start, string.length()).toString();
	}

	/**
	 * Parse a string literal hex escape of arbitrary length, and append the code point using a string builder.
	 *
	 * @param sb the string builder used to build the string
	 * @param string the string literal being parsed
	 * @param index the index of the first hex character in the string literal
	 * @return the index after the hex escape
	 */
	private static int parseStringLiteralHex(final StringBuilder sb, final String string, final int index) {
		int codePoint = 0;
		int i = index;

		for (; i < string.length() && string.charAt(i) < CHAR_VALUE.length; i++) {
			final int value = CHAR_VALUE[string.charAt(i)];

			if ((value & 0xF0) != 0) {
				break;
			}

			codePoint = codePoint * 16 + value;
		}

		sb.appendCodePoint(codePoint);
		return i;
	}

	/**
	 * Processes the specified operation. The operand stack will be updated with the results of evaluating the operator.
	 *
	 * @param namedExpressions the map used to lookup named expressions
	 * @param expressions the map used to lookup the index of an expression
	 * @param identifiers the map used to lookup the index of an identifier
	 * @param operands the operand stack for the expression
	 * @param operators the operator stack to evaluate
	 */
	private static void processOperation(final Map<String, Expression> namedExpressions, final Map<Expression, Integer> expressions, final HashMap<Identifier, Integer> identifiers, final PersistentStack<Operand> operands, final PersistentStack<Operator> operators) {
		final Operator operator = operators.pop();
		final Operand right;

		if (operator.has(Operator.NAVIGATION)) { // Navigation operator is handled during parsing
			return;
		} else if (operator.has(Operator.METHOD_CALL)) { // Check for a method call
			final int parameterCount = operator.getRightExpressions();

			if (!operator.has(Operator.KNOWN_OBJECT) && parameterCount <= 1) {
				final Expression namedExpression = namedExpressions.get(operator.getString());

				// Check for a named expression
				if (namedExpression != null) {
					final MethodBuilder expressionResult = new MethodBuilder();
					Integer index = expressions.get(namedExpression);

					if (index == null) {
						index = expressions.size();
						expressions.put(namedExpression, index);
					}

					// Load the context and evaluate the expression
					if (parameterCount != 0) {
						expressionResult.addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).append(operands.peek().toObject(false)).addInvoke(PERSISTENT_STACK_PUSH_OBJECT).addCode(POP);
					}

					expressionResult.addCode(Evaluable.LOAD_EXPRESSIONS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).addInvoke(EXPRESSION_EVALUATE);

					if (parameterCount != 0) {
						expressionResult.addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(PERSISTENT_STACK_POP).addCode(POP);
					}

					operands.pop(parameterCount + 3).push(new Operand(Object.class, expressionResult));
					return;
				}
			}

			// Create the identifier, then get and invoke the appropriate method
			final int index = getIdentifierIndex(identifiers, new Identifier(operator.getString(), parameterCount));
			final MethodBuilder methodResult = operands.peek(parameterCount).builder.addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).append(operands.peek(parameterCount + 1).builder);

			if (parameterCount == 0) {
				methodResult.addCode(ACONST_NULL);
			} else {
				methodResult.pushNewObject(Object.class, parameterCount);

				// Convert all parameters to objects and store them in the array
				for (int i = 0; i < parameterCount; i++) {
					methodResult.addCode(DUP).pushConstant(i).append(operands.peek(parameterCount - 1 - i).toObject(false)).addCode(AASTORE);
				}
			}

			operands.push(new Operand(Object.class, methodResult.append(operands.pop(parameterCount + 2).pop().builder)));
			return;
		} else if (operator.has(Operator.RIGHT_EXPRESSION)) {
			right = operands.pop();
			assert !Entry.class.equals(right.type) || operator.has(Operator.ALLOW_PAIRS) : Entry.class + " cannot be passed to operator";
		} else {
			right = null;
		}

		final Operand left = operator.has(Operator.LEFT_EXPRESSION) ? operands.pop() : null;

		switch (operator.getString()) {
			// Array / Map Operations
			case "[":
			case "?[?":
				if (left != null) {
					if (!Object.class.equals(left.type)) {
						throw new IllegalArgumentException("Unexpected \"" + operator.getString() + "\" operator applied to " + (left.type == null ? "numeric" : left.type.getName()) + " value, expecting map or array type value");
					}

					final Label end = left.builder.newLabel();

					if (operator.has(Operator.SAFE)) {
						left.builder.addCode(DUP).addBranch(IFNULL, end);
					}

					operands.push(new Operand(Object.class, left.builder.append(right.toObject(false)).addInvoke(ACCESSOR_LOOKUP).updateLabel(end)));
					break;
				} // Intentional fall-through if left is null
			case "{": {
				int pairs = 0;

				// Find the number of pairs
				for (int i = 0; i < operator.getRightExpressions(); i++) {
					if (Entry.class.equals(operands.peek(i + pairs).type)) {
						pairs++;
						assert Entry.class.equals(operands.peek(i + pairs).type) : Entry.class + " must occur in pairs on the operand stack";
					}
				}

				if (pairs > 0) { // Create a map
					final int totalPairs = pairs;
					final MethodBuilder builder = new MethodBuilder().pushNewObject(LinkedHashMap.class).addCode(DUP).pushConstant((operator.getRightExpressions() * 4 + 2) / 3).addInvoke(LINKED_HASH_MAP_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						final Operand first = operands.peek(i + pairs);

						if (Entry.class.equals(first.type)) {
							builder.addCode(DUP).append(first.toObject(false)).append(operands.peek(i + --pairs).toObject(false)).addInvoke(MAP_PUT).addCode(POP);
						} else {
							builder.addCode(DUP).append(first.toObject(false)).addCode(DUP).addInvoke(MAP_PUT).addCode(POP);
						}
					}

					operands.pop(operator.getRightExpressions() + totalPairs).push(new Operand(Object.class, builder));
				} else if ("[".equals(operator.getString())) { // Create a list
					final MethodBuilder builder = new MethodBuilder().pushNewObject(ArrayList.class).addCode(DUP).pushConstant(operator.getRightExpressions()).addInvoke(ARRAY_LIST_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						builder.addCode(DUP).append(operands.peek(i).toObject(false)).addInvoke(LIST_ADD).addCode(POP);
					}

					operands.pop(operator.getRightExpressions()).push(new Operand(Object.class, builder));
				} else { // Create a set
					final MethodBuilder builder = new MethodBuilder().pushNewObject(LinkedHashSet.class).addCode(DUP).pushConstant((operator.getRightExpressions() * 4 + 2) / 3).addInvoke(LINKED_HASH_SET_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						builder.addCode(DUP).append(operands.peek(i).toObject(false)).addInvoke(SET_ADD).addCode(POP);
					}

					operands.pop(operator.getRightExpressions()).push(new Operand(Object.class, builder));
				}

				break;
			}
			case "[:]":
				operands.push(new Operand(Object.class, new MethodBuilder().pushNewObject(LinkedHashMap.class).addCode(DUP).pushConstant(8).addInvoke(LINKED_HASH_MAP_CTOR_INT)));
				break;
			case "..": {
				final Label decreasing = left.builder.newLabel();
				final Label increasingLoop = left.builder.newLabel();
				final Label decreasingLoop = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				operands.push(new Operand(Object.class, left.toNumeric(false).addCode(POP, POP2, L2I, DUP).addAccess(ISTORE, Evaluable.FIRST_LOCAL_INDEX).append(right.toNumeric(false)).addCode(POP, POP2, L2I, DUP2).addBranch(IF_ICMPGT, decreasing)
						.addCode(SWAP, ISUB, ICONST_1, IADD, NEWARRAY, (byte)10, DUP).addAccess(ASTORE, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(ICONST_0).updateLabel(increasingLoop).addCode(DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(SWAP, DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL_INDEX).addCode(IADD, IASTORE, ICONST_1, IADD, DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(ARRAYLENGTH).addBranch(IF_ICMPLT, increasingLoop).addBranch(GOTO, end)
						.updateLabel(decreasing).addCode(ISUB, ICONST_1, IADD, NEWARRAY, (byte)10, DUP).addAccess(ASTORE, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(ICONST_0).updateLabel(decreasingLoop).addCode(DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(SWAP, DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL_INDEX).addCode(SWAP, ISUB, IASTORE, ICONST_1, IADD, DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(ARRAYLENGTH).addBranch(IF_ICMPLT, decreasingLoop)
						.updateLabel(end).addCode(POP)));
				break;
			}

			// Math Operations
			case "+":
				if (left == null) { // Unary +, basically do nothing except require a number
					operands.push(new Operand(null, right.toNumeric(true)));
				} else if (StringBuilder.class.equals(left.type)) { // Check for string concatenation
					operands.push().peek().builder.append(right.toObject(false)).addInvoke(STRING_BUILDER_APPEND_OBJECT);
				} else if (String.class.equals(left.type) || String.class.equals(right.type) || StringBuilder.class.equals(right.type)) {
					operands.push(new Operand(StringBuilder.class, left.toObject(false).pushNewObject(StringBuilder.class).addCode(DUP_X1, SWAP).addInvoke(STRING_VALUE_OF).addInvoke(STRING_BUILDER_INIT_STRING).append(right.toObject(false)).addInvoke(STRING_BUILDER_APPEND_OBJECT)));
				} else if ((left.type == null || (left.type.isPrimitive() && !boolean.class.equals(left.type))) && (right.type == null || (right.type.isPrimitive() && !boolean.class.equals(right.type)))) { // Mathematical addition
					operands.push(left.execMathOp(right, IADD, LADD, DADD, Evaluable.FIRST_LOCAL_INDEX));
				} else { // String concatenation, mathematical addition, or invalid
					final Label notStringBuilder = left.builder.newLabel();
					final Label isString = left.builder.newLabel();
					final Label notString = left.builder.newLabel();
					final Label end = left.builder.newLabel();

					operands.push(new Operand(Object.class, left.toObject(false).append(right.toObject(false)).addCode(SWAP, DUP_X1).addInstanceOfCheck(StringBuilder.class).addBranch(IFEQ, notStringBuilder).addCode(SWAP).addCast(StringBuilder.class).addCode(SWAP).addInvoke(STRING_BUILDER_APPEND_OBJECT).addBranch(GOTO, end)
							.updateLabel(notStringBuilder).addCode(SWAP, DUP_X1).addInstanceOfCheck(String.class).addBranch(IFNE, isString).addCode(DUP).addInstanceOfCheck(String.class).addBranch(IFNE, isString).addCode(DUP).addInstanceOfCheck(StringBuilder.class).addBranch(IFEQ, notString)
							.updateLabel(isString).addCode(SWAP).pushNewObject(StringBuilder.class).addCode(DUP_X2, SWAP).addInvoke(STRING_VALUE_OF).addInvoke(STRING_BUILDER_INIT_STRING).addInvoke(STRING_BUILDER_APPEND_OBJECT).addBranch(GOTO, end)
							.updateLabel(notString).addAccess(ASTORE, Evaluable.FIRST_LOCAL_INDEX)))
						.peek().execMathOp(new Operand(Object.class, new MethodBuilder().addAccess(ALOAD, Evaluable.FIRST_LOCAL_INDEX)), IADD, LADD, DADD, Evaluable.FIRST_LOCAL_INDEX).toObject(false).updateLabel(end);
				}

				break;
			case "-":
				if (left == null) {
					operands.push(right.execIntegralOp(INEG, LNEG));
				} else {
					operands.push(left.execMathOp(right, ISUB, LSUB, DSUB, Evaluable.FIRST_LOCAL_INDEX));
				}

				break;
			case "*":
				operands.push(left.execMathOp(right, IMUL, LMUL, DMUL, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case "/":
				operands.push(left.execMathOp(right, IDIV, LDIV, DDIV, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case "%":
				operands.push(left.execMathOp(right, IREM, LREM, DREM, Evaluable.FIRST_LOCAL_INDEX));
				break;

			// Integral Operations
			case "~": // Treat as x ^ -1
				operands.push(right.execIntegralOp(new Operand(int.class, new MethodBuilder().pushConstant(-1)), IXOR, LXOR, false, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case "<<":
				operands.push(left.execIntegralOp(right, ISHL, LSHL, true, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case ">>":
				operands.push(left.execIntegralOp(right, ISHR, LSHR, true, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case ">>>":
				operands.push(left.execIntegralOp(right, IUSHR, LUSHR, true, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case "&":
				operands.push(left.execIntegralOp(right, IAND, LAND, false, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case "^":
				operands.push(left.execIntegralOp(right, IXOR, LXOR, false, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case "|":
				operands.push(left.execIntegralOp(right, IOR, LOR, false, Evaluable.FIRST_LOCAL_INDEX));
				break;

			// Logical Operators
			case "!": {
				final Label notZero = right.builder.newLabel();
				final Label end = right.builder.newLabel();

				operands.push(new Operand(boolean.class, right.toBoolean().addBranch(IFNE, notZero).addCode(ICONST_1).addBranch(GOTO, end).updateLabel(notZero).addCode(ICONST_0).updateLabel(end)));
				break;
			}
			case "&&": {
				final Label notZero = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				operands.push(new Operand(boolean.class, left.toBoolean().addBranch(IFNE, notZero).addCode(ICONST_0).addBranch(GOTO, end).updateLabel(notZero).append(right.toBoolean()).updateLabel(end)));
				break;
			}
			case "||": {
				final Label zero = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				operands.push(new Operand(boolean.class, left.toBoolean().addBranch(IFEQ, zero).addCode(ICONST_1).addBranch(GOTO, end).updateLabel(zero).append(right.toBoolean()).updateLabel(end)));
				break;
			}

			// Comparison Operators
			case "<=":
				operands.push(left.execCompareOp(right, IFLE, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case ">=":
				operands.push(left.execCompareOp(right, IFGE, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case "<":
				operands.push(left.execCompareOp(right, IFLT, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case ">":
				operands.push(left.execCompareOp(right, IFGT, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case "==":
				operands.push(left.execCompareOp(right, IFEQ, Evaluable.FIRST_LOCAL_INDEX));
				break;
			case "!=":
				operands.push(left.execCompareOp(right, IFNE, Evaluable.FIRST_LOCAL_INDEX));
				break;

			// Ternary Operations
			case "??":
			case "?:": {
				final Label end = left.builder.newLabel();

				operands.push(new Operand(Object.class, left.toObject(false).addCode(DUP).addBranch(IFNONNULL, end).addCode(POP).append(right.toObject(false)).updateLabel(end)));
				break;
			}
			case "?": {
				if (!Entry.class.equals(right.type)) {
					throw new IllegalArgumentException("Incomplete ternary operator, missing \":\"");
				}

				assert Entry.class.equals(left.type);
				assert !operands.isEmpty();

				final Label isFalse = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				operands.push(new Operand(Object.class, operands.pop().toBoolean().addBranch(IFEQ, isFalse).append(left.builder).addBranch(GOTO, end).updateLabel(isFalse).append(right.builder).updateLabel(end)));
				break;
			}

			case ":":
				operands.push(new Operand(Entry.class, left.toObject(false))).push(new Operand(Entry.class, right.toObject(false)));

				if (operators.isEmpty() || !operators.peek().has(Operator.ALLOW_PAIRS)) {
					operators.push(Operator.get(",", true).withRightExpressions(-1));
				}

				break;

			case ";":
				if (left.type == null) {
					left.builder.addCode(POP, POP2, POP2);
				} else {
					left.builder.addCode(long.class.equals(left.type) || double.class.equals(left.type) ? POP2 : POP);
				}

				operands.push(new Operand(right.type, left.builder.append(right.builder)));
				break;

			case ",":
				processOperation(namedExpressions, expressions, identifiers, operands.push(), operators.push(Operator.get("[", false).withRightExpressions(operator.getRightExpressions() + 1)));
				break;

			case "#<": // Return
				operands.push(new Operand(Object.class, right.toObject(true)));
				break;

			case "(":
				operands.push();
				break;

			case "=":
				operands.push(new Operand(Object.class, right.toObject(false).addCode(DUP).append(left.builder)));
				break;

			case "\u2620": case "~:<": // Die
				operands.push(new Operand(Object.class, right.toObject(false).addInvoke(STRING_VALUE_OF).pushNewObject(HaltRenderingException.class).addCode(DUP_X1, SWAP).addInvoke(HALT_EXCEPTION_CTOR_STRING).addCode(ATHROW)));
				break;

			case "~@": { // Get class
				final Label isNull = right.builder.newLabel();

				operands.push(new Operand(Object.class, right.toObject(false).addCode(DUP).addBranch(IFNULL, isNull).addInvoke(OBJECT_TO_STRING).addCode(Evaluable.LOAD_CONTEXT).addCode(SWAP).addInvoke(EXPRESSION_GET_VALUE).updateLabel(isNull)));
				break;
			}

			default:
				throw new IllegalStateException("Unrecognized operator \"" + operator.getString() + "\" while parsing expression");
		}
	}

	/**
	 * Unescape a quoted identifier name.
	 *
	 * @param rawIdentifier the escaped name of the identifier
	 * @return the unescaped name of the identifier
	 */
	private static String unescapeQuotedIdentifier(final String rawIdentifier) {
		final StringBuilder sb = new StringBuilder(rawIdentifier.length());
		int i = 0;

		for (int end = rawIdentifier.indexOf('\\', i); end >= 0; i = end + 1, end = rawIdentifier.indexOf('\\', i + 1)) {
			sb.append(rawIdentifier, i, end);
		}

		return sb.append(rawIdentifier.substring(i)).toString();
	}

	/**
	 * Creates a new expression.
	 *
	 * @param location the location of the expression
	 * @param expressionName the name of the expression
	 * @param expressionString the trimmed, advanced expression string
	 * @param namedExpressions the map used to lookup named expressions
	 * @param horseshoeExpressions true to parse as a horseshoe expression, false to parse as a Mustache variable list
	 * @throws ReflectiveOperationException if an error occurs while resolving the reflective parts of the expression
	 */
	public Expression(final String location, final String expressionName, final CharSequence expressionString, final Map<String, Expression> namedExpressions, final boolean horseshoeExpressions) throws ReflectiveOperationException {
		final HashMap<Expression, Integer> expressionMap = new HashMap<>();
		final HashMap<Identifier, Integer> identifierMap = new HashMap<>();
		final PersistentStack<Operand> operands = new PersistentStack<>();
		int nextLocalBindingIndex = Evaluable.FIRST_LOCAL_INDEX + Operand.REQUIRED_LOCAL_VARIABLE_SLOTS;

		if (expressionName != null) {
			namedExpressions.put(expressionName, this);
		}

		this.location = location;
		this.originalString = expressionString.toString();

		if (".".equals(this.originalString)) {
			operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(0).addInvoke(PERSISTENT_STACK_PEEK)));
		} else if (!horseshoeExpressions) {
			final MethodBuilder mb = new MethodBuilder();
			final String[] names = Pattern.compile("\\s*[.]\\s*", Pattern.UNICODE_CHARACTER_CLASS).split(originalString, -1);

			// Push a new operand formed by invoking identifiers[index].getValue(context, backreach, access)
			operands.push(new Operand(Object.class, mb.addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(getIdentifierIndex(identifierMap, new Identifier(names[0]))).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).pushConstant(Identifier.UNSTATED_BACKREACH).addInvoke(IDENTIFIER_FIND_VALUE)));

			// Load the identifiers and invoke identifiers[index].getValue(object)
			for (int i = 1; i < names.length; i++) {
				mb.addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(getIdentifierIndex(identifierMap, new Identifier(names[i]))).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE);
			}
		} else { // Tokenize the entire expression, using the shunting yard algorithm
			final HashMap<String, Integer> localBindingMap = new HashMap<>();
			final PersistentStack<Operator> operators = new PersistentStack<>();
			final int length = expressionString.length();
			Operator lastOperator = Operator.get("(", false);

			// Loop through all input
			nextToken:
			for (final Matcher matcher = IDENTIFIER_WITH_PREFIX_PATTERN.matcher(expressionString); matcher.regionStart() < length; matcher.region(matcher.end(), length)) {
				final boolean hasLeftExpression = (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION | Operator.X_RIGHT_EXPRESSIONS));
				final boolean lastNavigation = (lastOperator != null && lastOperator.has(Operator.NAVIGATION));

				// Skip comments
				if (matcher.usePattern(COMMENTS_PATTERN).lookingAt()) {
					if (matcher.hitEnd()) {
						break;
					}

					matcher.region(matcher.end(), length);
				}

				// Check for identifier or literal
				if (!hasLeftExpression && matcher.usePattern(lastNavigation ? IDENTIFIER_PATTERN : IDENTIFIER_WITH_PREFIX_PATTERN).lookingAt()) { // Identifier
					processIdentifier:
					do {
						final String identifier = matcher.group("identifier");
						int backreach = 0;
						boolean isRoot = false;
						boolean unstatedBackreach = false;
						boolean isInternal = false;

						// Check for additional identifier properties
						if (!lastNavigation) {
							isInternal = matcher.group("internal") != null;
							final String prefixString = matcher.group("prefix");

							if (prefixString != null) {
								switch (prefixString) {
									case "/":
									case "\\":
										isRoot = true;
										break;
									default:
										throw new IllegalStateException("Unrecognized prefix: " + prefixString);
								}

								if (isInternal || identifier.endsWith("(") || (identifier.startsWith(".") && (!".".equals(identifier) || !isRoot))) {
									throw new IllegalArgumentException("Invalid identifier with prefix \"" + prefixString + "\" in expression at offset " + matcher.regionStart());
								}
							} else {
								final String backreachString = matcher.group("backreach");

								if (backreachString != null) {
									backreach = backreachString.length() / 3;
								} else if (matcher.group("current") == null) {
									// Check for literals
									switch (identifier) {
										case "true":
											operands.push(new Operand(boolean.class, new MethodBuilder().pushConstant(1)));
											break processIdentifier;
										case "false":
											operands.push(new Operand(boolean.class, new MethodBuilder().pushConstant(0)));
											break processIdentifier;
										case "null":
											operands.push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
											break processIdentifier;
										default:
											unstatedBackreach = true;
											break;
									}
								}
							}
						}

						// Process the identifier
						if (".".equals(identifier)) {
							if (isRoot) {
								operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(PERSISTENT_STACK_PEEK_BASE)));
							} else {
								operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK)));
							}
						} else if ("..".equals(identifier)) {
							operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach + 1).addInvoke(PERSISTENT_STACK_PEEK)));
						} else if (isInternal) { // Everything that starts with "." is considered internal
							switch (identifier) {
								case "index":
									operands.push(new Operand(int.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_INDEXED_DATA).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK).addInvoke(INDEXED_GET_INDEX)));
									break;
								case "hasNext":
									operands.push(new Operand(boolean.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_INDEXED_DATA).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK).addInvoke(INDEXED_HAS_NEXT)));
									break;
								case "isFirst":
									operands.push(new Operand(int.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_INDEXED_DATA).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK).addInvoke(INDEXED_GET_INDEX)));
									processOperation(namedExpressions, expressionMap, identifierMap, operands, operators.push(Operator.get("!", false)));
									break;
								default:
									operands.push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
									break;
							}
						} else if (identifier.endsWith("(")) { // Process the method call
							final String name = identifier.startsWith("`") ? unescapeQuotedIdentifier(identifier.substring(1, identifier.length() - 2)) : identifier.substring(0, identifier.length() - 1);

							if (lastNavigation) {
								// Create a new output formed by invoking identifiers[index].getValue(object, ...)
								final MethodBuilder objectBuilder = operands.pop().toObject(false);
								final Label skipFunc = objectBuilder.newLabel();

								operands.push(new Operand(Object.class, new MethodBuilder().addInvoke(IDENTIFIER_GET_VALUE_METHOD).updateLabel(skipFunc)));

								if (lastOperator.has(Operator.SAFE)) {
									operands.push(new Operand(Object.class, new MethodBuilder().addCode(SWAP)));
									operands.push(new Operand(Object.class, objectBuilder.addCode(DUP).addBranch(IFNULL, skipFunc)));
								} else {
									operands.push(new Operand(Object.class, objectBuilder));
									operands.push(new Operand(Object.class, new MethodBuilder()));
								}

								lastOperator = operators.push(Operator.createMethod(name, true)).peek();
							} else {
								// Create a new output formed by invoking identifiers[index].findValue(context, backreach, ...)
								operands.push(new Operand(Object.class, new MethodBuilder().addInvoke(IDENTIFIER_FIND_VALUE_METHOD)));
								operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).pushConstant(unstatedBackreach ? Identifier.UNSTATED_BACKREACH : backreach)));
								operands.push(new Operand(Object.class, new MethodBuilder()));
								lastOperator = operators.push(Operator.createMethod(name, !unstatedBackreach)).peek();
							}

							continue nextToken;
						} else { // Process the identifier
							final String name = identifier.startsWith("`") ? unescapeQuotedIdentifier(identifier.substring(1, identifier.length() - 1)) : identifier;

							if (lastNavigation) {
								final int index = getIdentifierIndex(identifierMap, new Identifier(name));

								if (operators.pop().has(Operator.SAFE)) {
									// Create a new output formed by invoking identifiers[index].getValue(object)
									final Label end = operands.peek().builder.newLabel();
									operands.push(new Operand(Object.class, operands.pop().toObject(false).addCode(DUP).addBranch(IFNULL, end).addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE).updateLabel(end)));
								} else {
									operands.push(new Operand(Object.class, operands.pop().toObject(false).addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE)));
								}
							} else {
								final Integer localBindingIndex = unstatedBackreach ? localBindingMap.get(name) : null;
								final Operator operator = Operator.get(matcher.group("assignment"), true);

								// Look-ahead for an assignment operation
								if (operator != null && operator.has(Operator.ASSIGNMENT)) {
									if (!unstatedBackreach) {
										throw new IllegalArgumentException("Invalid assignment to non-local variable in expression at offset " + matcher.regionStart());
									}

									localBindingMap.put(name, nextLocalBindingIndex);
									operands.push(new Operand(Object.class, new MethodBuilder().addAccess(ASTORE, nextLocalBindingIndex++)));
								} else if (localBindingIndex != null) { // Check for a local binding
									operands.push(new Operand(Object.class, new MethodBuilder().addAccess(ALOAD, localBindingIndex)));
								} else { // Resolve the identifier
									final int index = getIdentifierIndex(identifierMap, new Identifier(name));

									// Create a new output formed by invoking identifiers[index].getRootValue(context, access) or identifiers[index].findValue(context, backreach, access)
									if (isRoot) {
										operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).addInvoke(IDENTIFIER_GET_ROOT_VALUE)));
									} else {
										operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).pushConstant(unstatedBackreach ? Identifier.UNSTATED_BACKREACH : backreach).addInvoke(IDENTIFIER_FIND_VALUE)));
									}
								}
							}
						}
					} while (false);
				} else if (lastNavigation) { // An identifier must follow the navigation operator
					throw new IllegalArgumentException("Invalid identifier in expression at offset " + matcher.regionStart());
				} else if (!hasLeftExpression && matcher.usePattern(DOUBLE_PATTERN).lookingAt()) { // Double literal
					operands.push(new Operand(double.class, new MethodBuilder().pushConstant(Double.parseDouble(matcher.group("double")))));
				} else if (!hasLeftExpression && matcher.usePattern(LONG_PATTERN).lookingAt()) { // Long literal
					final String decimal = matcher.group("decimal");
					final long value = decimal == null ? Long.parseLong(matcher.group("hexsign") + matcher.group("hexadecimal"), 16) : Long.parseLong(decimal);

					if (matcher.group("isLong") != null || (int)value != value) {
						operands.push(new Operand(long.class, new MethodBuilder().pushConstant(value)));
					} else {
						operands.push(new Operand(int.class, new MethodBuilder().pushConstant((int)value)));
					}
				} else if (!hasLeftExpression && matcher.usePattern(STRING_PATTERN).lookingAt()) { // String literal
					operands.push(new Operand(String.class, new MethodBuilder().pushConstant(parseStringLiteral(matcher))));
				} else if (!hasLeftExpression && matcher.usePattern(REGEX_PATTERN).lookingAt()) { // Regular expression literal
					operands.push(new Operand(Object.class, new MethodBuilder().pushConstant(Pattern.compile(matcher.group("regex")).pattern()).addInvoke(PATTERN_COMPILE)));
				} else if (matcher.usePattern(OPERATOR_PATTERN).lookingAt()) { // Operator
					final String token = matcher.group("operator");
					final Operator operator = Operator.get(token, hasLeftExpression);

					if (operator != null) {
						// Shunting-yard Algorithm
						while (!operators.isEmpty() && !operators.peek().isContainer() && (operators.peek().getPrecedence() < operator.getPrecedence() || (operators.peek().getPrecedence() == operator.getPrecedence() && operator.isLeftAssociative()))) {
							processOperation(namedExpressions, expressionMap, identifierMap, operands, operators);
						}

						if (!operators.isEmpty() && operators.peek().has(Operator.X_RIGHT_EXPRESSIONS) && ",".equals(token)) {
							operators.push(operators.pop().addRightExpression());
						} else {
							operators.push(operator);
						}

						lastOperator = operator;
						continue nextToken;
					} else if (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION)) { // Check if this token ends an expression on the stack
						while (!operators.isEmpty()) {
							Operator closedOperator = operators.pop();

							if (closedOperator.has(Operator.X_RIGHT_EXPRESSIONS) && hasLeftExpression) { // Process multi-argument operators
								closedOperator = closedOperator.addRightExpression();
							}

							if (closedOperator.getClosingString() != null) {
								if (!closedOperator.getClosingString().equals(token)) { // Check for a mismatched closing string
									throw new IllegalArgumentException("Unexpected \"" + token + "\" in expression at offset " + matcher.regionStart() + ", expecting \"" + closedOperator.getClosingString() + "\"");
								}

								processOperation(namedExpressions, expressionMap, identifierMap, operands, operators.push(closedOperator));
								lastOperator = closedOperator.getClosingOperator();
								continue nextToken;
							}

							processOperation(namedExpressions, expressionMap, identifierMap, operands, operators.push(closedOperator));
						}
					}

					throw new IllegalArgumentException("Unexpected \"" + token + "\" in expression at offset " + matcher.regionStart());
				} else {
					throw new IllegalArgumentException("Unrecognized identifier in expression at offset " + matcher.regionStart());
				}

				lastOperator = null;
			}

			// Push everything to the output queue
			while (!operators.isEmpty()) {
				Operator operator = operators.pop();

				if (operator.getClosingString() != null) {
					throw new IllegalArgumentException("Unexpected end of expression, expecting \"" + operator.getClosingString() + "\" (unmatched \"" + operator.getString() + "\")");
				} else if (operator.has(Operator.X_RIGHT_EXPRESSIONS) && (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION | Operator.X_RIGHT_EXPRESSIONS))) { // Process multi-argument operators, but allow trailing commas
					operator = operator.addRightExpression();
				} else if (lastOperator != null && lastOperator.has(Operator.RIGHT_EXPRESSION)) {
					if (lastOperator.has(Operator.IGNORE_TRAILING)) {
						lastOperator = null;
						continue;
					} else {
						throw new IllegalArgumentException("Unexpected end of expression");
					}
				}

				processOperation(namedExpressions, expressionMap, identifierMap, operands, operators.push(operator));
			}
		}

		if (operands.isEmpty()) {
			throw new IllegalArgumentException("Unexpected empty expression");
		}

		// Populate all the expressions
		if (expressionMap.isEmpty()) {
			this.expressions = null;
		} else {
			this.expressions = new Expression[expressionMap.size()];

			for (final Entry<Expression, Integer> entry : expressionMap.entrySet()) {
				this.expressions[entry.getValue()] = entry.getKey();
			}
		}

		// Populate all the identifiers
		if (identifierMap.isEmpty()) {
			this.identifiers = null;
		} else {
			this.identifiers = new Identifier[identifierMap.size()];

			for (final Entry<Identifier, Integer> entry : identifierMap.entrySet()) {
				this.identifiers[entry.getValue()] = entry.getKey();
			}
		}

		// Create the evaluator
		final MethodBuilder initializeLocalBindings = new MethodBuilder();

		for (int i = Evaluable.FIRST_LOCAL_INDEX + Operand.REQUIRED_LOCAL_VARIABLE_SLOTS; i < nextLocalBindingIndex; i++) {
			initializeLocalBindings.addCode(ACONST_NULL).addAccess(ASTORE, i);
		}

		this.evaluable = initializeLocalBindings.append(operands.pop().toObject(true)).build(Expression.class.getPackage().getName() + ".dyn.Expression_" + DYN_INDEX.getAndIncrement(), Evaluable.class, Expression.class.getClassLoader()).getConstructor().newInstance();
		assert operands.isEmpty();
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof Expression) {
			return originalString.equals(((Expression)object).originalString) && location.equals(((Expression)object).location);
		}

		return false;
	}

	/**
	 * Evaluates the expression using the given render context.
	 *
	 * @param context the render context used to evaluate the object
	 * @return the evaluated expression or null if the expression could not be evaluated
	 */
	public Object evaluate(final RenderContext context) {
		try {
			return evaluable.evaluate(expressions, identifiers, context);
		} catch (final HaltRenderingException e) {
			throw e;
		} catch (final Exception | LinkageError e) { // Don't let any exceptions escape
			if (e.getMessage() == null) {
				context.getSettings().getLogger().log(Level.WARNING, e, "Failed to evaluate expression \"{0}\" ({1})", originalString, location);
			} else {
				context.getSettings().getLogger().log(Level.WARNING, e, "Failed to evaluate expression \"{0}\" ({1}): {2}", originalString, location, e.getMessage());
			}
		}

		return null;
	}

	@Override
	public int hashCode() {
		return originalString.hashCode();
	}

	@Override
	public String toString() {
		return originalString;
	}

}
