package horseshoe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AccessorsTests {

	private static Object invokeAccessor(final Object object, final String name) throws Throwable {
		final Accessor accessor = Accessors.get(object.getClass(), new Identifier(name));
		return accessor.get(object);
	}

	@Test
	void testEntries() throws Throwable {
		assertTrue(invokeAccessor(Collections.emptyMap(), "entries") instanceof Set);
		assertNull(Accessors.get(List.class, new Identifier("entries")));
	}

	@Test
	void testKey() throws Throwable {
		assertEquals("theKey", invokeAccessor(Collections.singletonMap("theKey", "theValue").entrySet().iterator().next(), "key"));
		assertNull(Accessors.get(List.class, new Identifier("key")));
	}

	@Test
	void testLength() throws Throwable {
		assertEquals(1, invokeAccessor(Collections.singletonMap("key", "value"), "length"));
		assertEquals(1, invokeAccessor(Collections.singletonList(5), "length"));
		assertEquals(1, invokeAccessor("5", "length"));
		assertEquals(1, invokeAccessor(new int[] { 5 }, "length"));
		assertNull(Accessors.get(Object.class, new Identifier("length")));
	}

	@Test
	void testSize() throws Throwable {
		assertEquals(0, invokeAccessor(Collections.emptyMap(), "size"));
		assertEquals(0, invokeAccessor(Collections.emptyList(), "size"));
		assertEquals(0, invokeAccessor("", "size"));
		assertEquals(0, invokeAccessor(new byte[0], "size"));
		assertNull(Accessors.get(Object.class, new Identifier("size")));
	}

	@Test
	void testValue() throws Throwable {
		assertEquals("theValue", invokeAccessor(Collections.singletonMap("theKey", "theValue").entrySet().iterator().next(), "value"));
		assertNull(Accessors.get(List.class, new Identifier("value")));
	}

}
