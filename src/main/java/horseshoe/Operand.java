package horseshoe;

import static horseshoe.bytecode.MethodBuilder.ICONST_0;
import static horseshoe.bytecode.MethodBuilder.ICONST_1;
import static horseshoe.bytecode.MethodBuilder.IFEQ;
import static horseshoe.bytecode.MethodBuilder.IFNE;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;

import horseshoe.bytecode.MethodBuilder;
import horseshoe.bytecode.MethodBuilder.Label;
import horseshoe.util.Identifier;
import horseshoe.util.Operands;

final class Operand {

	static final HashSet<Class<?>> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
			boolean.class, int.class, long.class, double.class, Double.class, Integer.class, Object.class, String.class, StringBuilder.class, Entry.class));

	// Reflected Methods
	private static final Method COMPARE = MethodBuilder.getMethod(Operands.class, "compare", boolean.class, Object.class, Object.class);
	private static final Method CONVERT_TO_BOOLEAN = MethodBuilder.getMethod(Operands.class, "convertToBoolean", Object.class);
	private static final Method TO_INTEGRAL = MethodBuilder.getMethod(Operands.class, "toIntegral", Object.class);
	private static final Method TO_NUMERIC = MethodBuilder.getMethod(Operands.class, "toNumeric", Object.class);
	private static final Method TO_STRING = MethodBuilder.getMethod(Object.class, "toString");

	/**
	 * The type associated with the operand.
	 * NOTE: A class of {@link Double} indicates any potential floating-point {@link Number}, and a class of {@link Integer} indicates any potential integral {@link Number} (including {@link Long}).
	 */
	final Class<?> type;

	/**
	 * The identifier associated with the operand if one exists, otherwise null.
	 */
	final Identifier identifier;

	final MethodBuilder builder;

	/**
	 * Creates a new operand of the specified type using the builder.
	 *
	 * @param type the type of the operand, or null to indicate a stack with { long longVal, double doubleVal, int type } on top
	 * @param builder the builder used to generate data of the specified type
	 */
	Operand(final Class<?> type, final MethodBuilder builder) {
		this(type, null, builder);
	}

	/**
	 * Creates a new operand of the specified type using the builder.
	 *
	 * @param type the type of the operand
	 * @param identifier the identifier associated with the operand
	 * @param builder the builder used to generate data of the specified type
	 */
	Operand(final Class<?> type, final Identifier identifier, final MethodBuilder builder) {
		this.type = type;
		this.identifier = identifier;
		this.builder = builder;

		assert ALLOWED_TYPES.contains(type);
	}

	/**
	 * Generates an operand that is the result of comparing this operand and the other specified operand.
	 *
	 * @param other the other operand used to calculate the result
	 * @param compareBranchOpcode the opcode used to branch after a compare of the two operands
	 * @return the resulting operand from the comparison
	 */
	Operand execCompareOp(final Operand other, final byte compareBranchOpcode) {
		// Object compare
		final Label trueLabel = builder.newLabel();
		final Label end = builder.newLabel();

		return new Operand(boolean.class, new MethodBuilder().pushConstant(compareBranchOpcode == IFEQ || compareBranchOpcode == IFNE).append(toObject()).append(other.toObject()).addInvoke(COMPARE).addBranch(compareBranchOpcode, trueLabel).addCode(ICONST_0).addGoto(end, 1)
				.updateLabel(trueLabel).addCode(ICONST_1).updateLabel(end));
	}

	/**
	 * Converts the operand to a boolean. The resulting operand will have a type of null or a primitive type. A failure will be logged in the case that the conversion fails.
	 *
	 * @return the resulting boolean operand
	 */
	MethodBuilder toBoolean() {
		if (type.isPrimitive()) {
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
		if (type.isPrimitive()) {
			if (boolean.class.equals(type)) {
				return builder.addThrow(IllegalArgumentException.class, "Unexpected boolean value, expecting numeric value", 0);
			} else if (double.class.equals(type)) {
				if (allowFloating) {
					return builder.addPrimitiveConversion(type, Number.class);
				}

				return builder.addThrow(IllegalArgumentException.class, "Unexpected " + type.getSimpleName() + " value, expecting integral value", 1);
			} else if (long.class.equals(type)) {
				return builder.addPrimitiveConversion(type, Number.class);
			} else if (int.class.equals(type)) {
				return builder.addPrimitiveConversion(type, Number.class);
			}
		} else if (Integer.class.equals(type) || (allowFloating && Double.class.equals(type))) {
			return builder;
		}

		if (allowFloating) {
			return builder.addInvoke(TO_NUMERIC);
		}

		return builder.addInvoke(TO_INTEGRAL);
	}

	/**
	 * Converts the operand to an object.
	 *
	 * @return the resulting object operand
	 */
	MethodBuilder toObject() {
		if (type.isPrimitive()) {
			return builder.addPrimitiveConversion(type, Object.class);
		} else if (StringBuilder.class.equals(type)) {
			return builder.addInvoke(TO_STRING);
		}

		return builder;
	}

	@Override
	public String toString() {
		return builder == null ? "" : builder.toString();
	}

}
