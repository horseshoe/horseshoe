package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import horseshoe.internal.MethodBuilder.Label;

final class Operand {

	public static final int REQUIRED_LOCAL_VARIABLE_SLOTS = 5;

	public static final int INT_TYPE    = 0b00;
	public static final int LONG_TYPE   = 0b01;
	public static final int DOUBLE_TYPE = 0b11;

	private static final Set<Class<?>> ALLOWED_TYPES = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(
			boolean.class, int.class, long.class, double.class, Object.class, String.class, StringBuilder.class, Entry.class, null)));

	// Reflected Methods
	private static final Method DOUBLE_VALUE;
	private static final Method LONG_VALUE;
	private static final Method INT_VALUE;
	private static final Method CHAR_VALUE;
	private static final Method COMPARE;
	private static final Method CONVERT_TO_BOOLEAN;
	private static final Method TO_STRING;

	public final Class<?> type; // null indicates a stack with { long longVal, double doubleVal, int type } on top
	public final MethodBuilder builder;

	static {
		try {
			DOUBLE_VALUE = Number.class.getMethod("doubleValue");
			LONG_VALUE = Number.class.getMethod("longValue");
			INT_VALUE = Number.class.getMethod("intValue");
			CHAR_VALUE = Character.class.getMethod("charValue");
			COMPARE = Expression.class.getMethod("compare", boolean.class, Object.class, Object.class);
			CONVERT_TO_BOOLEAN = Expression.class.getMethod("convertToBoolean", Object.class);
			TO_STRING = Object.class.getMethod("toString");
		} catch (final ReflectiveOperationException e) {
			throw new ExceptionInInitializerError("Failed to get required class member: " + e.getMessage());
		}
	}

	/**
	 * Creates a new operand of the specified type using the builder.
	 *
	 * @param type the type of the operand, or null to indicate a stack with { long longVal, double doubleVal, int type } on top
	 * @param builder the builder used to generate data of the specified type
	 */
	Operand(final Class<?> type, final MethodBuilder builder) {
		this.type = type;
		this.builder = builder;

		assert ALLOWED_TYPES.contains(type);
	}

	/**
	 * Generates an operand that is the result of comparing this operand and the other specified operand.
	 *
	 * @param other the other operand used to calculate the result
	 * @param compareBranchOpcode the opcode used to branch after a compare of the two operands
	 * @param firstLocalIndex the first local variable index that can be used for temporary storage
	 * @return the resulting operand from the comparison
	 */
	Operand execCompareOp(final Operand other, final byte compareBranchOpcode, final int firstLocalIndex) {
		if ((type == null || (type.isPrimitive() && !boolean.class.equals(type))) && (other.type == null || (other.type.isPrimitive() && !boolean.class.equals(other.type)))) { // Mathematical comparison
			final int typeIndex = firstLocalIndex;
			final int doubleValueIndex = firstLocalIndex + 1;
			final int longValueIndex = firstLocalIndex + 3;
			final Label isFloating = builder.newLabel();
			final Label process2nd = builder.newLabel();
			final Label use2ndDouble = builder.newLabel();
			final Label trueLabel = builder.newLabel();
			final Label end = builder.newLabel();

			return new Operand(boolean.class, toNumeric(true).append(other.toNumeric(true)).addAccess(ISTORE, typeIndex).addAccess(DSTORE, doubleValueIndex).addAccess(LSTORE, longValueIndex)
					.addCode(DUP).addAccess(ILOAD, typeIndex).addCode(IOR).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating).addCode(POP, POP2).addAccess(LLOAD, longValueIndex).addCode(LCMP).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
					.updateLabel(isFloating).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, process2nd).addCode(POP2, L2D).addAccess(DLOAD, doubleValueIndex).addCode(DCMPG).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
					.updateLabel(process2nd).addCode(DUP2_X2, POP2, POP2).addAccess(ILOAD, typeIndex).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, use2ndDouble).addAccess(LLOAD, longValueIndex).addCode(L2D, DCMPG).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
					.updateLabel(use2ndDouble).addAccess(DLOAD, doubleValueIndex).addCode(DCMPG).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
					.updateLabel(trueLabel).addCode(ICONST_1).updateLabel(end));
		}

		// Object compare
		final Label trueLabel = builder.newLabel();
		final Label end = builder.newLabel();

		return new Operand(boolean.class, new MethodBuilder().pushConstant(compareBranchOpcode == IFEQ || compareBranchOpcode == IFNE).append(toObject(false)).append(other.toObject(false)).addInvoke(COMPARE).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addBranch(GOTO, end)
				.updateLabel(trueLabel).addCode(ICONST_1).updateLabel(end));
	}

	/**
	 * Generates an operand that is the result of the specified mathematical operation on this operand and the other specified operand.
	 *
	 * @param other the other operand used to calculate the result
	 * @param intOpcode the opcode used to compute the result of the operation on two int operands
	 * @param longOpcode the opcode used to compute the result of the operation on two long operands
	 * @param doubleOpcode the opcode used to compute the result of two double operands
	 * @param firstLocalIndex the first local variable index that can be used for temporary storage
	 * @return the resulting operand from the operation
	 */
	Operand execMathOp(final Operand other, final byte intOpcode, final byte longOpcode, final byte doubleOpcode, final int firstLocalIndex) {
		final int typeIndex = firstLocalIndex;
		final int doubleValueIndex = firstLocalIndex + 1;
		final int longValueIndex = firstLocalIndex + 3;
		final Label notInt = builder.newLabel();
		final Label isFloating = builder.newLabel();
		final Label process2nd = builder.newLabel();
		final Label use2ndDouble = builder.newLabel();
		final Label end = builder.newLabel();

		return new Operand(null, toNumeric(true).append(other.toNumeric(true)).addAccess(ISTORE, typeIndex).addAccess(DSTORE, doubleValueIndex).addAccess(LSTORE, longValueIndex)
				.addCode(DUP).addAccess(ILOAD, typeIndex).addCode(IOR, DUP).addBranch(IFNE, notInt).addCode(POP2, POP2, L2I).addAccess(LLOAD, longValueIndex).addCode(L2I, intOpcode, I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, end)
				.updateLabel(notInt).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating).addCode(POP, POP2).addAccess(LLOAD, longValueIndex).addCode(longOpcode, DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, end)
				.updateLabel(isFloating).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, process2nd).addCode(POP2, DUP2, L2D).addAccess(DLOAD, doubleValueIndex).addCode(doubleOpcode).pushConstant(DOUBLE_TYPE).addBranch(GOTO, end)
				.updateLabel(process2nd).addAccess(ILOAD, typeIndex).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, use2ndDouble).addAccess(LLOAD, longValueIndex).addCode(L2D, doubleOpcode).pushConstant(DOUBLE_TYPE).addBranch(GOTO, end)
				.updateLabel(use2ndDouble).addAccess(DLOAD, doubleValueIndex).addCode(doubleOpcode).pushConstant(DOUBLE_TYPE).updateLabel(end));
	}

	/**
	 * Generates an operand that is the result of the specified integral unary operation on this operand.
	 *
	 * @param intOpcode the opcode used to compute the result of the operation
	 * @param longOpcode the opcode used to compute the result of the operation
	 * @return the resulting operand from the operation
	 */
	Operand execIntegralOp(final byte intOpcode, final byte longOpcode) {
		final Label notInt = builder.newLabel();
		final Label end = builder.newLabel();

		return new Operand(null, toNumeric(false).addBranch(IFNE, notInt).addCode(POP2, L2I, intOpcode, I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, end)
				.updateLabel(notInt).addCode(POP2, longOpcode, DCONST_0).pushConstant(LONG_TYPE).updateLabel(end));
	}

	/**
	 * Generates an operand that is the result of the specified integral operation on this operand and the other specified operand.
	 *
	 * @param other the other operand used to calculate the result
	 * @param intOpcode the opcode used to compute the result of two int operands
	 * @param longOpcode the opcode used to compute the result of two long operands
	 * @param secondOperandInt true if the second operand should always be interpreted as an int, otherwise false
	 * @param firstLocalIndex the first local variable index that can be used for temporary storage
	 * @return the resulting operand from the operation
	 */
	Operand execIntegralOp(final Operand other, final byte intOpcode, final byte longOpcode, final boolean secondOperandInt, final int firstLocalIndex) {
		final int typeIndex = firstLocalIndex;
		final int longValueIndex = firstLocalIndex + 1;
		final Label notInt = builder.newLabel();
		final Label isFloating = builder.newLabel();
		final Label end = builder.newLabel();

		return new Operand(null, toNumeric(false).append(other.toNumeric(false)).addAccess(ISTORE, typeIndex).addCode(POP2).addAccess(LSTORE, longValueIndex)
				.addCode(DUP).addAccess(ILOAD, typeIndex).addCode(IOR, DUP).addBranch(IFNE, notInt).addCode(POP2, POP2, L2I).addAccess(LLOAD, longValueIndex).addCode(L2I, intOpcode, I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, end)
				.updateLabel(notInt).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating).addCode(POP, POP2).addAccess(LLOAD, longValueIndex).addCode((secondOperandInt ? L2I : NOP), longOpcode, DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, end)
				.updateLabel(isFloating).addThrow(IllegalArgumentException.class, "Unexpected floating-point value, expecting integral value").addCode(ACONST_NULL, ARETURN).updateLabel(end));
	}

	/**
	 * Converts the operand to a boolean. The resulting operand will have a type of null or a primitive type. A failure will be logged in the case that the conversion fails.
	 *
	 * @return the resulting boolean operand
	 */
	MethodBuilder toBoolean() {
		if (type == null) {
			return builder.addCode(POP, DCONST_0, DCMPG, DUP_X2, POP, LCONST_0, LCMP, IOR);
		} else if (type.isPrimitive()) {
			return builder.addPrimitiveConversion(type, boolean.class);
		}

		return builder.addInvoke(CONVERT_TO_BOOLEAN);
	}

	/**
	 * Converts the operand to a number. The resulting operand will have a type of null or a primitive type. A failure will be logged in the case that the conversion fails.
	 *
	 * @param allowFloating true to allow floating point values, otherwise false
	 * @return the resulting primitive, numeric operand
	 */
	MethodBuilder toNumeric(final boolean allowFloating) {
		if (type == null) {
			if (allowFloating) {
				return builder;
			}

			final Label end = builder.newLabel();
			return builder.addCode(DUP).pushConstant(LONG_TYPE).addBranch(IF_ICMPLE, end).addThrow(IllegalArgumentException.class, "Unexpected floating point value, expecting integral value").updateLabel(end);
		} else if (type.isPrimitive()) {
			if (boolean.class.equals(type)) {
				return builder.addThrow(IllegalArgumentException.class, "Unexpected boolean value, expecting numeric value");
			} else if (double.class.equals(type) || float.class.equals(type)) {
				if (allowFloating) {
					return builder.addPrimitiveConversion(type, double.class).addCode(LCONST_0, DUP2_X2, POP2).pushConstant(DOUBLE_TYPE);
				}

				return builder.addThrow(IllegalArgumentException.class, "Unexpected " + type.getSimpleName() + " value, expecting integral value");
			} else if (long.class.equals(type)) {
				return builder.addCode(DCONST_0).pushConstant(LONG_TYPE);
			}

			return builder.addPrimitiveConversion(type, long.class).addCode(DCONST_0).pushConstant(INT_TYPE);
		}

		final Label getDouble = builder.newLabel();
		final Label notDouble = builder.newLabel();
		final Label getLong = builder.newLabel();
		final Label notLong = builder.newLabel();
		final Label notNumber = builder.newLabel();
		final Label elseCase = builder.newLabel();
		final Label end = builder.newLabel();

		/* The builder implements the following algorithm to convert to a numeric value:
		 *
		 *	if (instanceof Number) {
		 *		if (instanceof Double || instanceof Float || instanceof BigDecimal) {
		 *			use .doubleValue();
		 *		} else if (instanceof Long || instanceof BigInteger || instanceof AtomicLong) {
		 *			use .longValue();
		 *		} else {
		 *			use .intValue();
		 *		}
		 *	} else if (second instanceof Character) {
		 *		use .charValue();
		 *	}
		 */
		builder.addCode(DUP).addInstanceOfCheck(Number.class).addBranch(IFEQ, notNumber)
				.addCode(DUP).addInstanceOfCheck(Double.class).addBranch(IFNE, getDouble).addCode(DUP).addInstanceOfCheck(Float.class).addBranch(IFNE, getDouble).addCode(DUP).addInstanceOfCheck(BigDecimal.class).addBranch(IFNE, getDouble).addBranch(GOTO, notDouble);

		if (allowFloating) {
			builder.updateLabel(getDouble).addCast(Number.class).addInvoke(DOUBLE_VALUE).addCode(LCONST_0, DUP2_X2, POP2).pushConstant(DOUBLE_TYPE).addBranch(GOTO, end);
		} else {
			builder.updateLabel(getDouble).addThrow(IllegalArgumentException.class, "Unexpected floating point value, expecting integral value");
		}

		return builder.updateLabel(notDouble).addCode(DUP).addInstanceOfCheck(Long.class).addBranch(IFNE, getLong).addCode(DUP).addInstanceOfCheck(BigInteger.class).addBranch(IFNE, getLong).addCode(DUP).addInstanceOfCheck(AtomicLong.class).addBranch(IFNE, getLong).addBranch(GOTO, notLong)
				.updateLabel(getLong).addCast(Number.class).addInvoke(LONG_VALUE).addCode(DCONST_0).pushConstant(LONG_TYPE).addBranch(GOTO, end)
				.updateLabel(notLong).addCast(Number.class).addInvoke(INT_VALUE).addCode(I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, end)
				.updateLabel(notNumber).addCode(DUP).addInstanceOfCheck(Character.class).addBranch(IFEQ, elseCase).addCast(Character.class).addInvoke(CHAR_VALUE).addCode(I2L, DCONST_0).pushConstant(INT_TYPE).addBranch(GOTO, end)
				.updateLabel(elseCase).addThrow(IllegalArgumentException.class, "Invalid object, expecting boxed numeric primitive").updateLabel(end);
	}

	/**
	 * Converts the operand to an object.
	 *
	 * @param generateReturn true to generate return instructions rather than place the object on the stack, otherwise false
	 * @return the resulting object operand
	 */
	MethodBuilder toObject(final boolean generateReturn) {
		if (type == null) {
			final Label isFloating = builder.newLabel();
			final Label isLong = builder.newLabel();

			if (generateReturn) {
				builder.addCode(DUP).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating)
					.addBranch(IFNE, isLong).addCode(POP2, L2I).addPrimitiveConversion(int.class, Object.class).addCode(ARETURN)
					.updateLabel(isLong).addCode(POP2).addPrimitiveConversion(long.class, Object.class).addCode(ARETURN)
					.updateLabel(isFloating).addCode(POP).addPrimitiveConversion(double.class, Object.class);
			} else {
				final Label end = builder.newLabel();

				builder.addCode(DUP).pushConstant(LONG_TYPE).addBranch(IF_ICMPGT, isFloating)
					.addBranch(IFNE, isLong).addCode(POP2, L2I).addPrimitiveConversion(int.class, Object.class).addBranch(GOTO, end)
					.updateLabel(isLong).addCode(POP2).addPrimitiveConversion(long.class, Object.class).addBranch(GOTO, end)
					.updateLabel(isFloating).addCode(POP, DUP2_X2, POP2, POP2).addPrimitiveConversion(double.class, Object.class).updateLabel(end);
			}
		} else if (type.isPrimitive()) {
			builder.addPrimitiveConversion(type, Object.class);
		} else if (StringBuilder.class.equals(type)) {
			builder.addInvoke(TO_STRING);
		}

		return generateReturn ? builder.addCode(ARETURN) : builder;
	}

	@Override
	public String toString() {
		return builder == null ? "" : builder.toString();
	}

}
