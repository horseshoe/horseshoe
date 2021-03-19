package horseshoe.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

import horseshoe.internal.Accessor.MethodSignature;

abstract class MethodHandler {

	private static final Lookup lookup = MethodHandles.lookup();
	private static final LookerUpper lookerUpper = Properties.JAVA_VERSION >= 9.0 ? new PrivateLookerUpper() : x -> lookup;

	private interface LookerUpper {
		Lookup lookup(Class<?> targetClass) throws IllegalAccessException;
	}

	private static class PrivateLookerUpper implements LookerUpper {

		private static final MethodHandle privateLookup;

		static {
			try {
				privateLookup = lookup.unreflect(MethodHandles.class.getMethod("privateLookupIn", Class.class, Lookup.class));
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		public Lookup lookup(Class<?> targetClass) throws IllegalAccessException {
			if (targetClass.isAnonymousClass()) {
				try {
					return (Lookup) privateLookup.invokeExact(targetClass, lookup);
				} catch (final Throwable t) {
					throw new InternalError(t.getMessage());
				}
			}
			return lookup;
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
	public static void getPublicMethods(final Collection<MethodHandle> methodHandles, final Class<?> parent, final boolean isStatic, final MethodSignature signature, final int parameterCount) throws IllegalAccessException {
		for (final Method method : parent.getMethods()) {
			if (Modifier.isStatic(method.getModifiers()) == isStatic && !method.isSynthetic() && method.getParameterTypes().length == parameterCount && signature.matches(method)) {
				methodHandles.add(lookerUpper.lookup(parent).unreflect(method).asSpreader(Object[].class, parameterCount));

				if (parameterCount == 0) {
					return;
				}
			}
		}
	}

}
