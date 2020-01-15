package horseshoe.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public abstract class Accessor {

	private static final class MethodSignature {
		private final String name;
		private final String types[];

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

				for (int end = signature.indexOf(',', start), i = 0; end >= 0; start = end + 1, end = signature.indexOf(',', start)) {
					types[i++] = signature.substring(start, end);
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
				final Class<?> parameterTypes[] = method.getParameterTypes();
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
		} else if (context instanceof Object[]) {
			return ((Object[])context)[((Number)lookup).intValue()];
		} else if (context instanceof int[]) {
			return ((int[])context)[((Number)lookup).intValue()];
		} else if (context instanceof byte[]) {
			return ((byte[])context)[((Number)lookup).intValue()];
		} else if (context instanceof double[]) {
			return ((double[])context)[((Number)lookup).intValue()];
		} else if (context instanceof boolean[]) {
			return ((boolean[])context)[((Number)lookup).intValue()];
		} else if (context instanceof float[]) {
			return ((float[])context)[((Number)lookup).intValue()];
		} else if (context instanceof long[]) {
			return ((long[])context)[((Number)lookup).intValue()];
		} else if (context instanceof char[]) {
			return ((char[])context)[((Number)lookup).intValue()];
		} else {
			return ((short[])context)[((Number)lookup).intValue()];
		}
	}

	/**
	 * Sets the value of an identifier given the specified context as part of an expression.
	 *
	 * @param context the context object to resolve
	 * @param value the value to assign the identifier
	 * @return the assigned value
	 * @throws ReflectiveOperationException if the value cannot be set due to an invalid reflection call
	 */
	public abstract Object set(final Object context, final Object value) throws ReflectiveOperationException;

	/**
	 * Accesses a static method or class property method in a class
	 */
	private static final class ClassMethodAccessor extends Accessor {
		private final Method method;

		private ClassMethodAccessor(final Method method) {
			this.method = method;
		}

		/**
		 * Creates a new static method accessor.
		 *
		 * @param parent the parent class for the method
		 * @param methodSignature the signature of the method in the form [name]:[parameterType0],...
		 * @param parameterCount the number of parameters that the method takes
		 */
		public static ClassMethodAccessor create(final Class<?> parent, final String methodSignature, final int parameterCount) {
			final MethodSignature signature = new MethodSignature(methodSignature);

			if (Modifier.isPublic(parent.getModifiers())) {
				for (final Method method : parent.getMethods()) {
					if (Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == parameterCount && signature.matches(method)) {
						method.setAccessible(true);
						return new ClassMethodAccessor(method);
					}
				}
			}

			for (final Method method : Class.class.getMethods()) {
				if (!Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == parameterCount && signature.matches(method)) {
					method.setAccessible(true);
					return new ClassMethodAccessor(method);
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

		@Override
		public Object set(final Object context, final Object value) throws ReflectiveOperationException {
			return null;
		}
	}

	/**
	 * Accesses a field in a class
	 */
	private static final class FieldAccessor extends Accessor {
		private final Field field;

		private FieldAccessor(final Field field) {
			this.field = field;
		}

		public static FieldAccessor create(final Class<?> parent, final String fieldName) {
			if (Modifier.isPublic(parent.getModifiers())) {
				for (final Field field : parent.getFields()) {
					if (!Modifier.isStatic(field.getModifiers()) && fieldName.equals(field.getName())) {
						field.setAccessible(true);
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

		@Override
		public Object set(final Object context, final Object value) throws ReflectiveOperationException {
			field.set(context, value);
			return value;
		}
	}

	/**
	 * Accesses a value in a map using the specified key
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

		@SuppressWarnings({ "unchecked" })
		@Override
		public Object set(final Object context, final Object value) throws ReflectiveOperationException {
			((Map<String, Object>)context).put(key, value);
			return value;
		}
	}

	/**
	 * Accesses a method in a class
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
		 */
		public static MethodAccessor create(Class<?> parent, final String methodSignature, final int parameterCount) {
			final MethodSignature signature = new MethodSignature(methodSignature);

			// Find the first public parent class, analyzing all interfaces along the way
			for (; parent != null; parent = parent.getSuperclass()) {
				if (Modifier.isPublic(parent.getModifiers())) {
					for (final Method method : parent.getMethods()) {
						if (!Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == parameterCount && signature.matches(method)) {
							method.setAccessible(true);
							return new MethodAccessor(method);
						}
					}

					break;
				} else {
					for (final Class<?> iface : parent.getInterfaces()) {
						if (Modifier.isPublic(iface.getModifiers())) {
							for (final Method method : iface.getMethods()) {
								if (!Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == parameterCount && signature.matches(method)) {
									method.setAccessible(true);
									return new MethodAccessor(method);
								}
							}
						}
					}
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

		@Override
		public Object set(final Object context, final Object value) throws ReflectiveOperationException {
			return null;
		}
	}

	/**
	 * Accesses a static field in a class
	 */
	private static final class StaticFieldAccessor extends Accessor {
		private final Field field;

		private StaticFieldAccessor(final Field field) {
			this.field = field;
		}

		public static StaticFieldAccessor create(final Class<?> parent, final String fieldName) {
			if (Modifier.isPublic(parent.getModifiers())) {
				for (final Field field : parent.getFields()) {
					if (Modifier.isStatic(field.getModifiers()) && fieldName.equals(field.getName())) {
						field.setAccessible(true);
						return new StaticFieldAccessor(field);
					}
				}
			}

			return null;
		}

		@Override
		public Object get(final Object context) throws ReflectiveOperationException {
			return field.get(context);
		}

		@Override
		public Object set(final Object context, final Object value) throws ReflectiveOperationException {
			field.set(context, value);
			return value;
		}
	}

	static class Factory {

		public Accessor create(final Object context, final Identifier identifier, final int parameters) {
			final Class<?> contextClass = context.getClass();

			if (Class.class.equals(contextClass)) { // Static
				if (identifier.isMethod()) { // Method
					return ClassMethodAccessor.create((Class<?>)context, identifier.getName(), parameters);
				} else { // Field
					return StaticFieldAccessor.create((Class<?>)context, identifier.getName());
				}
			} else if (identifier.isMethod()) { // Method
				return MethodAccessor.create(contextClass, identifier.getName(), parameters);
			} else if (Map.class.isAssignableFrom(contextClass)) {
				return new MapAccessor(identifier.getName());
			}

			// Field
			return FieldAccessor.create(contextClass, identifier.getName());
		}

	}

	static final Factory FACTORY = new Factory();

}
