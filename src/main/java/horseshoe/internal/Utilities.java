package horseshoe.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Map;

public final class Utilities {

	private static final byte WHITESPACE_PROPERTY = 0x01;
	private static final byte END_OF_LINE_PROPERTY = 0x02;

	private static final byte[] ASCII_PROPERTIES = new byte[] {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 3, 3, 3, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	};

	private static final MethodHandle MAP_GET_OR_DEFAULT;
	private static final MethodHandle MAP_PUT_IF_ABSENT;

	static {
		final Lookup lookup = MethodHandles.publicLookup();

		MethodHandle mapGetOrDefault = null;
		MethodHandle mapPutIfAbsent = null;

		if (!Properties.USE_JAVA_7 && Properties.JAVA_VERSION >= 8.0) {
			try {
				mapGetOrDefault = lookup.unreflect(Map.class.getMethod("getOrDefault", Object.class, Object.class));
				mapPutIfAbsent = lookup.unreflect(Map.class.getMethod("putIfAbsent", Object.class, Object.class));
			} catch (final ReflectiveOperationException e) {
				throw new ExceptionInInitializerError("Failed to load Java 8 specialization: " + e.getMessage());
			}
		}

		MAP_GET_OR_DEFAULT = mapGetOrDefault;
		MAP_PUT_IF_ABSENT = mapPutIfAbsent;
	}

	public static final class Range {

		public final int start;
		public final int end;

		public Range(final int start, final int end) {
			this.start = start;
			this.end = end;
		}

	}

	public static final class TrimmedString {

		public final int start;
		public final String string;

		public TrimmedString(final int start, final String string) {
			this.start = start;
			this.string = string;
		}

	}

	/**
	 * Finds the next end of line sequence. An end of line sequence is equivalent to the following pattern: \r\n|[\n\x0B\x0C\r\u0085\u2028\u2029]
	 *
	 * @param value the character sequence to search for the next end of line sequence
	 * @param start the starting index where the search will be performed
	 * @param end the ending index where the search will be performed
	 * @return the character range of the next end of line sequence, or null if none could be found
	 */
	public static Range findNewLine(final char[] value, final int start, final int end) {
		for (int index = start; index < end; index++) {
			final char c = value[index];

			if (c >= ASCII_PROPERTIES.length) {
				if (c >= 0x2028 && c <= 0x2029) {
					return new Range(index, index + 1);
				}
			} else if ((ASCII_PROPERTIES[c] & END_OF_LINE_PROPERTY) != 0) {
				if (c == '\r' && index + 1 < end && value[index + 1] == '\n') {
					return new Range(index, index + 2);
				}

				return new Range(index, index + 1);
			}
		}

		return null;
	}

	/**
	 * Finds the next end of line sequence. An end of line sequence is equivalent to the following pattern: \r\n|[\n\x0B\x0C\r\u0085\u2028\u2029]
	 *
	 * @param value the character sequence to search for the next end of line sequence
	 * @param start the starting index where the search will be performed
	 * @param end the ending index where the search will be performed
	 * @return the character range of the next end of line sequence, or null if none could be found
	 */
	public static Range findNewLine(final String value, final int start, final int end) {
		return findNewLine(value.toCharArray(), start, end);
	}

	/**
	 * Gets the specified value from a map using the key, or the default value if the key does not exist in the map.
	 *
	 * @param <V> the type of the map values
	 * @param map the map used to lookup the value
	 * @param key the key of the value in the map
	 * @param defaultValue the default value that will be returned if the key is not found in the map
	 * @return the specified value from the map corresponding to the key, or the default value if the key does not exist
	 */
	@SuppressWarnings("unchecked")
	public static <V> V getMapValueOrDefault(final Map<?, V> map, final Object key, final V defaultValue) {
		if (MAP_GET_OR_DEFAULT != null) {
			try {
				return (V)MAP_GET_OR_DEFAULT.invokeExact(map, key, defaultValue);
			} catch (final Throwable throwable) {
				throw new InternalError(throwable.getMessage());
			}
		}

		final Object result = map.get(key);
		return result != null || map.containsKey(key) ? (V)result : defaultValue;
	}

	/**
	 * Gets the specified value from a map using the key, or sets the value if the key does not exist in the map and returns it.
	 *
	 * @param <K> the type of the map keys
	 * @param <V> the type of the map values
	 * @param map the map used to lookup the value
	 * @param key the key of the value in the map
	 * @param value the value that will be set and returned if the key is not found in the map
	 * @return the specified value from the map corresponding to the key, or the value if the key does not exist
	 */
	public static <K, V> V getOrAddMapValue(final Map<K, V> map, final K key, final V value) {
		if (MAP_PUT_IF_ABSENT != null) {
			try {
				final V existingValue = (V)MAP_PUT_IF_ABSENT.invokeExact(map, key, value);
				return existingValue == null ? value : existingValue;
			} catch (final Throwable throwable) {
				throw new InternalError(throwable.getMessage());
			}
		}

		final V existingValue = map.get(key);

		if (existingValue != null) {
			return existingValue;
		}

		map.put(key, value);
		return value;
	}

	/**
	 * Gets whether or not the character is a whitespace character.
	 *
	 * @param c the character to test for a whitespace character
	 * @return true if the character is a whitespace character, otherwise false
	 */
	private static boolean isWhitespace(final char c) {
		if (c >= ASCII_PROPERTIES.length) {
			return Character.isSpaceChar(c);
		}

		return (ASCII_PROPERTIES[c] & WHITESPACE_PROPERTY) != 0;
	}

	/**
	 * Gets whether or not all characters are whitespace characters.
	 *
	 * @param value the string to test for all whitespace characters
	 * @return true if all characters are whitespace characters, otherwise false
	 */
	public static boolean isWhitespace(final String value) {
		final int length = value.length();

		for (int index = 0; index < length; index++) {
			if (!isWhitespace(value.charAt(index))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the result of calling {@code toString} for a non-{@code null} argument and throws an exception if object is {@code null}. Used for constructing {@link StringBuilder}s or invoking {@link StringBuilder#append(String)}.
	 *
	 * @param object an {@code Object}
	 * @return the result of calling {@code toString} for a non-{@code null} argument
	 * @throws NullPointerException if the object is {@code null}
	 */
	public static String requireNonNullToString(final Object object) {
		if (object == null) {
			throw new NullPointerException("Invalid object cannot be concatenated: null");
		}
		return object.toString();
	}

	/**
	 * Gets the trimmed string from the specified starting index.
	 *
	 * @param value the string to trim
	 * @param start the starting index (inclusive) in the string
	 * @return the trimmed string
	 */
	public static TrimmedString trim(final String value, final int start) {
		for (int i = start; i < value.length(); i++) {
			if (!isWhitespace(value.charAt(i))) {
				int j;

				for (j = value.length(); isWhitespace(value.charAt(j - 1)); j--);

				return new TrimmedString(i, value.substring(i, j));
			}
		}

		return new TrimmedString(start, "");
	}

	/**
	 * Disables creation of the character sequence utils class.
	 */
	private Utilities() {
	}

}
