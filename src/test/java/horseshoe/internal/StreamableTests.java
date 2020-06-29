package horseshoe.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Test;

public class StreamableTests {

	private static <T> int count(final Streamable<T> streamable) {
		int i = 0;

		for (final Iterator<T> it = streamable.iterator(); it.hasNext(); it.next()) {
			i++;
		}

		return i;
	}

	private static <T> Streamable<T> remap(final Streamable<T> streamable) {
		for (final Iterator<T> it = streamable.iterator(); it.hasNext(); ) {
			streamable.add(it.next());
		}

		return streamable;
	}

	@Test
	public void testArray() {
		assertEquals(1, count(remap(Streamable.ofUnknown(new String[] { "Test" }))));
	}

	@Test
	public void testArray2() {
		assertEquals(2, count(remap(Streamable.of("Test", null))));
	}

	@Test
	public void testArray3() {
		assertEquals(2, count(remap(Streamable.ofUnknown(new int[] { 0, 1 }))));
	}

	@Test(expected = NoSuchElementException.class)
	public void testBadIterator() {
		final Iterator<Object> it = remap(Streamable.of(Arrays.asList("Test", "Test 2", 5, (Object)null))).iterator();

		while (it.hasNext()) {
			it.next();
		}

		it.next();
	}

	@Test(expected = NoSuchElementException.class)
	public void testBadIterator2() {
		final Iterator<?> it = Streamable.ofUnknown("Test").iterator();

		it.next();
		it.remove();
		it.next();
	}

	@Test
	public void testCollection() {
		assertEquals(4, count(remap(Streamable.ofUnknown(Arrays.asList("Test", "Test 2", 5, (Object)null)))));
	}

	@Test
	public void testIterable() {
		assertEquals(4, count(remap(Streamable.ofUnknown(new Iterable<Object>() {
			@Override
			public Iterator<Object> iterator() {
				return Arrays.asList("Test", "Test 2", 5, (Object)null).iterator();
			}
		}))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFlatMap() {
		final Streamable<Object> streamable = (Streamable<Object>)Streamable.ofUnknown(Arrays.asList(Arrays.asList(1, 2), new Object[] { 3, 4 }));

		streamable.forEach(a -> {
			if (a instanceof Iterable) {
				streamable.flatAdd((Iterable<Object>)a);
			} else if (a instanceof Object[]) {
				streamable.flatAdd((Object[])a);
			} else {
				throw new RuntimeException("Value was not an iterable or array");
			}
		});

		final Object[] results = new Object[] { 1, 2, 3, 4 };
		final Iterator<Object> it = streamable.iterator();

		for (final Object result : results) {
			assertEquals(result, it.next());
		}

		assertFalse(it.hasNext());
	}

	@Test
	public void testNull() {
		assertEquals(0, count(remap(Streamable.ofUnknown(null))));
	}

	@Test
	public void testOptional() {
		assertEquals(1, count(remap(Streamable.ofUnknown(Optional.of("Test")))));
	}

	@Test
	public void testOveradd() {
		final Streamable<Object> streamable = remap(Streamable.of(Arrays.asList("Test", "Test 2", 5, (Object)null)));

		for (final Iterator<Object> it = streamable.iterator(); it.hasNext(); it.remove()) {
			streamable.add(it.next());
			streamable.add(0);
		}

		assertEquals(8, count(streamable));
	}

	@Test
	public void testStream() {
		assertEquals(4, count(remap(Streamable.ofUnknown(Arrays.asList("Test", "Test 2", 5, (Object)null).stream()))));
	}

	@Test
	public void testStreamable() {
		assertEquals(4, count(remap(Streamable.ofUnknown(Streamable.ofUnknown(Arrays.asList("Test", "Test 2", 5, (Object)null))))));
	}

	@Test
	public void testString() {
		assertEquals(1, count(remap(Streamable.ofUnknown("Test"))));
	}

}
