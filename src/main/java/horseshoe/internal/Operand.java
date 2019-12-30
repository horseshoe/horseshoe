package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import horseshoe.internal.Expression.Evaluable;
import horseshoe.internal.MethodBuilder.Label;

final class Operand {

	public static final int INT_TYPE    = 0b00;
	public static final int LONG_TYPE   = 0b01;
	public static final int DOUBLE_TYPE = 0b11;

	private static final Set<Class<?>> ALLOWED_TYPES = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(
			boolean.class, int.class, long.class, double.class, Object.class, String.class, StringBuilder.class, Entry.class, null)));

	// Reflected Methods
	private static final Method BOOLEAN_VALUE;
	private static final Method DOUBLE_VALUE;
	private static final Method LONG_VALUE;
	private static final Method INT_VALUE;
	private static final Method COMPARE_TO;
	private static final Method EQUALS;
	private static final Method TO_STRING;

	static {
		try {
			BOOLEAN_VALUE = Boolean.class.getMethod("booleanValue");
			DOUBLE_VALUE = Number.class.getMethod("doubleValue");
			LONG_VALUE = Long.class.getMethod("longValue");
			INT_VALUE = Number.class.getMethod("intValue");
			COMPARE_TO = Comparable.class.getMethod("compareTo", Object.class);
			EQUALS = Object.class.getMethod("equals", Object.class);
			TO_STRING = Object.class.getMethod("toString");
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
	 * Generates an operand that is the result of comparing this operand and the other specified operand.
	 *
	 * @param other the other operand used to calculate the result
	 * @param intBranchOpcode the opcode used to compare and branch the two int operands
	 * @param compareBranchOpcode the opcode used to branch after a compare of the two operands
	 * @return the resulting operand from the comparison
	 */
	public Operand execCompareOp(final Operand other, final byte intBranchOpcode, final byte compareBranchOpcode) {
		if ((type == null || (type.isPrimitive() && !boolean.class.equals(type))) && (other.type == null || (other.type.isPrimitive() && !boolean.class.equals(other.type)))) { // Mathematical comparison
			final Label isFloating = builder.newLabel();
			final Label process2nd = builder.newLabel();
			final Label use2ndDouble = builder.newLabel();
			final Label trueLabel = builder.newLabel();
			final Label end = builder.newLabel();

			return new Operand(boolean.class, toNumeric(true).append(other.toNumeric(true)).addAccess(ISTORE, Evaluable.FIRST_LOCAL).addAccess(DSTORE, Evaluable.FIRST_LOCAL + 1).addAccess(LSTORE, Evaluable.FIRST_LOCAL + 3)
					.addCode(DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL).addCode(IOR).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating).addCode(POP, POP2).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(LCMP).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
					.updateLabel(isFloating).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, process2nd).addCode(POP2, L2D).addAccess(DLOAD, Evaluable.FIRST_LOCAL + 1).addCode(DCMPG).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
					.updateLabel(process2nd).addCode(DUP2_X2, POP2, POP2).addAccess(ILOAD, Evaluable.FIRST_LOCAL).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, use2ndDouble).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(L2D, DCMPG).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
					.updateLabel(use2ndDouble).addAccess(DLOAD, Evaluable.FIRST_LOCAL + 1).addCode(DCMPG).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
					.updateLabel(trueLabel).addCode(ICONST_1).updateLabel(end));
		}

		// Object compare
		final Label notNumber = builder.newLabel();
		final Label testSecondStringBuilder = builder.newLabel();
		final Label compareObjects = builder.newLabel();
		final Label firstIsNull = builder.newLabel();
		final Label failure = builder.newLabel();
		final Label trueLabel = builder.newLabel();
		final Label end = builder.newLabel();

		final MethodBuilder newBuilder = toObject(false).append(other.toObject(false)).addCode(SWAP, DUP_X1).addInstanceOfCheck(Number.class).addBranch(IFEQ, notNumber).addCode(DUP).addInstanceOfCheck(Number.class).addBranch(IFEQ, notNumber).addCode(SWAP).addCast(Number.class).addInvoke(DOUBLE_VALUE).addCode(DUP2_X1, POP2).addCast(Number.class).addInvoke(DOUBLE_VALUE).addCode(DCMPG).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
			.updateLabel(notNumber).addCode(SWAP, DUP_X1).addInstanceOfCheck(StringBuilder.class).addBranch(IFEQ, testSecondStringBuilder).addCode(SWAP).addInvoke(TO_STRING).addCode(SWAP)
			.updateLabel(testSecondStringBuilder).addCode(DUP).addInstanceOfCheck(StringBuilder.class).addBranch(IFEQ, compareObjects).addInvoke(TO_STRING);

		if (compareBranchOpcode == IFEQ) {
			newBuilder.updateLabel(compareObjects).addCode(SWAP, DUP_X1).addBranch(IFNULL, firstIsNull).addInvoke(EQUALS).addBranch(GOTO, end)
				.updateLabel(firstIsNull).addCode(SWAP, POP).addBranch(IFNULL, trueLabel).addCode(ICONST_0).addBranch(GOTO, end);
		} else if (compareBranchOpcode == IFNE) {
			newBuilder.updateLabel(compareObjects).addCode(SWAP, DUP_X1).addBranch(IFNULL, firstIsNull).addInvoke(EQUALS).addBranch(IFEQ, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
				.updateLabel(firstIsNull).addCode(SWAP, POP).addBranch(IFNONNULL, trueLabel).addCode(ICONST_0).addBranch(GOTO, end);
		} else {
			newBuilder.updateLabel(compareObjects).addCode(SWAP, DUP_X1).addInstanceOfCheck(Comparable.class).addBranch(IFEQ, failure).addCode(SWAP).addCast(Comparable.class).addCode(SWAP).addInvoke(COMPARE_TO).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end);
		}

		return new Operand(boolean.class, newBuilder.updateLabel(failure).addThrow(RuntimeException.class, "Unexpected object, expecting comparable object").addCode(ACONST_NULL, ARETURN).updateLabel(trueLabel).addCode(ICONST_1).updateLabel(end));
	}

	/**
	 * Generates an operand that is the result of the specified mathematical operation on this operand and the other specified operand.
	 *
	 * @param other the other operand used to calculate the result
	 * @param intOpcode the opcode used to compute the result of the operation on two int operands
	 * @param longOpcode the opcode used to compute the result of the operation on two long operands
	 * @param doubleOpcode the opcode used to compute the result of two double operands
	 * @return the resulting operand from the operation
	 */
	public Operand execMathOp(final Operand other, final byte intOpcode, final byte longOpcode, final byte doubleOpcode) {
		final Label notInt = builder.newLabel();
		final Label isFloating = builder.newLabel();
		final Label process2nd = builder.newLabel();
		final Label use2ndDouble = builder.newLabel();
		final Label end = builder.newLabel();

		return new Operand(null, toNumeric(true).append(other.toNumeric(true)).addAccess(ISTORE, Evaluable.FIRST_LOCAL).addAccess(DSTORE, Evaluable.FIRST_LOCAL + 1).addAccess(LSTORE, Evaluable.FIRST_LOCAL + 3)
				.addCode(DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL).addCode(IOR, DUP).addBranch(IFNE, notInt).addCode(POP2, POP2, L2I).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(L2I, intOpcode, I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, end)
				.updateLabel(notInt).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating).addCode(POP, POP2).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(longOpcode, DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, end)
				.updateLabel(isFloating).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, process2nd).addCode(POP2, DUP2, L2D).addAccess(DLOAD, Evaluable.FIRST_LOCAL + 1).addCode(doubleOpcode).pushConstant(DOUBLE_TYPE).addBranch(GOTO, end)
				.updateLabel(process2nd).addAccess(ILOAD, Evaluable.FIRST_LOCAL).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, use2ndDouble).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(L2D, doubleOpcode).pushConstant(DOUBLE_TYPE).addBranch(GOTO, end)
				.updateLabel(use2ndDouble).addAccess(DLOAD, Evaluable.FIRST_LOCAL + 1).addCode(doubleOpcode).pushConstant(DOUBLE_TYPE).updateLabel(end));
	}

	/**
	 * Generates an operand that is the result of the specified integral unary operation on this operand.
	 *
	 * @param intOpcode the opcode used to compute the result of the operation
	 * @param longOpcode the opcode used to compute the result of the operation
	 * @return the resulting operand from the operation
	 */
	public Operand execIntegralOp(final byte intOpcode, final byte longOpcode) {
		final Label notInt = builder.newLabel();
		final Label isFloating = builder.newLabel();
		final Label end = builder.newLabel();

		return new Operand(null, toNumeric(false).addCode(DUP).addBranch(IFNE, notInt).addCode(POP, POP2, L2I, intOpcode, I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, end)
				.updateLabel(notInt).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating).addCode(POP2, longOpcode, DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, end)
				.updateLabel(isFloating).addThrow(RuntimeException.class, "Unexpected floating-point value, expecting integral value").addCode(ACONST_NULL, ARETURN).updateLabel(end));
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
		final Label notInt = builder.newLabel();
		final Label isFloating = builder.newLabel();
		final Label end = builder.newLabel();

		return new Operand(null, toNumeric(false).append(other.toNumeric(false)).addAccess(ISTORE, Evaluable.FIRST_LOCAL).addAccess(DSTORE, Evaluable.FIRST_LOCAL + 1).addAccess(LSTORE, Evaluable.FIRST_LOCAL + 3)
				.addCode(DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL).addCode(IOR, DUP).addBranch(IFNE, notInt).addCode(POP2, POP2, L2I).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode(L2I, intOpcode, I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, end)
				.updateLabel(notInt).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating).addCode(POP, POP2).addAccess(LLOAD, Evaluable.FIRST_LOCAL + 3).addCode((secondOperandInt ? L2I : NOP), longOpcode, DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, end)
				.updateLabel(isFloating).addThrow(RuntimeException.class, "Unexpected floating-point value, expecting integral value").addCode(ACONST_NULL, ARETURN).updateLabel(end));
	}

	/**
	 * Converts the operand to a boolean. The resulting operand will have a type of null or a primitive type. A failure will be logged in the case that the conversion fails.
	 *
	 * @return the resulting boolean operand
	 */
	public MethodBuilder toBoolean() {
		if (type == null) {
			final Label isFloating = builder.newLabel();
			final Label end = builder.newLabel();

			return builder.pushConstant(LONG_TYPE).addBranch(IFGT, isFloating).addCode(POP2, LCONST_0, LCMP).addBranch(GOTO, end)
					.updateLabel(isFloating).addAccess(DSTORE, Evaluable.FIRST_LOCAL).addCode(POP2).addAccess(DLOAD, Evaluable.FIRST_LOCAL).addCode(DCONST_0, DCMPG).updateLabel(end);
		} else if (type.isPrimitive()) {
			return builder.addPrimitiveConversion(type, boolean.class);
		}

		final Label notBoolean = builder.newLabel();
		final Label notNumber = builder.newLabel();
		final Label notNull = builder.newLabel();
		final Label end = builder.newLabel();

		return builder.addCode(DUP).addInstanceOfCheck(Boolean.class).addBranch(IFEQ, notBoolean).addCast(Boolean.class).addInvoke(BOOLEAN_VALUE).addBranch(GOTO, end)
				.updateLabel(notBoolean).addCode(DUP).addInstanceOfCheck(Number.class).addBranch(IFEQ, notNumber).addCast(Number.class).addInvoke(DOUBLE_VALUE).addCode(DCONST_0, DCMPG).addBranch(GOTO, end)
				.updateLabel(notNumber).addBranch(IFNONNULL, notNull).addCode(ICONST_0).addBranch(GOTO, end)
				.updateLabel(notNull).addCode(ICONST_1).updateLabel(end);
	}

	/**
	 * Converts the operand to a number. The resulting operand will have a type of null or a primitive type. A failure will be logged in the case that the conversion fails.
	 *
	 * @param allowFloating true to allow floating point values, otherwise false
	 * @return the resulting primitive, numeric operand
	 */
	public MethodBuilder toNumeric(final boolean allowFloating) {
		if (type == null) {
			return builder;
		} else if (type.isPrimitive()) {
			if (boolean.class.equals(type)) {
				throw new RuntimeException("Unexpected boolean value, expecting numeric value");
			} else if (double.class.equals(type) || float.class.equals(type)) {
				if (allowFloating) {
					return builder.addCode(LCONST_0, DUP2_X2, POP2).pushConstant(DOUBLE_TYPE);
				} else {
					throw new RuntimeException("Unexpected " + type.getName() + " value, expecting integral value");
				}
			} else if (long.class.equals(type)) {
				return builder.addCode(DCONST_0).pushConstant(LONG_TYPE);
			} else {
				return builder.addCode(I2L, DCONST_0).pushConstant(INT_TYPE);
			}
		}

		final Label notNumber = builder.newLabel();
		final Label notDouble = builder.newLabel();
		final Label notLong = builder.newLabel();
		final Label notFloat = builder.newLabel();
		final Label end = builder.newLabel();

		// TODO: Handle AtomicLong, BigInteger & BigDecimal?
		return builder.addCode(DUP, DUP).addInstanceOfCheck(Number.class).addBranch(IFEQ, notNumber)
				.addInstanceOfCheck(Double.class).addBranch(IFEQ, notDouble).addCast(Double.class).addInvoke(DOUBLE_VALUE).addCode(LCONST_0, DUP2_X2, POP2).pushConstant(DOUBLE_TYPE).addBranch(GOTO, end)
				.updateLabel(notDouble).addCode(DUP).addInstanceOfCheck(Long.class).addBranch(IFEQ, notLong).addCast(Long.class).addInvoke(LONG_VALUE).addCode(DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, end)
				.updateLabel(notLong).addCode(DUP).addInstanceOfCheck(Float.class).addBranch(IFEQ, notFloat).addCast(Float.class).addInvoke(DOUBLE_VALUE).addCode(LCONST_0, DUP2_X2, POP2).pushConstant(DOUBLE_TYPE).addBranch(GOTO, end)
				.updateLabel(notFloat).addCast(Number.class).addInvoke(INT_VALUE).addCode(I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, end)
				.updateLabel(notNumber).addThrow(RuntimeException.class, "Invalid object, expecting boxed numeric primitive").addCode(ACONST_NULL, ARETURN).updateLabel(end);
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
