package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SectionTests {

	@Test
	void testToString() {
		assertEquals("\"Name\"", new Section(null, "Name", null, null, null, false).toString());
		assertEquals("\"Name\"", new Section(null, "Name", "Name", null, null, false).toString());
		assertEquals("\"Name\" (Location/Line)", new Section(null, "Name", "Location/Line", null, null, false).toString());
		assertEquals("Location/Line", new Section(null, "", "Location/Line", null, null, false).toString());
	}

}
