package horseshoe.internal;

/**
 * A stack that persists items popped off the top, so that they can be repushed when needed.
 *
 * @param <T> the type of items contained in the stack
 */
public class PersistentStack<T> implements Iterable<T> {

	@SuppressWarnings("unchecked")
	private T array[] = (T[])new Object[8];
	private int size = 0;

	private class LIFOIterator implements java.util.Iterator<T> {
		int i = size;

		@Override
		public boolean hasNext() {
			return i != 0;
		}

		@Override
		public T next() {
			return array[--i];
		}
	}

	@Override
	public java.util.Iterator<T> iterator() {
		return new LIFOIterator();
	}

	/**
	 * Clears the stack. This functions the same as calling pop() until the stack is empty.
	 */
	public void clear() {
		size = 0;
	}

	/**
	 * Checks if a previously popped item exists in the stack. An item only exists if it is non-null.
	 *
	 * @return true if a previously popped item exists in the stack, otherwise false
	 */
	public boolean hasPoppedItem() {
		return size < array.length && array[size] != null;
	}

	/**
	 * Checks if the stack is empty.
	 *
	 * @return true if the stack is empty, otherwise false
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns the item on the top of the stack.
	 *
	 * @return the item on the top of the stack
	 */
	public T peek() {
		return array[size - 1];
	}

	/**
	 * Returns the item at the specified index of the stack.
	 *
	 * @param index the index of the item to return
	 * @return the item at the specified index of the stack
	 */
	public T peek(final int index) {
		return array[size - 1 - index];
	}

	/**
	 * Pops the top item off the stack.
	 *
	 * @return the item popped off the top of the stack
	 */
	public T pop() {
		return array[--size];
	}

	/**
	 * Replaces the top item on the stack.
	 *
	 * @param item the item to place of the stack
	 * @return the item that was replaced
	 */
	public T replace(final T item) {
		return array[size - 1] = item;
	}

	/**
	 * Pushes an item onto the stack.
	 *
	 * @param obj the object to place on the stack
	 * @return the object placed on the stack
	 */
	public T push(final T obj) {
		// Check if the array needs to be resized
		if (size == array.length) {
			@SuppressWarnings("unchecked")
			final T newArray[] = (T[])new Object[array.length * 2];

			for (int i = 0; i < size; i++) {
				newArray[i] = array[i];
			}

			array = newArray;
		}

		return array[size++] = obj;
	}

	/**
	 * Pushes the previous item back onto the stack.
	 *
	 * @return the object pushed back on the stack
	 */
	public T push() {
		return array[size++];
	}

	/**
	 * Gets the current size of the stack.
	 *
	 * @return the current size of the stack
	 */
	public int size() {
		return size;
	}

}
