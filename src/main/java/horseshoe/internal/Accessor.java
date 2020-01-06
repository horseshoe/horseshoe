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

		/**
		 * Creates a new static method accessor.
		 *
		 * @param parent the parent class for the method
		 * @param methodSignature the signature of the method in the form [name]:[parameterType0],...
		 * @param parameterCount the number of parameters that the method takes
		 */
		public ClassMethodAccessor(final Class<?> parent, final String methodSignature, final int parameterCount) {
			final MethodSignature signature = new MethodSignature(methodSignature);

			for (final Method method : parent.getMethods()) {
				if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == parameterCount && signature.matches(method)) {
					method.setAccessible(true);
					this.method = method;
					return;
				}
			}

			for (final Method method : Class.class.getMethods()) {
				if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == parameterCount && signature.matches(method)) {
					method.setAccessible(true);
					this.method = method;
					return;
				}
			}

			throw new java.lang.NoSuchMethodError("Method \"" + signature.name + "\" taking " + parameterCount + " parameters not found in class " + parent.getName());
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

		public FieldAccessor(final Class<?> parent, final String fieldName) {
			for (final Field field : parent.getFields()) {
				if (!Modifier.isStatic(field.getModifiers()) && fieldName.equals(field.getName())) {
					field.setAccessible(true);
					this.field = field;
					return;
				}
			}

			throw new java.lang.NoSuchFieldError("Field \"" + fieldName + "\" not found in class " + parent.getName());
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

		/**
		 * Creates a new method accessor.
		 *
		 * @param parent the parent class for the method
		 * @param methodSignature the signature of the method in the form [name]:[parameterType0],...
		 * @param parameterCount the number of parameters that the method takes
		 */
		public MethodAccessor(final Class<?> parent, final String methodSignature, final int parameterCount) {
			final MethodSignature signature = new MethodSignature(methodSignature);

			for (final Method method : parent.getMethods()) {
				if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == parameterCount && signature.matches(method)) {
					method.setAccessible(true);
					this.method = method;
					return;
				}
			}

			throw new java.lang.NoSuchMethodError("Method \"" + signature.name + "\" taking " + parameterCount + " parameters not found in class " + parent.getName());
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

		public StaticFieldAccessor(final Class<?> parent, final String fieldName) {
			for (final Field field : parent.getFields()) {
				if (Modifier.isStatic(field.getModifiers()) && fieldName.equals(field.getName())) {
					field.setAccessible(true);
					this.field = field;
					return;
				}
			}

			throw new java.lang.NoSuchFieldError("Static field \"" + fieldName + "\" not found in class " + parent.getName());
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
					return new ClassMethodAccessor((Class<?>)context, identifier.getName(), parameters);
				} else { // Field
					return new StaticFieldAccessor((Class<?>)context, identifier.getName());
				}
			} else if (identifier.isMethod()) { // Method
				return new MethodAccessor(contextClass, identifier.getName(), parameters);
			} else if (Map.class.isAssignableFrom(contextClass)) {
				return new MapAccessor(identifier.getName());
			}

			// Field
			return new FieldAccessor(contextClass, identifier.getName());
		}

	}

	static final Factory FACTORY = new Factory();

}
