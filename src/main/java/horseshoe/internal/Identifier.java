package horseshoe.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;

import horseshoe.Settings.ContextAccess;

public final class Identifier {

	public static final int UNSPECIFIED_BACKREACH = -1;
	public static final String PATTERN = "[\\p{L}_\\$][\\p{L}\\p{Nd}_\\$]*";

	private final HashMap<Class<?>, Accessor> accessorDatabase = new LinkedHashMap<>();
	private final int backreach;
	private final String name;
	private final boolean isMethod;

	/**
	 * Creates a new identifier from some amount of backreach and a name.
	 *
	 * @param backreach the backreach for the identifier, or less than 0 to indicate an unspecified backreach
	 * @param name the name of the identifier
	 * @param isMethod true if the identifier is a method, otherwise false
	 */
	public Identifier(final int backreach, final String name, final boolean isMethod) {
		this.backreach = backreach;
		this.name = name;
		this.isMethod = isMethod;
	}

	/**
	 * Creates a new identifier from a name.
	 *
	 * @param name the name of the identifier
	 */
	public Identifier(final String name) {
		this(UNSPECIFIED_BACKREACH, name, false);
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof Identifier) {
			return backreach == ((Identifier)object).backreach && name.equals(((Identifier)object).name) && isMethod == ((Identifier)object).isMethod;
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
	 * Gets the value of the identifier given the context object.
	 *
	 * @param context the context object used to get the value of the identifier
	 * @param access the access used to get the value of the identifier
	 * @return the value of the identifier
	 * @throws ReflectiveOperationException if an error occurs while getting the value of the identifier
	 */
	public Object getValue(final PersistentStack<Object> context, final ContextAccess access) throws ReflectiveOperationException {
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
	 * Evaluates the method identifier given the context object and parameters.
	 *
	 * @param context the context object used to get the value of the identifier
	 * @param access the access used to get the value of the identifier
	 * @param parameters the parameters used to evaluate the object
	 * @return the value of the identifier
	 * @throws ReflectiveOperationException if an error occurs while getting the value of the identifier
	 */
	public Object getValue(final PersistentStack<Object> context, final ContextAccess access, final Object... parameters) throws ReflectiveOperationException {
		// Try to get value at the specified scope
		boolean skippedAccessor = false;
		Object object = context.peek(Math.max(backreach, 0));
		Class<?> objectClass = Class.class.equals(object.getClass()) ? (Class<?>)object : object.getClass();
		Accessor accessor = accessorDatabase.get(objectClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(object, this, parameters == null ? 0 : parameters.length);
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
						accessor = Accessor.FACTORY.create(object, this, parameters == null ? 0 : parameters.length);
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
			accessor = Accessor.FACTORY.create(context, this, parameters == null ? 0 : parameters.length);

			if (accessor == null) {
				throw new NoSuchMethodError("Method \"" + name + "\" not found in class " + objectClass.getName());
			}

			accessorDatabase.put(objectClass, accessor);
		}

		return accessor.get(context, parameters);
	}

	@Override
	public int hashCode() {
		return backreach + name.hashCode() + (isMethod ? Integer.MAX_VALUE : 0);
	}

	/**
	 * Checks if the identifier is a method.
	 *
	 * @return True if the identifier is a method, otherwise false
	 */
	public boolean isMethod() {
		return isMethod;
	}

	@Override
	public String toString() {
		return name;
	}

}
