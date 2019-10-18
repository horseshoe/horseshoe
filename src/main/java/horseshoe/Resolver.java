package horseshoe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import horseshoe.internal.Properties;

abstract class Resolver {

	/**
	 * Resolves one segment of an expression.
	 *
	 * @param context the result of the previous segment of the expression
	 * @param args evaluated arguments that help resolve this segment of the expression
	 * @return the resolved value of the expression segment
	 */
	abstract Object resolve(final Object context, final Object args[]);

	/**
	 * Resolves to a constant value
	 */
	public static class ArrayItemResolver extends Resolver {
		private final Object value;

		public ArrayItemResolver(final Object value) {
			this.value = value;
		}

		@Override
		public Object resolve(final Object context, final Object args[]) {
			return value;
		}
	}

	/**
	 * Resolves a value from a map using the specified key
	 */
	public static class MapResolver extends Resolver {
		private final String key;

		MapResolver(final String key) {
			this.key = key;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object resolve(final Object context, final Object args[]) {
			return ((Map<String, Object>)context).get(key);
		}
	}

	public static class FieldResolver extends Resolver {
		private final Field field;

		public FieldResolver(final ResolverContext context, final String fieldName) {
			for (Class<?> objectClass = context.getObjectClass(); objectClass != null; objectClass = objectClass.getSuperclass()) {
				for (final Field field : objectClass.getDeclaredFields()) {
					if (Modifier.isPublic(field.getModifiers()) && fieldName.equals(field.getName())) {
						try {
							field.setAccessible(true);
						} catch (final SecurityException e) {
						}

						this.field = field;
						return;
					}
				}
			}

			this.field = null;
		}

		@Override
		public Object resolve(final Object context, final Object args[]) {
			try {
				return field.get(context);
			} catch (final ReflectiveOperationException e) {
			}

			return null;
		}
	}

	public static class MethodResolver extends Resolver {
		private final Method method;

		public MethodResolver(final Method method) {
			this.method = method;
			this.method.setAccessible(true);
		}

		@Override
		public Object resolve(final Object context, final Object args[]) {
			try {
				return method.invoke(context, args);
			} catch (final ReflectiveOperationException e) {
			}

			return null;
		}
	}

	public static class StaticFieldResolver extends Resolver {
		private final String fieldName;
		private final Map<Class<?>, Field> fields = new LinkedHashMap<>();

		public StaticFieldResolver(final String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public Object resolve(final Object context, final Object args[]) {
			try {
				final Class<?> fieldClass = (Class<?>)context;
				final Field found = fields.get(fieldClass);

				if (found != null) {
					return found.get(fieldClass);
				}

				for (final Field field : fieldClass.getDeclaredFields()) {
					if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && fieldName.equals(field.getName())) {
						try {
							field.setAccessible(true);
						} catch (final SecurityException e) {
						}

						fields.put(fieldClass, field);
						return field.get(fieldClass);
					}
				}
			} catch (final ReflectiveOperationException e) {
			}

			return null;
		}
	}

	public static class StaticMethodResolver extends Resolver {
		private final String methodName;
		private final Map<Class<?>, Method> fields = new LinkedHashMap<>();

		public StaticMethodResolver(final String methodName) {
			this.methodName = methodName;
		}

		@Override
		public Object resolve(final Object context, final Object args[]) {
			try {
				final Class<?> methodClass = (Class<?>)context;
				final Method found = fields.get(methodClass);

				if (found != null) {
					return found.invoke(methodClass, args);
				}

				for (final Method method : methodClass.getDeclaredMethods()) {
					if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()) && methodName.equals(method.getName())) {
						fields.put(methodClass, method);
						return method.invoke(methodClass, args);
					}
				}
			} catch (final ReflectiveOperationException e) {
			}

			return null;
		}
	}

	public static class Factory {

		public Resolver create(final ResolverContext context, final String identifier) {
			if (Map.class.isAssignableFrom(context.getObjectClass())) {
				return new MapResolver(identifier);
			} else if (Class.class.equals(context.getObjectClass())) {
				if (context.getArgumentClasses() == null) { // Field
					return new StaticFieldResolver(identifier);
				} else { // Method
				}
			} else if (context.getArgumentClasses() == null) { // Field
				return new FieldResolver(context, identifier);
			}

			return null;
		}

	}

	static final Factory FACTORY;

	static {
		Factory factory = new Factory();

		try { // Try to load the Java 8+ version
			if (Properties.JAVA_VERSION >= 8.0) {
				factory = (Factory)Factory.class.getClassLoader().loadClass(Factory.class.getName().replace("Resolver", "Resolver_8")).getConstructor().newInstance();
			}
		} catch (final ReflectiveOperationException e) {
		}

		FACTORY = factory;
	}

}
