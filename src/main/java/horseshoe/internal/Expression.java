package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.Settings;
import horseshoe.Settings.ContextAccess;
import horseshoe.internal.MethodBuilder.Label;

public final class Expression {

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

	public static abstract class Evaluable {
		public static final byte LOAD_IDENTIFIERS = ALOAD_1;
		public static final byte LOAD_CONTEXT = ALOAD_2;
		public static final byte LOAD_ACCESS = ALOAD_3;
		public static final byte LOAD_INDEXES[] = new byte[] { ALOAD, 4 };
		public static final byte LOAD_ERRORS[] = new byte[] { ALOAD, 5 };
		public static final int FIRST_LOCAL = 6;

		/**
		 * Evaluates the object using the specified parameters.
		 *
		 * @param identifiers the identifiers used to evaluate the object
		 * @param context the context used to evaluate the object
		 * @param access the access for the context for evaluating the object
		 * @param errors the list of errors encountered while evaluating the object
		 * @return the result of evaluating the object
		 * @throws ReflectiveOperationException if an error occurs while evaluating the reflective parts of the object
		 */
		public abstract Object evaluate(final Identifier identifiers[], final PersistentStack<Object> context, final ContextAccess access, final PersistentStack<Indexed> indexes, final List<String> errors) throws ReflectiveOperationException;
	}

	private static final AtomicInteger DYN_INDEX = new AtomicInteger();
	private static final Operator NAVIGATE_OPERATOR = Operator.get(".");

	// Reflected Methods
	private static final Method ENTRY_GET_KEY;
	private static final Method ENTRY_GET_VALUE;
	private static final Method IDENTIFIER_GET_VALUE;
	private static final Method IDENTIFIER_GET_VALUE_BACKREACH;
	private static final Method IDENTIFIER_GET_VALUE_METHOD;
	private static final Method INDEXED_GET_INDEX;
	private static final Method INDEXED_HAS_NEXT;
	private static final Method MAP_PUT;
	private static final Method PERSISTENT_STACK_PEEK;
	private static final Method STRING_BUILDER_APPEND_OBJECT;
	private static final Constructor<StringBuilder> STRING_BUILDER_INIT_STRING;
	private static final Method STRING_VALUE_OF;

	static {
		try {
			ENTRY_GET_KEY = Entry.class.getMethod("getKey");
			ENTRY_GET_VALUE = Entry.class.getMethod("getValue");
			IDENTIFIER_GET_VALUE = Identifier.class.getMethod("getValue", Object.class);
			IDENTIFIER_GET_VALUE_BACKREACH = Identifier.class.getMethod("getValue", PersistentStack.class, Settings.ContextAccess.class);
			IDENTIFIER_GET_VALUE_METHOD = Identifier.class.getMethod("getValue", Object.class, Object[].class);
			INDEXED_GET_INDEX = Indexed.class.getMethod("getIndex");
			INDEXED_HAS_NEXT = Indexed.class.getMethod("hasNext");
			MAP_PUT = Map.class.getMethod("put", Object.class, Object.class);
			PERSISTENT_STACK_PEEK = PersistentStack.class.getMethod("peek", int.class);
			STRING_BUILDER_APPEND_OBJECT = StringBuilder.class.getMethod("append", Object.class);
			STRING_BUILDER_INIT_STRING = StringBuilder.class.getConstructor(String.class);
			STRING_VALUE_OF = String.class.getMethod("valueOf", Object.class);
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException("Bad reflection operation: " + e.getMessage(), e);
		}
	}

	// The patterns used for parsing the grammar
	private static final Pattern IDENTIFIER_BACKREACH_PATTERN = Pattern.compile("\\s*([.][.]/)+");
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\s*([.][/.])*((" + Identifier.PATTERN + "|`([^`\\\\]|\\\\[`\\\\])+`)[(]?)");
	private static final Pattern INTERNAL_PATTERN = Pattern.compile("\\s*([.][/.])*([.]\\p{L}*)");
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("\\s*([0-9][0-9]*[.][0-9]+)");
	private static final Pattern LONG_PATTERN = Pattern.compile("\\s*(?:0[Xx](?<hexadecimal>[0-9A-Fa-f]+)|(?<decimal>[0-9]+))");
	private static final Pattern STRING_PATTERN = Pattern.compile("\\s*\"(([^\"\\\\]|\\\\[\\\\\"'btnfr]|\\\\x[0-9A-Fa-f]{1,8}|\\\\u[0-9A-Fa-f]{4}|\\\\U[0-9A-Fa-f]{8})*)\"");
	private static final Pattern OPERATOR_PATTERN;

	static {
		final StringBuilder sb = new StringBuilder("\\s*(?<operator>");
		final Set<String> patterns = new TreeSet<>(new Comparator<String>() {
			@Override
			public int compare(final String o1, final String o2) {
				final int lengthDiff = o2.length() - o1.length();
				return lengthDiff == 0 ? o1.compareTo(o2) : lengthDiff;
			}
		});

		// Quote each operator as part of the operator pattern
		for (final Operator operator : Operator.getAll()) {
			patterns.add(operator.getString());

			if (operator.getClosingString() != null) {
				patterns.add(operator.getClosingString());
			}
		}

		for (final String pattern : patterns) {
			sb.append(Pattern.quote(pattern)).append('|');
		}

		// Add comma as a separator
		OPERATOR_PATTERN = Pattern.compile(sb.append(",)").toString());
	}

	/**
	 * Generates the appropriate instructions to log an error.
	 *
	 * @param mb the builder to use for logging the error
	 * @param error the error to log
	 * @return the method builder
	 */
	static MethodBuilder logError(final MethodBuilder mb, final String error) {
		try {
			return mb.addCode(Evaluable.LOAD_ERRORS).pushConstant(error).addInvoke(List.class.getMethod("add", Object.class));
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Processes the operation
	 *
	 * @param mb
	 * @param identifiers
	 * @param operands
	 * @param operator
	 * @throws ReflectiveOperationException
	 */
	private static void processOperation(final HashMap<Identifier, Integer> identifiers, final PersistentStack<Operand> operands, final Operator operator) throws ReflectiveOperationException {
		final Operand right[];

		if (operator == NAVIGATE_OPERATOR) { // Navigation operator is handled during parsing
			return;
		} else if (operator.has(Operator.METHOD_CALL)) { // Check for a method call
			final MethodBuilder mb = operands.peek(operator.getRightExpressions()).builder;

			if (operator.getRightExpressions() == 0) {
				mb.addCode(ACONST_NULL);
			} else {
				mb.pushNewObject(Object.class, operator.getRightExpressions());
			}

			// Convert all parameters to objects and store them in the array
			for (int i = 0; i < operator.getRightExpressions(); i++) {
				mb.addCode(DUP).pushConstant(i).append(operands.peek(operator.getRightExpressions() - 1 - i).toObject(false)).addCode(AASTORE);
			}

			operands.pop(1 + operator.getRightExpressions()).push(new Operand(Object.class, mb.addInvoke(IDENTIFIER_GET_VALUE_METHOD)));
			return;
		} else if (operator.has(Operator.X_RIGHT_EXPRESSIONS)) { // Determine the normalized class of all right hand arguments
			right = new Operand[operator.getRightExpressions()];

			for (int i = operator.getRightExpressions(); --i >= 0; ) {
				right[i] = operands.pop();
			}
		} else if (operator.has(Operator.RIGHT_EXPRESSION)) {
			right = new Operand[1];
			right[0] = operands.pop();
		} else {
			right = null;
		}

		final Operand left = operator.has(Operator.LEFT_EXPRESSION) ? operands.pop() : null;

		switch (operator.getString()) {
		case "{": { // TODO: Iterable?
			final MethodBuilder builder = new MethodBuilder().pushNewObject(LinkedHashMap.class).addCode(DUP).addInvoke(LinkedHashMap.class.getConstructor());

			for (int i = 0; i < right.length; i++) {
				if (Entry.class.equals(right[i].type)) {
					builder.addCode(DUP).append(right[i].builder).addCode(DUP).addInvoke(ENTRY_GET_KEY).addCode(SWAP).addInvoke(ENTRY_GET_VALUE).addInvoke(MAP_PUT).addCode(POP);
				} else {
					builder.addCode(DUP).pushConstant(i).addPrimitiveConversion(int.class, Object.class).append(right[i].toObject(false)).addInvoke(MAP_PUT).addCode(POP);
				}
			}

			operands.push(new Operand(Object.class, builder));
			break;
		}

		// Math Operations
		case "+":
			if (left == null) { // Unary +, basically do nothing except require a number
				operands.push(new Operand(null, right[0].toNumeric(true)));
			} else if (StringBuilder.class.equals(left.type)) { // Check for string concatenation
				operands.push().builder.append(right[0].toObject(false)).addInvoke(STRING_BUILDER_APPEND_OBJECT);
			} else if (String.class.equals(left.type) || String.class.equals(right[0].type) || StringBuilder.class.equals(right[0].type)) {
				operands.push(new Operand(StringBuilder.class, left.toObject(false).pushNewObject(StringBuilder.class).addCode(DUP_X1, SWAP).addInvoke(STRING_VALUE_OF).addInvoke(STRING_BUILDER_INIT_STRING).append(right[0].toObject(false)).addInvoke(STRING_BUILDER_APPEND_OBJECT)));
			} else if ((left.type == null || (left.type.isPrimitive() && !boolean.class.equals(left.type))) && (right[0].type == null || (right[0].type.isPrimitive() && !boolean.class.equals(right[0].type)))) { // Mathematical addition
				operands.push(left.execMathOp(right[0], IADD, LADD, DADD));
			} else { // String concatenation, mathematical addition, or invalid
				final Label notStringBuilder = left.builder.newLabel();
				final Label isString = left.builder.newLabel();
				final Label notString = left.builder.newLabel();
				final Label end = left.builder.newLabel();
				final Operand result = new Operand(Object.class, left.toObject(false).append(right[0].toObject(false)).addCode(SWAP, DUP_X1).addInstanceOfCheck(StringBuilder.class).addBranch(IFEQ, notStringBuilder).addCode(SWAP).addCast(StringBuilder.class).addCode(SWAP).addInvoke(STRING_BUILDER_APPEND_OBJECT).addBranch(GOTO, end)
					.updateLabel(notStringBuilder).addCode(SWAP, DUP_X1).addInstanceOfCheck(String.class).addBranch(IFNE, isString).addCode(DUP).addInstanceOfCheck(String.class).addBranch(IFNE, isString).addCode(DUP).addInstanceOfCheck(StringBuilder.class).addBranch(IFEQ, notString)
					.updateLabel(isString).addCode(SWAP).pushNewObject(StringBuilder.class).addCode(DUP_X2, SWAP).addInvoke(STRING_VALUE_OF).addInvoke(STRING_BUILDER_INIT_STRING).addInvoke(STRING_BUILDER_APPEND_OBJECT).addBranch(GOTO, end)
					.updateLabel(notString).addAccess(ASTORE, Evaluable.FIRST_LOCAL));

				result.execMathOp(new Operand(Object.class, new MethodBuilder().addAccess(ALOAD, Evaluable.FIRST_LOCAL)), IADD, LADD, DADD).toObject(false).updateLabel(end);
				operands.push(result);
			}

			break;
		case "-":
			if (left == null) {
				operands.push(right[0].execIntegralOp(INEG, LNEG));
			} else {
				operands.push(left.execMathOp(right[0], ISUB, LSUB, DSUB));
			}

			break;
		case "*":
			operands.push(left.execMathOp(right[0], IMUL, LMUL, DMUL));
			break;
		case "/":
			operands.push(left.execMathOp(right[0], IDIV, LDIV, DDIV));
			break;
		case "%":
			operands.push(left.execMathOp(right[0], IREM, LREM, DREM));
			break;

		// Integral Operations
		case "~": // Treat as x ^ -1
			operands.push(right[0].execIntegralOp(new Operand(int.class, new MethodBuilder().pushConstant(-1)), IXOR, LXOR, false));
			break;
		case "<<":
			operands.push(left.execIntegralOp(right[0], ISHL, LSHL, true));
			break;
		case ">>":
			operands.push(left.execIntegralOp(right[0], ISHR, LSHR, true));
			break;
		case ">>>":
			operands.push(left.execIntegralOp(right[0], IUSHR, LUSHR, true));
			break;
		case "&":
			operands.push(left.execIntegralOp(right[0], IAND, LAND, false));
			break;
		case "^":
			operands.push(left.execIntegralOp(right[0], IXOR, LXOR, false));
			break;
		case "|":
			operands.push(left.execIntegralOp(right[0], IOR, LOR, false));
			break;

		// Logical Operators
		case "!": {
			final Label notZero = right[0].builder.newLabel();
			final Label end = right[0].builder.newLabel();

			operands.push(new Operand(boolean.class, right[0].toBoolean().addBranch(IFNE, notZero).addCode(ICONST_1).addBranch(GOTO, end).updateLabel(notZero).addCode(ICONST_0).updateLabel(end)));
			break;
		}
		case "&&": {
			final Label notZero = left.builder.newLabel();
			final Label end = left.builder.newLabel();

			operands.push(new Operand(boolean.class, left.toBoolean().addBranch(IFNE, notZero).addCode(ICONST_0).addBranch(GOTO, end).updateLabel(notZero).append(right[0].builder).updateLabel(end)));
			break;
		}
		case "||": {
			final Label zero = left.builder.newLabel();
			final Label end = left.builder.newLabel();

			operands.push(new Operand(boolean.class, left.toBoolean().addBranch(IFEQ, zero).addCode(ICONST_1).addBranch(GOTO, end).updateLabel(zero).append(right[0].builder).updateLabel(end)));
			break;
		}

		// Comparison Operators
		case "<=":
			operands.push(left.execCompareOp(right[0], IF_ICMPLE, IFLE));
			break;
		case ">=":
			operands.push(left.execCompareOp(right[0], IF_ICMPGE, IFGE));
			break;
		case "<":
			operands.push(left.execCompareOp(right[0], IF_ICMPLT, IFLT));
			break;
		case ">":
			operands.push(left.execCompareOp(right[0], IF_ICMPGT, IFGT));
			break;
		case "==":
			operands.push(left.execCompareOp(right[0], IF_ICMPEQ, IFEQ));
			break;
		case "!=":
			operands.push(left.execCompareOp(right[0], IF_ICMPNE, IFNE));
			break;

		case ":":
			operands.push(new Operand(Entry.class, new MethodBuilder().pushNewObject(AbstractMap.SimpleEntry.class).addCode(DUP).append(left.toObject(false)).append(right[0].toObject(false).addInvoke(AbstractMap.SimpleEntry.class.getConstructor(Object.class, Object.class)))));
			break;

		case ",":
			if (left.type == null) {
				left.builder.addCode(POP, POP2, POP2);
			} else {
				left.builder.addCode(long.class.equals(left.type) || double.class.equals(left.type) ? POP2 : POP);
			}

			operands.push(new Operand(right[0].type, left.builder.append(right[0].builder)));
			break;

		default:
			throw new RuntimeException("Unrecognized operator '" + operator.getString() + "' while parsing expression");
		}
	}

	private final String originalString;
	private final Identifier identifiers[];
	private final Evaluable evaluable;
	private final List<String> errors = new ArrayList<>();

	/**
	 * Creates a new expression.
	 *
	 * @param expressionString the advanced expression string
	 * @param simpleExpression true to parse as a simple expression, false to parse as an advanced expression
	 * @param maxBackreach the maximum backreach allowed by the expression
	 * @throws ReflectiveOperationException if an error occurs while resolving the reflective parts of the expression
	 */
	public Expression(final CharSequence expressionString, final boolean simpleExpression, final int maxBackreach) throws ReflectiveOperationException {
		final HashMap<Identifier, Integer> identifiers = new HashMap<>();
		final PersistentStack<Operand> operands = new PersistentStack<>();

		this.originalString = expressionString.toString();

		if (".".equals(this.originalString)) {
			operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).pushConstant(0).addInvoke(PERSISTENT_STACK_PEEK)));
		} else if (simpleExpression) {
			final MethodBuilder mb = new MethodBuilder();
			final String names[] = originalString.split("\\.", -1);

			// Load the identifiers
			for (int i = 0; i < names.length; i++) {
				final Identifier identifier = new Identifier(names[i].trim());
				Integer index = identifiers.get(identifier);

				if (index == null) {
					index = identifiers.size();
					identifiers.put(identifier, index);
				}

				if (i != 0) {
					// Create a new output formed by invoking identifiers[index].getValue(object)
					mb.addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE);
				} else {
					// Create a new output formed by invoking identifiers[index].getValue(context, access)
					mb.addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).addCode(Evaluable.LOAD_ACCESS).addInvoke(IDENTIFIER_GET_VALUE_BACKREACH);
				}
			}

			operands.push(new Operand(Object.class, mb));
		} else {
			// Tokenize the entire expression, using the shunting yard algorithm
			final PersistentStack<Operator> operators = new PersistentStack<>();
			final int length = expressionString.length();
			Operator lastOperator = Operator.get("(");

			// Loop through all input
			nextToken:
			for (final Matcher matcher = IDENTIFIER_BACKREACH_PATTERN.matcher(expressionString); !matcher.hitEnd(); matcher.region(matcher.end(), length)) {
				final boolean hasLeftExpression = (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION | Operator.X_RIGHT_EXPRESSIONS));
				int backreach = 0;

				// Check for backreach
				if (matcher.usePattern(IDENTIFIER_BACKREACH_PATTERN).lookingAt()) {
					if (hasLeftExpression || lastOperator == NAVIGATE_OPERATOR) {
						throw new RuntimeException("Unexpected backreach in expression at offset " + matcher.start(1));
					}

					backreach = (matcher.end(1) - matcher.start(1)) / 3;

					if (backreach > maxBackreach) {
						throw new RuntimeException("Backreach too far (max: " + maxBackreach + ") in expression at offset " + matcher.start(1));
					}

					matcher.region(matcher.end(), length);
				}

				// Check for identifier or literals
				if (!hasLeftExpression && matcher.usePattern(IDENTIFIER_PATTERN).lookingAt()) { // Identifier
					final String match = matcher.group(2);

					// Check for keywords that look like literals
					if ("true".equals(match) && backreach == 0 && lastOperator != NAVIGATE_OPERATOR) {
						operands.push(new Operand(boolean.class, new MethodBuilder().pushConstant(1)));
					} else if ("false".equals(match) && backreach == 0 && lastOperator != NAVIGATE_OPERATOR) {
						operands.push(new Operand(boolean.class, new MethodBuilder().pushConstant(0)));
					} else if ("null".equals(match) && backreach == 0 && lastOperator != NAVIGATE_OPERATOR) {
						operands.push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
					} else if (match.endsWith("(")) { // Method
						final String name = match.startsWith("`") ? match.substring(1, match.length() - 2).replaceAll("\\\\(.)", "\\1") : match.substring(0, match.length() - 1);
						final Identifier identifier = new Identifier(backreach, name, true);
						Integer index = identifiers.get(identifier);

						if (index == null) {
							index = identifiers.size();
							identifiers.put(identifier, index);
						}

						if (lastOperator == NAVIGATE_OPERATOR) {
							// Create a new output formed by invoking identifiers[index].getValue(object, ...)
							operands.push(new Operand(Object.class, operands.pop().toObject(false).addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP)));
							operators.pop();
						} else {
							// Create a new output formed by invoking identifiers[index].getValue(context.peek(backreach), ...)
							operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK)));
						}

						lastOperator = operators.push(Operator.createMethod(name));
						continue nextToken;
					} else { // Identifier
						final String name = match.startsWith("`") ? match.substring(1, match.length() - 1).replaceAll("\\\\(.)", "\\1") : match;
						final Identifier identifier = new Identifier(backreach, name, false);
						Integer index = identifiers.get(identifier);

						if (index == null) {
							index = identifiers.size();
							identifiers.put(identifier, index);
						}

						if (lastOperator == NAVIGATE_OPERATOR) {
							// Create a new output formed by invoking identifiers[index].getValue(object)
							operands.push(new Operand(Object.class, operands.pop().toObject(false).addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE)));
							operators.pop();
						} else {
							// Create a new output formed by invoking identifiers[index].getValue(context, access)
							operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).addCode(Evaluable.LOAD_ACCESS).addInvoke(IDENTIFIER_GET_VALUE_BACKREACH)));
						}
					}
				} else if (!hasLeftExpression && matcher.usePattern(INTERNAL_PATTERN).lookingAt()) { // Internal identifier
					final String name = matcher.group(2);

					if (".index".equals(name)) {
						operands.push(new Operand(int.class, new MethodBuilder().addCode(Evaluable.LOAD_INDEXES).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK).addInvoke(INDEXED_GET_INDEX)));
					} else if (".hasNext".equals(name)) {
						operands.push(new Operand(boolean.class, new MethodBuilder().addCode(Evaluable.LOAD_INDEXES).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK).addInvoke(INDEXED_HAS_NEXT)));
					} else if (".".equals(name)) {
						operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK)));
					} else { // TODO: Unknown internal identifier = null?
						operands.push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
					}
				} else if (backreach != 0 || lastOperator == NAVIGATE_OPERATOR) { // Any backreach must have an identifier associated with it, and identifiers must follow the member selection operator
					throw new RuntimeException("Invalid identifier in expression at offset " + matcher.regionStart());
				} else if (matcher.hitEnd()) {
					break;
				} else if (matcher.usePattern(DOUBLE_PATTERN).lookingAt()) { // Double literal
					operands.push(new Operand(double.class, new MethodBuilder().pushConstant(Double.parseDouble(matcher.group(1)))));
				} else if (matcher.usePattern(LONG_PATTERN).lookingAt()) { // Long literal
					final String decimal = matcher.group("decimal");
					final long value = decimal == null ? Long.parseLong(matcher.group("hexadecimal"), 16) : Long.parseLong(decimal);

					if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
						operands.push(new Operand(long.class, new MethodBuilder().pushConstant(value)));
					} else {
						operands.push(new Operand(int.class, new MethodBuilder().pushConstant((int)value)));
					}
				} else if (matcher.usePattern(STRING_PATTERN).lookingAt()) { // String literal
					final String literal = matcher.group(1);
					int backslash = 0;

					if (literal == null) {
						operands.push(new Operand(String.class, new MethodBuilder().pushConstant("")));
					} else if ((backslash = literal.indexOf('\\')) < 0) {
						operands.push(new Operand(String.class, new MethodBuilder().pushConstant(literal)));
					} else { // Find escape sequences and replace them with the proper character sequences
						final StringBuilder sb = new StringBuilder(literal.length());
						int start = 0;

						do {
							sb.append(literal, start, backslash);

							if (backslash + 1 >= literal.length()) {
								throw new RuntimeException("Invalid '\\' at end of string literal in expression at offset " + matcher.start(1));
							}

							switch (literal.charAt(backslash + 1)) {
							case '\\': sb.append('\\'); break;
							case '"':  sb.append('\"'); break;
							case '\'': sb.append('\''); break;
							case 'b':  sb.append('\b'); break;
							case 't':  sb.append('\t'); break;
							case 'n':  sb.append('\n'); break;
							case 'f':  sb.append('\f'); break;
							case 'r':  sb.append('\r'); break;

							case 'x':
								int codePoint = 0;

								for (int i = 0; i < 8 && backslash + 2 < literal.length(); i++, backslash++) {
									final int digit = literal.charAt(backslash + 2);

									if (digit >= '0' && digit <= '9') {
										codePoint = codePoint * 16 + digit - '0';
									} else if (digit >= 'A' && digit <= 'F') {
										codePoint = codePoint * 16 + digit + (10 - 'A');
									} else if (digit >= 'a' && digit <= 'f') {
										codePoint = codePoint * 16 + digit + (10 - 'a');
									} else {
										break;
									}
								}

								sb.append(Character.toChars(codePoint));
								break;

							case 'u':
								if (backslash + 5 >= literal.length()) {
									throw new RuntimeException("Invalid '\\u' at end of string literal in expression at offset " + matcher.start(1));
								}

								sb.append(Character.toChars(Integer.parseInt(literal.substring(backslash + 2, backslash + 6), 16)));
								backslash += 4;
								break;

							case 'U':
								if (backslash + 9 >= literal.length()) {
									throw new RuntimeException("Invalid '\\U' at end of string literal in expression at offset " + matcher.start(1));
								}

								sb.append(Character.toChars(Integer.parseInt(literal.substring(backslash + 2, backslash + 10), 16)));
								backslash += 8;
								break;

							default: throw new RuntimeException("Invalid character (" + literal.charAt(backslash + 1) + ") following '\\' in string literal (offset: " + backslash + ") in expression at offset " + matcher.start(1));
							}

							start = backslash + 2;
						} while ((backslash = literal.indexOf('\\', start)) >= 0);

						operands.push(new Operand(String.class, new MethodBuilder().pushConstant(sb.append(literal, start, literal.length()).toString())));
					}
				} else if (matcher.usePattern(OPERATOR_PATTERN).lookingAt()) { // Operator
					final String token = matcher.group("operator");
					Operator operator = Operator.get(token);

					for (; operator != null && hasLeftExpression != operator.has(Operator.LEFT_EXPRESSION); ) {
						operator = operator.next();
					}

					if (operator != null) {
						// Shunting-yard Algorithm
						while (!operators.isEmpty() && !operators.peek().has(Operator.X_RIGHT_EXPRESSIONS) && !"(".equals(operators.peek().getString()) && (operators.peek().getPrecedence() < operator.getPrecedence() || (operators.peek().getPrecedence() == operator.getPrecedence() && operator.isLeftAssociative()))) {
							if (operators.peek().getClosingString() != null) {
								throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.start(1) + ", expecting '" + operators.peek().getClosingString() + "'");
							}

							processOperation(identifiers, operands, operators.pop());
						}

						if (!operators.isEmpty() && operators.peek().has(Operator.X_RIGHT_EXPRESSIONS) && ",".equals(token)) {
							if (!hasLeftExpression) { // Check for invalid and empty expressions
								throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.start(1));
							}

							operators.push(operators.pop().addRightExpression());
						} else {
							operators.push(operator);
						}

						lastOperator = operator;
						continue nextToken;
					}

					if (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION)) { // Check if this token ends an expression on the stack
						while (!operators.isEmpty()) {
							if (operators.peek().getClosingString() != null) {
								Operator closedOperator = operators.pop();

								if (!closedOperator.getClosingString().equals(token)) { // Check for a mismatched closing string
									throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.start(1) + ", expecting '" + closedOperator.getClosingString() + "'");
								} else if (closedOperator.has(Operator.X_RIGHT_EXPRESSIONS)) { // Process multi-argument operators
									if (hasLeftExpression) { // Allow trailing commas
										closedOperator = closedOperator.addRightExpression();
									}

									processOperation(identifiers, operands, closedOperator);
								} else if (!hasLeftExpression) { // Check for empty expressions
									throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.start(1));
								} else if (closedOperator.has(Operator.LEFT_EXPRESSION)) { // If the token is not a parenthetical, process the operation
									processOperation(identifiers, operands, closedOperator);
								}

								lastOperator = closedOperator.getClosingOperator();
								continue nextToken;
							}

							processOperation(identifiers, operands, operators.pop());
						}
					}

					throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.start(1));
				} else {
					throw new RuntimeException("Unrecognized identifier in expression at offset " + matcher.regionStart());
				}

				// Check for multiple consecutive identifiers or literals
				if (hasLeftExpression) {
					throw new RuntimeException("Unexpected '" + matcher.group(1) + "' in expression at offset " + matcher.start(1) + ", expecting operator");
				}

				lastOperator = null;
			}

			// Push everything to the output queue
			while (!operators.isEmpty()) {
				if (operators.peek().getClosingString() != null) {
					throw new RuntimeException("Unexpected end of expression, expecting '" + operators.peek().getClosingString() + "' (unmatched '" + operators.peek().getString() + "')");
				}

				processOperation(identifiers, operands, operators.pop());
			}
		}

		// Populate all the identifiers and create the evaluator
		this.identifiers = new Identifier[identifiers.size()];
		this.evaluable = operands.pop().toObject(true).load(Expression.class.getPackage().getName() + ".dyn.Expression_" + DYN_INDEX.getAndIncrement(), Evaluable.class, Expression.class.getClassLoader()).getConstructor().newInstance();
		assert operands.isEmpty();

		for (final Entry<Identifier, Integer> entry : identifiers.entrySet()) {
			this.identifiers[entry.getValue()] = entry.getKey();
		}
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof Expression) {
			return originalString.equals(((Expression)object).originalString);
		}

		return false;
	}

	/**
	 * Evaluates the expression using the given context, global data, and access
	 *
	 * @param context the context to use for evaluating the expression
	 * @return the evaluated expression or null if the expression could not be evaluated
	 */
	public Object evaluate(final PersistentStack<Object> context, final Settings.ContextAccess access, final PersistentStack<Indexed> indices) {
		try {
			return evaluable.evaluate(identifiers, context, access, indices, errors);
		} catch (final Throwable t) { // Don't let any exceptions escape
			errors.add(t.getMessage());
		}

		return null;
	}

	/**
	 * Gets a list of errors from the expression.
	 *
	 * @return the list of errors for the expression
	 */
	public List<String> getErrors() {
		return errors;
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
