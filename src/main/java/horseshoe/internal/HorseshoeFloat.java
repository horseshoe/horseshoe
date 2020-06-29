package horseshoe.internal;

public final class HorseshoeFloat extends HorseshoeNumber {

	private static final long serialVersionUID = 1L;

	private final double value;

	/**
	 * Gets the Horseshoe floating point of the value specified.
	 *
	 * @param value the value of the number
	 * @return the Horseshoe floating point
	 */
	public static HorseshoeFloat of(final double value) {
		return new HorseshoeFloat(value);
	}

	/**
	 * Creates a Horseshoe floating point of the given value and type.
	 *
	 * @param value the value of the double
	 * @param isLong true if the value is a long, otherwise false
	 */
	private HorseshoeFloat(final double value) {
		this.value = value;
	}

	@Override
	public HorseshoeFloat add(final HorseshoeNumber other) {
		return of(value + other.doubleValue());
	}

	@Override
	public int compareTo(final HorseshoeNumber other) {
		return Double.compare(value, other.doubleValue());
	}

	@Override
	public HorseshoeFloat divide(final HorseshoeNumber other) {
		return of(value / other.doubleValue());
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Number &&
				value == ((Number)obj).doubleValue();
	}

	@Override
	public float floatValue() {
		return (float)value;
	}

	@Override
	public int hashCode() {
		final long longValue = Double.doubleToRawLongBits(value);
		return (16777619 * (int)(longValue >> 32)) ^ (int)longValue ^ -527759659;
	}

	@Override
	public int intValue() {
		return (int)value;
	}

	@Override
	public long longValue() {
		return (long)value;
	}

	@Override
	public HorseshoeFloat modulo(final HorseshoeNumber other) {
		return of(value % other.doubleValue());
	}

	@Override
	public HorseshoeFloat multiply(final HorseshoeNumber other) {
		return of(value * other.doubleValue());
	}

	@Override
	public HorseshoeFloat negate() {
		return of(-value);
	}

	@Override
	public HorseshoeFloat subtract(final HorseshoeNumber other) {
		return of(value - other.doubleValue());
	}

	@Override
	public boolean toBoolean() {
		return value != 0.0 && !Double.isNaN(value);
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}

}
