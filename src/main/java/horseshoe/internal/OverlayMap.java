package horseshoe.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * A map that overlays other maps, caching items retrieved from the base map.
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 */
public final class OverlayMap<K, V> extends HashMap<K, V> {

	private static final long serialVersionUID = 1L;

	private final OverlayMap<K, V> baseOverlayMap;
	private final Map<K, V> baseMap;

	/**
	 * Creates a new overlay map using the specified base map.
	 *
	 * @param baseMap the base map to overlay
	 */
	public OverlayMap(final Map<K, V> baseMap) {
		if (baseMap instanceof OverlayMap) {
			this.baseOverlayMap = (OverlayMap<K, V>)baseMap;
			this.baseMap = null;
		} else {
			this.baseOverlayMap = null;
			this.baseMap = baseMap;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public V get(final Object key) {
		final V value = super.get(key);

		if (value != null || super.containsKey(key)) {
			return value;
		}

		final V newValue = (baseOverlayMap != null ? baseOverlayMap.underlyingGet(key) : baseMap.get(key));

		super.put((K)key, newValue);
		return newValue;
	}

	/**
	 * Gets the value matching the specified key from the map or an underlying map, or null if no such item exists. This method does not cache the value in the current map.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
	 */
	public V underlyingGet(final Object key) {
		final V value = super.get(key);

		if (value != null || super.containsKey(key)) {
			return value;
		}

		return (baseOverlayMap != null ? baseOverlayMap.underlyingGet(key) : baseMap.get(key));
	}

}
