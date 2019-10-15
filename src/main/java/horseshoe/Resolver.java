package horseshoe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

		public FieldResolver(final Field field) {
			this.field = field;
			this.field.setAccessible(true);
		}

		@Override
		public Object resolve(final Object context, final Object args[]) {
			try {
				return field.get(context);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				return null;
			}
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
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				return null;
			}
		}
	}

	public static class Factory {

		public Resolver create(final ResolverContext context, final String identifier) {
			if (Map.class.isAssignableFrom(context.getObjectClass())) {
				return new MapResolver(identifier);
			} else if (Map.class.isAssignableFrom(context.getObjectClass())) {
				return null;
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
