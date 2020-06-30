package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.ICONST_0;
import static horseshoe.internal.MethodBuilder.ICONST_1;
import static horseshoe.internal.MethodBuilder.IFEQ;
import static horseshoe.internal.MethodBuilder.IFNE;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import horseshoe.internal.MethodBuilder.Label;

final class Operand {

	private static final Set<Class<?>> ALLOWED_TYPES = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(
			boolean.class, int.class, long.class, double.class, HorseshoeNumber.class, HorseshoeInt.class, Object.class, String.class, StringBuilder.class, Entry.class)));

	// Reflected Methods
	private static final Method COMPARE;
	private static final Method CONVERT_TO_BOOLEAN;
	private static final Method HORSESHOE_FLOAT_OF_DOUBLE;
	private static final Method HORSESHOE_INT_OF_LONG;
	private static final Method HORSESHOE_INT_OF_INT;
	private static final Method HORSESHOE_INT_OF_UNKNOWN;
	private static final Method HORSESHOE_NUMBER_OF_UNKNOWN;
	private static final Method TO_STRING;

	public final Class<?> type; // null indicates a stack with { long longVal, double doubleVal, int type } on top
	public final MethodBuilder builder;

	static {
		try {
			COMPARE = Operands.class.getMethod("compare", boolean.class, Object.class, Object.class);
			CONVERT_TO_BOOLEAN = Operands.class.getMethod("convertToBoolean", Object.class);
			HORSESHOE_FLOAT_OF_DOUBLE = HorseshoeFloat.class.getMethod("of", double.class);
			HORSESHOE_INT_OF_LONG = HorseshoeInt.class.getMethod("of", long.class);
			HORSESHOE_INT_OF_INT = HorseshoeInt.class.getMethod("of", int.class);
			HORSESHOE_INT_OF_UNKNOWN = HorseshoeInt.class.getMethod("ofUnknown", Object.class);
			HORSESHOE_NUMBER_OF_UNKNOWN = HorseshoeNumber.class.getMethod("ofUnknown", Object.class);
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
					return builder.addInvoke(HORSESHOE_FLOAT_OF_DOUBLE);
				}

				return builder.addThrow(IllegalArgumentException.class, "Unexpected " + type.getSimpleName() + " value, expecting integral value", 1);
			} else if (long.class.equals(type)) {
				return builder.addInvoke(HORSESHOE_INT_OF_LONG);
			} else if (int.class.equals(type)) {
				return builder.addInvoke(HORSESHOE_INT_OF_INT);
			}
		} else if (HorseshoeInt.class.equals(type) || (allowFloating && HorseshoeNumber.class.equals(type))) {
			return builder;
		}

		if (allowFloating) {
			return builder.addInvoke(HORSESHOE_NUMBER_OF_UNKNOWN);
		}

		return builder.addInvoke(HORSESHOE_INT_OF_UNKNOWN);
	}

	/**
	 * Converts the operand to an object.
	 *
	 * @param generateReturn true to generate return instructions rather than place the object on the stack, otherwise false
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
