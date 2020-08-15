package horseshoe.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Accessor {

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

		public Range(final int start, final int end, final int length) {
			this.end = end < 0 ? Math.max(0, length + end) : Math.min(length, end);
			this.start = start < 0 ? Math.max(0, Math.min(length + start, this.end)) : Math.min(this.end, start);
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
	 * @throws ReflectiveOperationException if the value cannot be retrieved due to an invalid reflection call
	 */
	public abstract Object get(final Object context) throws ReflectiveOperationException;

	/**
	 * Gets the value of a method identifier given the specified context and parameters as part of an expression.
	 *
	 * @param context the context object to resolve
	 * @param parameters the parameters to the method
	 * @return the result of invoking the method
	 * @throws ReflectiveOperationException if the value cannot be retrieved due to an invalid reflection call
	 */
	public Object get(final Object context, final Object... parameters) throws ReflectiveOperationException {
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
	 * @return the result of the lookup
	 */
	public static Object lookup(final Object context, final Object lookup) {
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
		if (lookup instanceof Collection) {
			final Collection<Number> collection = (Collection<Number>)lookup;
			final List<Object> list = new ArrayList<>(collection.size());

			for (final Number number : collection) {
				list.add(lookupArray(context, number.intValue()));
			}

			return list;
		}

		final Class<?> componentType = context.getClass().getComponentType();

		if (!componentType.isPrimitive()) {
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

		if (!componentType.isPrimitive()) {
			final Object[] array = (Object[])context;
			final Range range = new Range(start, end, array.length);
			return Arrays.copyOfRange(array, range.start, range.end);
		} else if (int.class.equals(componentType)) {
			final int[] array = (int[])context;
			final Range range = new Range(start, end, array.length);
			return Arrays.copyOfRange(array, range.start, range.end);
		} else if (byte.class.equals(componentType)) {
			final byte[] array = (byte[])context;
			final Range range = new Range(start, end, array.length);
			return Arrays.copyOfRange(array, range.start, range.end);
		} else if (double.class.equals(componentType)) {
			final double[] array = (double[])context;
			final Range range = new Range(start, end, array.length);
			return Arrays.copyOfRange(array, range.start, range.end);
		} else if (boolean.class.equals(componentType)) {
			final boolean[] array = (boolean[])context;
			final Range range = new Range(start, end, array.length);
			return Arrays.copyOfRange(array, range.start, range.end);
		} else if (float.class.equals(componentType)) {
			final float[] array = (float[])context;
			final Range range = new Range(start, end, array.length);
			return Arrays.copyOfRange(array, range.start, range.end);
		} else if (long.class.equals(componentType)) {
			final long[] array = (long[])context;
			final Range range = new Range(start, end, array.length);
			return Arrays.copyOfRange(array, range.start, range.end);
		} else if (char.class.equals(componentType)) {
			final char[] array = (char[])context;
			final Range range = new Range(start, end, array.length);
			return Arrays.copyOfRange(array, range.start, range.end);
		}

		final short[] array = (short[])context;
		final Range range = new Range(start, end, array.length);
		return Arrays.copyOfRange(array, range.start, range.end);
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
		if (lookup instanceof Collection) {
			final Collection<Number> collection = (Collection<Number>)lookup;
			final List<Object> list = new ArrayList<>(collection.size());

			for (final Number number : collection) {
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
		return new ArrayList<Object>(context.subList(range.start, range.end));
	}

	/**
	 * Looks up a value based on a map and the lookup operand.
	 *
	 * @param context the map object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupMap(final Map<?, ?> context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final Map<Object, Object> map = new LinkedHashMap<>();

			for (final Object object : collection) {
				if (context.containsKey(object)) {
					map.put(object, context.get(object));
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

		for (int i = start; i < end; i++) {
			if (context.containsKey(i)) {
				map.put(i, context.get(i));
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
	 * @return the result of the lookup
	 */
	public static Object lookupRange(final Object context, final Object start, final Object end) {
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
	}

	/**
	 * Looks up a value based on a set and the lookup operand.
	 *
	 * @param context the set object used to perform the lookup
	 * @param lookup the value to lookup
	 * @return the result of the lookup
	 */
	private static Object lookupSet(final Set<?> context, final Object lookup) {
		if (lookup instanceof Collection) {
			final Collection<?> collection = (Collection<?>)lookup;
			final Set<Object> set = new LinkedHashSet<>();

			for (final Object object : collection) {
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

		for (int i = start; i < end; i++) {
			if (context.contains(i)) {
				set.add(i);
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
		if (lookup instanceof Collection) {
			final Collection<Number> collection = (Collection<Number>)lookup;
			final char[] chars = new char[collection.size()];
			int i = 0;

			for (final Number number : collection) {
				chars[i++] = context.charAt(calculateIndex(context.length(), number.intValue()));
			}

			return new String(chars);
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
		return context.substring(range.start, range.end);
	}

	/**
	 * Gets the value of an identifier given the specified context as part of an expression. This method may return an invalid object. All return values should be checked with the {@link #isValid()} method.
	 *
	 * @param context the context object to resolve
	 * @return the value of the identifier or an invalid value
	 * @throws ReflectiveOperationException if the value cannot be retrieved due to an invalid reflection call
	 */
	public Object tryGet(final Object context) throws ReflectiveOperationException {
		return get(context);
	}

	/**
	 * Gets the value of a method identifier given the specified context and parameters as part of an expression. This method may return an invalid object. All return values should be checked with the {@link #isValid()} method.
	 *
	 * @param context the context object to resolve
	 * @param parameters the parameters to the method
	 * @return the result of invoking the method or an invalid value
	 * @throws ReflectiveOperationException if the value cannot be retrieved due to an invalid reflection call
	 */
	public Object tryGet(final Object context, final Object... parameters) throws ReflectiveOperationException {
		return get(context, parameters);
	}

	/**
	 * An array length accessor provides access to the length of an array.
	 */
	private static final class ArrayLengthAccessor extends Accessor {
		@Override
		public Object get(final Object context) throws ReflectiveOperationException {
			return Array.getLength(context);
		}
	}

	/**
	 * A field accessor provides access to a field in a class.
	 */
	private static final class FieldAccessor extends Accessor {

		private final Field field;

		private FieldAccessor(final Field field) {
			this.field = field;
		}

		/**
		 * Creates a new field accessor.
		 *
		 * @param parent the parent class for the field
		 * @param fieldName the name of the field
		 * @return the new accessor, or null if no field could be found
		 */
		public static FieldAccessor create(final Class<?> parent, final String fieldName) {
			// Find first matching field
			for (Class<?> ancestor = parent; ancestor != null; ancestor = ancestor.getSuperclass()) {
				if (Modifier.isPublic(ancestor.getModifiers())) {
					for (final Field field : ancestor.getFields()) {
						if (!Modifier.isStatic(field.getModifiers()) && fieldName.equals(field.getName())) {
							field.setAccessible(true); // By-pass internal checks on access
							return new FieldAccessor(field);
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
		 */
		public static FieldAccessor createStatic(final Class<?> parent, final String fieldName) {
			// Get public static fields only from the current class if it is public
			if (Modifier.isPublic(parent.getModifiers())) {
				for (final Field field : parent.getFields()) {
					if (Modifier.isStatic(field.getModifiers()) && fieldName.equals(field.getName())) {
						field.setAccessible(true); // By-pass internal checks on access
						return new FieldAccessor(field);
					}
				}
			}

			return null;
		}

		@Override
		public Object get(final Object context) throws ReflectiveOperationException {
			return field.get(context);
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
	private static final class MapAccessor extends Accessor {

		private static final MapAccessorFactory FACTORY;

		private final String key;

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
	private static final class MethodAccessor extends Accessor {

		private final Method method;

		private MethodAccessor(final Method method) {
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
			return method.invoke(context, parameters);
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
	private static final class MethodsAccessor extends Accessor {

		private final Method[] methods;
		private int mruIndex = 0;

		private MethodsAccessor(final Method[] methods) {
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

		public Accessor create(final Object context, final Identifier identifier) {
			final Class<?> contextClass = context.getClass();

			if (identifier.isMethod()) { // Method
				if (Class.class.equals(contextClass)) { // Class method
					return MethodAccessor.createStaticOrClass((Class<?>)context, identifier.getName(), identifier.getParameterCount());
				}

				return MethodAccessor.create(contextClass, identifier.getName(), identifier.getParameterCount());
			} else if (Class.class.equals(contextClass)) { // Static field
				return FieldAccessor.createStatic((Class<?>)context, identifier.getName());
			} else if (Map.class.isAssignableFrom(contextClass)) {
				return MapAccessor.FACTORY.create(identifier.getName());
			} else if (contextClass.isArray() && "length".equals(identifier.getName())) {
				return new ArrayLengthAccessor();
			}

			// Field
			return FieldAccessor.create(contextClass, identifier.getName());
		}

	}

}
