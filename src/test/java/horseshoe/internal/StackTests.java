package horseshoe.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;

public class StackTests {

	@Test
	public void testClear() {
		final Stack<Object> objects = new Stack<>();

		objects.push("Test");
		objects.clear();
		assertTrue(objects.isEmpty());
	}

	@Test
	public void testIsEmpty() {
		final Stack<Object> objects = new Stack<>();

		assertTrue(objects.isEmpty());
		objects.push("Test");
		assertFalse(objects.isEmpty());
	}

	@Test (expected = NoSuchElementException.class)
	public void testIteratorNext() {
		final Stack<Object> objects = new Stack<>();

		objects.push("Test");

		final Iterator<Object> it = objects.iterator();

		while (it.hasNext()) {
			assertEquals("Test", it.next());
		}

		assertEquals("Test", it.next());
	}

	@Test (expected = UnsupportedOperationException.class)
	public void testIteratorRemove() {
		final Stack<Object> objects = new Stack<>();

		objects.push("Test");

		for (final Iterator<Object> it = objects.iterator(); it.hasNext(); ) {
			assertEquals("Test", it.next());
			it.remove();
		}
	}

	@Test
	public void testPeek() {
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
	public void testSize() {
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
