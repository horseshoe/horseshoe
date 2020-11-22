package horseshoe.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;

import horseshoe.RenderContext;
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
	 * Returns null if failures are ignored. Otherwise, throws the exception with the specified message.
	 *
	 * @param ignoreFailures true to return null for failures, false to throw the exception
	 * @param message a detailed exception message
	 * @return null
	 * @throws NoSuchFieldException if failures are not ignored
	 */
	private static Object throwNoSuchFieldException(final boolean ignoreFailures, final String message) throws NoSuchFieldException {
		if (ignoreFailures) {
			return null;
		}

		throw new NoSuchFieldException(message);
	}

	/**
	 * Returns null if failures are ignored. Otherwise, throws the exception with the specified message.
	 *
	 * @param ignoreFailures true to return null for failures, false to throw the exception
	 * @param message a detailed exception message
	 * @return null
	 * @throws NoSuchMethodException if failures are not ignored
	 */
	private static Object throwNoSuchMethodException(final boolean ignoreFailures, final String message) throws NoSuchMethodException {
		if (ignoreFailures) {
			return null;
		}

		throw new NoSuchMethodException(message);
	}

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
		return object instanceof Identifier &&
				name.equals(((Identifier)object).name) &&
				parameterCount == ((Identifier)object).parameterCount;
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
	 * @throws Throwable if accessing the value throws
	 */
	public Object findValue(final RenderContext context, final int backreach) throws Throwable {
		// Try to get value at the specified scope
		boolean skippedAccessor = false;
		final Object object = Expression.peekStack(context.getSectionData(), backreach < 0 ? 0 : backreach, name);
		final Accessor accessor = getOrAddAccessor(object);

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
					final Object backreachObject = context.getSectionData().peek(i);
					final Accessor backreachAccessor = getOrAddAccessor(backreachObject);

					if (backreachAccessor != null) {
						final Object result = backreachAccessor.tryGet(backreachObject);

						if (Accessor.isValid(result)) {
							return result;
						}

						skippedAccessor = true;
					}
				}
			} else if (context.getSettings().getContextAccess() == ContextAccess.CURRENT_AND_ROOT) {
				return getValue(context.getSectionData().peekBase(), false);
			}
		}

		if (skippedAccessor) {
			return null;
		}

		throw new NoSuchFieldException("Field \"" + name + "\" not found in object of type " + object.getClass().getName());
	}

	/**
	 * Finds and evaluates the method identifier given the context object and parameters.
	 *
	 * @param context the context object used to get the value of the identifier
	 * @param backreach the backreach for the identifier, or less than 0 to indicate an unspecified backreach
	 * @param parameters the parameters used to evaluate the object
	 * @return the value of the identifier
	 * @throws Throwable if accessing the value throws
	 */
	public Object findValue(final RenderContext context, final int backreach, final Object... parameters) throws Throwable {
		// Try to get value at the specified scope
		boolean skippedAccessor = false;
		final Object object = Expression.peekStack(context.getSectionData(), backreach < 0 ? 0 : backreach, name);
		final Accessor accessor = getOrAddAccessor(object);

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
					final Object backreachObject = context.getSectionData().peek(i);
					final Accessor backreachAccessor = getOrAddAccessor(backreachObject);

					if (backreachAccessor != null) {
						final Object result = backreachAccessor.tryGet(backreachObject, parameters);

						if (Accessor.isValid(result)) {
							return result;
						}

						skippedAccessor = true;
					}
				}
			} else if (context.getSettings().getContextAccess() == ContextAccess.CURRENT_AND_ROOT) {
				return getValue(context.getSectionData().peekBase(), false, parameters);
			}
		}

		if (skippedAccessor) {
			return null;
		}

		throw new NoSuchMethodException("Method \"" + name + "\" not found in object of type " + object.getClass().getName());
	}

	/**
	 * Gets the accessor for the specified object, or creates and adds an accessor for the object if one does not exist.
	 *
	 * @param object the object used to get the accessor
	 * @return the accessor for the specified object
	 * @throws IllegalAccessException if the value cannot be retrieved due to an illegal access
	 */
	private Accessor getOrAddAccessor(final Object object) throws IllegalAccessException {
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
	 * @throws Throwable if accessing the value throws
	 */
	public Object getRootValue(final RenderContext context) throws Throwable {
		return getValue(context.getSectionData().peekBase(), false);
	}

	/**
	 * Gets the value of the identifier from the root context object.
	 *
	 * @param context the context object used to get the value of the identifier
	 * @param parameters the parameters used to evaluate the object
	 * @return the value of the identifier
	 * @throws Throwable if accessing the value throws
	 */
	public Object getRootValue(final RenderContext context, final Object... parameters) throws Throwable {
		return getValue(context.getSectionData().peekBase(), false, parameters);
	}

	/**
	 * Gets the value of the identifier given the context object.
	 *
	 * @param context the context object to evaluate
	 * @param ignoreFailures true to return null for failures, false to throw exceptions
	 * @return the result of the evaluation
	 * @throws Throwable if accessing the value throws
	 */
	public Object getValue(final Object context, final boolean ignoreFailures) throws Throwable {
		if (context == null) {
			return throwNoSuchFieldException(ignoreFailures, "Field \"" + name + "\" not found in null object");
		}

		final Class<?> objectClass = context.getClass();
		final Class<?> lookupClass = Class.class.equals(objectClass) ? (Class<?>)context : objectClass;
		Accessor accessor = accessorDatabase.get(lookupClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this);

			if (accessor == null) {
				return throwNoSuchFieldException(ignoreFailures, "Field \"" + name + "\" not found in object of type " + objectClass.getName());
			}

			accessorDatabase.put(lookupClass, accessor);
		}

		return accessor.get(context);
	}

	/**
	 * Evaluates the method identifier given the context object and parameters.
	 *
	 * @param context the context object to evaluate
	 * @param ignoreFailures true to return null for failures, false to throw exceptions
	 * @param parameters the parameters used to evaluate the object
	 * @return the result of the evaluation
	 * @throws Throwable if accessing the value throws
	 */
	public Object getValue(final Object context, final boolean ignoreFailures, final Object... parameters) throws Throwable {
		if (context == null) {
			return throwNoSuchMethodException(ignoreFailures, "Method \"" + name + "\" not found in null object");
		}

		final Class<?> objectClass = context.getClass();
		final Class<?> lookupClass = Class.class.equals(objectClass) ? (Class<?>)context : objectClass;
		Accessor accessor = accessorDatabase.get(lookupClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this);

			if (accessor == null) {
				return throwNoSuchMethodException(ignoreFailures, "Method \"" + name + "\" not found in object of type " + objectClass.getName());
			}

			accessorDatabase.put(lookupClass, accessor);
		}

		return accessor.get(context, parameters);
	}

	@Override
	public int hashCode() {
		return parameterCount * 0x1010101 + name.hashCode();
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
