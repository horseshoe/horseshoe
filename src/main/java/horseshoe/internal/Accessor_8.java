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
	static class MapAccessor extends Accessor.MapAccessor {

		MapAccessor(final String key) {
			super(key);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object tryGet(final Object context) {
			return ((Map<?, Object>)context).getOrDefault(key, Accessor.INVALID);
		}

	}

	private Accessor_8() { }

}
