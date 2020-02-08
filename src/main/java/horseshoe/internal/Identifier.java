package horseshoe.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;

import horseshoe.Settings.ContextAccess;

public final class Identifier {

	public static final int UNSTATED_BACKREACH = -1;
	public static final int NOT_A_METHOD = -1;
	public static final String PATTERN = "[\\p{L}_\\$][\\p{L}\\p{Nd}_\\$]*";

	private final HashMap<Class<?>, Accessor> accessorDatabase = new LinkedHashMap<>();
	private final String name;
	private final int parameterCount;

	/**
	 * Creates a new identifier from a name and parameter count.
	 *
	 * @param name the name of the identifier
	 * @param parameterCount the number of parameters for the method identifier
	 */
	public Identifier(final String name, final int parameterCount) {
		this.name = name;
		this.parameterCount = parameterCount;
	}

	/**
	 * Creates a new identifier from a name.
	 *
	 * @param name the name of the identifier
	 */
	public Identifier(final String name) {
		this(name, NOT_A_METHOD);
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof Identifier) {
			return name.equals(((Identifier)object).name) && parameterCount == ((Identifier)object).parameterCount;
		}

		return false;
	}

	/**
	 * Gets the name of the identifier.
	 *
	 * @return the name of the identifier
	 */
	public String getName() {
		return name;
	}

	/**
	 * Finds and gets the value of the identifier given the context object.
	 *
	 * @param context the context object used to get the value of the identifier
	 * @param backreach the backreach for the identifier, or less than 0 to indicate an unspecified backreach
	 * @param access the access used to get the value of the identifier
	 * @return the value of the identifier
	 * @throws ReflectiveOperationException if an error occurs while getting the value of the identifier
	 */
	public Object findValue(final PersistentStack<Object> context, final int backreach, final ContextAccess access) throws ReflectiveOperationException {
		// Try to get value at the specified scope
		boolean skippedAccessor = false;
		Object object = context.peek(Math.max(backreach, 0));
		Class<?> objectClass = Class.class.equals(object.getClass()) ? (Class<?>)object : object.getClass();
		Accessor accessor = accessorDatabase.get(objectClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(object, this, 0);
			accessorDatabase.put(objectClass, accessor);
		}

		if (accessor != null) {
			final Object result = accessor.get(object);

			if (result != null || accessor.has(object)) {
				return result;
			}

			skippedAccessor = true;
		}

		// If there is no value at the specified scope and the backreach is unspecified, then try to get the value at a different scope
		if (backreach < 0) {
			if (access == ContextAccess.FULL) {
				for (int i = 1; i < context.size(); i++) {
					object = context.peek(i);
					objectClass = Class.class.equals(object.getClass()) ? (Class<?>)object : object.getClass();
					accessor = accessorDatabase.get(objectClass);

					// Try to create the accessor and add it to the database
					if (accessor == null) {
						accessor = Accessor.FACTORY.create(object, this, 0);
						accessorDatabase.put(objectClass, accessor);
					}

					if (accessor != null) {
						final Object result = accessor.get(object);

						if (result != null || accessor.has(object)) {
							return result;
						}

						skippedAccessor = true;
					}
				}
			} else if (access == ContextAccess.CURRENT_AND_ROOT) {
				return getValue(context.peekBase());
			}
		}

		if (skippedAccessor) {
			return null;
		}

		throw new NoSuchFieldException("Field \"" + name + "\" not found in class " + objectClass.getName());
	}

	/**
	 * Finds and evaluates the method identifier given the context object and parameters.
	 *
	 * @param context the context object used to get the value of the identifier
	 * @param backreach the backreach for the identifier, or less than 0 to indicate an unspecified backreach
	 * @param access the access used to get the value of the identifier
	 * @param parameters the parameters used to evaluate the object
	 * @return the value of the identifier
	 * @throws ReflectiveOperationException if an error occurs while getting the value of the identifier
	 */
	public Object findValue(final PersistentStack<Object> context, final int backreach, final ContextAccess access, final Object... parameters) throws ReflectiveOperationException {
		// Try to get value at the specified scope
		boolean skippedAccessor = false;
		Object object = context.peek(Math.max(backreach, 0));
		Class<?> objectClass = Class.class.equals(object.getClass()) ? (Class<?>)object : object.getClass();
		Accessor accessor = accessorDatabase.get(objectClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(object, this, parameterCount);
			accessorDatabase.put(objectClass, accessor);
		}

		if (accessor != null) {
			final Object result = accessor.get(object, parameters);

			if (result != null || accessor.has(object)) {
				return result;
			}

			skippedAccessor = true;
		}

		// If there is no value at the specified scope and the backreach is unspecified, then try to get the value at a different scope
		if (backreach < 0) {
			if (access == ContextAccess.FULL) {
				for (int i = 1; i < context.size(); i++) {
					object = context.peek(i);
					objectClass = Class.class.equals(object.getClass()) ? (Class<?>)object : object.getClass();
					accessor = accessorDatabase.get(objectClass);

					// Try to create the accessor and add it to the database
					if (accessor == null) {
						accessor = Accessor.FACTORY.create(object, this, parameterCount);
						accessorDatabase.put(objectClass, accessor);
					}

					if (accessor != null) {
						final Object result = accessor.get(object, parameters);

						if (result != null || accessor.has(object)) {
							return result;
						}

						skippedAccessor = true;
					}
				}
			} else if (access == ContextAccess.CURRENT_AND_ROOT) {
				return getValue(context.peekBase(), parameters);
			}
		}

		if (skippedAccessor) {
			return null;
		}

		throw new NoSuchMethodError("Method \"" + name + "\" not found in class " + objectClass.getName());
	}

	/**
	 * Gets the value of the identifier from the root context object.
	 *
	 * @param context the context object used to get the value of the identifier
	 * @param access the access used to get the value of the identifier
	 * @return the value of the identifier
	 * @throws ReflectiveOperationException if an error occurs while getting the value of the identifier
	 */
	public Object getRootValue(final PersistentStack<Object> context, final ContextAccess access) throws ReflectiveOperationException {
		if (access == ContextAccess.FULL || access == ContextAccess.CURRENT_AND_ROOT || context.size() == 1) {
			return getValue(context.peekBase());
		}

		throw new NoSuchFieldException("Root field \"" + name + "\" could not be accessed due to insufficient permissions");
	}

	/**
	 * Gets the value of the identifier given the context object.
	 *
	 * @param context the context object to evaluate
	 * @return the result of the evaluation
	 * @throws ReflectiveOperationException if an error occurs while evaluating the value of the identifier
	 */
	public Object getValue(final Object context) throws ReflectiveOperationException {
		final Class<?> objectClass = Class.class.equals(context.getClass()) ? (Class<?>)context : context.getClass();
		Accessor accessor = accessorDatabase.get(objectClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this, 0);

			if (accessor == null) {
				throw new NoSuchFieldException("Field \"" + name + "\" not found in class " + objectClass.getName());
			}

			accessorDatabase.put(objectClass, accessor);
		}

		return accessor.get(context);
	}

	/**
	 * Evaluates the method identifier given the context object and parameters.
	 *
	 * @param context the context object to evaluate
	 * @param parameters the parameters used to evaluate the object
	 * @return the result of the evaluation
	 * @throws ReflectiveOperationException if an error occurs while evaluating the value of the identifier
	 */
	public Object getValue(final Object context, final Object... parameters) throws ReflectiveOperationException {
		final Class<?> objectClass = Class.class.equals(context.getClass()) ? (Class<?>)context : context.getClass();
		Accessor accessor = accessorDatabase.get(objectClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this, parameterCount);

			if (accessor == null) {
				throw new NoSuchMethodError("Method \"" + name + "\" not found in class " + objectClass.getName());
			}

			accessorDatabase.put(objectClass, accessor);
		}

		return accessor.get(context, parameters);
	}

	@Override
	public int hashCode() {
		return name.hashCode() + parameterCount;
	}

	/**
	 * Checks if the identifier is a method.
	 *
	 * @return True if the identifier is a method, otherwise false
	 */
	public boolean isMethod() {
		return parameterCount >= 0;
	}

	@Override
	public String toString() {
		return name;
	}

}
