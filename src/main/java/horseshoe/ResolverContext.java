package horseshoe;

import java.util.Arrays;

final class ResolverContext {

	private final Class<?> objectClass;
	private final Class<?> argumentClasses[];

	public ResolverContext(final Class<?> objectClass, final Class<?> argumentClasses[]) {
		this.objectClass = objectClass;
		this.argumentClasses = argumentClasses;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || obj.getClass() != ResolverContext.class) {
			return false;
		}

		final ResolverContext other = (ResolverContext)obj;

		return objectClass == other.objectClass && Arrays.equals(argumentClasses, other.argumentClasses);
	}

	public Class<?>[] getArgumentClasses() {
		return argumentClasses;
	}

	public Class<?> getObjectClass() {
		return objectClass;
	}

	@Override
	public int hashCode() {
		int hash = objectClass.hashCode();

		if (argumentClasses != null) {
			for (int i = 0; i < argumentClasses.length; i++) {
				hash ^= (argumentClasses[i] == null ? 0 : argumentClasses[i].hashCode()) + (1 << (i & 31));
			}
		}

		return hash;
	}

}