package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class StackTests {

	@Test
	void testClear() {
		final Stack<Object> objects = new Stack<>();

		objects.push("Test");
		objects.clear();
		assertTrue(objects.isEmpty());
	}

	@Test
	void testIsEmpty() {
		final Stack<Object> objects = new Stack<>();

		assertTrue(objects.isEmpty());
		objects.push("Test");
		assertFalse(objects.isEmpty());
	}

	@Test
	void testIteratorNext() {
		final Stack<Object> objects = new Stack<>();

		objects.push("Test");

		final Iterator<Object> it = objects.iterator();

		while (it.hasNext()) {
			assertEquals("Test", it.next());
		}

		assertThrows(NoSuchElementException.class, () -> it.next());
	}

	@Test
	void testIteratorRemove() {
		final Stack<Object> objects = new Stack<>();

		objects.push("Test");

		for (final Iterator<Object> it = objects.iterator(); it.hasNext(); ) {
			assertEquals("Test", it.next());
			assertThrows(UnsupportedOperationException.class, () -> it.remove());
		}
	}

	@Test
	void testPeek() {
		final Stack<Object> objects = new Stack<>();

		assertNotNull(objects.toString());
		objects.push("Test");
		objects.pop();
		objects.push("Test");
		objects.push("Test2");
		assertEquals("Test2", objects.peek());
		assertEquals("Test2", objects.peek(0));
		assertEquals("Test", objects.peek(1));
		assertEquals("Test", objects.peekBase());
		assertNotNull(objects.toString());
	}

	@Test
	void testSize() {
		final Stack<Object> objects = new Stack<>();

		assertEquals(0, objects.size());
		assertTrue(objects.capacity() >= objects.size());
		objects.push("Test");
		assertEquals(1, objects.size());
		assertTrue(objects.capacity() >= objects.size());
		objects.push("Test2");
		assertEquals(2, objects.size());
		assertTrue(objects.capacity() >= objects.size());
	}

}
