package horseshoe.internal;

import horseshoe.internal.Accessor.MethodSignature;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

abstract class MethodHandler {

	private static final Factory FACTORY;

	static {
		Factory factory = new Factory();

		if (Properties.JAVA_VERSION >= 9.0) {
			try {
				factory = (Factory)MethodHandler.class.getClassLoader().loadClass(Factory.class.getName() + "9").getConstructor().newInstance();
			} catch (final ReflectiveOperationException e) {
				throw new ExceptionInInitializerError("Failed to load Java 9 specialization: " + e.getMessage());
			}
		}

		FACTORY = factory;
	}

	private static class Factory {

		protected static final Lookup LOOKUP = MethodHandles.lookup();

		protected Lookup lookup(Class<?> targetClass) throws IllegalAccessException {
			return LOOKUP;
		}

	}

	@SuppressWarnings("unused")
	private static class Factory9 extends Factory {

		public Factory9() {
			// public constructor to support reflection access
		}

		@Override
		protected Lookup lookup(Class<?> targetClass) throws IllegalAccessException {
			if (targetClass.isAnonymousClass()) {
				return MethodHandles.privateLookupIn(targetClass, LOOKUP);
			}
			return LOOKUP;
		}

	}

	/**
	 * Gets the public methods of the specified parent class that match the given information.
	 *
	 * @param methodHandles the collection used to store the matching method handles
	 * @param parent the parent class
	 * @param isStatic true to match only static methods, false to match only non-static methods
	 * @param signature the method signature
	 * @param parameterCount the parameter count of the method
	 * @throws IllegalAccessException if a matching method is found, but it cannot be accessed
	 */
	static void getPublicMethods(final Collection<MethodHandle> methodHandles, final Class<?> parent, final boolean isStatic, final MethodSignature signature, final int parameterCount) throws IllegalAccessException {
		for (final Method method : parent.getMethods()) {
			if (Modifier.isStatic(method.getModifiers()) == isStatic && !method.isSynthetic() && method.getParameterTypes().length == parameterCount && signature.matches(method)) {
				methodHandles.add(FACTORY.lookup(parent).unreflect(method).asSpreader(Object[].class, parameterCount));

				if (parameterCount == 0) {
					return;
				}
			}
		}
	}

	MethodHandler() {
		throw new UnsupportedOperationException();
	}

}
