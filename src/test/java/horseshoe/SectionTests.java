package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class SectionTests {

	@Test
	void testReiterator() {
		final SectionRenderer.Reiterable<Integer> reiterable = new SectionRenderer.Reiterable<>(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)).iterator());

		while (reiterable.hasNext()) {
			reiterable.next();
		}

		reiterable.remove();

		assertIterableEquals(Arrays.asList(1, 2, 3, 4), reiterable);
	}

	@Test
	void testToString() {
		assertEquals("\"Name\"", new Section(null, "Name", null, null, null, false).toString());
		assertEquals("\"Name\"", new Section(null, "Name", "Name", null, null, false).toString());
		assertEquals("\"Name\" (Location/Line)", new Section(null, "Name", "Location/Line", null, null, false).toString());
		assertEquals("Location/Line", new Section(null, "", "Location/Line", null, null, false).toString());
	}

}
