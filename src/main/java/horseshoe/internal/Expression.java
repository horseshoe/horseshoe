package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

	public static interface ErrorLogger {
		/**
		 * Logs the specified error.
		 *
		 * @param error the error to log
		 */
		public void log(final Throwable error);
	}

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
		public static final int FIRST_LOCAL = 5;

		/**
		 * Evaluates the object using the specified parameters.
		 *
		 * @param identifiers the identifiers used to evaluate the object
		 * @param context the context used to evaluate the object
		 * @param access the access for the context for evaluating the object
		 * @return the result of evaluating the object
		 * @throws ReflectiveOperationException if an error occurs while evaluating the reflective parts of the object
		 */
		public abstract Object evaluate(final Identifier identifiers[], final PersistentStack<Object> context, final ContextAccess access, final PersistentStack<Indexed> indexes) throws ReflectiveOperationException;
	}

	private static final AtomicInteger DYN_INDEX = new AtomicInteger();

	// Reflected Methods
	private static final Method ACCESSOR_LOOKUP;
	private static final Method IDENTIFIER_GET_VALUE;
	private static final Method IDENTIFIER_GET_VALUE_BACKREACH;
	private static final Method IDENTIFIER_GET_VALUE_METHOD;
	private static final Method INDEXED_GET_INDEX;
	private static final Method INDEXED_HAS_NEXT;
	@SuppressWarnings("rawtypes")
	private static final Constructor<LinkedHashMap> LINKED_HASH_MAP_CTOR_INT;
	private static final Method MAP_PUT;
	private static final Method PERSISTENT_STACK_PEEK;
	private static final Method STRING_BUILDER_APPEND_OBJECT;
	private static final Constructor<StringBuilder> STRING_BUILDER_INIT_STRING;
	private static final Method STRING_VALUE_OF;

	static {
		try {
			ACCESSOR_LOOKUP = Accessor.class.getMethod("lookup", Object.class, Object.class);
			IDENTIFIER_GET_VALUE = Identifier.class.getMethod("getValue", Object.class);
			IDENTIFIER_GET_VALUE_BACKREACH = Identifier.class.getMethod("getValue", PersistentStack.class, Settings.ContextAccess.class);
			IDENTIFIER_GET_VALUE_METHOD = Identifier.class.getMethod("getValue", Object.class, Object[].class);
			INDEXED_GET_INDEX = Indexed.class.getMethod("getIndex");
			INDEXED_HAS_NEXT = Indexed.class.getMethod("hasNext");
			LINKED_HASH_MAP_CTOR_INT = LinkedHashMap.class.getConstructor(int.class);
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
	private static final Pattern IDENTIFIER_BACKREACH_PATTERN = Pattern.compile("(?<backreach>(?:[.][.]/)+)\\s*");
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(?:[.]/)*(?<identifier>(?:" + Identifier.PATTERN + "|`(?:[^`\\\\]|\\\\[`\\\\])+`)[(]?)\\s*");
	private static final Pattern INTERNAL_PATTERN = Pattern.compile("(?:[.]/)*(?<identifier>[.]\\p{L}*)\\s*");
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("(?<double>[0-9][0-9]*[.][0-9]+)\\s*");
	private static final Pattern LONG_PATTERN = Pattern.compile("(?:0[Xx](?<hexadecimal>[0-9A-Fa-f]+)|(?<decimal>[0-9]+))\\s*");
	private static final Pattern STRING_PATTERN = Pattern.compile("\"(?<string>(?:[^\"\\\\]|\\\\[\\\\\"'btnfr]|\\\\x[0-9A-Fa-f]{1,8}|\\\\u[0-9A-Fa-f]{4}|\\\\U[0-9A-Fa-f]{8})*)\"\\s*");
	private static final Pattern OPERATOR_PATTERN;

	static {
		final StringBuilder sb = new StringBuilder("(?<operator>");
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
		OPERATOR_PATTERN = Pattern.compile(sb.append(",)\\s*").toString());
	}

	/**
	 * Processes the specified operation. The operand stack will be updated with the results of evaluating the operator.
	 *
	 * @param identifiers the identifiers for the expression
	 * @param operands the operand stack for the expression
	 * @param operator the operator to evaluate
	 */
	private static void processOperation(final HashMap<Identifier, Integer> identifiers, final PersistentStack<Operand> operands, final Operator operator) {
		final Operand right;

		if (operator.has(Operator.NAVIGATION)) { // Navigation operator is handled during parsing
			return;
		} else if (operator.has(Operator.METHOD_CALL)) { // Check for a method call
			final MethodBuilder object = operands.peek(operator.getRightExpressions() + 1).builder;
			final Label end = object.newLabel();

			if (operator.has(Operator.SAFE)) {
				object.addCode(DUP).addBranch(IFNULL, end);
			}

			object.append(operands.peek(operator.getRightExpressions()).builder).addCode(SWAP);

			if (operator.getRightExpressions() == 0) {
				object.addCode(ACONST_NULL);
			} else {
				object.pushNewObject(Object.class, operator.getRightExpressions());

				// Convert all parameters to objects and store them in the array
				for (int i = 0; i < operator.getRightExpressions(); i++) {
					object.addCode(DUP).pushConstant(i).append(operands.peek(operator.getRightExpressions() - 1 - i).toObject(false)).addCode(AASTORE);
				}
			}

			operands.pop(operator.getRightExpressions() + 2).push(new Operand(Object.class, object.addInvoke(IDENTIFIER_GET_VALUE_METHOD).updateLabel(end)));
			return;
		} else if (operator.has(Operator.RIGHT_EXPRESSION)) {
			right = operands.pop();

			if (Entry.class.equals(right.type) && !operator.has(Operator.ALLOW_PAIRS)) {
				throw new RuntimeException("Invalid pair operator (':') in expression");
			}
		} else {
			right = null;
		}

		final Operand left = operator.has(Operator.LEFT_EXPRESSION) ? operands.pop() : null;

		switch (operator.getString()) {
		// Array / Map Operations
		case "[": case "?[": if (left != null) {
			if (!Object.class.equals(left.type)) {
				throw new RuntimeException("Unexpected '" + operator.getString() + "' operator applied to " + (left.type == null ? "numeric" : left.type.getName()) + " value, expecting map or array type value");
			}

			final Label end = left.builder.newLabel();

			if (operator.has(Operator.SAFE)) {
				left.builder.addCode(DUP).addBranch(IFNULL, end);
			}

			operands.push(new Operand(Object.class, left.builder.append(right.toObject(false)).addInvoke(ACCESSOR_LOOKUP).updateLabel(end)));
			break;
		}
		case "{": {
			int pairs = 0;

			// Find the number of pairs
			for (int i = 0; i < operator.getRightExpressions(); i++) {
				if (Entry.class.equals(operands.peek(i + pairs).type)) {
					pairs++;
					assert Entry.class.equals(operands.peek(i + pairs).type);
				}
			}

			if (pairs > 0) { // Create a map
				final int totalPairs = pairs;
				final MethodBuilder builder = new MethodBuilder().pushNewObject(LinkedHashMap.class).addCode(DUP).pushConstant((operator.getRightExpressions() * 4 + 2) / 3).addInvoke(LINKED_HASH_MAP_CTOR_INT);

				for (int i = operator.getRightExpressions() - 1, j = 0; i >= 0; j++, i--) {
					final Operand first = operands.peek(i + pairs);

					if (Entry.class.equals(first.type)) {
						builder.addCode(DUP).append(first.toObject(false)).append(operands.peek(i + --pairs).toObject(false)).addInvoke(MAP_PUT).addCode(POP);
					} else {
						builder.addCode(DUP).pushConstant(j).addPrimitiveConversion(int.class, Object.class).append(first.toObject(false)).addInvoke(MAP_PUT).addCode(POP);
					}
				}

				operands.pop(operator.getRightExpressions() + totalPairs).push(new Operand(Object.class, builder));
			} else { // Create an array
				final MethodBuilder builder = new MethodBuilder().pushNewObject(Object.class, operator.getRightExpressions());

				for (int i = operator.getRightExpressions() - 1, j = 0; i >= 0; j++, i--) {
					builder.addCode(DUP).pushConstant(j).append(operands.peek(i).toObject(false)).addCode(AASTORE);
				}

				operands.pop(operator.getRightExpressions()).push(new Operand(Object.class, builder));
			}

			break;
		}
		case "..": {
			final Label decreasing = left.builder.newLabel();
			final Label increasingLoop = left.builder.newLabel();
			final Label decreasingLoop = left.builder.newLabel();
			final Label end = left.builder.newLabel();

			operands.push(new Operand(Object.class, left.toNumeric(false).addCode(POP, POP2, L2I, DUP).addAccess(ISTORE, Evaluable.FIRST_LOCAL).append(right.toNumeric(false)).addCode(POP, POP2, L2I, DUP2).addBranch(IF_ICMPGT, decreasing)
					.addCode(SWAP, ISUB, ICONST_1, IADD, NEWARRAY, (byte)10, DUP).addAccess(ASTORE, Evaluable.FIRST_LOCAL + 1).addCode(ICONST_0).updateLabel(increasingLoop).addCode(DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL + 1).addCode(SWAP, DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL).addCode(IADD, IASTORE, ICONST_1, IADD, DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL + 1).addCode(ARRAYLENGTH).addBranch(IF_ICMPLT, increasingLoop).addBranch(GOTO, end)
					.updateLabel(decreasing).addCode(ISUB, ICONST_1, IADD, NEWARRAY, (byte)10, DUP).addAccess(ASTORE, Evaluable.FIRST_LOCAL + 1).addCode(ICONST_0).updateLabel(decreasingLoop).addCode(DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL + 1).addCode(SWAP, DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL).addCode(SWAP, ISUB, IASTORE, ICONST_1, IADD, DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL + 1).addCode(ARRAYLENGTH).addBranch(IF_ICMPLT, decreasingLoop)
					.updateLabel(end).addCode(POP)));
			break;
		}

		// Math Operations
		case "+":
			if (left == null) { // Unary +, basically do nothing except require a number
				operands.push(new Operand(null, right.toNumeric(true)));
			} else if (StringBuilder.class.equals(left.type)) { // Check for string concatenation
				operands.push().builder.append(right.toObject(false)).addInvoke(STRING_BUILDER_APPEND_OBJECT);
			} else if (String.class.equals(left.type) || String.class.equals(right.type) || StringBuilder.class.equals(right.type)) {
				operands.push(new Operand(StringBuilder.class, left.toObject(false).pushNewObject(StringBuilder.class).addCode(DUP_X1, SWAP).addInvoke(STRING_VALUE_OF).addInvoke(STRING_BUILDER_INIT_STRING).append(right.toObject(false)).addInvoke(STRING_BUILDER_APPEND_OBJECT)));
			} else if ((left.type == null || (left.type.isPrimitive() && !boolean.class.equals(left.type))) && (right.type == null || (right.type.isPrimitive() && !boolean.class.equals(right.type)))) { // Mathematical addition
				operands.push(left.execMathOp(right, IADD, LADD, DADD));
			} else { // String concatenation, mathematical addition, or invalid
				final Label notStringBuilder = left.builder.newLabel();
				final Label isString = left.builder.newLabel();
				final Label notString = left.builder.newLabel();
				final Label end = left.builder.newLabel();
				final Operand result = new Operand(Object.class, left.toObject(false).append(right.toObject(false)).addCode(SWAP, DUP_X1).addInstanceOfCheck(StringBuilder.class).addBranch(IFEQ, notStringBuilder).addCode(SWAP).addCast(StringBuilder.class).addCode(SWAP).addInvoke(STRING_BUILDER_APPEND_OBJECT).addBranch(GOTO, end)
					.updateLabel(notStringBuilder).addCode(SWAP, DUP_X1).addInstanceOfCheck(String.class).addBranch(IFNE, isString).addCode(DUP).addInstanceOfCheck(String.class).addBranch(IFNE, isString).addCode(DUP).addInstanceOfCheck(StringBuilder.class).addBranch(IFEQ, notString)
					.updateLabel(isString).addCode(SWAP).pushNewObject(StringBuilder.class).addCode(DUP_X2, SWAP).addInvoke(STRING_VALUE_OF).addInvoke(STRING_BUILDER_INIT_STRING).addInvoke(STRING_BUILDER_APPEND_OBJECT).addBranch(GOTO, end)
					.updateLabel(notString).addAccess(ASTORE, Evaluable.FIRST_LOCAL));

				result.execMathOp(new Operand(Object.class, new MethodBuilder().addAccess(ALOAD, Evaluable.FIRST_LOCAL)), IADD, LADD, DADD).toObject(false).updateLabel(end);
				operands.push(result);
			}

			break;
		case "-":
			if (left == null) {
				operands.push(right.execIntegralOp(INEG, LNEG));
			} else {
				operands.push(left.execMathOp(right, ISUB, LSUB, DSUB));
			}

			break;
		case "*":
			operands.push(left.execMathOp(right, IMUL, LMUL, DMUL));
			break;
		case "/":
			operands.push(left.execMathOp(right, IDIV, LDIV, DDIV));
			break;
		case "%":
			operands.push(left.execMathOp(right, IREM, LREM, DREM));
			break;

		// Integral Operations
		case "~": // Treat as x ^ -1
			operands.push(right.execIntegralOp(new Operand(int.class, new MethodBuilder().pushConstant(-1)), IXOR, LXOR, false));
			break;
		case "<<":
			operands.push(left.execIntegralOp(right, ISHL, LSHL, true));
			break;
		case ">>":
			operands.push(left.execIntegralOp(right, ISHR, LSHR, true));
			break;
		case ">>>":
			operands.push(left.execIntegralOp(right, IUSHR, LUSHR, true));
			break;
		case "&":
			operands.push(left.execIntegralOp(right, IAND, LAND, false));
			break;
		case "^":
			operands.push(left.execIntegralOp(right, IXOR, LXOR, false));
			break;
		case "|":
			operands.push(left.execIntegralOp(right, IOR, LOR, false));
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

			operands.push(new Operand(boolean.class, left.toBoolean().addBranch(IFNE, notZero).addCode(ICONST_0).addBranch(GOTO, end).updateLabel(notZero).append(right.builder).updateLabel(end)));
			break;
		}
		case "||": {
			final Label zero = left.builder.newLabel();
			final Label end = left.builder.newLabel();

			operands.push(new Operand(boolean.class, left.toBoolean().addBranch(IFEQ, zero).addCode(ICONST_1).addBranch(GOTO, end).updateLabel(zero).append(right.builder).updateLabel(end)));
			break;
		}

		// Comparison Operators
		case "<=":
			operands.push(left.execCompareOp(right, IF_ICMPLE, IFLE));
			break;
		case ">=":
			operands.push(left.execCompareOp(right, IF_ICMPGE, IFGE));
			break;
		case "<":
			operands.push(left.execCompareOp(right, IF_ICMPLT, IFLT));
			break;
		case ">":
			operands.push(left.execCompareOp(right, IF_ICMPGT, IFGT));
			break;
		case "==":
			operands.push(left.execCompareOp(right, IF_ICMPEQ, IFEQ));
			break;
		case "!=":
			operands.push(left.execCompareOp(right, IF_ICMPNE, IFNE));
			break;

		// Ternary Operations
		case "??": case "?:": {
			final Label end = left.builder.newLabel();

			operands.push(new Operand(Object.class, left.toObject(false).addCode(DUP).addBranch(IFNONNULL, end).addCode(POP).append(right.toObject(false)).updateLabel(end)));
			break;
		}
		case "?": {
			if (!Entry.class.equals(right.type)) {
				throw new RuntimeException("Incomplete ternary operator, missing ':'");
			}

			assert Entry.class.equals(left.type);
			assert !operands.isEmpty();

			final Label isFalse = left.builder.newLabel();
			final Label end = left.builder.newLabel();

			operands.push(new Operand(Object.class, operands.pop().toBoolean().addBranch(IFEQ, isFalse).append(left.builder).addBranch(GOTO, end).updateLabel(isFalse).append(right.builder).updateLabel(end)));
			break;
		}

		case ":":
			operands.push(new Operand(Entry.class, left.toObject(false)));
			operands.push(new Operand(Entry.class, right.toObject(false)));
			break;

		case ",":
			if (left.type == null) {
				left.builder.addCode(POP, POP2, POP2);
			} else {
				left.builder.addCode(long.class.equals(left.type) || double.class.equals(left.type) ? POP2 : POP);
			}

			operands.push(new Operand(right.type, left.builder.append(right.builder)));
			break;

		default:
			throw new RuntimeException("Unrecognized operator '" + operator.getString() + "' while parsing expression");
		}
	}

	private final String originalString;
	private final Identifier identifiers[];
	private final Evaluable evaluable;
	private ErrorLogger errorLogger = new ErrorLogger() {
		@Override
		public void log(final Throwable error) {
			if (error.getMessage() == null) {
				System.err.println("Encountered " + error.getClass().getName() + " while processing expression \"" + originalString + "\"");
			} else {
				System.err.println("Encountered " + error.getClass().getName() + " while processing expression \"" + originalString + "\": " + error.getMessage());
			}
		}
	};

	/**
	 * Creates a new expression.
	 *
	 * @param expressionString the trimmed, advanced expression string
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
				final boolean lastNavigation = (lastOperator != null && lastOperator.has(Operator.NAVIGATION));
				final String token;
				int backreach = 0;

				// Check for backreach
				if (matcher.usePattern(IDENTIFIER_BACKREACH_PATTERN).lookingAt()) {
					if (hasLeftExpression || lastNavigation) {
						throw new RuntimeException("Unexpected backreach in expression at offset " + matcher.regionStart());
					}

					backreach = (matcher.end("backreach") - matcher.regionStart()) / 3;

					if (backreach > maxBackreach) {
						throw new RuntimeException("Backreach too far (max: " + maxBackreach + ") in expression at offset " + matcher.regionStart());
					}

					matcher.region(matcher.end(), length);
				}

				// Check for identifier or literals
				if (!hasLeftExpression && matcher.usePattern(IDENTIFIER_PATTERN).lookingAt()) { // Identifier
					token = matcher.group("identifier");

					// Check for keywords that look like literals
					if ("true".equals(token) && backreach == 0 && !lastNavigation) {
						operands.push(new Operand(boolean.class, new MethodBuilder().pushConstant(1)));
					} else if ("false".equals(token) && backreach == 0 && !lastNavigation) {
						operands.push(new Operand(boolean.class, new MethodBuilder().pushConstant(0)));
					} else if ("null".equals(token) && backreach == 0 && !lastNavigation) {
						operands.push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
					} else if (token.endsWith("(")) { // Method
						final String name = token.startsWith("`") ? token.substring(1, token.length() - 2).replaceAll("\\\\(.)", "\\1") : token.substring(0, token.length() - 1);
						final Identifier identifier = new Identifier(backreach, name, true);
						Integer index = identifiers.get(identifier);

						if (index == null) {
							index = identifiers.size();
							identifiers.put(identifier, index);
						}

						if (!lastNavigation) {
							// Create a new output formed by invoking identifiers[index].getValue(context.peek(backreach), ...)
							operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK)));
							operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD)));
							lastOperator = operators.push(Operator.createMethod(name, false));
						} else {
							// Create a new output formed by invoking identifiers[index].getValue(object, ...)
							operands.push(new Operand(Object.class, operands.pop().toObject(false)));
							operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD)));
							lastOperator = operators.push(Operator.createMethod(name, lastOperator.has(Operator.SAFE)));
						}

						continue nextToken;
					} else { // Identifier
						final String name = token.startsWith("`") ? token.substring(1, token.length() - 1).replaceAll("\\\\(.)", "\\1") : token;
						final Identifier identifier = new Identifier(backreach, name, false);
						Integer index = identifiers.get(identifier);

						if (index == null) {
							index = identifiers.size();
							identifiers.put(identifier, index);
						}

						if (!lastNavigation) {
							// Create a new output formed by invoking identifiers[index].getValue(context, access)
							operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).addCode(Evaluable.LOAD_ACCESS).addInvoke(IDENTIFIER_GET_VALUE_BACKREACH)));
							// Create a new output formed by invoking identifiers[index].getValue(object)
						} else if (operators.pop().has(Operator.SAFE)) {
							final Label end = operands.peek().builder.newLabel();
							operands.push(new Operand(Object.class, operands.pop().toObject(false).addCode(DUP).addBranch(IFNULL, end).addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE).updateLabel(end)));
						} else {
							operands.push(new Operand(Object.class, operands.pop().toObject(false).addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE)));
						}
					}
				} else if (!hasLeftExpression && matcher.usePattern(INTERNAL_PATTERN).lookingAt()) { // Internal identifier
					token = matcher.group("identifier");

					if (".index".equals(token)) {
						operands.push(new Operand(int.class, new MethodBuilder().addCode(Evaluable.LOAD_INDEXES).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK).addInvoke(INDEXED_GET_INDEX)));
					} else if (".isFirst".equals(token)) {
						operands.push(new Operand(int.class, new MethodBuilder().addCode(Evaluable.LOAD_INDEXES).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK).addInvoke(INDEXED_GET_INDEX)));
						processOperation(identifiers, operands, Operator.get("!"));
					} else if (".hasNext".equals(token)) {
						operands.push(new Operand(boolean.class, new MethodBuilder().addCode(Evaluable.LOAD_INDEXES).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK).addInvoke(INDEXED_HAS_NEXT)));
					} else if (".".equals(token)) {
						operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK)));
					} else { // Unknown internal identifier = null
						operands.push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
					}
				} else if (backreach != 0 || lastNavigation) { // Any backreach must have an identifier associated with it, and identifiers must follow the member selection operator
					throw new RuntimeException("Invalid identifier in expression at offset " + matcher.regionStart());
				} else if (matcher.hitEnd()) {
					break;
				} else if (matcher.usePattern(DOUBLE_PATTERN).lookingAt()) { // Double literal
					token = matcher.group("double");
					operands.push(new Operand(double.class, new MethodBuilder().pushConstant(Double.parseDouble(token))));
				} else if (matcher.usePattern(LONG_PATTERN).lookingAt()) { // Long literal
					token = matcher.group("decimal");
					final long value = token == null ? Long.parseLong(matcher.group("hexadecimal"), 16) : Long.parseLong(token);

					if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
						operands.push(new Operand(long.class, new MethodBuilder().pushConstant(value)));
					} else {
						operands.push(new Operand(int.class, new MethodBuilder().pushConstant((int)value)));
					}
				} else if (matcher.usePattern(STRING_PATTERN).lookingAt()) { // String literal
					token = matcher.group("string");
					int backslash = 0;

					if (token == null) {
						operands.push(new Operand(String.class, new MethodBuilder().pushConstant("")));
					} else if ((backslash = token.indexOf('\\')) < 0) {
						operands.push(new Operand(String.class, new MethodBuilder().pushConstant(token)));
					} else { // Find escape sequences and replace them with the proper character sequences
						final StringBuilder sb = new StringBuilder(token.length());
						int start = 0;

						do {
							sb.append(token, start, backslash);

							if (backslash + 1 >= token.length()) {
								throw new RuntimeException("Invalid '\\' at end of string literal in expression at offset " + matcher.regionStart());
							}

							switch (token.charAt(backslash + 1)) {
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

								for (int i = 0; i < 8 && backslash + 2 < token.length(); i++, backslash++) {
									final int digit = token.charAt(backslash + 2);

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
								if (backslash + 5 >= token.length()) {
									throw new RuntimeException("Invalid '\\u' at end of string literal in expression at offset " + matcher.regionStart());
								}

								sb.append(Character.toChars(Integer.parseInt(token.substring(backslash + 2, backslash + 6), 16)));
								backslash += 4;
								break;

							case 'U':
								if (backslash + 9 >= token.length()) {
									throw new RuntimeException("Invalid '\\U' at end of string literal in expression at offset " + matcher.regionStart());
								}

								sb.append(Character.toChars(Integer.parseInt(token.substring(backslash + 2, backslash + 10), 16)));
								backslash += 8;
								break;

							default: throw new RuntimeException("Invalid character (" + token.charAt(backslash + 1) + ") following '\\' in string literal (offset: " + backslash + ") in expression at offset " + matcher.regionStart());
							}

							start = backslash + 2;
						} while ((backslash = token.indexOf('\\', start)) >= 0);

						operands.push(new Operand(String.class, new MethodBuilder().pushConstant(sb.append(token, start, token.length()).toString())));
					}
				} else if (matcher.usePattern(OPERATOR_PATTERN).lookingAt()) { // Operator
					token = matcher.group("operator");
					Operator operator = Operator.get(token);

					for (; operator != null && hasLeftExpression != operator.has(Operator.LEFT_EXPRESSION); ) {
						operator = operator.next();
					}

					if (operator != null) {
						// Shunting-yard Algorithm
						while (!operators.isEmpty() && !operators.peek().has(Operator.X_RIGHT_EXPRESSIONS) && !"(".equals(operators.peek().getString()) && (operators.peek().getPrecedence() < operator.getPrecedence() || (operators.peek().getPrecedence() == operator.getPrecedence() && operator.isLeftAssociative()))) {
							if (operators.peek().getClosingString() != null) {
								throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.regionStart() + ", expecting '" + operators.peek().getClosingString() + "'");
							}

							processOperation(identifiers, operands, operators.pop());
						}

						if (!operators.isEmpty() && operators.peek().has(Operator.X_RIGHT_EXPRESSIONS) && ",".equals(token)) {
							if (!hasLeftExpression) { // Check for invalid and empty expressions
								throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.regionStart());
							}

							operators.push(operators.pop().addRightExpression());
						} else {
							operators.push(operator);
						}

						lastOperator = operator;
						continue nextToken;
					}

					if (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION) || ",".equals(lastOperator.getString())) { // Check if this token ends an expression on the stack
						while (!operators.isEmpty()) {
							if (operators.peek().getClosingString() != null) {
								Operator closedOperator = operators.pop();

								if (!closedOperator.getClosingString().equals(token)) { // Check for a mismatched closing string
									throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.regionStart() + ", expecting '" + closedOperator.getClosingString() + "'");
								} else if (closedOperator.has(Operator.X_RIGHT_EXPRESSIONS)) { // Process multi-argument operators
									if (hasLeftExpression) { // Allow trailing commas
										closedOperator = closedOperator.addRightExpression();
									}

									processOperation(identifiers, operands, closedOperator);
								} else if (!hasLeftExpression) { // Check for empty expressions
									throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.regionStart());
								} else if (closedOperator.has(Operator.LEFT_EXPRESSION)) { // If the token is not a parenthetical, process the operation
									processOperation(identifiers, operands, closedOperator);
								}

								lastOperator = closedOperator.getClosingOperator();
								continue nextToken;
							}

							processOperation(identifiers, operands, operators.pop());
						}
					}

					throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.regionStart());
				} else {
					throw new RuntimeException("Unrecognized identifier in expression at offset " + matcher.regionStart());
				}

				// Check for multiple consecutive identifiers or literals
				if (hasLeftExpression) {
					throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.regionStart() + ", expecting operator");
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
			return evaluable.evaluate(identifiers, context, access, indices);
		} catch (final Throwable t) { // Don't let any exceptions escape
			errorLogger.log(t);
		}

		return null;
	}

	/**
	 * Sets the error logger for the expression.
	 *
	 * @param errorLogger the error logger to use for this expression
	 * @return this expression
	 */
	public Expression setErrorLogger(final ErrorLogger errorLogger) {
		this.errorLogger = errorLogger;
		return this;
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
