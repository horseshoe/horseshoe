package horseshoe;

import java.util.Objects;

final class ThrowableComparator {

	private final Throwable throwable;
	private final StackTraceElement[] trace;

	ThrowableComparator(final Throwable throwable) {
		this.throwable = throwable;
		this.trace = throwable.getStackTrace();
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof ThrowableComparator) || !throwable.getClass().equals(((ThrowableComparator) object).throwable.getClass()) ||
				!Objects.equals(throwable.getMessage(), ((ThrowableComparator) object).throwable.getMessage())) {
			return false;
		}

		// Compare stack traces up to the last expression element
		final StackTraceElement[] otherTrace = ((ThrowableComparator) object).trace;
		final int length = Math.min(trace.length, otherTrace.length);
		int i;

		for (i = 0; i < length && trace[i].equals(otherTrace[i]); i++);

		for (int j = i; j < trace.length; j++) {
			if (Expression.class.getName().equals(trace[j].getClassName())) {
				return false;
			}
		}

		for (int j = i; j < otherTrace.length; j++) {
			if (Expression.class.getName().equals(otherTrace[j].getClassName())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(throwable.getClass(), throwable.getMessage());
	}
}
