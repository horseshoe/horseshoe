package horseshoe.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;

import horseshoe.Settings.ContextAccess;

public final class Identifier {

	public static final int UNSTATED_BACKREACH = -1;
	public static final int NOT_A_METHOD = -1;

	private static final String LETTER_CHARACTERS = "\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}"; // Character.isLetter()
	private static final String CHARACTERS = LETTER_CHARACTERS + // Derived from Character.isJavaIdentifierPart()
			"\\p{Sc}" + // Currency symbol
			"\\p{Pc}" + // Connecting punctuation character
			"\\p{Nd}" + // Digit
			"\\p{Nl}" + // Numeric letter
			"\\p{Mc}" + // Combining mark
			"\\p{Mn}" + // Non-spacing mark
			"\\u0000-\\u0008\\u000E-\\u001B\\u007F-\\u009F\\p{Zl}\\p{Zp}\\p{Cf}"; // Character.isIdentifierIgnorable()

	public static final String CHARACTER_CLASS = "[" + CHARACTERS + "]";
	public static final String NEGATED_CHARACTER_CLASS = "[^" + CHARACTERS + "]";
	public static final String PATTERN = "[" + LETTER_CHARACTERS + "\\p{Nl}\\p{Sc}\\p{Pc}" + "]" + // Derived from Character.isJavaIdentifierStart()
			CHARACTER_CLASS + "*"; // Derived from Character.isJavaIdentifierPart()

	private final HashMap<Class<?>, Accessor> accessorDatabase = new LinkedHashMap<>(4);
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
	 * @return the value of the identifier
	 * @throws ReflectiveOperationException if an error occurs while getting the value of the identifier
	 */
	public Object findValue(final RenderContext context, final int backreach) throws ReflectiveOperationException {
		// Try to get value at the specified scope
		boolean skippedAccessor = false;
		Object object = context.getSectionData().peek(Math.max(backreach, 0));
		Accessor accessor = getOrAddAccessor(object);

		if (accessor != null) {
			final Object result = accessor.tryGet(object);

			if (Accessor.isValid(result)) {
				return result;
			}

			skippedAccessor = true;
		}

		// If there is no value at the specified scope and the backreach is unspecified, then try to get the value at a different scope
		if (backreach < 0) {
			if (context.getSettings().getContextAccess() == ContextAccess.FULL) {
				for (int i = 1; i < context.getSectionData().size(); i++) {
					object = context.getSectionData().peek(i);
					accessor = getOrAddAccessor(object);

					if (accessor != null) {
						final Object result = accessor.tryGet(object);

						if (Accessor.isValid(result)) {
							return result;
						}

						skippedAccessor = true;
					}
				}
			} else if (context.getSettings().getContextAccess() == ContextAccess.CURRENT_AND_ROOT) {
				return getValue(context.getSectionData().peekBase());
			}
		}

		if (skippedAccessor) {
			return null;
		}

		throw new NoSuchFieldException("Field \"" + name + "\" not found");
	}

	/**
	 * Finds and evaluates the method identifier given the context object and parameters.
	 *
	 * @param context the context object used to get the value of the identifier
	 * @param backreach the backreach for the identifier, or less than 0 to indicate an unspecified backreach
	 * @param parameters the parameters used to evaluate the object
	 * @return the value of the identifier
	 * @throws ReflectiveOperationException if an error occurs while getting the value of the identifier
	 */
	public Object findValue(final RenderContext context, final int backreach, final Object... parameters) throws ReflectiveOperationException {
		// Try to get value at the specified scope
		boolean skippedAccessor = false;
		Object object = context.getSectionData().peek(Math.max(backreach, 0));
		Accessor accessor = getOrAddAccessor(object);

		if (accessor != null) {
			final Object result = accessor.tryGet(object, parameters);

			if (Accessor.isValid(result)) {
				return result;
			}

			skippedAccessor = true;
		}

		// If there is no value at the specified scope and the backreach is unspecified, then try to get the value at a different scope
		if (backreach < 0) {
			if (context.getSettings().getContextAccess() == ContextAccess.FULL) {
				for (int i = 1; i < context.getSectionData().size(); i++) {
					object = context.getSectionData().peek(i);
					accessor = getOrAddAccessor(object);

					if (accessor != null) {
						final Object result = accessor.tryGet(object, parameters);

						if (Accessor.isValid(result)) {
							return result;
						}

						skippedAccessor = true;
					}
				}
			} else if (context.getSettings().getContextAccess() == ContextAccess.CURRENT_AND_ROOT) {
				return getValue(context.getSectionData().peekBase(), parameters);
			}
		}

		if (skippedAccessor) {
			return null;
		}

		throw new NoSuchMethodError("Method \"" + name + "\" not found");
	}

	/**
	 * Gets the accessor for the specified object, or creates and adds an accessor for the object if one does not exist.
	 *
	 * @param object the object used to get the accessor
	 * @return the accessor for the specified object
	 */
	private Accessor getOrAddAccessor(final Object object) {
		final Class<?> objectClass = object.getClass();
		final Class<?> lookupClass = Class.class.equals(objectClass) ? (Class<?>)object : objectClass;
		final Accessor accessor = accessorDatabase.get(lookupClass);

		if (accessor != null) {
			return accessor;
		}

		final Accessor newAccessor = Accessor.FACTORY.create(object, this);
		accessorDatabase.put(lookupClass, newAccessor);
		return newAccessor;
	}

	/**
	 * Gets the number of parameters for a method identifier.
	 *
	 * @return the number of parameters for a method identifier, or -1 if the identifier is not a method
	 */
	public int getParameterCount() {
		return parameterCount;
	}

	/**
	 * Gets the value of the identifier from the root context object.
	 *
	 * @param context the context object used to get the value of the identifier
	 * @return the value of the identifier
	 * @throws ReflectiveOperationException if an error occurs while getting the value of the identifier
	 */
	public Object getRootValue(final RenderContext context) throws ReflectiveOperationException {
		if (context.getSettings().getContextAccess() == ContextAccess.FULL || context.getSettings().getContextAccess() == ContextAccess.CURRENT_AND_ROOT || context.getSectionData().size() == 1) {
			return getValue(context.getSectionData().peekBase());
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
		final Class<?> objectClass = context.getClass();
		final Class<?> lookupClass = Class.class.equals(objectClass) ? (Class<?>)context : objectClass;
		Accessor accessor = accessorDatabase.get(lookupClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this);

			if (accessor == null) {
				throw new NoSuchFieldException("Field \"" + name + "\" not found");
			}

			accessorDatabase.put(lookupClass, accessor);
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
		final Class<?> objectClass = context.getClass();
		final Class<?> lookupClass = Class.class.equals(objectClass) ? (Class<?>)context : objectClass;
		Accessor accessor = accessorDatabase.get(lookupClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this);

			if (accessor == null) {
				throw new NoSuchMethodError("Method \"" + name + "\" not found");
			}

			accessorDatabase.put(lookupClass, accessor);
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
