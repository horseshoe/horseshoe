package horseshoe.internal;

final class BytecodeContainer {

	private final byte[] bytecode;
	private final int startOfName;
	private final int endOfName;
	private final int hashCode;

	/**
	 * Creates a new byte array container, assuming the array is never modified.
	 *
	 * @param bytecode the array used to initialize the container
	 */
	public BytecodeContainer(final byte[] bytecode, final int startOfName, final int endOfName) {
		this.bytecode = bytecode;
		this.startOfName = startOfName;
		this.endOfName = endOfName;

		int hashValue = 1;

		for (int i = 0; i < startOfName; i++) {
			hashValue = 31 * hashValue + bytecode[i];
		}

		for (int i = endOfName; i < bytecode.length; i++) {
			hashValue = 31 * hashValue + bytecode[i];
		}

		this.hashCode = hashValue;
	}

	/**
	 * Gets the bytecode of the container.
	 *
	 * @return the bytecode of the container
	 */
	public byte[] getBytecode() {
		return bytecode;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof BytecodeContainer)) {
			return false;
		}

		final BytecodeContainer other = (BytecodeContainer) obj;

		if (bytecode.length != other.bytecode.length || startOfName != other.startOfName || endOfName != other.endOfName) {
			return false;
		}

		for (int i = 0; i < startOfName; i++) {
			if (bytecode[i] != other.bytecode[i]) {
				return false;
			}
		}

		for (int i = endOfName; i < bytecode.length; i++) {
			if (bytecode[i] != other.bytecode[i]) {
				return false;
			}
		}

		return true;
	}

}
