package horseshoe;

import org.junit.Assert;
import org.junit.Test;

public class SectionTests {

	@Test
	public void testToString() {
		Assert.assertEquals("\"Name\"", new Section(null, "Name", null, null, null, false).toString());
		Assert.assertEquals("\"Name\"", new Section(null, "Name", "Name", null, null, false).toString());
		Assert.assertEquals("\"Name\" (Location/Line)", new Section(null, "Name", "Location/Line", null, null, false).toString());
		Assert.assertEquals("Location/Line", new Section(null, "", "Location/Line", null, null, false).toString());
	}

}
