package horseshoe.internal;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class CacheList<T> {

	private final Map<T, T> backingSet;
	private final Map<T, Integer> map = new LinkedHashMap<>();

	/**
	 * Creates a map collection using the specified backing set.
	 *
	 * @param backingSet the backing set for the map collection
	 */
	public CacheList(final Map<T, T> backingSet) {
		this.backingSet = backingSet;
	}

	/**
	 * Creates a map collection with no backing set.
	 */
	public CacheList() {
		this(null);
	}

	/**
	 * Checks if this collection contains the same items as the list.
	 *
	 * @param list the array of items to compare
	 * @return true if this collection is empty and list is null or if they both contain the same items in the same order, otherwise false
	 */
	public boolean equalsArray(final T[] list) {
		if (map.size() != list.length) {
			return false;
		}

		int i = 0;

		for (final Iterator<T> it = map.keySet().iterator(); it.hasNext(); i++) {
			if (!it.next().equals(list[i])) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Gets the index for the specified item. If the item does not exist, null is returned.
	 *
	 * @param item the item to get the index of
	 * @return the index of the specified item, or null if the item does not exist
	 */
	public Integer get(final T item) {
		return map.get(item);
	}

	/**
	 * Gets the index for the specified item. If the item does not exist, a new entry will be created and that index will be returned.
	 *
	 * @param item the item to get the index of
	 * @return the index of the specified item
	 */
	public int getOrAdd(final T item) {
		Integer index = map.get(item);

		if (index != null) {
			return index;
		}

		final int newIndex = map.size();
		T itemToAdd = item;

		if (backingSet != null) {
			final T cachedItem = backingSet.get(item);

			if (cachedItem != null) {
				itemToAdd = cachedItem;
			} else {
				backingSet.put(item, item);
			}
		}

		map.put(itemToAdd, newIndex);
		return newIndex;
	}

	/**
	 * Checks if the collection is empty.
	 *
	 * @return true if the collection is empty, otherwise false
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Gets the size of the collection.
	 *
	 * @return the size of the collection
	 */
	public int size() {
		return map.size();
	}

	/**
	 * Creates an array of all the items.
	 *
	 * @param type the type of the items in the list
	 * @param emptyArray the value to return if the list is empty
	 * @return an array of all the items
	 */
	@SuppressWarnings("unchecked")
	public T[] toArray(final Class<T> type, final T[] emptyArray) {
		if (isEmpty()) {
			return emptyArray;
		}

		final T[] array = (T[])Array.newInstance(type, map.size());

		for (final Entry<T, Integer> entry : map.entrySet()) {
			array[entry.getValue()] = entry.getKey();
		}

		return array;
	}

}
