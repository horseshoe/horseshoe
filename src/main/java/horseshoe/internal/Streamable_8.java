package horseshoe.internal;

import java.util.Optional;
import java.util.stream.Stream;

import horseshoe.internal.Streamable.StreamableList;

/**
 * Provides Java 8 extensions for streamables.
 */
final class Streamable_8 {

	private Streamable_8() { }

	/**
	 * Java 8 extensions for creating streamables.
	 */
	public static final class Factory extends Streamable.Factory {
		@SuppressWarnings("unchecked")
		@Override
		Streamable<?> create(final Object value) {
			if (value instanceof Stream) {
				return new StreamableList<>(8, ((Stream<Object>)value).iterator());
			} else if (value instanceof Optional) {
				return Streamable.of(((Optional<Object>)value).orElse(null));
			}

			return super.create(value);
		}
	}

}
