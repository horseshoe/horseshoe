package horseshoe.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public abstract class Accessor {

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
	static final Factory FACTORY = new Factory();
	static final Object INVALID = new Object();

	private static final class MethodSignature {

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
		private boolean matches(final Method method) {
			if (!name.equals(method.getName())) {
				return false;
			}

			if (types != null) {
				final Class<?>[] parameterTypes = method.getParameterTypes();
				final int length = Math.min(parameterTypes.length, types.length);

				for (int i = 0; i < length; i++) {
					if (!types[i].isEmpty() && !types[i].equals(parameterTypes[i].getSimpleName()) && !types[i].equals(parameterTypes[i].getCanonicalName())) {
						return false;
					}
				}
			}

			return true;
		}

	}

	private static final class Range {
		public final int start;
		public final int end;
		public final int size;
		public final int increment;

		public Range(final int start, final int end, final int length) {
			this.start = calculateIndex(length, start);
			this.end = calculateIndex(length, end);

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
			if (context instanceof Map) {
				return lookupMap((Map<?, ?>)context, lookup);
			} else if (context instanceof List) {
				return lookupList((List<?>)context, lookup);
			} else if (context instanceof String) {
				return lookupString((String)context, lookup);
			} else if (context instanceof Set) {
				return lookupSet((Set<?>)context, lookup);
			}

			return lookupArray(context, lookup);
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
	@SuppressWarnings("unchecked")
	private static Object lookupArray(final Object context, final Object lookup) {
		if (lookup instanceof Iterable) {
			final List<Object> list = (lookup instanceof Collection ? new ArrayList<>(((Collection<?>)lookup).size()) : new ArrayList<>());

			for (final Number number : (Iterable<Number>)lookup) {
				list.add(lookupArray(context, number.intValue()));
			}

			return list;
		}

		final Class<?> componentType = context.getClass().getComponentType();

		if (componentType == null) {
			throw new ClassCastException(context.getClass().getName() + " cannot be cast to an array type");
		} else if (!componentType.isPrimitive()) {
			return ((Object[])context)[((Number)lookup).intValue()];
		} else if (int.class.equals(componentType)) {
			return ((int[])context)[((Number)lookup).intValue()];
		} else if (byte.class.equals(componentType)) {
			return ((byte[])context)[((Number)lookup).intValue()];
		} else if (double.class.equals(componentType)) {
			return ((double[])context)[((Number)lookup).intValue()];
		} else if (boolean.class.equals(componentType)) {
			return ((boolean[])context)[((Number)lookup).intValue()];
		} else if (float.class.equals(componentType)) {
			return ((float[])context)[((Number)lookup).intValue()];
		} else if (long.class.equals(componentType)) {
			return ((long[])context)[((Number)lookup).intValue()];
		} else if (char.class.equals(componentType)) {
			return ((char[])context)[((Number)lookup).intValue()];
		}

		return ((short[])context)[((Number)lookup).intValue()];
	}

	/**
	 * Looks up a value based on an array and a range.
	 *
	 * @param context the array object used to perform the lookup
	 * @param start the start of the range to lookup
	 * @param end the end of the range to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupArrayRange(final Object context, final int start, final int end) {
		final Class<?> componentType = context.getClass().getComponentType();

		if (componentType == null) {
			throw new ClassCastException(context.getClass().getName() + " cannot be cast to an array type");
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
	private static Object lookupListRange(final List<?> context, final int start, final int end) {
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
	private static Object lookupMapRange(final Map<?, ?> context, final int start, final int end) {
		final Map<Object, Object> map = new LinkedHashMap<>();
		final int increment = end < start ? -1 : 1;

		for (int i = start; i != end; i += increment) {
			final Integer integer = Integer.valueOf(i);
			final Object value = context.get(integer);

			if (value != null || context.containsKey(integer)) {
				map.put(integer, value);
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
	public static Object lookupRange(final Object context, final Object start, final Object end, final boolean ignoreFailures) {
		try {
			if (context instanceof Map) {
				return lookupMapRange((Map<?, ?>)context, ((Number)start).intValue(), ((Number)end).intValue());
			} else if (context instanceof List) {
				return lookupListRange((List<?>)context, ((Number)start).intValue(), ((Number)end).intValue());
			} else if (context instanceof String) {
				return lookupStringRange((String)context, ((Number)start).intValue(), ((Number)end).intValue());
			} else if (context instanceof Set) {
				return lookupSetRange((Set<?>)context, ((Number)start).intValue(), ((Number)end).intValue());
			}

			return lookupArrayRange(context, ((Number)start).intValue(), ((Number)end).intValue());
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
	private static Object lookupSetRange(final Set<?> context, final int start, final int end) {
		final Set<Object> set = new LinkedHashSet<>();
		final int increment = end < start ? -1 : 1;

		for (int i = start; i != end; i += increment) {
			final Integer integer = Integer.valueOf(i);

			if (context.contains(integer)) {
				set.add(integer);
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
	private static Object lookupStringRange(final String context, final int start, final int end) {
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
	 * An array length accessor provides access to the length of an array.
	 */
	static class ArrayLengthAccessor extends Accessor {
		@Override
		public Object get(final Object context) throws ReflectiveOperationException {
			return Array.getLength(context);
		}
	}

	/**
	 * A map accessor factory allows a new map accessor to be created.
	 */
	static class MapAccessorFactory {

		/**
		 * Creates a new map accessor.
		 *
		 * @param key the key used to get the value from the map
		 * @return the map accessor
		 */
		Accessor create(final String key) {
			return new MapAccessor(key);
		}

	}

	/**
	 * A map accessor provides access to a value in a map using the specified key.
	 */
	static class MapAccessor extends Accessor {

		private static final MapAccessorFactory FACTORY;

		final String key;

		static {
			MapAccessorFactory factory = new MapAccessorFactory();

			if (Properties.JAVA_VERSION >= 8.0) {
				try {
					factory = (MapAccessorFactory)Accessor.class.getClassLoader().loadClass(Accessor.class.getName() + "_8$" + MapAccessorFactory.class.getSimpleName()).getConstructor().newInstance();
				} catch (final ReflectiveOperationException e) {
					throw new ExceptionInInitializerError("Failed to load Java 8 specialization: " + e.getMessage());
				}
			}

			FACTORY = factory;
		}

		MapAccessor(final String key) {
			this.key = key;
		}

		@Override
		public Object get(final Object context) {
			return ((Map<?, ?>)context).get(key);
		}

		@Override
		public Object tryGet(final Object context) {
			final Map<?, ?> map = (Map<?, ?>)context;
			final Object result = map.get(key);

			return result != null || map.containsKey(key) ? result : INVALID;
		}

	}

	/**
	 * A method accessor provides access to a method.
	 */
	static class MethodAccessor extends Accessor {

		final Method method;

		MethodAccessor(final Method method) {
			this.method = method;
		}

		/**
		 * Creates a new method accessor.
		 *
		 * @param parent the parent class for the method
		 * @param methodSignature the signature of the method in the form [name]:[parameterType0],...
		 * @param parameterCount the number of parameters that the method takes
		 * @return the new accessor, or null if no method could be found
		 */
		public static Accessor create(final Class<?> parent, final String methodSignature, final int parameterCount) {
			final MethodSignature signature = new MethodSignature(methodSignature);
			final List<Method> methods = new ArrayList<>(2);

			// Find all matching methods in the first public ancestor class, including all interfaces along the way
			for (Class<?> ancestor = parent; true; ancestor = ancestor.getSuperclass()) {
				if (Modifier.isPublic(ancestor.getModifiers())) {
					getPublicMethods(methods, ancestor, false, signature, parameterCount);
					break;
				}

				for (final Class<?> iface : ancestor.getInterfaces()) {
					if (Modifier.isPublic(iface.getModifiers())) {
						getPublicMethods(methods, iface, false, signature, parameterCount);

						if (parameterCount == 0 && !methods.isEmpty()) {
							return new MethodAccessor(methods.get(0));
						}
					}
				}
			}

			if (methods.size() > 1) {
				return new MethodsAccessor(methods.toArray(new Method[0]));
			} else if (!methods.isEmpty()) {
				return new MethodAccessor(methods.get(0));
			}

			return null;
		}

		/**
		 * Creates a new static or class method accessor.
		 *
		 * @param parent the parent class for the method
		 * @param methodSignature the signature of the method in the form [name]:[parameterType0],...
		 * @param parameterCount the number of parameters that the method takes
		 * @return the new accessor, or null if no method could be found
		 */
		public static Accessor createStaticOrClass(final Class<?> parent, final String methodSignature, final int parameterCount) {
			final MethodSignature signature = new MethodSignature(methodSignature);

			// Find all matching static methods
			if (Modifier.isPublic(parent.getModifiers())) {
				final List<Method> methods = new ArrayList<>(2);

				getPublicMethods(methods, parent, true, signature, parameterCount);

				if (methods.size() > 1) {
					return new MethodsAccessor(methods.toArray(new Method[0]));
				} else if (!methods.isEmpty()) {
					return new MethodAccessor(methods.get(0));
				}
			}

			// Find the Class<?> method
			for (final Method method : Class.class.getMethods()) {
				if (!Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == parameterCount && signature.matches(method)) {
					method.setAccessible(true); // By-pass internal checks on access
					return new MethodAccessor(method);
				}
			}

			return null;
		}

		@Override
		public Object get(final Object context) throws ReflectiveOperationException {
			return method;
		}

		@Override
		public Object get(final Object context, final Object... parameters) throws ReflectiveOperationException {
			try {
				return method.invoke(context, parameters);
			} catch (final ReflectiveOperationException e) {
				throw new ReflectiveOperationException("Failed to invoke method \"" + method.getName() + "\": " + e.getMessage(), e);
			}
		}

		/**
		 * Gets the public methods of the specified parent class that match the given information.
		 *
		 * @param methods the list used to store the matching methods
		 * @param parent the parent class
		 * @param isStatic true to match only static methods, false to match only non-static methods
		 * @param signature the method signature
		 * @param parameterCount the parameter count of the method
		 */
		public static void getPublicMethods(final List<Method> methods, final Class<?> parent, final boolean isStatic, final MethodSignature signature, final int parameterCount) {
			for (final Method method : parent.getMethods()) {
				if (Modifier.isStatic(method.getModifiers()) == isStatic && method.getParameterTypes().length == parameterCount && signature.matches(method)) {
					try {
						method.setAccessible(true); // By-pass internal checks on access
						methods.add(method);

						if (parameterCount == 0) {
							return;
						}
					} catch (final SecurityException e) {
						// Probably a java 9+ module access error
					}
				}
			}
		}

	}

	/**
	 * A methods accessor allows access to one of a number of methods in a class.
	 */
	static class MethodsAccessor extends Accessor {

		final Method[] methods;
		int mruIndex = 0;

		MethodsAccessor(final Method[] methods) {
			this.methods = methods;
		}

		@Override
		public Object get(final Object context) throws ReflectiveOperationException {
			return methods;
		}

		@Override
		public Object get(final Object context, final Object... parameters) throws ReflectiveOperationException {
			return getWithDefault(null, context, parameters);
		}

		@Override
		public Object tryGet(final Object context, final Object... parameters) throws ReflectiveOperationException {
			return getWithDefault(INVALID, context, parameters);
		}

		private Object getWithDefault(final Object defaultValue, final Object context, final Object... parameters) throws ReflectiveOperationException {
			final int mru = mruIndex;

			// We could do something fancy and try to find the most correct method to call, but for now just try to call them all starting with the most recently used.
			//  Note: this may invoke the wrong method for closely-related overloads.
			try {
				return methods[mru].invoke(context, parameters);
			} catch (final IllegalArgumentException e) {
				for (int i = 0; i < methods.length; i++) {
					if (i != mru) {
						try {
							final Object result = methods[i].invoke(context, parameters);
							mruIndex = i;
							return result;
						} catch (final IllegalArgumentException e1) {
							// Ignore failures along the way
						}
					}
				}

				return defaultValue;
			}
		}

	}

	static class Factory {

		public Accessor create(final Object context, final Identifier identifier) throws IllegalAccessException {
			final Class<?> contextClass = context.getClass();

			if (identifier.isMethod()) { // Method
				if (Class.class.equals(contextClass)) { // Class method
					return MethodAccessor.createStaticOrClass((Class<?>)context, identifier.getName(), identifier.getParameterCount());
				}

				return MethodAccessor.create(contextClass, identifier.getName(), identifier.getParameterCount());
			} else if (Class.class.equals(contextClass)) { // Static field
				return createStaticFieldAccessor((Class<?>)context, identifier.getName());
			} else if (Map.class.isAssignableFrom(contextClass)) {
				return MapAccessor.FACTORY.create(identifier.getName());
			} else if (contextClass.isArray() && "length".equals(identifier.getName())) {
				return new ArrayLengthAccessor();
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
		 * @throws IllegalAccessException never
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
		 * Creates a new static field accessor.
		 *
		 * @param parent the parent class for the field
		 * @param fieldName the name of the field
		 * @return the new accessor, or null if no field could be found
		 * @throws IllegalAccessException never
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
