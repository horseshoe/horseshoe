package horseshoe;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

class ExpressionSegment {

	private static class MapResolver implements Resolver {
		private final String key;

		MapResolver(final String key) {
			this.key = key;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object resolve(final Object context, final Object... args) {
			return ((Map<String, Object>)context).get(key);
		}
	}

	/*
	private static class FieldResolver implements Resolver {
		private final Field field;

		FieldResolver(final Field field) {
			this.field = field;
			this.field.setAccessible(true);
		}

		@Override
		public Object resolve(final Object context, final Object... args) {
			return field.get(context);
		}
	}

	private static class MethodResolver implements Resolver {
		private final Method method;

		MethodResolver(final Method method) {
			this.method = method;
			this.method.setAccessible(true);
		}

		@Override
		public Object resolve(final Object context, final Object... args) {
			return method.invoke(context, args);
		}
	}
	*/

	private static final class Context {
		private final Class<?> object;
		private final Class<?> args[];

		public Context(final Class<?> object, final Class<?> args[]) {
			this.object = object;
			this.args = args;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null || obj.getClass() != ExpressionSegment.Context.class) {
				return false;
			}

			final ExpressionSegment.Context other = (ExpressionSegment.Context)obj;

			return object == other.object && Arrays.equals(args, other.args);
		}

		@Override
		public int hashCode() {
			int hash = object.hashCode();

			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					hash ^= (args[i] == null ? 0 : args[i].hashCode()) + (1 << (i & 31));
				}
			}

			return hash;
		}
	}

	private final Map<Context, Resolver> resolverDatabase = new LinkedHashMap<>();
	private final String identifier;
	private final Expression args[];

	public ExpressionSegment(final String identifier, final Expression args[]) {
		this.identifier = identifier;
		this.args = args;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof ExpressionSegment) {
			return identifier.equals(((ExpressionSegment)obj).identifier) && Arrays.equals(args, ((ExpressionSegment)obj).args);
		} else if (obj instanceof CharSequence) {
			return false; // TODO
		}

		return false;
	}

	public Object evaluate(final RenderContext context, final Object obj) {
		// Evaluate arguments
		Object evaluatedArgs[] = null;
		Class<?> evaluatedClasses[] = null;

		if (args != null) {
			evaluatedArgs = new Object[args.length];
			evaluatedClasses = new Class<?>[args.length];

			for (int i = 0; i < args.length; i++) {
				evaluatedArgs[i] = args[i].evaluate(context);

				if (evaluatedArgs[i] != null) {
					evaluatedClasses[i] = evaluatedArgs[i].getClass();
				}
			}
		}

		// Get resolver
		final Context expressionContext = new Context(obj.getClass(), evaluatedClasses);
		Resolver resolver = resolverDatabase.get(expressionContext);

		if (resolver == null) {
			resolver = createResolver(expressionContext);
			resolverDatabase.put(expressionContext, resolver);
		}

		return resolver.resolve(obj, evaluatedArgs);
	}

	private Resolver createResolver(final Context context) {
		if (Map.class.isAssignableFrom(context.object)) {
			return new MapResolver(identifier);
		} else if (Map.class.isAssignableFrom(context.object)) {
			return null;
		}

		return null;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}

}
