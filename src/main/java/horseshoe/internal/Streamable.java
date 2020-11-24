package horseshoe.internal;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A streamable enables chaining of operations. Both individual items and groups of items can be iterated using a streamable. Each iteration of the streamable clears all items in the streamable, so they must be readded to the streamable.
 *
 * @param <T> the type of items contained in the streamable
 */
public abstract class Streamable<T> implements Iterable<T> {

	private static final Factory FACTORY;

	static {
		Factory factory = new Factory();

		if (Properties.JAVA_VERSION >= 8.0) {
			try {
				factory = (Factory)Streamable.class.getClassLoader().loadClass(Streamable.class.getName() + "_8$" + Factory.class.getSimpleName()).getConstructor().newInstance();
			} catch (final ReflectiveOperationException e) {
				throw new ExceptionInInitializerError("Failed to load Java 8 specialization: " + e.getMessage());
			}
		}

		FACTORY = factory;
	}

	/**
	 * A factory class for creating streamables.
	 */
	static class Factory {
		/**
		 * Creates a streamable of the specified value.
		 *
		 * @param value the value to reiterate
		 * @return a streamable of the specified value
		 */
		@SuppressWarnings("unchecked")
		Streamable<?> create(final Object value) {
			if (value instanceof Collection) {
				return Streamable.of((Collection<Object>)value);
			} else if (value instanceof Iterable) {
				return Streamable.of((Iterable<Object>)value);
			} else if (value instanceof Iterator) {
				return Streamable.of((Iterator<Object>)value);
			} else if (value.getClass().isArray()) {
				if (value.getClass().getComponentType().isPrimitive()) {
					final Object[] array = new Object[Array.getLength(value)];

					for (int i = 0; i < array.length; i++) {
						array[i] = Array.get(value, i);
					}

					return Streamable.of(array);
				}

				final Object[] original = (Object[])value;
				return Streamable.of(Arrays.copyOf(original, original.length));
			}

			return Streamable.of(value);
		}
	}

	/**
	 * A streamable list enables chaining over an iterable type.
	 *
	 * @param <T> the type of items contained in the streamable list
	 */
	static final class StreamableList<T> extends Streamable<T> {

		private T[] array;
		private T[] iteratorArray;
		private int size = 0;
		private Iterator<T> initialIterator = null;

		@SuppressWarnings("unchecked")
		private StreamableList(final T[] array) {
			this.array = array;
			this.iteratorArray = (T[])new Object[array.length];
			this.size = array.length;
		}

		@SuppressWarnings("unchecked")
		StreamableList(final int initialSize, final Iterator<T> iterator) {
			this.array = (T[])new Object[initialSize];
			this.iteratorArray = (T[])new Object[initialSize];
			this.initialIterator = iterator;
		}

		@Override
		public void add(final T value) {
			if (size == array.length) {
				@SuppressWarnings("unchecked")
				final T[] newArray = (T[])new Object[array.length * 2];

				System.arraycopy(array, 0, newArray, 0, size);
				array = newArray;
			}

			array[size++] = value;
		}

		@Override
		public Iterator<T> iterator() {
			final Iterator<T> oldIterator = initialIterator;

			if (oldIterator != null) {
				initialIterator = null;
				return oldIterator;
			}

			final T[] oldArray = array;
			final Iterator<T> it = new Iterator<T>() {
				private final int iteratorSize = size;
				private int index = 0;

				@Override
				public boolean hasNext() {
					return index < iteratorSize;
				}

				@Override
				public T next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}

					return iteratorArray[index++];
				}

				@Override
				public void remove() {
					// All values are automatically removed and must be re-added manually on every iteration
				}
			};

			array = iteratorArray;
			iteratorArray = oldArray;
			size = 0;
			return it;
		}

	}

	/**
	 * A streamable value enables chaining over an optional value.
	 *
	 * @param <T> the type of item contained in the streamable value
	 */
	static final class StreamableValue<T> extends Streamable<T> {

		private T value;

		StreamableValue(final T value) {
			this.value = value;
		}

		@Override
		public void add(final T value) {
			this.value = value;
		}

		@Override
		public Iterator<T> iterator() {
			return new Iterator<T>() {
				boolean hasNext = (value != null);

				@Override
				public boolean hasNext() {
					return hasNext;
				}

				@Override
				public T next() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}

					final T oldValue = value;

					hasNext = false;
					value = null;
					return oldValue;
				}

				@Override
				public void remove() {
					// All values are automatically removed and must be re-added manually on every iteration
				}
			};
		}

	}

	/**
	 * Returns a streamable of the specified value.
	 *
	 * @param <T> the type of streamable item
	 * @param value the value to stream
	 * @return a streamable of the specified type
	 */
	public static <T> Streamable<T> of(final T value) {
		return new StreamableValue<>(value);
	}

	/**
	 * Returns a streamable of the specified collection.
	 *
	 * @param <T> the type of streamable item
	 * @param collection the collection to reiterate
	 * @return a streamable of the specified type
	 */
	public static <T> Streamable<T> of(final Collection<T> collection) {
		return new StreamableList<>(collection.size(), collection.iterator());
	}

	/**
	 * Returns a streamable of the specified iterable.
	 *
	 * @param <T> the type of streamable item
	 * @param iterable the iterable to reiterate
	 * @return a streamable of the specified type
	 */
	public static <T> Streamable<T> of(final Iterable<T> iterable) {
		return of(iterable.iterator());
	}

	/**
	 * Returns a streamable of the specified iterator.
	 *
	 * @param <T> the type of streamable item
	 * @param iterator the iterator
	 * @return a streamable of the specified type
	 */
	public static <T> Streamable<T> of(final Iterator<T> iterator) {
		return new StreamableList<>(8, iterator);
	}

	/**
	 * Returns a streamable of the specified values.
	 *
	 * @param <T> the type of streamable item
	 * @param values the values to reiterate
	 * @return a streamable of the specified type
	 */
	@SafeVarargs
	public static <T> Streamable<T> of(final T... values) {
		return new StreamableList<>(Arrays.copyOf(values, values.length));
	}

	/**
	 * Returns a streamable of a value of unknown type.
	 *
	 * @param value the value to reiterate
	 * @return a streamable of the specified value
	 */
	public static Streamable<?> ofUnknown(final Object value) {
		if (value == null) {
			return new StreamableValue<>(null);
		} else if (value instanceof Streamable<?>) {
			return (Streamable<?>)value;
		}

		return FACTORY.create(value);
	}

	/**
	 * Adds a value to the streamable.
	 *
	 * @param value the value to add
	 */
	public abstract void add(final T value);

	/**
	 * Adds values in an iterable to the streamable.
	 *
	 * @param iterable the iterable to add as flattened items
	 */
	public void flatAdd(final Iterable<? extends T> iterable) {
		for (final T item : iterable) {
			add(item);
		}
	}

	/**
	 * Adds values in an array to the streamable.
	 *
	 * @param values the values to add
	 */
	public void flatAdd(final T[] values) {
		for (final T item : values) {
			add(item);
		}
	}

}
