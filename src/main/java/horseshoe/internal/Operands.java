package horseshoe.internal;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import horseshoe.RenderContext;

public final class Operands {

	/**
	 * Adds two objects together. Handles numeric addition, string concatenation,
	 * and map / collection combination.
	 *
	 * @param left  the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Object add(final Object left, final Object right) {
		if (left instanceof Number || left instanceof Character) {
			if (right instanceof Number || right instanceof Character) {
				return add(toNumeric(left), toNumeric(right));
			}
		} else if (left instanceof String) {
			return (String)left + right;
		} else if (left instanceof Set) {
			if (right instanceof Collection) {
				final Set<Object> result = new LinkedHashSet<>((Set<?>)left);

				result.addAll((Collection<?>)right);
				return result;
			} else if (right instanceof Map) {
				final Map<?, ?> rightMap = (Map<?, ?>)right;
				final Map<Object, Object> result = new LinkedHashMap<>(rightMap.size());

				for (final Object object : (Set<?>)left) {
					result.put(object, object);
				}

				result.putAll(rightMap);
				return result;
			}
		} else if (left instanceof Collection) {
			if (right instanceof Set) {
				final Set<Object> result = new LinkedHashSet<>((Collection<?>)left);

				result.addAll((Set<?>)right);
				return result;
			} else if (right instanceof Collection) {
				final Collection<?> leftCollection = (Collection<?>)left;
				final Collection<?> rightCollection = (Collection<?>)right;
				final List<Object> result = new ArrayList<>(leftCollection.size() + rightCollection.size());

				result.addAll(leftCollection);
				result.addAll(rightCollection);
				return result;
			} else if (right instanceof Map) {
				final Map<?, ?> rightMap = (Map<?, ?>)right;
				final Map<Object, Object> result = new LinkedHashMap<>(rightMap.size());

				for (final Object object : (Collection<?>)left) {
					result.put(object, object);
				}

				result.putAll(rightMap);
				return result;
			}
		} else if (left instanceof Map) {
			if (right instanceof Map) {
				final Map<Object, Object> result = new LinkedHashMap<>((Map<?, ?>)left);

				result.putAll((Map<?, ?>)right);
				return result;
			} else if (right instanceof Collection) {
				final Map<Object, Object> result = new LinkedHashMap<>((Map<?, ?>)left);

				for (final Object object : (Collection<?>)right) {
					result.put(object, object);
				}

				return result;
			}
		}

		if (right instanceof String) {
			return left + (String)right;
		}

		throw new IllegalArgumentException("Invalid objects cannot be added: " + (left == null ? "null" : left.getClass().getName()) + " + " + (right == null ? "null" : right.getClass().getName()));
	}

	/**
	 * Adds two numerics.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	private static Number add(final Number left, final Number right) {
		if (left instanceof Long) {
			if (right instanceof Double) {
				return ((Long)left).longValue() + ((Double)right).doubleValue();
			}

			return ((Long)left).longValue() + right.longValue();
		} else if (left instanceof Integer) {
			if (right instanceof Long) {
				return ((Integer)left).intValue() + ((Long)right).longValue();
			} else if (right instanceof Integer) {
				return ((Integer)left).intValue() + ((Integer)right).intValue();
			}

			return ((Integer)left).intValue() + ((Double)right).doubleValue();
		}

		return ((Double)left).doubleValue() + right.doubleValue();
	}

	/**
	 * Performs a bitwise AND on two integral values.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Number and(final Number left, final Number right) {
		if (left instanceof Integer && right instanceof Integer) {
			return ((Integer)left).intValue() & ((Integer)right).intValue();
		}

		return left.longValue() & right.longValue();
	}

	/**
	 * Compares two objects.
	 *
	 * @param equality true to test for equality, false to perform 3-way comparison
	 * @param first the object to compare
	 * @param second the object being compared
	 * @return the result of comparing the two values where a negative value indicates the first object is less than the second, a positive value indicates the first object is greater than the second, and a value of zero indicates the objects are equivalent
	 */
	@SuppressWarnings("unchecked")
	public static int compare(final boolean equality, final Object first, final Object second) {
		if (first == null) {
			return second == null ? 0 : -1;
		} else if (second == null) {
			return 1;
		} else if (first instanceof Number) {
			if (second instanceof Number || second instanceof Character) {
				return compare(toNumeric(first), toNumeric(second));
			}
		} else if (first instanceof Character) {
			if (second instanceof Number || second instanceof Character) {
				return compare(toNumeric(first), toNumeric(second));
			} else if (second instanceof String) {
				return first.toString().compareTo((String)second);
			}
		} else if (first instanceof String &&
				(second instanceof String || second instanceof Character || second instanceof Enum)) {
			return ((String)first).compareTo(second.toString());
		} else if (first instanceof Enum && second instanceof String) {
			return first.toString().compareTo((String)second);
		} else if (equality) {
			return first.equals(second) ? 0 : 1;
		} else if (first instanceof Comparable) {
			return ((Comparable<Object>)first).compareTo(second);
		}

		if (equality) {
			return 1; // Indicate not equal
		}

		throw new IllegalArgumentException("Unexpected object, expecting comparable object");
	}

	/**
	 * Compare two numerics.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static int compare(final Number left, final Number right) {
		if (left instanceof Long) {
			if (right instanceof Double) {
				return Double.compare(((Long)left).longValue(), ((Double)right).doubleValue());
			}

			return Long.compare(((Long)left).longValue(), right.longValue());
		} else if (left instanceof Integer) {
			if (right instanceof Long) {
				return Long.compare(((Integer)left).intValue(), ((Long)right).longValue());
			} else if (right instanceof Integer) {
				return Integer.compare(((Integer)left).intValue(), ((Integer)right).intValue());
			}

			return Double.compare(((Integer)left).intValue(), ((Double)right).doubleValue());
		}

		return Double.compare(((Double)left).doubleValue(), right.doubleValue());
	}

	/**
	 * Converts an object to a boolean.
	 *
	 * @param object the object to convert to a boolean
	 * @return the result of converting the object to a boolean
	 */
	public static boolean convertToBoolean(final Object object) {
		if (object == null) {
			return false;
		} else if (object instanceof Number) {
			final Number number = toNumeric(object);

			if (number instanceof Double) {
				return ((Double)number).doubleValue() != 0.0 && !((Double)number).isNaN();
			}

			return number.longValue() != 0;
		} else if (object instanceof CharSequence) {
			return ((CharSequence)object).length() != 0;
		} else if (object instanceof Collection) {
			return !((Collection<?>)object).isEmpty();
		} else if (object instanceof Map) {
			return !((Map<?, ?>)object).isEmpty();
		} else if (object.getClass().isArray()) {
			return Array.getLength(object) != 0;
		} else if (object instanceof Boolean) {
			return ((Boolean)object).booleanValue();
		} else if (object instanceof Character) {
			return ((Character)object).charValue() != 0;
		}

		return true;
	}

	/**
	 * Creates a range.
	 *
	 * @param start the start of the range
	 * @param end the end of the range
	 * @param exclusive true if the end is exclusive, otherwise false
	 * @return the result int array containing the range of values
	 */
	public static int[] createRange(final Number start, final Number end, final boolean exclusive) {
		final int left = start.intValue();
		final int right = end.intValue();

		// Check for reverse range
		if (right < left) {
			final int length = left - right + (exclusive ? 0 : 1);
			final int[] result = new int[length];

			for (int i = 0, j = left; i < length; j--, i++) {
				result[i] = j;
			}

			return result;
		}

		final int length = right - left + (exclusive ? 0 : 1);
		final int[] result = new int[length];

		for (int i = 0, j = left; i < length; j++, i++) {
			result[i] = j;
		}

		return result;
	}

	/**
	 * Divides two numerics.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Number divide(final Number left, final Number right) {
		if (left instanceof Long) {
			if (right instanceof Double) {
				return ((Long)left).longValue() / ((Double)right).doubleValue();
			}

			return ((Long)left).longValue() / right.longValue();
		} else if (left instanceof Integer) {
			if (right instanceof Long) {
				return ((Integer)left).intValue() / ((Long)right).longValue();
			} else if (right instanceof Integer) {
				return ((Integer)left).intValue() / ((Integer)right).intValue();
			}

			return ((Integer)left).intValue() / ((Double)right).doubleValue();
		}

		return ((Double)left).doubleValue() / right.doubleValue();
	}

	/**
	 * Exponentiates two numerics.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Double exponentiate(final Number left, final Number right) {
		return Math.pow(left.doubleValue(), right.doubleValue());
	}

	/**
	 * Checks if the pattern matches any part of the input.
	 *
	 * @param input the input being matched against the pattern
	 * @param pattern the pattern used to test the input
	 * @return true if the pattern matches any part of the input, otherwise false
	 */
	public static boolean findPattern(final Object input, final Object pattern) {
		if (pattern instanceof CharSequence) {
			return Pattern.compile(pattern.toString()).matcher((CharSequence)input).find();
		}

		return ((Pattern)pattern).matcher((CharSequence)input).find();
	}

	/**
	 * Gets the class of the specified name.
	 *
	 * @param context the context used to get the class
	 * @param name the name of the class to get
	 * @return the class associated with the specified name
	 */
	public static Class<?> getClass(final RenderContext context, final String name) {
		return context.getClass(name);
	}

	/**
	 * Checks if the item is in the container.
	 *
	 * @param item the object being tested for membership
	 * @param container the container being tested against
	 * @return true if the item is contained in the container, otherwise false
	 */
	public static boolean isIn(final Object item, final Object container) {
		if (container instanceof Collection) {
			return ((Collection<?>)container).contains(item);
		} else if (container instanceof Map) {
			return ((Map<?, ?>)container).containsKey(item);
		}

		return false;
	}

	/**
	 * Checks if the pattern matches the whole input.
	 *
	 * @param input the input being matched against the pattern
	 * @param pattern the pattern used to test the input
	 * @return true if the pattern matches the whole, otherwise false
	 */
	public static boolean matchesPattern(final Object input, final Object pattern) {
		if (pattern instanceof CharSequence) {
			return Pattern.compile(pattern.toString()).matcher((CharSequence)input).matches();
		}

		return ((Pattern)pattern).matcher((CharSequence)input).matches();
	}

	/**
	 * Computes one numeric value modulo another numeric.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Number modulo(final Number left, final Number right) {
		if (left instanceof Long) {
			if (right instanceof Double) {
				return ((Long)left).longValue() % ((Double)right).doubleValue();
			}

			return ((Long)left).longValue() % right.longValue();
		} else if (left instanceof Integer) {
			if (right instanceof Long) {
				return ((Integer)left).intValue() % ((Long)right).longValue();
			} else if (right instanceof Integer) {
				return ((Integer)left).intValue() % ((Integer)right).intValue();
			}

			return ((Integer)left).intValue() % ((Double)right).doubleValue();
		}

		return ((Double)left).doubleValue() % right.doubleValue();
	}

	/**
	 * Multiplies two numerics.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Number multiply(final Number left, final Number right) {
		if (left instanceof Long) {
			if (right instanceof Double) {
				return ((Long)left).longValue() * ((Double)right).doubleValue();
			}

			return ((Long)left).longValue() * right.longValue();
		} else if (left instanceof Integer) {
			if (right instanceof Long) {
				return ((Integer)left).intValue() * ((Long)right).longValue();
			} else if (right instanceof Integer) {
				return ((Integer)left).intValue() * ((Integer)right).intValue();
			}

			return ((Integer)left).intValue() * ((Double)right).doubleValue();
		}

		return ((Double)left).doubleValue() * right.doubleValue();
	}

	/**
	 * Negates a numeric value.
	 *
	 * @param value the operand
	 * @return the result of the operation
	 */
	public static Number negate(final Number value) {
		if (value instanceof Long) {
			return -((Long)value).longValue();
		} else if (value instanceof Integer) {
			return -((Integer)value).intValue();
		}

		return -((Double)value).doubleValue();
	}

	/**
	 * Performs a bitwise OR on an integral value.
	 *
	 * @param value the operand
	 * @return the result of the operation
	 */
	public static Number not(final Number value) {
		if (value instanceof Integer) {
			return ~((Integer)value).intValue();
		}

		return ~((Long)value).longValue();
	}

	/**
	 * Performs a bitwise OR on two integral values.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Number or(final Number left, final Number right) {
		if (left instanceof Integer && right instanceof Integer) {
			return ((Integer)left).intValue() | ((Integer)right).intValue();
		}

		return left.longValue() | right.longValue();
	}

	/**
	 * Performs a left shift on two integral values.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Number shiftLeft(final Number left, final Number right) {
		if (left instanceof Integer) {
			return ((Integer)left).intValue() << right.intValue();
		}

		return left.longValue() << right.intValue();
	}

	/**
	 * Performs a right shift on two integral values.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Number shiftRight(final Number left, final Number right) {
		if (left instanceof Integer) {
			return ((Integer)left).intValue() >> right.intValue();
		}

		return left.longValue() >> right.intValue();
	}

	/**
	 * Performs a right shift zero on two integral values.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Number shiftRightZero(final Number left, final Number right) {
		if (left instanceof Integer) {
			return ((Integer)left).intValue() >>> right.intValue();
		}

		return left.longValue() >>> right.intValue();
	}

	/**
	 * Subtracts an object from another. Handles numeric subtraction, and map / collection removal.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Object subtract(final Object left, final Object right) {
		if (left instanceof Number || left instanceof Character) {
			if (right instanceof Number || right instanceof Character) {
				return subtract(toNumeric(left), toNumeric(right));
			}
		} else if (left instanceof Set) {
			if (right instanceof Collection) {
				final Set<Object> result = new LinkedHashSet<>((Set<?>)left);

				result.removeAll((Collection<?>)right);
				return result;
			} else if (right instanceof Map) {
				final Set<Object> result = new LinkedHashSet<>((Set<?>)left);

				result.removeAll(((Map<?, ?>)right).keySet());
				return result;
			}
		} else if (left instanceof Collection) {
			if (right instanceof Collection) {
				final List<Object> result = new ArrayList<>((Collection<?>)left);

				result.removeAll((Collection<?>)right);
				return result;
			} else if (right instanceof Map) {
				final List<Object> result = new ArrayList<>((Collection<?>)left);

				result.removeAll(((Map<?, ?>)right).keySet());
				return result;
			}
		} else if (left instanceof Map) {
			if (right instanceof Map) {
				final Map<Object, Object> result = new LinkedHashMap<>((Map<?, ?>)left);

				result.keySet().removeAll(((Map<?, ?>)right).keySet());
				return result;
			} else if (right instanceof Collection) {
				final Map<Object, Object> result = new LinkedHashMap<>((Map<?, ?>)left);

				result.keySet().removeAll((Collection<?>)right);
				return result;
			}
		}

		throw new IllegalArgumentException("Invalid objects cannot be subtracted: " + (left == null ? "null" : left.getClass().getName()) + " - " + (right == null ? "null" : right.getClass().getName()));
	}

	/**
	 * Subtracts two numerics.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	private static Number subtract(final Number left, final Number right) {
		if (left instanceof Long) {
			if (right instanceof Double) {
				return ((Long)left).longValue() - ((Double)right).doubleValue();
			}

			return ((Long)left).longValue() - right.longValue();
		} else if (left instanceof Integer) {
			if (right instanceof Long) {
				return ((Integer)left).intValue() - ((Long)right).longValue();
			} else if (right instanceof Integer) {
				return ((Integer)left).intValue() - ((Integer)right).intValue();
			}

			return ((Integer)left).intValue() - ((Double)right).doubleValue();
		}

		return ((Double)left).doubleValue() - right.doubleValue();
	}

	/**
	 * Converts an object to an integral value (either an Integer or Long).
	 *
	 * @param object the object to convert to an integral
	 * @return the integral value
	 */
	public static Number toIntegral(final Object object) {
		if (object instanceof Number) {
			if (object instanceof Long || object instanceof Integer) {
				return (Number)object;
			} else if (object instanceof Byte || object instanceof Short || object instanceof AtomicInteger) {
				return ((Number)object).intValue();
			} else if (object instanceof BigInteger || object instanceof AtomicLong) {
				return ((Number)object).longValue();
			}
		} else if (object instanceof Character) {
			return (int)((Character)object).charValue();
		}

		throw new IllegalArgumentException("Unexpected " + (object == null ? "null" : object.getClass().getName()) + " value, expecting integral value");
	}

	/**
	 * Converts an object to a numeric value (either an Integer, Long, or Double).
	 *
	 * @param object the object to convert to a numeric
	 * @return the numeric value
	 */
	public static Number toNumeric(final Object object) {
		if (object instanceof Number) {
			if (object instanceof Long || object instanceof Integer || object instanceof Double) {
				return (Number)object;
			} else if (object instanceof Float || object instanceof BigDecimal) {
				return ((Number)object).doubleValue();
			} else if (object instanceof Byte || object instanceof Short || object instanceof AtomicInteger) {
				return ((Number)object).intValue();
			} else if (object instanceof BigInteger || object instanceof AtomicLong) {
				return ((Number)object).longValue();
			}
		} else if (object instanceof Character) {
			return (int)((Character)object).charValue();
		}

		throw new IllegalArgumentException("Unexpected " + (object == null ? "null" : object.getClass().getName()) + " value, expecting numeric value");
	}

	/**
	 * Performs a bitwise XOR on two integral values.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Number xor(final Number left, final Number right) {
		if (left instanceof Integer && right instanceof Integer) {
			return ((Integer)left).intValue() ^ ((Integer)right).intValue();
		}

		return left.longValue() ^ right.longValue();
	}

	private Operands() {
	}

}
