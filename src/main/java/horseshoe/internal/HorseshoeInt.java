package horseshoe.internal;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class HorseshoeInt extends HorseshoeNumber {

	private static final long serialVersionUID = 1L;

	private final long value;
	private final boolean isLong;

	/**
	 * Gets the Horseshoe integer of the value specified.
	 *
	 * @param value the value of the number
	 * @return the Horseshoe integer
	 */
	public static HorseshoeInt of(final int value) {
		return new HorseshoeInt(value, false);
	}

	/**
	 * Gets the Horseshoe integer of the value specified.
	 *
	 * @param value the value of the number
	 * @return the Horseshoe integer
	 */
	public static HorseshoeInt of(final long value) {
		return new HorseshoeInt(value, true);
	}

	/**
	 * Gets the Horseshoe integer of the value specified.
	 *
	 * @param value the value of the number
	 * @return the Horseshoe integer
	 */
	public static HorseshoeInt ofUnknown(final Object value) {
		if (value instanceof HorseshoeInt) {
			return (HorseshoeInt)value;
		} else if (value instanceof Number) {
			if (value instanceof Long) {
				return of(((Long)value).longValue());
			} else if (value instanceof Integer) {
				return of(((Integer)value).intValue());
			} else if (value instanceof BigInteger || value instanceof AtomicLong) {
				return of(((Number)value).longValue());
			} else if (value instanceof Short || value instanceof Byte || value instanceof AtomicInteger) {
				return of(((Number)value).intValue());
			}
		} else if (value instanceof Character) {
			return of(((Character)value).charValue());
		}

		throw new IllegalArgumentException("Unexpected " + (value == null ? "null" : value.getClass().getName()) + " value, expecting integral value");
	}

	/**
	 * Creates a Horseshoe integer of the given value and type.
	 *
	 * @param value the value of the integer
	 * @param isLong true if the value is a long, otherwise false
	 */
	private HorseshoeInt(final long value, final boolean isLong) {
		this.value = value;
		this.isLong = isLong;
	}

	@Override
	public HorseshoeNumber add(final HorseshoeNumber other) {
		if (!(other instanceof HorseshoeInt)) {
			return HorseshoeFloat.of(value + other.doubleValue());
		} else if ((isLong | ((HorseshoeInt)other).isLong)) {
			return of(value + ((HorseshoeInt)other).value);
		}

		return of((int)(value + ((HorseshoeInt)other).value));
	}

	/**
	 * Returns the result of a bitwise AND of this Horseshoe integer and another Horseshoe integer.
	 *
	 * @param other the other Horseshoe integer
	 * @return the resulting Horseshoe integer
	 */
	public HorseshoeInt and(final HorseshoeInt other) {
		if ((isLong | other.isLong)) {
			return of(value & other.value);
		}

		return of((int)(value & other.value));
	}

	@Override
	public int compareTo(final HorseshoeNumber other) {
		if (!(other instanceof HorseshoeInt)) {
			return Double.compare(value, other.doubleValue());
		}

		return Long.compare(value, ((HorseshoeInt)other).value);
	}

	@Override
	public HorseshoeNumber divide(final HorseshoeNumber other) {
		if (!(other instanceof HorseshoeInt)) {
			return HorseshoeFloat.of(value / other.doubleValue());
		} else if ((isLong | ((HorseshoeInt)other).isLong)) {
			return of(value / ((HorseshoeInt)other).value);
		}

		return of((int)(value / ((HorseshoeInt)other).value));
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof HorseshoeInt) {
			return value == ((HorseshoeInt)obj).value;
		}

		return obj instanceof Number &&
				value == ((Number)obj).longValue() && value == ((Number)obj).doubleValue();
	}

	@Override
	public float floatValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return (16777619 * (int)(value >> 32)) ^ (int)value ^ 286170609;
	}

	@Override
	public int intValue() {
		return (int)value;
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public HorseshoeNumber modulo(final HorseshoeNumber other) {
		if (!(other instanceof HorseshoeInt)) {
			return HorseshoeFloat.of(value % other.doubleValue());
		} else if ((isLong | ((HorseshoeInt)other).isLong)) {
			return of(value % ((HorseshoeInt)other).value);
		}

		return of((int)(value % ((HorseshoeInt)other).value));
	}

	@Override
	public HorseshoeNumber multiply(final HorseshoeNumber other) {
		if (!(other instanceof HorseshoeInt)) {
			return HorseshoeFloat.of(value * other.doubleValue());
		} else if ((isLong | ((HorseshoeInt)other).isLong)) {
			return of(value * ((HorseshoeInt)other).value);
		}

		return of((int)(value * ((HorseshoeInt)other).value));
	}

	/**
	 * Returns the result of negating this Horseshoe integer.
	 *
	 * @return the resulting Horseshoe integer
	 */
	@Override
	public HorseshoeInt negate() {
		return new HorseshoeInt(-value, isLong);
	}

	/**
	 * Returns the result of a bitwise NOT of this Horseshoe integer.
	 *
	 * @return the resulting Horseshoe integer
	 */
	public HorseshoeInt not() {
		return new HorseshoeInt(~value, isLong);
	}

	/**
	 * Returns the result of a bitwise OR of this Horseshoe integer and another Horseshoe integer.
	 *
	 * @param other the other Horseshoe integer
	 * @return the resulting Horseshoe integer
	 */
	public HorseshoeInt or(final HorseshoeInt other) {
		if ((isLong | other.isLong)) {
			return of(value | other.value);
		}

		return of((int)(value | other.value));
	}

	/**
	 * Returns the result of a shift left of this Horseshoe integer by another Horseshoe integer.
	 *
	 * @param other the other Horseshoe integer
	 * @return the resulting Horseshoe integer
	 */
	public HorseshoeInt shiftLeft(final HorseshoeInt other) {
		if (isLong) {
			return of(value << other.value);
		}

		return of((int)(value << other.value));
	}

	/**
	 * Returns the result of a shift right of this Horseshoe integer by another Horseshoe integer.
	 *
	 * @param other the other Horseshoe integer
	 * @return the resulting Horseshoe integer
	 */
	public HorseshoeInt shiftRight(final HorseshoeInt other) {
		return new HorseshoeInt(value >> other.value, isLong);
	}

	/**
	 * Returns the result of a shift right zero of this Horseshoe integer by another Horseshoe integer.
	 *
	 * @param other the other Horseshoe integer
	 * @return the resulting Horseshoe integer
	 */
	public HorseshoeInt shiftRightZero(final HorseshoeInt other) {
		if (isLong) {
			return of(value >>> other.value);
		}

		return of((int)value >>> other.value);
	}

	@Override
	public HorseshoeNumber subtract(final HorseshoeNumber other) {
		if (!(other instanceof HorseshoeInt)) {
			return HorseshoeFloat.of(value - other.doubleValue());
		} else if ((isLong | ((HorseshoeInt)other).isLong)) {
			return of(value - ((HorseshoeInt)other).value);
		}

		return of((int)(value - ((HorseshoeInt)other).value));
	}

	@Override
	public boolean toBoolean() {
		return value != 0;
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}

	/**
	 * Returns the result of a bitwise XOR of this Horseshoe integer and another Horseshoe integer.
	 *
	 * @param other the other Horseshoe integer
	 * @return the resulting Horseshoe integer
	 */
	public HorseshoeInt xor(final HorseshoeInt other) {
		if ((isLong | other.isLong)) {
			return of(value ^ other.value);
		}

		return of((int)(value ^ other.value));
	}

}
