package horseshoe.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A streamable enables chaining of operations. Both individual items and groups of items can be iterated using a streamable. Each iteration of the streamable clears all items in the streamable, so they must be readded to the streamable.
 *
 * @param <T> the type of items contained in the streamable
 */
public final class Streamable<T> implements Iterable<T> {

	private static final Object[] EMPTY_ARRAY = new Object[0];

	private T[] array;
	private Iterator<T> initialIterator = null;
	private int initialSize = 0;
	private int size = 0;

	private Streamable(final T[] array) {
		this.array = array;
		this.size = array.length;
	}

	@SuppressWarnings("unchecked")
	private Streamable(final Iterator<T> iterator) {
		this.array = (T[]) EMPTY_ARRAY;
		this.initialIterator = iterator;
	}

	private Streamable(final Iterator<T> iterator, final int initialSize) {
		this(iterator);
		this.initialSize = initialSize;
	}

	/**
	 * Adds a value to the streamable.
	 *
	 * @param value the value to add
	 */
	public void add(final T value) {
		if (size == array.length) {
			array = Arrays.copyOf(array, array.length == 0 ? Math.max(8, initialSize) : array.length * 2);
		}

		array[size++] = value;
	}

	@Override
	public Iterator<T> iterator() {
		final Iterator<T> oldIterator = initialIterator;

		if (oldIterator != null) {
			initialIterator = null;
			return new Iterator<T>() {
				@Override
				public boolean hasNext() {
					return oldIterator.hasNext();
				}

				@Override
				public T next() {
					final T item = oldIterator.next();

					add(item);
					return item;
				}
			};
		}

		return new Iterator<T>() {
			private final T[] itArray = array;
			private final int itSize = size;
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < itSize;
			}

			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}

				return itArray[index++];
			}
		};
	}

	/**
	 * Streams the data. All existing data will be cleared.
	 *
	 * @return an iterator to the stream
	 */
	@SuppressWarnings("unchecked")
	public Iterator<T> stream() {
		final Iterator<T> oldIterator = initialIterator;

		if (oldIterator != null) {
			initialIterator = null;
			return oldIterator;
		}

		final Iterator<T> it = iterator();

		initialSize = array.length;
		array = (T[]) EMPTY_ARRAY;
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

	/**
	 * Returns a streamable of the specified collection.
	 *
	 * @param <T> the type of streamable item
	 * @param collection the collection to reiterate
	 * @return a streamable of the specified type
	 */
	public static <T> Streamable<T> of(final Collection<T> collection) {
		return new Streamable<>(collection.iterator(), collection.size());
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
		return new Streamable<>(iterator);
	}

	/**
	 * Returns a streamable of the specified value.
	 *
	 * @param <T> the type of streamable item
	 * @param value the value to stream
	 * @return a streamable of the specified type
	 */
	@SuppressWarnings("unchecked")
	public static <T> Streamable<T> of(final T value) {
		return new Streamable<>((T[]) (value == null ? EMPTY_ARRAY : new Object[]{value}));
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
		return new Streamable<>(values);
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
			return new Streamable<>(EMPTY_ARRAY);
		} else if (value instanceof Streamable) {
			return (Streamable<Object>) value;
		} else if (value.getClass().isArray()) {
			if (value.getClass().getComponentType().isPrimitive()) {
				final Object[] array = new Object[Array.getLength(value)];

				for (int i = 0; i < array.length; i++) {
					array[i] = Array.get(value, i);
				}

				return Streamable.of(array);
			}

			return Streamable.of((Object[]) value);
		} else if (value instanceof Optional) {
			return Streamable.of(((Optional<Object>) value).orElse(null));
		} else if (value instanceof Collection) {
			return Streamable.of((Collection<Object>) value);
		} else if (value instanceof Stream) {
			return new Streamable<>(((Stream<Object>) value).iterator());
		} else if (value instanceof Iterable) {
			return Streamable.of((Iterable<Object>) value);
		} else if (value instanceof Iterator) {
			return Streamable.of((Iterator<Object>) value);
		}

		return Streamable.of(value);
	}

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
