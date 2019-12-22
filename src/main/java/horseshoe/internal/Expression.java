package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

	private static class Operand {
		public static final int INT_TYPE    = 0b00;
		public static final int LONG_TYPE   = 0b01;
		public static final int DOUBLE_TYPE = 0b11;

		private static final Set<Class<?>> ALLOWED_TYPES = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(
				boolean.class, int.class, long.class, double.class, Object.class, String.class, StringBuilder.class, null)));

		// Reflected Methods
		private static final Method DOUBLE_VALUE;
		private static final Method LONG_VALUE;
		private static final Method INT_VALUE;
		private static final Method CHAR_VALUE;

		static {
			try {
				DOUBLE_VALUE = Number.class.getMethod("doubleValue");
				LONG_VALUE = Long.class.getMethod("longValue");
				INT_VALUE = Number.class.getMethod("intValue");
				CHAR_VALUE = Character.class.getMethod("charValue");
			} catch (final ReflectiveOperationException e) {
				throw new RuntimeException("Bad reflection operation: " + e.getMessage(), e);
			}
		}

		public final Class<?> type; // null indicates a stack with { long longVal, double doubleVal, int type } on top
		public final MethodBuilder builder;

		public Operand(final Class<?> type, final MethodBuilder builder) {
			this.type = type;
			this.builder = builder;

			assert ALLOWED_TYPES.contains(type);
		}

		/**
		 * Converts the operand to a number. The resulting operand will have a type of null or a primitive type. A failure will be logged in the case that the conversion fails.
		 *
		 * @return the resulting primitive, numeric operand
		 */
		private Operand toNumeric() {
			if (type == null || type.isPrimitive()) {
				return this;
			}

			final Label notNumber = builder.newLabel();
			final Label notDouble = builder.newLabel();
			final Label notLong = builder.newLabel();
			final Label notFloat = builder.newLabel();
			final Label getChar = builder.newLabel();
			final Label success = builder.newLabel();

			// TODO: Handle Atomic* & Big*
			return new Operand(null, logError(builder.addCode(DUP, DUP).addInstanceOfCheck(Number.class).addBranch(IFEQ, notNumber)
					.addInstanceOfCheck(Double.class).addBranch(IFEQ, notDouble).addCast(Double.class).addInvoke(DOUBLE_VALUE).addCode(LCONST_0, DUP2_X2, POP2).pushConstant(DOUBLE_TYPE).addBranch(GOTO, success)
					.updateLabel(notDouble).addCode(DUP).addInstanceOfCheck(Long.class).addBranch(IFEQ, notLong).addCast(Long.class).addInvoke(LONG_VALUE).addCode(DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, success)
					.updateLabel(notLong).addCode(DUP).addInstanceOfCheck(Float.class).addBranch(IFEQ, notFloat).addCast(Float.class).addInvoke(DOUBLE_VALUE).addCode(LCONST_0, DUP2_X2, POP2).pushConstant(DOUBLE_TYPE).addBranch(GOTO, success)
					.updateLabel(notFloat).addCast(Number.class).addInvoke(INT_VALUE).addCode(I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, success)
					.updateLabel(notNumber).addInstanceOfCheck(Character.class).addBranch(IFNE, getChar), "Invalid object, expecting boxed numeric primitive").addCode(ACONST_NULL, ARETURN)
					.updateLabel(getChar).addCast(Character.class).addInvoke(CHAR_VALUE).addCode(I2L, DCONST_0).pushConstant(INT_TYPE).updateLabel(success));
		}

		/**
		 * Generates an operand that is the result of the specified mathematical operation on this operand and the other specified operand.
		 *
		 * @param other the other operand used to calculate the result
		 * @param intOpcode the opcode used to compute the result of two int operands
		 * @param longOpcode the opcode used to compute the result of two long operands
		 * @param doubleOpcode the opcode used to compute the result of two double operands
		 * @return the resulting operand from the operation
		 */
		public Operand execMathOp(final Operand other, final byte intOpcode, final byte longOpcode, final byte doubleOpcode) {
			for (final Operand op : new Operand[] { this, other }) {
				if (op.type != null) {
					assert op.type.isPrimitive();

					if (boolean.class.equals(op.type)) {
						throw new RuntimeException("Unexpected " + op.type.getName() + " value, expecting numeric value");
					} else if (double.class.equals(op.type)) {
						op.builder.addCode(LCONST_0, DUP2_X2, POP2).pushConstant(DOUBLE_TYPE);
					} else if (long.class.equals(op.type)) {
						op.builder.addCode(DCONST_0).pushConstant(LONG_TYPE);
					} else {
						op.builder.addCode(I2L, DCONST_0).pushConstant(INT_TYPE);
					}
				}
			}

			final Label notInt = builder.newLabel();
			final Label isFloating = builder.newLabel();
			final Label process2nd = builder.newLabel();
			final Label use2ndDouble = builder.newLabel();
			final Label success = builder.newLabel();

			return new Operand(null, builder.append(other.builder).addAccess(ISTORE, Evaluable.FIRST_LOCAL).addAccess(DSTORE, Evaluable.FIRST_LOCAL + 1).addAccess(LSTORE, Evaluable.FIRST_LOCAL + 3)
					.addCode(DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL).addCode(IOR, DUP).addBranch(IFNE, notInt).addCode(POP2, POP2, L2I).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(L2I, intOpcode, I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, success)
					.updateLabel(notInt).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating).addCode(POP, POP2).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(longOpcode, DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, success)
					.updateLabel(isFloating).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, process2nd).addCode(POP2, DUP2, L2D).addAccess(DLOAD, Evaluable.FIRST_LOCAL + 1).addCode(doubleOpcode).pushConstant(DOUBLE_TYPE).addBranch(GOTO, success)
					.updateLabel(process2nd).addAccess(ILOAD, Evaluable.FIRST_LOCAL).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, use2ndDouble).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(L2D, doubleOpcode).pushConstant(DOUBLE_TYPE).addBranch(GOTO, success)
					.updateLabel(use2ndDouble).addAccess(DLOAD, Evaluable.FIRST_LOCAL + 1).addCode(doubleOpcode).pushConstant(DOUBLE_TYPE).updateLabel(success));
		}

		/**
		 * Generates an operand that is the result of the specified integral operation on this operand and the other specified operand.
		 *
		 * @param other the other operand used to calculate the result
		 * @param intOpcode the opcode used to compute the result of two int operands
		 * @param longOpcode the opcode used to compute the result of two long operands
		 * @param secondOperandInt true if the second operand should always be interpreted as an int, otherwise false
		 * @return the resulting operand from the operation
		 */
		public Operand execIntegralOp(final Operand other, final byte intOpcode, final byte longOpcode, final boolean secondOperandInt) {
			for (final Operand op : new Operand[] { this, other }) {
				if (op.type != null) {
					assert op.type.isPrimitive();

					if (boolean.class.equals(op.type) || double.class.equals(op.type) || float.class.equals(op.type)) {
						throw new RuntimeException("Unexpected " + op.type.getName() + " value, expecting integral value");
					} else if (long.class.equals(op.type)) {
						op.builder.addCode(DCONST_0).pushConstant(LONG_TYPE);
					} else {
						op.builder.addCode(I2L, DCONST_0).pushConstant(INT_TYPE);
					}
				}
			}

			final Label notInt = builder.newLabel();
			final Label isFloating = builder.newLabel();
			final Label success = builder.newLabel();

			return new Operand(null, logError(builder.append(other.builder).addAccess(ISTORE, Evaluable.FIRST_LOCAL).addAccess(DSTORE, Evaluable.FIRST_LOCAL + 1).addAccess(LSTORE, Evaluable.FIRST_LOCAL + 3)
					.addCode(DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL).addCode(IOR, DUP).addBranch(IFNE, notInt).addCode(POP2, POP2, L2I).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(L2I, intOpcode, I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, success)
					.updateLabel(notInt).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating).addCode(POP, POP2).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode((secondOperandInt ? L2I : NOP), longOpcode, DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, success)
					.updateLabel(isFloating), "Unexpected floating-point value, expecting integral value").addCode(ACONST_NULL, ARETURN).updateLabel(success));
		}

		/**
		 * Converts the operand to an object, combining all builders into the single builder specified. All existing builders are cleared.
		 *
		 * @param builder the builder used to combine all other builders
		 * @return the combined builder that was passed to the method
		 */
		public MethodBuilder toObject(final boolean generateReturn) {
			if (type == null) {
				final MethodBuilder.Label isFloating = builder.newLabel();
				final MethodBuilder.Label isLong = builder.newLabel();

				if (generateReturn) {
					builder.addCode(DUP).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating)
						.addBranch(IFNE, isLong).addCode(POP2, L2I).addPrimitiveConversion(int.class, Object.class).addCode(ARETURN)
						.updateLabel(isLong).addCode(POP2).addPrimitiveConversion(long.class, Object.class).addCode(ARETURN)
						.updateLabel(isFloating).addCode(POP).addPrimitiveConversion(double.class, Object.class);
				} else {
					final MethodBuilder.Label end = builder.newLabel();

					builder.addCode(DUP).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating)
						.addBranch(IFNE, isLong).addCode(POP2, L2I).addPrimitiveConversion(int.class, Object.class).addBranch(GOTO, end)
						.updateLabel(isLong).addCode(POP2).addPrimitiveConversion(long.class, Object.class).addBranch(GOTO, end)
						.updateLabel(isFloating).addCode(POP, DUP2_X2, POP2, POP2).addPrimitiveConversion(double.class, Object.class).updateLabel(end);
				}
			} else if (type.isPrimitive()) {
				builder.addPrimitiveConversion(type, Object.class);
			}

			return generateReturn ? builder.addCode(ARETURN) : builder;
		}

		@Override
		public String toString() {
			return builder == null ? "" : builder.toString();
		}
	}

	private static final AtomicInteger DYN_INDEX = new AtomicInteger();
	private static final Operator NAVIGATE_OPERATOR = Operator.get(".");

	// Reflected Methods
	private static final Method IDENTIFIER_GET_VALUE;
	private static final Method IDENTIFIER_GET_VALUE_BACKREACH;
	private static final Method IDENTIFIER_GET_VALUE_METHOD;
	private static final Method INDEXED_GET_INDEX;
	private static final Method INDEXED_HAS_NEXT;
	private static final Method PERSISTENT_STACK_PEEK;
	private static final Method STRING_BUILDER_APPEND_OBJECT;
	private static final Constructor<StringBuilder> STRING_BUILDER_INIT_STRING;

	static {
		try {
			IDENTIFIER_GET_VALUE = Identifier.class.getMethod("getValue", Object.class);
			IDENTIFIER_GET_VALUE_BACKREACH = Identifier.class.getMethod("getValue", PersistentStack.class, Settings.ContextAccess.class);
			IDENTIFIER_GET_VALUE_METHOD = Identifier.class.getMethod("getValue", Object.class, Object[].class);
			INDEXED_GET_INDEX = Indexed.class.getMethod("getIndex");
			INDEXED_HAS_NEXT = Indexed.class.getMethod("hasNext");
			PERSISTENT_STACK_PEEK = PersistentStack.class.getMethod("peek", int.class);
			STRING_BUILDER_APPEND_OBJECT = StringBuilder.class.getMethod("append", Object.class);
			STRING_BUILDER_INIT_STRING = StringBuilder.class.getConstructor(String.class);
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException("Bad reflection operation: " + e.getMessage(), e);
		}
	}

	// The patterns used for parsing the grammar
	private static final Pattern IDENTIFIER_BACKREACH_PATTERN = Pattern.compile("\\s*([.][.]/)+");
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\s*([.][/.])*(([A-Za-z_\\$][A-Za-z0-9_\\$]*|`([^`\\\\]|\\\\[`\\\\])+`)[(]?)");
	private static final Pattern INTERNAL_PATTERN = Pattern.compile("\\s*([.][/.])*([.][A-Za-z]*)");
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("\\s*([0-9][0-9]*[.][0-9]+)");
	private static final Pattern LONG_PATTERN = Pattern.compile("\\s*([0-9](x[0-9A-Fa-f]+|[0-9]*))");
	private static final Pattern STRING_PATTERN = Pattern.compile("\\s*\"(([^\"\\\\]|\\\\[\\\\\"'btnfr]|\\\\x[0-9A-Fa-f]{1,8}|\\\\u[0-9A-Fa-f]{4}|\\\\U[0-9A-Fa-f]{8})*)\"");
	private static final Pattern OPERATOR_PATTERN;

	static {
		final StringBuilder sb = new StringBuilder("\\s*(");
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

		sb.setLength(sb.length() - 1);
		OPERATOR_PATTERN = Pattern.compile(sb.append(")").toString());
	}

	/**
	 * Generates the appropriate instructions to log an error.
	 *
	 * @param mb the builder to use for logging the error
	 * @param error the error to log
	 * @return the method builder
	 */
	private static MethodBuilder logError(final MethodBuilder mb, final String error) {
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
		} else if (operator.has(Operator.RIGHT_EXPRESSION)) {
			right = new Operand[1];
			right[0] = operands.pop();
		} else {
			right = null;
		}

		final Operand left = operator.has(Operator.LEFT_EXPRESSION) ? operands.pop() : null;

		switch (operator.getString()) {
		// Math Operations
		case "+":
			if (left == null) {
			// TODO: Unary plus
			} else if (StringBuilder.class.equals(left.type)) {
				operands.push();
				left.builder.append(right[0].toObject(false)).addInvoke(STRING_BUILDER_APPEND_OBJECT);
			} else if (String.class.equals(left.type)) {
				operands.push(new Operand(StringBuilder.class, left.builder.pushNewObject(StringBuilder.class).addCode(DUP_X1, SWAP).addInvoke(STRING_BUILDER_INIT_STRING).append(right[0].toObject(false)).addInvoke(STRING_BUILDER_APPEND_OBJECT)));
			} else if (left.type == null) { // TODO: fix
				operands.push(left.execMathOp(right[0].toNumeric(), IADD, LADD, DADD));
			} else { // TODO: fix
				operands.push(left.toNumeric().execMathOp(right[0].toNumeric(), IADD, LADD, DADD));
			}

			break; // TODO (See '*')
		case "-":
			if (left == null) {
			// TODO: Unary minus
			} else {
				operands.push(left.toNumeric().execMathOp(right[0].toNumeric(), ISUB, LSUB, DSUB));
			}

			break;
		case "*":
			operands.push(left.toNumeric().execMathOp(right[0].toNumeric(), IMUL, LMUL, DMUL));
			break;
		case "/":
			operands.push(left.toNumeric().execMathOp(right[0].toNumeric(), IDIV, LDIV, DDIV));
			break;
		case "%":
			operands.push(left.toNumeric().execMathOp(right[0].toNumeric(), IREM, LREM, DREM));
			break;

		// Integral Operations
		case "<<":
			operands.push(left.toNumeric().execIntegralOp(right[0].toNumeric(), ISHL, LSHL, true));
			break;
		case ">>":
			operands.push(left.toNumeric().execIntegralOp(right[0].toNumeric(), ISHR, LSHR, true));
			break;
		case ">>>":
			operands.push(left.toNumeric().execIntegralOp(right[0].toNumeric(), IUSHR, LUSHR, true));
			break;
		case "&":
			operands.push(left.toNumeric().execIntegralOp(right[0].toNumeric(), IAND, LAND, false));
			break;
		case "^":
			operands.push(left.toNumeric().execIntegralOp(right[0].toNumeric(), IXOR, LXOR, false));
			break;
		case "|":
			operands.push(left.toNumeric().execIntegralOp(right[0].toNumeric(), IOR, LOR, false));
			break;

		case ",": {
			if (left.type == null) {
				left.builder.addCode(POP, POP2, POP2);
			} else {
				left.builder.addCode(long.class.equals(left.type) || double.class.equals(left.type) ? POP2 : POP);
			}

			operands.push(new Operand(right[0].type, left.builder.append(right[0].builder)));
			break;
		}

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
					final String literal = matcher.group(1);
					final long value = Long.parseLong(literal, literal.contains("x") ? 16 : 10);

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
					final String token = matcher.group(1);
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

						if (!operators.isEmpty() && operators.peek().has(Operator.X_RIGHT_EXPRESSIONS) && ",".equals(operator.getString())) {
							if (!hasLeftExpression) { // Check for invalid and empty expressions
								throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.start(1));
							}

							operators.peek().addRightExpression();
						} else {
							operators.push(operator);
						}

						lastOperator = operator;
						continue nextToken;
					}

					if (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION)) { // Check if this token ends an expression on the stack
						while (!operators.isEmpty()) {
							if (operators.peek().getClosingString() != null) {
								final Operator closedOperator = operators.pop();

								if (!closedOperator.getClosingString().equals(token)) { // Check for a mismatched closing string
									throw new RuntimeException("Unexpected '" + token + "' in expression at offset " + matcher.start(1) + ", expecting '" + closedOperator.getClosingString() + "'");
								} else if (closedOperator.has(Operator.X_RIGHT_EXPRESSIONS)) { // Process multi-argument operators
									if (hasLeftExpression) { // Allow trailing commas
										closedOperator.addRightExpression().getRightExpressions();
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
