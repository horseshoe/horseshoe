package horseshoe.internal;

import java.util.Map;

final class Accessor_8 {

	public static class MapAccessorFactory extends Accessor.MapAccessorFactory {

		@Override
		Accessor create(final String key) {
			return new MapAccessor(key);
		}

	}

	/**
	 * A map accessor provides access to a value in a map using the specified key.
	 */
	static final class MapAccessor extends Accessor {

		private final String key;

		MapAccessor(final String key) {
			this.key = key;
		}

		@Override
		public Object get(final Object context) {
			return ((Map<?, ?>)context).get(key);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object tryGet(final Object context) {
			return ((Map<?, Object>)context).getOrDefault(key, Accessor.INVALID);
		}

	}

	private Accessor_8() { }

}
