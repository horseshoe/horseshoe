package horseshoe.internal;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A streamable enables chaining of operations. Both individual items and groups of items can be iterated using a streamable. Each iteration of the streamable clears all items in the streamable, so they must be readded to the streamable.
 *
 * @param <T> the type of items contained in the streamable
 */
public abstract class Streamable<T> implements Iterable<T> {

	/**
	 * A streamable sequence enables chaining over an iterable type.
	 *
	 * @param <T> the type of items contained in the streamable sequence
	 */
	static final class StreamableSequence<T> extends Streamable<T> {

		private T[] array;
		private T[] iteratorArray;
		private int size = 0;
		private Iterator<T> initialIterator = null;

		@SuppressWarnings("unchecked")
		private StreamableSequence(final T[] array) {
			this.array = array;
			this.iteratorArray = (T[])new Object[array.length];
			this.size = array.length;
		}

		@SuppressWarnings("unchecked")
		StreamableSequence(final int initialSize, final Iterator<T> iterator) {
			this.array = (T[])new Object[initialSize];
			this.iteratorArray = (T[])new Object[initialSize];
			this.initialIterator = iterator;
		}

		@Override
		public void add(final T value) {
			if (size == array.length) {
				array = Arrays.copyOf(array, array.length * 2);
			}

			array[size++] = value;
		}

		@Override
		public boolean isEmpty() {
			return size == 0;
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

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder("[");
			final Iterator<T> iter = iterator();

			if (iter.hasNext()) {
				builder.append(' ').append(iter.next());
				while (iter.hasNext()) {
					builder.append(", ").append(iter.next());
				}
			}

			return builder.append(" ]").toString();
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
		public boolean isEmpty() {
			return value == null;
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

		@Override
		public String toString() {
			return Objects.toString(value);
		}

	}

	/**
	 * Creates a streamable of the specified value.
	 *
	 * @param value the value to reiterate
	 * @return a streamable of the specified value
	 */
	@SuppressWarnings("unchecked")
	static Streamable<Object> create(final Object value) {
		if (value instanceof Collection) {
			return Streamable.of((Collection<Object>)value);
		} else if (value instanceof Stream) {
			return new StreamableSequence<>(8, ((Stream<Object>)value).iterator());
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
		} else if (value instanceof Optional) {
			return Streamable.of(((Optional<Object>)value).orElse(null));
		}

		return Streamable.of(value);
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
		return new StreamableSequence<>(collection.size(), collection.iterator());
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
		return new StreamableSequence<>(8, iterator);
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
		return new StreamableSequence<>(Arrays.copyOf(values, values.length));
	}

	/**
	 * Returns a streamable of a value of unknown type.
	 *
	 * @param value the value to reiterate
	 * @return a streamable of the specified value
	 */
	@SuppressWarnings("unchecked")
	public static Streamable<Object> ofUnknown(final Object value) {
		if (value == null) {
			return new StreamableValue<>(null);
		} else if (value instanceof Streamable) {
			return (Streamable<Object>)value;
		}

		return create(value);
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

	/**
	 * Checks if the streamable is empty.
	 *
	 * @return true if the streamable is empty, otherwise false
	 */
	public abstract boolean isEmpty();

}
