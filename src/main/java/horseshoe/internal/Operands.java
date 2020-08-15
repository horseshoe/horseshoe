package horseshoe.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import horseshoe.RenderContext;

public final class Operands {

	/**
	 * Adds two objects together. Handles numeric addition, string concatenation, and map / collection combination.
	 *
	 * @param left the left operand
	 * @param right the right operand
	 * @return the result of the operation
	 */
	public static Object add(final Object left, final Object right) {
		if (left instanceof Number || left instanceof Character) {
			if (right instanceof Number || right instanceof Character) {
				return HorseshoeNumber.ofUnknown(left).add(HorseshoeNumber.ofUnknown(right));
			}
		} else if (left instanceof StringBuilder) {
			return ((StringBuilder)left).append(right);
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
		} else if (left instanceof String) {
			return new StringBuilder((String)left).append(right);
		}

		if (right instanceof StringBuilder) {
			final String leftString = String.valueOf(left);
			return new StringBuilder(leftString.length() + ((StringBuilder)right).length()).append(leftString).append((StringBuilder)right);
		} else if (right instanceof String) {
			return new StringBuilder().append(left).append((String)right);
		}

		throw new IllegalArgumentException("Invalid objects cannot be added: " + (left == null ? "null" : left.getClass().getName()) + " + " + (right == null ? "null" : right.getClass().getName()));
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
		if (first instanceof Number) {
			if (second instanceof Number || second instanceof Character) {
				return HorseshoeNumber.ofUnknown(first).compareTo(HorseshoeNumber.ofUnknown(second));
			}
		} else if (first instanceof Character) {
			if (second instanceof Number || second instanceof Character) {
				return HorseshoeNumber.ofUnknown(first).compareTo(HorseshoeNumber.ofUnknown(second));
			} else if (second instanceof StringBuilder || second instanceof String) {
				return first.toString().compareTo(second.toString());
			}
		} else if ((first instanceof StringBuilder || first instanceof String) &&
				(second instanceof StringBuilder || second instanceof String || second instanceof Character)) {
			return first.toString().compareTo(second.toString());
		} else if (equality) {
			return Objects.equals(first, second) ? 0 : 1;
		} else if (first instanceof Comparable) {
			return ((Comparable<Object>)first).compareTo(second);
		}

		if (equality) {
			return 1; // Indicate not equal
		}

		throw new IllegalArgumentException("Unexpected object, expecting comparable object");
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
	 * Converts an object to a boolean.
	 *
	 * @param object the object to convert to a boolean
	 * @return the result of converting the object to a boolean
	 */
	public static boolean convertToBoolean(final Object object) {
		if (object == null) {
			return false;
		} else if (object instanceof Number) {
			return HorseshoeNumber.ofUnknown(object).toBoolean();
		} else if (object instanceof Boolean) {
			return ((Boolean)object).booleanValue();
		} else if (object instanceof Character) {
			return ((Character)object).charValue() != 0;
		}

		return true;
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
				return HorseshoeNumber.ofUnknown(left).subtract(HorseshoeNumber.ofUnknown(right));
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

	private Operands() {
	}

}
