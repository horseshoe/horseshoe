package horseshoe.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;

import horseshoe.Settings.ContextAccess;

public final class Identifier {

	private final HashMap<Class<?>, Accessor> accessorDatabase = new LinkedHashMap<>();
	private final int backreach;
	private final String name;
	private final boolean isMethod;

	/**
	 * Creates a new identifier from some amount of backreach and a name.
	 *
	 * @param backreach the backreach for the identifier
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
		this(0, name, false);
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof Identifier) {
			return backreach == ((Identifier)object).backreach && name.equals(((Identifier)object).name) && isMethod == ((Identifier)object).isMethod;
		}

		return false;
	}

	/**
	 * Gets the backreach of the identifier.
	 *
	 * @return the backreach of the identifier
	 */
	public int getBackreach() {
		return backreach;
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
	 * Evaluates the identifier given the context object. For methods, this returns the method to invoke.
	 *
	 * @param context the context object to evaluate
	 * @return the result of the evaluation
	 * @throws ReflectiveOperationException
	 */
	public Object getValue(final PersistentStack<Object> context, final ContextAccess access) throws ReflectiveOperationException {
		// Try to get value at the specified scope
		final Object value = getValue(context.peek(backreach));

		if (value != null) {
			return value;
		}

		// If there is no value at the specified scope and the backreach is 0, then try to get the value at a different scope
		if (backreach == 0) {
			if (access == ContextAccess.FULL) {
				for (int i = 1; i < context.size(); i++) {
					final Object levelValue = getValue(context.peek(i));

					if (levelValue != null) {
						return levelValue;
					}
				}
			} else if (access == ContextAccess.CURRENT_AND_ROOT) {
				return getValue(context.peekBase());
			}
		}

		return null;
	}

	/**
	 * Evaluates the identifier given the context object. For methods, this returns the method to invoke.
	 *
	 * @param context the context object to evaluate
	 * @return the result of the evaluation
	 * @throws ReflectiveOperationException
	 */
	public Object getValue(final Object context) throws ReflectiveOperationException {
		final Class<?> objectClass = context.getClass();
		Accessor accessor = accessorDatabase.get(objectClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this, 0);
			accessorDatabase.put(objectClass, accessor);
		}

		return accessor.get(context);
	}

	/**
	 * Evaluates the method identifier given the context object and the parameters.
	 *
	 * @param context the context object to evaluate
	 * @param parameters the parameters used to evaluate the object
	 * @return the result of the evaluation
	 * @throws ReflectiveOperationException
	 */
	public Object getValue(final Object context, final Object... parameters) throws ReflectiveOperationException {
		final Class<?> objectClass = context.getClass();
		Accessor accessor = accessorDatabase.get(objectClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this, parameters.length);
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
