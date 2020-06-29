package horseshoe.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class HorseshoeNumber extends Number implements Comparable<HorseshoeNumber> {

	private static final long serialVersionUID = 1L;

	/**
	 * Gets the Horseshoe number of the value specified.
	 *
	 * @param value the value of the number
	 * @return the Horseshoe number
	 */
	public static HorseshoeNumber ofUnknown(final Object value) {
		if (value instanceof HorseshoeNumber) {
			return (HorseshoeNumber)value;
		} else if (value instanceof Number) {
			if (value instanceof Long) {
				return HorseshoeInt.of(((Long)value).longValue());
			} else if (value instanceof Integer) {
				return HorseshoeInt.of(((Integer)value).intValue());
			} else if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
				return HorseshoeFloat.of(((Number)value).doubleValue());
			} else if (value instanceof BigInteger || value instanceof AtomicLong) {
				return HorseshoeInt.of(((Number)value).longValue());
			}

			return HorseshoeInt.of(((Number)value).intValue());
		} else if (value instanceof Character) {
			return HorseshoeInt.of(((Character)value).charValue());
		}

		throw new IllegalArgumentException("Unexpected " + (value == null ? "null" : value.getClass().getName()) + " value, expecting numeric value");
	}

	/**
	 * Returns the result of adding this Horseshoe number to another Horseshoe number.
	 *
	 * @param other the other Horseshoe number
	 * @return the resulting Horseshoe number
	 */
	public abstract HorseshoeNumber add(HorseshoeNumber other);

	/**
	 * Returns the result of dividing this Horseshoe number by another Horseshoe number.
	 *
	 * @param other the other Horseshoe number
	 * @return the resulting Horseshoe number
	 */
	public abstract HorseshoeNumber divide(HorseshoeNumber other);

	/**
	 * Returns the result of reducing this Horseshoe number modulo another Horseshoe number.
	 *
	 * @param other the other Horseshoe number
	 * @return the resulting Horseshoe number
	 */
	public abstract HorseshoeNumber modulo(HorseshoeNumber other);

	/**
	 * Returns the result of multiplying this Horseshoe number by another Horseshoe number.
	 *
	 * @param other the other Horseshoe number
	 * @return the resulting Horseshoe number
	 */
	public abstract HorseshoeNumber multiply(HorseshoeNumber other);

	/**
	 * Returns the result of negating this Horseshoe number.
	 *
	 * @return the resulting Horseshoe number
	 */
	public abstract HorseshoeNumber negate();

	/**
	 * Returns the result of subtracting another Horseshoe number from this Horseshoe number.
	 *
	 * @param other the other Horseshoe number
	 * @return the resulting Horseshoe number
	 */
	public abstract HorseshoeNumber subtract(HorseshoeNumber other);

	/**
	 * Converts the number to a boolean.
	 *
	 * @return true if the number is non-zero, otherwise false
	 */
	public abstract boolean toBoolean();

}
