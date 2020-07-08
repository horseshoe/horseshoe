package horseshoe;

import java.util.NoSuchElementException;

/**
 * A simple, sane stack implementation.
 *
 * @param <T> the type of items contained in the stack
 */
public final class Stack<T> implements Iterable<T> {

	@SuppressWarnings("unchecked")
	private T[] array = (T[])new Object[8];
	private int size = 0;

	private class LIFOIterator implements java.util.Iterator<T> {
		private int index = size;

		@Override
		public boolean hasNext() {
			return index != 0;
		}

		@Override
		public T next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			return array[--index];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Gets the current capacity of the stack.
	 *
	 * @return the current capacity of the stack
	 */
	public int capacity() {
		return array.length;
	}

	/**
	 * Clears the stack. This functions the same as calling pop() until the stack is empty.
	 *
	 * @return this stack
	 */
	public Stack<T> clear() {
		for (int i = 0; i < size; i++) {
			array[i] = null;
		}

		size = 0;
		return this;
	}

	/**
	 * Checks if the stack is empty.
	 *
	 * @return true if the stack is empty, otherwise false
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public java.util.Iterator<T> iterator() {
		return new LIFOIterator();
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
	 * Returns the item on the bottom of the stack.
	 *
	 * @return the item on the bottom of the stack
	 */
	public T peekBase() {
		return array[0];
	}

	/**
	 * Pops the top item off the stack.
	 *
	 * @return the item popped off the top of the stack
	 */
	public T pop() {
		final T obj = array[--size];

		array[size] = null;
		return obj;
	}

	/**
	 * Pops the specified number of items off the stack.
	 *
	 * @param size the number of items to pop off the stack
	 * @return this stack
	 */
	public Stack<T> pop(final int size) {
		final int oldSize = this.size;
		this.size -= size;

		for (int i = this.size; i < oldSize; i++) {
			array[i] = null;
		}

		return this;
	}

	/**
	 * Pushes an item onto the stack.
	 *
	 * @param obj the object to place onto the stack
	 * @return this stack
	 */
	public Stack<T> push(final T obj) {
		// Check if the array needs to be resized
		if (size == array.length) {
			@SuppressWarnings("unchecked")
			final T[] newArray = (T[])new Object[array.length * 2];

			System.arraycopy(array, 0, newArray, 0, size);
			array = newArray;
		}

		array[size++] = obj;
		return this;
	}

	/**
	 * Gets the current size of the stack.
	 *
	 * @return the current size of the stack
	 */
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("[");

		if (size > 0) {
			sb.append(array[size - 1]);

			for (int i = size - 2; i >= 0; i--) {
				sb.append(", ").append(array[i]);
			}
		}

		return sb.append(']').toString();
	}

}
