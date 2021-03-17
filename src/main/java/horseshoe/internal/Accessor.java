package horseshoe.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Accessor {

	public static final Object TO_BEGINNING = new Object();

	static final Factory FACTORY = new Factory();
	static final Object INVALID = new Object();

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	static final class MethodSignature {

		private final String name;
		private final String[] types;

		/**
		 * Creates a new method signature from a string.
		 *
		 * @param signature the signature of the method in the form [name]:[parameterType0],...
		 */
		private MethodSignature(final String signature) {
			final int endOfName = signature.indexOf(':');

			if (endOfName < 0) {
				name = signature;
				types = null;
			} else {
				int count = 0;
				int start = endOfName + 1;

				for (int i = signature.indexOf(',', start); i >= 0; i = signature.indexOf(',', i + 1)) {
					count++;
				}

				name = signature.substring(0, endOfName);
				types = new String[count + 1];

				for (int end = signature.indexOf(',', start), i = 0; end >= 0; i++, start = end + 1, end = signature.indexOf(',', start)) {
					types[i] = signature.substring(start, end);
				}

				types[count] = signature.substring(start);
			}
		}

		/**
		 * Checks if this signature matches the specified method.
		 *
		 * @param method the method to compare with this signature
		 * @return true if the method matches this signature, otherwise false
		 */
		boolean matches(final Method method) {
			if (!name.equals(method.getName())) {
				return false;
			}

			return matchesParameters(method.getParameterTypes());
		}

		/**
		 * Checks if this signature matches the specified parameters.
		 *
		 * @param parameters the parameters to compare with this signature
		 * @return true if the parameters matches this signature, otherwise false
		 */
		private boolean matchesParameters(final Class<?>[] parameters) {
			if (types != null) {
				final int length = Math.min(parameters.length, types.length);

				for (int i = 0; i < length; i++) {
					if (!types[i].isEmpty() && !types[i].equals(parameters[i].getSimpleName()) && !types[i].equals(parameters[i].getCanonicalName())) {
						return false;
					}
				}
			}

			return true;
		}

	}

	public static final class PatternMatcher implements Iterable<PatternMatcher>, MatchResult {
		private final Matcher matcher;
		private final CharSequence input;

		/**
		 * Creates a pattern matcher from a pattern and character sequence input.
		 *
		 * @param pattern the pattern used to create the pattern matcher
		 * @param input the input used to create the pattern matcher
		 * @return a new pattern matcher if the pattern is found in the input, otherwise null
		 */
		public static PatternMatcher fromInput(final Pattern pattern, final CharSequence input) {
			final Matcher matcher = pattern.matcher(input);

			if (matcher.find()) {
				return new PatternMatcher(matcher, input);
			}

			return null;
		}

		private PatternMatcher(final Matcher matcher, final CharSequence input) {
			this.matcher = matcher;
			this.input = input;
		}

		@Override
		public int start() {
			return matcher.start();
		}

		@Override
		public int start(int group) {
			return matcher.start(group);
		}

		@Override
		public int end() {
			return matcher.end();
		}

		@Override
		public int end(int group) {
			return matcher.end(group);
		}

		@Override
		public String group() {
			return matcher.group();
		}

		@Override
		public String group(int group) {
			return matcher.group(group);
		}

		/**
		 * Returns the input subsequence captured by the given named-capturing group during the previous match operation.
		 *
		 * @param name the name of a named-capturing group in this matcher's pattern
		 * @return the subsequence captured by the named group during the previous match, or null if the group failed to match part of the input
		 */
		public String group(String name) {
			return matcher.group(name);
		}

		@Override
		public int groupCount() {
			return matcher.groupCount();
		}

		@Override
		public Iterator<PatternMatcher> iterator() {
			return new Iterator<PatternMatcher>() {
				PatternMatcher current = null;
				PatternMatcher next = PatternMatcher.this;

				@Override
				public boolean hasNext() {
					if (current != next) {
						return next != null;
					}

					final Matcher nextMatcher = current.matcher.pattern().matcher(input);

					if (nextMatcher.find(current.end())) {
						next = new PatternMatcher(nextMatcher, input);
						return true;
					}

					next = null;
					return false;
				}

				@Override
				public PatternMatcher next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}

					current = next;
					return next;
				}
			};
		}

		@Override
		public String toString() {
			return matcher.group();
		}
	}

	private static final class Range {
		public final int start;
		public final int end;
		public final int size;
		public final int increment;

		public Range(final int start, final Object end, final int length) {
			this.start = calculateIndex(length, start);

			if (end == null) {
				this.end = length;
			} else if (end == TO_BEGINNING) {
				this.end = -1;
			} else {
				this.end = calculateIndex(length, ((Number)end).intValue());
			}

			if (this.end < this.start) {
				if (this.end < -1) {
					throw new ArrayIndexOutOfBoundsException(this.end);
				} else if (this.start >= length) {
					throw new ArrayIndexOutOfBoundsException(this.start);
				}

				this.size = this.start - this.end;
				this.increment = -1;
			} else {
				if (this.start < 0) {
					throw new ArrayIndexOutOfBoundsException(this.start);
				} else if (this.end > length) {
					throw new ArrayIndexOutOfBoundsException(this.end);
				}

				this.size = this.end - this.start;
				this.increment = 1;
			}
		}
	}

	/**
	 * Calculates the index of a lookup. Negative values indicate an index from the end.
	 *
	 * @param size the size of the container
	 * @param index the index to lookup
	 * @return the calculated index
	 */
	private static int calculateIndex(final int size, final int index) {
		return index < 0 ? size + index : index;
	}

	/**
	 * Gets the value of an identifier given the specified context as part of an expression.
	 *
	 * @param context the context object to resolve
	 * @return the value of the identifier
	 * @throws Throwable if the underlying operation throws
	 */
	public abstract Object get(final Object context) throws Throwable;

	/**
	 * Gets the value of a method identifier given the specified context and parameters as part of an expression.
	 *
	 * @param context the context object to resolve
	 * @param parameters the parameters to the method
	 * @return the result of invoking the method
	 * @throws Throwable if the underlying operation throws
	 */
	public Object get(final Object context, final Object... parameters) throws Throwable {
		return get(context);
	}

	/**
	 * Checks if the result is valid.
	 *
	 * @param result the result to check
	 * @return true if the result is valid, otherwise false
	 */
	public static boolean isValid(final Object result) {
		return result != INVALID;
	}

	/**
	 * Looks up a value based on a map, array, list, set, or string and the lookup operand.
	 *
	 * @param context the context object to resolve
	 * @param lookup the value to lookup within the context
	 * @param ignoreFailures true to return null for failures, false to throw exceptions
	 * @return the result of the lookup
	 */
	public static Object lookup(final Object context, final Object lookup, boolean ignoreFailures) {
		try {
			if (context == null) {
				if (!ignoreFailures) {
					throw new NullPointerException("The lookup operator cannot be applied to a null object");
				}
			} else if (context instanceof Map) {
				return lookupMap((Map<?, ?>)context, lookup);
			} else if (context instanceof List) {
				return lookupList((List<?>)context, lookup);
			} else if (context instanceof String) {
				return lookupString((String)context, lookup);
			} else if (context instanceof Set) {
				return lookupSet((Set<?>)context, lookup);
			} else {
				return lookupArray(context, lookup);
			}
		} catch (final IndexOutOfBoundsException | ClassCastException e) {
			if (!ignoreFailures) {
				throw e;
			}
		}

		return null;
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final Object context, final Object lookup) {
		final Class<?> componentType = context.getClass().getComponentType();

		if (componentType == null) {
			throw new ClassCastException("The lookup operator cannot be applied to a " + context.getClass().getName() + " object");
		} else if (!componentType.isPrimitive()) {
			return lookupArray((Object[])context, lookup);
		} else if (int.class.equals(componentType)) {
			return lookupArray((int[])context, lookup);
		} else if (byte.class.equals(componentType)) {
			return lookupArray((byte[])context, lookup);
		} else if (double.class.equals(componentType)) {
			return lookupArray((double[])context, lookup);
		} else if (boolean.class.equals(componentType)) {
			return lookupArray((boolean[])context, lookup);
		} else if (float.class.equals(componentType)) {
			return lookupArray((float[])context, lookup);
		} else if (long.class.equals(componentType)) {
			return lookupArray((long[])context, lookup);
		} else if (char.class.equals(componentType)) {
			return lookupArray((char[])context, lookup);
		}

		return lookupArray((short[])context, lookup);
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final Object[] context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final Object[] newArray = new Object[collection.size()];
			int i = 0;

			for (final Object object : collection) {
				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return newArray;
		} else if (lookup instanceof Iterable) {
			Object[] newArray = new Object[10];
			int i = 0;

			for (final Object object : (Iterable<?>)lookup) {
				if (i >= newArray.length) {
					newArray = Arrays.copyOf(newArray, newArray.length * 2);
				}

				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return i == newArray.length ? newArray : Arrays.copyOf(newArray, i);
		}

		return context[calculateIndex(context.length, ((Number)lookup).intValue())];
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final int[] context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final int[] newArray = new int[collection.size()];
			int i = 0;

			for (final Object object : collection) {
				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return newArray;
		} else if (lookup instanceof Iterable) {
			int[] newArray = new int[10];
			int i = 0;

			for (final Object object : (Iterable<?>)lookup) {
				if (i >= newArray.length) {
					newArray = Arrays.copyOf(newArray, newArray.length * 2);
				}

				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return i == newArray.length ? newArray : Arrays.copyOf(newArray, i);
		}

		return context[calculateIndex(context.length, ((Number)lookup).intValue())];
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final byte[] context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final byte[] newArray = new byte[collection.size()];
			int i = 0;

			for (final Object object : collection) {
				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return newArray;
		} else if (lookup instanceof Iterable) {
			byte[] newArray = new byte[10];
			int i = 0;

			for (final Object object : (Iterable<?>)lookup) {
				if (i >= newArray.length) {
					newArray = Arrays.copyOf(newArray, newArray.length * 2);
				}

				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return i == newArray.length ? newArray : Arrays.copyOf(newArray, i);
		}

		return context[calculateIndex(context.length, ((Number)lookup).intValue())];
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final double[] context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final double[] newArray = new double[collection.size()];
			int i = 0;

			for (final Object object : collection) {
				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return newArray;
		} else if (lookup instanceof Iterable) {
			double[] newArray = new double[10];
			int i = 0;

			for (final Object object : (Iterable<?>)lookup) {
				if (i >= newArray.length) {
					newArray = Arrays.copyOf(newArray, newArray.length * 2);
				}

				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return i == newArray.length ? newArray : Arrays.copyOf(newArray, i);
		}

		return context[calculateIndex(context.length, ((Number)lookup).intValue())];
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final boolean[] context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final boolean[] newArray = new boolean[collection.size()];
			int i = 0;

			for (final Object object : collection) {
				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return newArray;
		} else if (lookup instanceof Iterable) {
			boolean[] newArray = new boolean[10];
			int i = 0;

			for (final Object object : (Iterable<?>)lookup) {
				if (i >= newArray.length) {
					newArray = Arrays.copyOf(newArray, newArray.length * 2);
				}

				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return i == newArray.length ? newArray : Arrays.copyOf(newArray, i);
		}

		return context[calculateIndex(context.length, ((Number)lookup).intValue())];
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final float[] context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final float[] newArray = new float[collection.size()];
			int i = 0;

			for (final Object object : collection) {
				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return newArray;
		} else if (lookup instanceof Iterable) {
			float[] newArray = new float[10];
			int i = 0;

			for (final Object object : (Iterable<?>)lookup) {
				if (i >= newArray.length) {
					newArray = Arrays.copyOf(newArray, newArray.length * 2);
				}

				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return i == newArray.length ? newArray : Arrays.copyOf(newArray, i);
		}

		return context[calculateIndex(context.length, ((Number)lookup).intValue())];
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final long[] context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final long[] newArray = new long[collection.size()];
			int i = 0;

			for (final Object object : collection) {
				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return newArray;
		} else if (lookup instanceof Iterable) {
			long[] newArray = new long[10];
			int i = 0;

			for (final Object object : (Iterable<?>)lookup) {
				if (i >= newArray.length) {
					newArray = Arrays.copyOf(newArray, newArray.length * 2);
				}

				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return i == newArray.length ? newArray : Arrays.copyOf(newArray, i);
		}

		return context[calculateIndex(context.length, ((Number)lookup).intValue())];
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final char[] context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final char[] newArray = new char[collection.size()];
			int i = 0;

			for (final Object object : collection) {
				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return newArray;
		} else if (lookup instanceof Iterable) {
			char[] newArray = new char[10];
			int i = 0;

			for (final Object object : (Iterable<?>)lookup) {
				if (i >= newArray.length) {
					newArray = Arrays.copyOf(newArray, newArray.length * 2);
				}

				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return i == newArray.length ? newArray : Arrays.copyOf(newArray, i);
		}

		return context[calculateIndex(context.length, ((Number)lookup).intValue())];
	}

	/**
	 * Looks up a value based on an array and the lookup operand.
	 *
	 * @param context the array object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArray(final short[] context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final short[] newArray = new short[collection.size()];
			int i = 0;

			for (final Object object : collection) {
				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return newArray;
		} else if (lookup instanceof Iterable) {
			short[] newArray = new short[10];
			int i = 0;

			for (final Object object : (Iterable<?>)lookup) {
				if (i >= newArray.length) {
					newArray = Arrays.copyOf(newArray, newArray.length * 2);
				}

				newArray[i] = context[calculateIndex(context.length, ((Number)object).intValue())];
				i++;
			}

			return i == newArray.length ? newArray : Arrays.copyOf(newArray, i);
		}

		return context[calculateIndex(context.length, ((Number)lookup).intValue())];
	}

	/**
	 * Looks up a value based on an array and a range.
	 *
	 * @param context the array object used to perform the lookup
	 * @param start the start of the range to lookup
	 * @param end the end of the range to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArrayRange(final Object context, final int start, final Object end) {
		final Class<?> componentType = context.getClass().getComponentType();

		if (componentType == null) {
			throw new ClassCastException("The lookup operator cannot be applied to a " + context.getClass().getName() + " object");
		} else if (!componentType.isPrimitive()) {
			final Object[] array = (Object[])context;
			final Range range = new Range(start, end, array.length);
			final Object[] newArray = new Object[range.size];

			for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
				newArray[j] = array[i];
			}

			return newArray;
		} else if (int.class.equals(componentType)) {
			final int[] array = (int[])context;
			final Range range = new Range(start, end, array.length);
			final int[] newArray = new int[range.size];

			for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
				newArray[j] = array[i];
			}

			return newArray;
		} else if (byte.class.equals(componentType)) {
			final byte[] array = (byte[])context;
			final Range range = new Range(start, end, array.length);
			final byte[] newArray = new byte[range.size];

			for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
				newArray[j] = array[i];
			}

			return newArray;
		} else if (double.class.equals(componentType)) {
			final double[] array = (double[])context;
			final Range range = new Range(start, end, array.length);
			final double[] newArray = new double[range.size];

			for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
				newArray[j] = array[i];
			}

			return newArray;
		} else if (boolean.class.equals(componentType)) {
			final boolean[] array = (boolean[])context;
			final Range range = new Range(start, end, array.length);
			final boolean[] newArray = new boolean[range.size];

			for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
				newArray[j] = array[i];
			}

			return newArray;
		} else if (float.class.equals(componentType)) {
			final float[] array = (float[])context;
			final Range range = new Range(start, end, array.length);
			final float[] newArray = new float[range.size];

			for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
				newArray[j] = array[i];
			}

			return newArray;
		} else if (long.class.equals(componentType)) {
			final long[] array = (long[])context;
			final Range range = new Range(start, end, array.length);
			final long[] newArray = new long[range.size];

			for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
				newArray[j] = array[i];
			}

			return newArray;
		} else if (char.class.equals(componentType)) {
			final char[] array = (char[])context;
			final Range range = new Range(start, end, array.length);
			final char[] newArray = new char[range.size];

			for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
				newArray[j] = array[i];
			}

			return newArray;
		}

		final short[] array = (short[])context;
		final Range range = new Range(start, end, array.length);
		final short[] newArray = new short[range.size];

		for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
			newArray[j] = array[i];
		}

		return newArray;
	}

	/**
	 * Looks up a value based on a list and the lookup operand.
	 *
	 * @param context the list object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	@SuppressWarnings("unchecked")
	private static Object lookupList(final List<?> context, final Object lookup) {
		if (lookup instanceof Iterable) {
			final List<Object> list = (lookup instanceof Collection ? new ArrayList<>(((Collection<?>)lookup).size()) : new ArrayList<>());

			for (final Number number : (Iterable<Number>)lookup) {
				list.add(context.get(calculateIndex(context.size(), number.intValue())));
			}

			return list;
		}

		return context.get(calculateIndex(context.size(), ((Number)lookup).intValue()));
	}

	/**
	 * Looks up a value based on a list and a range.
	 *
	 * @param context the list object used to perform the lookup
	 * @param start the start of the range to lookup
	 * @param end the end of the range to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupListRange(final List<?> context, final int start, final Object end) {
		final Range range = new Range(start, end, context.size());
		final List<Object> newList = new ArrayList<>(range.size);

		if (range.increment > 0) {
			for (final ListIterator<?> it = context.listIterator(range.start); it.nextIndex() != range.end; ) {
				newList.add(it.next());
			}
		} else {
			for (final ListIterator<?> it = context.listIterator(range.start + 1); it.previousIndex() != range.end; ) {
				newList.add(it.previous());
			}
		}

		return newList;
	}

	/**
	 * Looks up a value based on a map and the lookup operand.
	 *
	 * @param context the map object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupMap(final Map<?, ?> context, final Object lookup) {
		if (lookup instanceof Iterable) {
			final Map<Object, Object> map = new LinkedHashMap<>();

			for (final Object object : (Iterable<?>)lookup) {
				final Object value = context.get(object);

				if (value != null || context.containsKey(object)) {
					map.put(object, value);
				}
			}

			return map;
		}

		return context.get(lookup);
	}

	/**
	 * Looks up a value based on a map and a range.
	 *
	 * @param context the map object used to perform the lookup
	 * @param start the start of the range to lookup
	 * @param end the end of the range to lookup
	 * @return the result of the lookup
	 */
	@SuppressWarnings("unchecked")
	private static <K extends Comparable<K>> Map<K, Object> lookupMapRange(final Map<K, ?> context, final K start, final Object end) {
		final Map<K, Object> map = new LinkedHashMap<>();

		if (end == null) {
			for (final Entry<K, ?> entry : context.entrySet()) {
				final K key = entry.getKey();

				if (key != null && start.compareTo(key) <= 0) {
					map.put(key, entry.getValue());
				}
			}
		} else if (end == TO_BEGINNING) {
			for (final Entry<K, ?> entry : context.entrySet()) {
				final K key = entry.getKey();

				if (key != null && start.compareTo(key) >= 0) {
					map.put(key, entry.getValue());
				}
			}
		} else {
			final K endKey = (K)end;
			final int startEnd = start.compareTo(endKey);

			if (startEnd == 0) {
				return map;
			} else if (startEnd < 0) {
				for (final Entry<K, ?> entry : context.entrySet()) {
					final K key = entry.getKey();

					if (key != null && start.compareTo(key) <= 0 && key.compareTo(endKey) < 0) {
						map.put(key, entry.getValue());
					}
				}
			} else {
				for (final Entry<K, ?> entry : context.entrySet()) {
					final K key = entry.getKey();

					if (key != null && start.compareTo(key) >= 0 && key.compareTo(endKey) > 0) {
						map.put(key, entry.getValue());
					}
				}
			}
		}

		return map;
	}

	/**
	 * Looks up a value based on a map, array, list, set, or string and a range.
	 *
	 * @param context the context object to resolve
	 * @param start the start of the range to lookup
	 * @param end the end of the range to lookup
	 * @param ignoreFailures true to return null for failures, false to throw exceptions
	 * @return the result of the lookup
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> Object lookupRange(final Object context, final T start, final Object end, final boolean ignoreFailures) {
		try {
			if (context == null) {
				if (!ignoreFailures) {
					throw new NullPointerException("The lookup operator cannot be applied to a null object");
				}
			} else if (context instanceof Map) {
				return lookupMapRange((Map<T, ?>)context, start, end);
			} else if (context instanceof List) {
				return lookupListRange((List<?>)context, ((Number)start).intValue(), end);
			} else if (context instanceof String) {
				return lookupStringRange((String)context, ((Number)start).intValue(), end);
			} else if (context instanceof Set) {
				return lookupSetRange((Set<T>)context, start, end);
			} else {
				return lookupArrayRange(context, ((Number)start).intValue(), end);
			}
		} catch (final IndexOutOfBoundsException | ClassCastException e) {
			if (!ignoreFailures) {
				throw e;
			}
		}

		return null;
	}

	/**
	 * Looks up a value based on a set and the lookup operand.
	 *
	 * @param context the set object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupSet(final Set<?> context, final Object lookup) {
		if (lookup instanceof Iterable) {
			final Set<Object> set = new LinkedHashSet<>();

			for (final Object object : (Iterable<?>)lookup) {
				if (context.contains(object)) {
					set.add(object);
				}
			}

			return set;
		}

		return context.contains(lookup) ? lookup : null;
	}

	/**
	 * Looks up a value based on a set and a range.
	 *
	 * @param context the set object used to perform the lookup
	 * @param start the start of the range to lookup
	 * @param end the end of the range to lookup
	 * @return the result of the lookup
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> Set<T> lookupSetRange(final Set<T> context, final T start, final Object end) {
		final Set<T> set = new LinkedHashSet<>();

		if (end == null) {
			for (final T entry : context) {
				if (entry != null && start.compareTo(entry) <= 0) {
					set.add(entry);
				}
			}
		} else if (end == TO_BEGINNING) {
			for (final T entry : context) {
				if (entry != null && start.compareTo(entry) >= 0) {
					set.add(entry);
				}
			}
		} else {
			final T endValue = (T)end;
			final int startEnd = start.compareTo(endValue);

			if (startEnd == 0) {
				return set;
			} else if (startEnd < 0) {
				for (final T entry : context) {
					if (entry != null && start.compareTo(entry) <= 0 && entry.compareTo(endValue) < 0) {
						set.add(entry);
					}
				}
			} else {
				for (final T entry : context) {
					if (entry != null && start.compareTo(entry) >= 0 && entry.compareTo(endValue) > 0) {
						set.add(entry);
					}
				}
			}
		}

		return set;
	}

	/**
	 * Looks up a value based on a list and the lookup operand.
	 *
	 * @param context the list object used to perform the lookup
	 * @param lookup the value to lookup within the list
	 * @return the result of the lookup
	 */
	@SuppressWarnings("unchecked")
	private static Object lookupString(final String context, final Object lookup) {
		if (lookup instanceof Iterable) {
			StringBuilder sb = (lookup instanceof Collection ? new StringBuilder(((Collection<?>)lookup).size()) : new StringBuilder());

			for (final Number number : (Iterable<Number>)lookup) {
				sb.append(context.charAt(calculateIndex(context.length(), number.intValue())));
			}

			return sb.toString();
		} else if (lookup instanceof Pattern) {
			return PatternMatcher.fromInput((Pattern)lookup, context);
		}

		return context.charAt(calculateIndex(context.length(), ((Number)lookup).intValue()));
	}

	/**
	 * Looks up a value based on a string and a range.
	 *
	 * @param context the string used to perform the lookup
	 * @param start the start of the range to lookup
	 * @param end the end of the range to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupStringRange(final String context, final int start, final Object end) {
		final Range range = new Range(start, end, context.length());
		final char[] newArray = new char[range.size];

		for (int i = range.start, j = 0; j < range.size; i += range.increment, j++) {
			newArray[j] = context.charAt(i);
		}

		return new String(newArray);
	}

	/**
	 * Gets the value of an identifier given the specified context as part of an expression. This method may return an invalid object. All return values should be checked with the {@link #isValid()} method.
	 *
	 * @param context the context object to resolve
	 * @return the value of the identifier or an invalid value
	 * @throws Throwable if the underlying operation throws
	 */
	public Object tryGet(final Object context) throws Throwable {
		return get(context);
	}

	/**
	 * Gets the value of a method identifier given the specified context and parameters as part of an expression. This method may return an invalid object. All return values should be checked with the {@link #isValid()} method.
	 *
	 * @param context the context object to resolve
	 * @param parameters the parameters to the method
	 * @return the result of invoking the method or an invalid value
	 * @throws Throwable if the underlying operation throws
	 */
	public Object tryGet(final Object context, final Object... parameters) throws Throwable {
		return get(context, parameters);
	}

	/**
	 * A map accessor provides access to a value in a map using the specified key.
	 */
	private static class MapAccessor extends Accessor {

		final String key;

		MapAccessor(final String key) {
			this.key = key;
		}

		@Override
		public Object get(final Object context) {
			return ((Map<?, ?>)context).get(key);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object tryGet(final Object context) {
			return Utilities.getMapValueOrDefault((Map<?, Object>)context, key, INVALID);
		}

	}

	/**
	 * A method accessor provides access to a method.
	 */
	private static class MethodAccessor extends Accessor {

		final MethodHandle methodHandle;

		MethodAccessor(final MethodHandle methodHandle) {
			this.methodHandle = methodHandle;
		}

		/**
		 * Creates a new method accessor.
		 *
		 * @param parent the parent class for the method
		 * @param signature the signature of the method in the form [name]:[parameterType0],...
		 * @param parameterCount the number of parameters that the method takes
		 * @return the new accessor, or null if no method could be found
		 */
		public static Accessor create(final Class<?> parent, final MethodSignature signature, final int parameterCount) {
			final Set<MethodHandle> methodHandles = new LinkedHashSet<>(2);

			// Find all matching methods in the first public ancestor class, including all interfaces along the way
			for (Class<?> ancestor = parent; true; ancestor = ancestor.getSuperclass()) {
				if (Modifier.isPublic(ancestor.getModifiers()) || ancestor.isAnonymousClass()) {
					try {
						MethodHandler.getPublicMethods(methodHandles, ancestor, false, signature, parameterCount);
						break;
					} catch (final IllegalAccessException e) {
						// Ignore illegal access errors
					}
				}

				getPublicInterfaceMethods(methodHandles, ancestor, signature, parameterCount);

				if (parameterCount == 0 && !methodHandles.isEmpty()) {
					return new MethodAccessor(methodHandles.iterator().next());
				}
			}

			if (methodHandles.size() > 1) {
				return new MethodsAccessor(methodHandles.toArray(new MethodHandle[0]));
			} else if (!methodHandles.isEmpty()) {
				return new MethodAccessor(methodHandles.iterator().next());
			}

			return null;
		}

		/**
		 * Creates a new static or class method accessor.
		 *
		 * @param parent the parent class for the method
		 * @param signature the signature of the method in the form [name]:[parameterType0],...
		 * @param parameterCount the number of parameters that the method takes
		 * @return the new accessor, or null if no method could be found
		 * @throws IllegalAccessException if a matching method is found, but it cannot be accessed
		 */
		public static Accessor createStaticOrClass(final Class<?> parent, final MethodSignature signature, final int parameterCount) throws IllegalAccessException {
			final List<MethodHandle> methodHandles = new ArrayList<>(2);

			// Find all matching static methods
			if (Modifier.isPublic(parent.getModifiers())) {
				try {
					MethodHandler.getPublicMethods(methodHandles, parent, true, signature, parameterCount);
				} catch (final IllegalAccessException e) {
					// Ignore illegal access errors
				}
			}

			// Find the Class<?> method
			if (methodHandles.isEmpty()) {
				MethodHandler.getPublicMethods(methodHandles, Class.class, false, signature, parameterCount);
			}

			if (methodHandles.size() > 1) {
				return new MethodsAccessor(methodHandles.toArray(new MethodHandle[0]));
			} else if (!methodHandles.isEmpty()) {
				return new MethodAccessor(methodHandles.get(0));
			}

			return null;
		}

		@Override
		public Object get(final Object context) throws ReflectiveOperationException {
			return methodHandle;
		}

		@Override
		public Object get(final Object context, final Object... parameters) throws Throwable {
			if (methodHandle.type().parameterCount() == 1) {
				return methodHandle.invoke(parameters);
			}

			return methodHandle.invoke(context, parameters);
		}

		/**
		 * Gets the public interface methods of the specified parent class that match the given information.
		 *
		 * @param methodHandles the collection used to store the matching method handles
		 * @param parent the parent class
		 * @param signature the method signature
		 * @param parameterCount the parameter count of the method
		 */
		public static void getPublicInterfaceMethods(final Collection<MethodHandle> methodHandles, final Class<?> parent, final MethodSignature signature, final int parameterCount) {
			for (final Class<?> iface : parent.getInterfaces()) {
				try {
					MethodHandler.getPublicMethods(methodHandles, iface, false, signature, parameterCount);

					if (parameterCount == 0 && !methodHandles.isEmpty()) {
						return;
					}
				} catch (final IllegalAccessException e) {
					// Ignore illegal access issues with a specific interface
				}

				getPublicInterfaceMethods(methodHandles, iface, signature, parameterCount);
			}
		}

	}

	/**
	 * A methods accessor allows access to one of a number of methods in a class.
	 */
	static class MethodsAccessor extends Accessor {

		final MethodHandle[] methodHandles;
		int mruIndex = 0;

		MethodsAccessor(final MethodHandle[] methodHandles) {
			this.methodHandles = methodHandles;
		}

		@Override
		public Object get(final Object context) throws ReflectiveOperationException {
			return methodHandles;
		}

		@Override
		public Object get(final Object context, final Object... parameters) throws Throwable {
			return getWithDefault(null, context, parameters);
		}

		@Override
		public Object tryGet(final Object context, final Object... parameters) throws Throwable {
			return getWithDefault(INVALID, context, parameters);
		}

		private Object getWithDefault(final Object defaultValue, final Object context, final Object... parameters) throws Throwable {
			final int mru = mruIndex;

			// We could do something fancy and try to find the most correct method to call, but for now just try to call them all starting with the most recently used.
			//  Note: this may invoke the wrong method for closely-related overloads.
			try {
				if (methodHandles[mru].type().parameterCount() == 1) {
					return methodHandles[mru].invoke(parameters);
				}

				return methodHandles[mru].invoke(context, parameters);
			} catch (final WrongMethodTypeException | ClassCastException e) {
				for (int i = 0; i < methodHandles.length; i++) {
					if (i != mru) {
						try {
							final Object result;

							if (methodHandles[i].type().parameterCount() == 1) {
								result = methodHandles[i].invoke(parameters);
							} else {
								result = methodHandles[i].invoke(context, parameters);
							}

							mruIndex = i;
							return result;
						} catch (final WrongMethodTypeException | ClassCastException e1) {
							// Ignore failures along the way
						}
					}
				}

				return defaultValue;
			}
		}

	}

	static class Factory {

		/**
		 * Creates an accessor for the given context and identifier.
		 *
		 * @param context the context object on which to invoke the accessor
		 * @param identifier the identifier used to create the accessor
		 * @return a new accessor corresponding to the given context and identifier
		 * @throws IllegalAccessException if a matching method or field is found, but it cannot be accessed
		 */
		public Accessor create(final Object context, final Identifier identifier) throws IllegalAccessException {
			final Class<?> contextClass = context.getClass();

			if (identifier.isMethod()) { // Method
				final MethodSignature signature = new MethodSignature(identifier.getName());

				if (!Class.class.equals(contextClass)) { // Instance method
					return MethodAccessor.create(contextClass, signature, identifier.getParameterCount());
				} else if ("new".equals(signature.name)) { // Object creation method
					return createNewObjectAccessor((Class<?>)context, signature, identifier.getParameterCount());
				}

				return MethodAccessor.createStaticOrClass((Class<?>)context, signature, identifier.getParameterCount());
			} else if (Class.class.equals(contextClass)) { // Static field
				return createStaticFieldAccessor((Class<?>)context, identifier.getName());
			}

			final Accessor accessor = Accessors.get(contextClass, identifier);

			if (accessor != null) {
				return accessor;
			} else if (Map.class.isAssignableFrom(contextClass)) {
				return new MapAccessor(identifier.getName());
			}

			// Field
			return createFieldAccessor(contextClass, identifier.getName());
		}

		/**
		 * Creates a new field accessor.
		 *
		 * @param parent the parent class for the field
		 * @param fieldName the name of the field
		 * @return the new accessor, or null if no field could be found
		 * @throws IllegalAccessException if a matching field is found, but it cannot be accessed
		 */
		private static Accessor createFieldAccessor(final Class<?> parent, final String fieldName) throws IllegalAccessException {
			// Find first matching field
			for (Class<?> ancestor = parent; ancestor != null; ancestor = ancestor.getSuperclass()) {
				if (Modifier.isPublic(ancestor.getModifiers())) {
					for (final Field field : ancestor.getFields()) {
						if (!Modifier.isStatic(field.getModifiers()) && fieldName.equals(field.getName())) {
							final MethodHandle getter = LOOKUP.unreflectGetter(field).asType(MethodType.methodType(Object.class, Object.class));

							return new Accessor() {
								@Override
								public Object get(final Object context) throws Throwable {
									return getter.invokeExact(context);
								}
							};
						}
					}
				}
			}

			return null;
		}

		/**
		 * Creates an accessor that creates a new instance of an object of the specified type.
		 *
		 * @param type the type of the new object that will be created
		 * @param signature the signature of the constructor in the form new:[parameterType0],...
		 * @param parameterCount the number of parameters that the constructor takes
		 * @return the new accessor, or null if no constructor could be found
		 */
		private static Accessor createNewObjectAccessor(final Class<?> type, final MethodSignature signature, final int parameterCount) {
			final List<MethodHandle> methodHandles = new ArrayList<>(2);

			// Find all matching methods in the first public ancestor class, including all interfaces along the way
			for (final Constructor<?> constructor : type.getConstructors()) {
				if (constructor.getParameterTypes().length == parameterCount && signature.matchesParameters(constructor.getParameterTypes())) {
					try {
						methodHandles.add(LOOKUP.unreflectConstructor(constructor).asSpreader(Object[].class, parameterCount));

						if (parameterCount == 0) {
							break;
						}
					} catch (final IllegalAccessException e) {
						// Probably a java 9+ module access error
					}
				}
			}

			if (methodHandles.size() > 1) {
				return new MethodsAccessor(methodHandles.toArray(new MethodHandle[0]));
			} else if (!methodHandles.isEmpty()) {
				return new MethodAccessor(methodHandles.get(0));
			}

			return null;
		}

		/**
		 * Creates a new static field accessor.
		 *
		 * @param parent the parent class for the field
		 * @param fieldName the name of the field
		 * @return the new accessor, or null if no field could be found
		 * @throws IllegalAccessException if a matching field is found, but it cannot be accessed
		 */
		public static Accessor createStaticFieldAccessor(final Class<?> parent, final String fieldName) throws IllegalAccessException {
			// Get public static fields only from the current class if it is public
			if (Modifier.isPublic(parent.getModifiers())) {
				for (final Field field : parent.getFields()) {
					if (Modifier.isStatic(field.getModifiers()) && fieldName.equals(field.getName())) {
						final MethodHandle getter = LOOKUP.unreflectGetter(field).asType(MethodType.methodType(Object.class));

						return new Accessor() {
							@Override
							public Object get(final Object context) throws Throwable {
								return getter.invokeExact();
							}
						};
					}
				}
			}

			return null;
		}

	}

}
