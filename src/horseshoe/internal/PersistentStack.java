package horseshoe.internal;

import java.util.ArrayList;

/**
 * A stack that persists items popped off the top, so that they can be repushed when needed.
 *
 * @param <T> the type of items contained in the stack
 */
public class PersistentStack<T> implements Iterable<T> {

	private final ArrayList<T> list = new ArrayList<>();
	private int size;

	private class LIFOIterator implements java.util.Iterator<T> {
		int i = size;

		@Override
		public boolean hasNext() {
			return i != 0;
		}

		@Override
		public T next() {
			return list.get(--i);
		}
	}

	@Override
	public java.util.Iterator<T> iterator() {
		return new LIFOIterator();
	}

	/**
	 * Clears the stack.
	 */
	public void clear() {
		list.clear();
		size = 0;
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
	 * Gets the largest size of the stack since the last clear.
	 *
	 * @return the largest size of the stack since the last clear
	 */
	public int largestSize() {
		return list.size();
	}

	/**
	 * Returns the item on the top of the stack.
	 *
	 * @return the item on the top of the stack
	 */
	public T peek() {
		return list.get(size - 1);
	}

	/**
	 * Returns the item at the specified index of the stack.
	 *
	 * @param index the index of the item to return
	 * @return the item at the specified index of the stack
	 */
	public T peek(final int index) {
		return list.get(size - 1 - index);
	}

	/**
	 * Pops the top item off the stack.
	 *
	 * @return the item popped off the top of the stack
	 */
	public T pop() {
		return list.get(--size);
	}

	/**
	 * Replaces the top item on the stack.
	 *
	 * @param item the item to place of the stack
	 * @return the item that was replaced
	 */
	public T replace(final T item) {
		return list.set(size - 1, item);
	}

	/**
	 * Pushes an item onto the stack.
	 *
	 * @param obj the object to place on the stack
	 * @return the object placed on the stack
	 */
	public T push(final T obj) {
		if (size == list.size()) {
			list.add(obj);
			size++;
		} else {
			list.set(size++, obj);
		}

		return obj;
	}

	/**
	 * Pushes the previous item back onto the stack.
	 *
	 * @return the object pushed back on the stack
	 */
	public T push() {
		return list.get(size++);
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
