package horseshoe.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

final class Accessors {

	/**
	 * An array length accessor provides access to the length of an array.
	 */
	static class ArrayLengthAccessor extends Accessor {
		private static final Accessor singleton = new ArrayLengthAccessor();

		@Override
		public Object get(final Object context) {
			return Array.getLength(context);
		}
	}

	/**
	 * A collection size accessor provides access to the size of a collection.
	 */
	static class CollectionSizeAccessor extends Accessor {
		private static final Accessor singleton = new CollectionSizeAccessor();

		@Override
		public Object get(final Object context) {
			return ((Collection<?>)context).size();
		}
	}

	/**
	 * A character sequence length accessor provides access to the length of a character sequence.
	 */
	static class CharSequenceLengthAccessor extends Accessor {
		private static final Accessor singleton = new CharSequenceLengthAccessor();

		@Override
		public Object get(final Object context) {
			return ((CharSequence)context).length();
		}
	}

	/**
	 * An entry key accessor provides access to the key of an entry.
	 */
	static class EntryKeyAccessor extends Accessor {
		private static final Accessor singleton = new EntryKeyAccessor();

		@Override
		public Object get(final Object context) {
			return ((Entry<?, ?>)context).getKey();
		}
	}

	/**
	 * An entry value accessor provides access to the value of an entry.
	 */
	static class EntryValueAccessor extends Accessor {
		private static final Accessor singleton = new EntryValueAccessor();

		@Override
		public Object get(final Object context) {
			return ((Entry<?, ?>)context).getValue();
		}
	}

	/**
	 * A map size accessor provides access to the size of a map.
	 */
	static class MapSizeAccessor extends Accessor {
		private static final Accessor singleton = new MapSizeAccessor();

		@Override
		public Object get(final Object context) {
			return ((Map<?, ?>)context).size();
		}
	}

	/**
	 * A map entries accessor provides access to the entries of a map.
	 */
	static class MapEntriesAccessor extends Accessor {
		private static final Accessor singleton = new MapEntriesAccessor();

		@Override
		public Object get(final Object context) {
			return ((Map<?, ?>)context).entrySet();
		}
	}

	/**
	 * Gets the appropriate accessor to retrieve the information from the specified context class.
	 *
	 * @param contextClass the class of the context for which information is being requested
	 * @param identifier the identifier used to represent the information being accessed
	 * @return the appropriate accessor if one is found, otherwise null
	 */
	static Accessor get(final Class<?> contextClass, final Identifier identifier) {
		switch (identifier.getName()) {
			case "entries":
				if (Map.class.isAssignableFrom(contextClass)) {
					return MapEntriesAccessor.singleton;
				}

				break;

			case "key":
				if (Entry.class.isAssignableFrom(contextClass)) {
					return EntryKeyAccessor.singleton;
				}

				break;

			case "length":
			case "size":
				if (Collection.class.isAssignableFrom(contextClass)) {
					return CollectionSizeAccessor.singleton;
				} else if (CharSequence.class.isAssignableFrom(contextClass)) {
					return CharSequenceLengthAccessor.singleton;
				} else if (Map.class.isAssignableFrom(contextClass)) {
					return MapSizeAccessor.singleton;
				} else if (contextClass.isArray()) {
					return ArrayLengthAccessor.singleton;
				}

				break;

			case "value":
				if (Entry.class.isAssignableFrom(contextClass)) {
					return EntryValueAccessor.singleton;
				}

				break;

			default: break;
		}

		return null;
	}

	private Accessors() { }

}
