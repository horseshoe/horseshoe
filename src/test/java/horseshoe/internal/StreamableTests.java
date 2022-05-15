package horseshoe.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class StreamableTests {

	private static <T> int count(final Streamable<T> streamable) {
		int i = 0;

		for (final Iterator<T> it = streamable.stream(); it.hasNext(); it.next()) {
			i++;
		}

		return i;
	}

	private static <T> Streamable<T> remap(final Streamable<T> streamable) {
		for (final Iterator<T> it = streamable.stream(); it.hasNext(); ) {
			streamable.add(it.next());
		}

		return streamable;
	}

	@Test
	void testArray() {
		assertEquals(1, count(remap(Streamable.ofUnknown(new String[] { "Test" }))));
	}

	@Test
	void testArray2() {
		assertEquals(2, count(remap(Streamable.of("Test", null))));
	}

	@Test
	void testArray3() {
		assertEquals(2, count(remap(Streamable.ofUnknown(new int[] { 0, 1 }))));
	}

	@Test
	void testBadIterator() {
		final Iterator<Object> it = remap(Streamable.of(Arrays.asList("Test", "Test 2", 5, (Object)null))).iterator();

		while (it.hasNext()) {
			it.next();
		}

		assertThrows(NoSuchElementException.class, () -> it.next());
	}

	@Test
	void testBadIterator2() {
		final Iterator<?> it = Streamable.ofUnknown("Test").iterator();

		it.next();
		assertThrows(NoSuchElementException.class, () -> it.next());
	}

	@Test
	void testCollection() {
		assertEquals(4, count(remap(Streamable.ofUnknown(Arrays.asList("Test", "Test 2", 5, (Object)null)))));
	}

	@Test
	void testIterable() {
		assertEquals(4, count(remap(Streamable.ofUnknown(new Iterable<Object>() {
			@Override
			public Iterator<Object> iterator() {
				return Arrays.asList("Test", "Test 2", 5, (Object)null).iterator();
			}
		}))));
	}

	@Test
	void testIterator() {
		assertEquals(4, count(remap(Streamable.ofUnknown(Arrays.asList("Test", "Test 2", 5, (Object)null).iterator()))));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testFlatMap() {
		final Streamable<Object> streamable = (Streamable<Object>)Streamable.ofUnknown(Arrays.asList(Arrays.asList(1, 2), new Object[] { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));

		streamable.stream().forEachRemaining(a -> {
			if (a instanceof Iterable) {
				streamable.flatAdd((Iterable<Object>)a);
			} else if (a instanceof Object[]) {
				streamable.flatAdd((Object[])a);
			} else {
				throw new RuntimeException("Value was not an iterable or array");
			}
		});

		final Object[] results = new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
		final Iterator<Object> it = streamable.stream();

		for (final Object result : results) {
			assertEquals(result, it.next());
		}

		assertFalse(it.hasNext());
	}

	@Test
	void testNull() {
		assertEquals(0, count(remap(Streamable.ofUnknown(null))));
	}

	@Test
	void testOptional() {
		assertEquals(1, count(remap(Streamable.ofUnknown(Optional.of("Test")))));
	}

	@Test
	void testOveradd() {
		final Streamable<Object> streamable = remap(Streamable.of(Arrays.asList("Test", "Test 2", 5, (Object)null)));

		for (final Iterator<Object> it = streamable.stream(); it.hasNext(); ) {
			streamable.add(it.next());
			streamable.add(0);
		}

		assertEquals(8, count(streamable));
	}

	@Test
	void testStream() {
		assertEquals(4, count(remap(Streamable.ofUnknown(Arrays.asList("Test", "Test 2", 5, (Object)null).stream()))));
	}

	@Test
	void testStreamable() {
		assertEquals(4, count(remap(Streamable.ofUnknown(Streamable.ofUnknown(Arrays.asList("Test", "Test 2", 5, (Object)null))))));
	}

	@Test
	void testString() {
		assertEquals(1, count(remap(Streamable.ofUnknown("Test"))));
	}

	@Test
	void testToString() {
		assertEquals("[ 1, 2, null ]", Streamable.of(Arrays.asList("1", 2, null)).toString());
		assertEquals("[ ]", Streamable.of(Arrays.asList()).toString());
		assertEquals("[ 1 ]", Streamable.of(Arrays.asList("1")).toString());
		assertEquals("[ Test ]", Streamable.of("Test").toString());
		assertEquals("[ ]", Streamable.of((Object)null).toString());
	}

}
