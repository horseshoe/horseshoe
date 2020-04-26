package horseshoe.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Accessor {

	static final Factory FACTORY = new Factory();

	private static final class MethodSignature {

		private final String name;
		private final String[] types;

		/**
		 * Creates a new method signature from a string.
		 *
		 * @param signature the signature of the method in the form [name]:[parameterType0],...
		 */
		MethodSignature(final String signature) {
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
		public boolean matches(final Method method) {
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

	/**
	 * Gets the value of an identifier given the specified context as part of an expression.
	 *
	 * @param context the context object to resolve
	 * @return the value
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
	 * Checks if the accessor has the ability to resolve given the specified context when get returns null.
	 *
	 * @param context the context object to resolve
	 * @return true if the context is resolvable, otherwise false
	 */
	public boolean has(final Object context) {
		return false;
	}

	/**
	 * Looks up a value based on a map or array and the lookup operator.
	 *
	 * @param context the context object to resolve
	 * @param lookup the value within the context to resolve
	 * @return the result of the lookup
	 */
	public static Object lookup(final Object context, final Object lookup) {
		if (context instanceof Map) {
			return ((Map<?, ?>)context).get(lookup);
		} else if (context instanceof List) {
			return ((List<?>)context).get(((Number)lookup).intValue());
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
	 * A map accessor provides access to a value in a map using the specified key.
	 */
	private static final class MapAccessor extends Accessor {

		private final String key;

		MapAccessor(final String key) {
			this.key = key;
		}

		@Override
		public Object get(final Object context) {
			return ((Map<?, ?>)context).get(key);
		}

		@Override
		public boolean has(final Object context) {
			return ((Map<?, ?>)context).containsKey(key);
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
			// We could do something fancy and try to find the most correct method to call, but for now just try to call them all starting with the most recently used.
			//  Note: this may invoke the wrong method for closely-related overloads.
			try {
				return methods[mruIndex].invoke(context, parameters);
			} catch (final IllegalArgumentException e) {
				for (int i = 0; i < methods.length; i++) {
					if (i != mruIndex) {
						try {
							final Object result = methods[i].invoke(context, parameters);
							mruIndex = i;
							return result;
						} catch (final IllegalArgumentException e1) {
							// Ignore failures along the way
						}
					}
				}

				throw e;
			}
		}

	}

	static class Factory {

		public Accessor create(final Object context, final Identifier identifier, final int parameters) {
			final Class<?> contextClass = context.getClass();

			if (identifier.isMethod()) { // Method
				if (Class.class.equals(contextClass)) { // Class method
					return MethodAccessor.createStaticOrClass((Class<?>)context, identifier.getName(), parameters);
				}

				return MethodAccessor.create(contextClass, identifier.getName(), parameters);
			} else if (Class.class.equals(contextClass)) { // Static field
				return FieldAccessor.createStatic((Class<?>)context, identifier.getName());
			} else if (Map.class.isAssignableFrom(contextClass)) {
				return new MapAccessor(identifier.getName());
			} else if (contextClass.isArray() && "length".equals(identifier.getName())) {
				return new ArrayLengthAccessor();
			}

			// Field
			return FieldAccessor.create(contextClass, identifier.getName());
		}

	}

}
