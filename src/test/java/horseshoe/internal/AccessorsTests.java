package horseshoe.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class AccessorsTests {

	private static Object invokeAccessor(final Object object, final String name) throws Throwable {
		final Accessor accessor = Accessors.get(object.getClass(), new Identifier(name));
		return accessor.get(object);
	}

	@Test
	public void testEntries() throws Throwable {
		assertTrue(invokeAccessor(Collections.emptyMap(), "entries") instanceof Set);
		assertNull(Accessors.get(List.class, new Identifier("entries")));
	}

	@Test
	public void testKey() throws Throwable {
		assertEquals("theKey", invokeAccessor(Collections.singletonMap("theKey", "theValue").entrySet().iterator().next(), "key"));
		assertNull(Accessors.get(List.class, new Identifier("key")));
	}

	@Test
	public void testLength() throws Throwable {
		assertEquals(1, invokeAccessor(Collections.singletonMap("key", "value"), "length"));
		assertEquals(1, invokeAccessor(Collections.singletonList(5), "length"));
		assertEquals(1, invokeAccessor("5", "length"));
		assertEquals(1, invokeAccessor(new int[] { 5 }, "length"));
		assertNull(Accessors.get(Object.class, new Identifier("length")));
	}

	@Test
	public void testSize() throws Throwable {
		assertEquals(0, invokeAccessor(Collections.emptyMap(), "size"));
		assertEquals(0, invokeAccessor(Collections.emptyList(), "size"));
		assertEquals(0, invokeAccessor("", "size"));
		assertEquals(0, invokeAccessor(new byte[0], "size"));
		assertNull(Accessors.get(Object.class, new Identifier("size")));
	}

	@Test
	public void testValue() throws Throwable {
		assertEquals("theValue", invokeAccessor(Collections.singletonMap("theKey", "theValue").entrySet().iterator().next(), "value"));
		assertNull(Accessors.get(List.class, new Identifier("value")));
	}

}
