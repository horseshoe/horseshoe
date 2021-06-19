package horseshoe.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;

import horseshoe.RenderContext;
import horseshoe.Settings.ContextAccess;

public final class Identifier {

	public static final int UNSTATED_BACKREACH = -1;
	public static final int NOT_A_METHOD = -1;
	public static final Object NULL_ORIGINAL_CONTEXT = new Object();

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
	 * @param <T> the type of exception to throw
	 * @param ignoreFailures true to return null for failures, false to throw the exception
	 * @param type the type of the exception to throw
	 * @param message a detailed exception message
	 * @return null
	 * @throws T if failures are not ignored
	 * @throws ReflectiveOperationException if the exception type does not support a single string constructor
	 */
	private static <T extends Throwable> Object throwException(final boolean ignoreFailures, final Class<T> type, final String message) throws T, ReflectiveOperationException {
		if (ignoreFailures) {
			return null;
		}

		throw type.getConstructor(String.class).newInstance(message);
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
		final Object object = Expression.peekStack(context.getSectionData(), backreach < 0 ? 0 : backreach, name).data;
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
					final Object backreachObject = context.getSectionData().peek(i).data;
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
				return getValue(context.getSectionData().peekBase().data, object == null ? NULL_ORIGINAL_CONTEXT : object, skippedAccessor);
			}
		}

		if (skippedAccessor) {
			return null;
		}

		throw new NoSuchFieldException("Field \"" + name + "\" not found in " + getObjectType(object) + " object");
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
		final Object object = Expression.peekStack(context.getSectionData(), backreach < 0 ? 0 : backreach, name).data;
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
					final Object backreachObject = context.getSectionData().peek(i).data;
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
				return getValue(context.getSectionData().peekBase().data, object == null ? NULL_ORIGINAL_CONTEXT : object, skippedAccessor, parameters);
			}
		}

		if (skippedAccessor) {
			return null;
		}

		throw new NoSuchMethodException("Method \"" + name + "\" not found in " + getObjectType(object) + " object");
	}

	/**
	 * Gets the string name of the type of an object, or "null" if the object is null.
	 *
	 * @param object the object to get the type of
	 * @return the name of the type for the specified object, or "null" if the object is null
	 */
	private static String getObjectType(final Object object) {
		return object == null ? "null" : object.getClass().getName();
	}

	/**
	 * Gets the string name of the type of an object, or the name of the object class if the original object is null.
	 *
	 * @param originalObject the original object to get the type of
	 * @param objectClass the object class to use if the original object is null
	 * @return the name of the type for the specified object, or the name of the object class if the original object is null
	 */
	private static String getObjectType(final Object originalObject, final Class<?> objectClass) {
		if (originalObject == null) {
			return objectClass.getName();
		}

		return NULL_ORIGINAL_CONTEXT.equals(originalObject) ? "null" : getObjectType(originalObject);
	}

	/**
	 * Gets the accessor for the specified object, or creates and adds an accessor for the object if one does not exist.
	 *
	 * @param object the object used to get the accessor
	 * @return the accessor for the specified object
	 * @throws IllegalAccessException if the value cannot be retrieved due to an illegal access
	 */
	private Accessor getOrAddAccessor(final Object object) throws IllegalAccessException {
		if (object == null) {
			return null;
		}

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
		return getValue(context.getSectionData().peekBase().data, NULL_ORIGINAL_CONTEXT, false);
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
		return getValue(context.getSectionData().peekBase().data, NULL_ORIGINAL_CONTEXT, false, parameters);
	}

	/**
	 * Gets the value of the identifier given the context object.
	 *
	 * @param context the context object to evaluate
	 * @param originalContext the original context object
	 * @param ignoreFailures true to return null for failures, false to throw exceptions
	 * @return the result of the evaluation
	 * @throws Throwable if accessing the value throws
	 */
	public Object getValue(final Object context, final Object originalContext, final boolean ignoreFailures) throws Throwable {
		if (context == null) {
			return throwException(ignoreFailures, NullPointerException.class, "Field \"" + name + "\" not found in null object");
		}

		final Class<?> objectClass = context.getClass();
		final Class<?> lookupClass = Class.class.equals(objectClass) ? (Class<?>)context : objectClass;
		Accessor accessor = accessorDatabase.get(lookupClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this);

			if (accessor == null) {
				return throwException(ignoreFailures, NoSuchFieldException.class, "Field \"" + name + "\" not found in " + getObjectType(originalContext, objectClass) + " object");
			}

			accessorDatabase.put(lookupClass, accessor);
		}

		return accessor.get(context);
	}

	/**
	 * Evaluates the method identifier given the context object and parameters.
	 *
	 * @param context the context object to evaluate
	 * @param originalContext the original context object
	 * @param ignoreFailures true to return null for failures, false to throw exceptions
	 * @param parameters the parameters used to evaluate the object
	 * @return the result of the evaluation
	 * @throws Throwable if accessing the value throws
	 */
	public Object getValue(final Object context, final Object originalContext, final boolean ignoreFailures, final Object... parameters) throws Throwable {
		if (context == null) {
			return throwException(ignoreFailures, NullPointerException.class, "Method \"" + name + "\" not found in null object");
		}

		final Class<?> objectClass = context.getClass();
		final Class<?> lookupClass = Class.class.equals(objectClass) ? (Class<?>)context : objectClass;
		Accessor accessor = accessorDatabase.get(lookupClass);

		if (accessor == null) {
			accessor = Accessor.FACTORY.create(context, this);

			if (accessor == null) {
				return throwException(ignoreFailures, NoSuchMethodException.class, "Method \"" + name + "\" not found in " + getObjectType(originalContext, objectClass) + " object");
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
