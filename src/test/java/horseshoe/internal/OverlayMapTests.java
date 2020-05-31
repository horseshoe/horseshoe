package horseshoe.internal;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class OverlayMapTests {

	@Test
	public void testAccess() {
		final Map<String, Object> innerMap = new HashMap<>();

		innerMap.put("a", 0);
		innerMap.put("b", 1);
		innerMap.put("A", "a");

		final OverlayMap<String, Object> map = new OverlayMap<>(innerMap);
		final OverlayMap<String, Object> map2 = new OverlayMap<>(map);

		Assert.assertEquals("a", map.get("A"));
		Assert.assertEquals(1, map.get("b"));
		Assert.assertEquals(null, map.get("B"));
		Assert.assertEquals("a", map.get("A"));

		Assert.assertNotEquals(null, map.toString());
		Assert.assertNotEquals(null, map2.toString());

		map.put("a", 2);
		map.put("b", null);

		Assert.assertEquals(2, map.get("a"));
		Assert.assertEquals(2, map2.get("a"));
		Assert.assertEquals(0, innerMap.get("a"));
		Assert.assertEquals(null, map.get("b"));
		Assert.assertEquals(null, map2.get("b"));
	}

	@Test
	public void testEquals() {
		final Map<String, Object> innerMap = new HashMap<>();

		innerMap.put("a", 0);

		final Map<String, Object> innerMap2 = new HashMap<>();

		innerMap2.put("a", 1);

		final OverlayMap<String, Object> map1 = new OverlayMap<>(innerMap);
		final OverlayMap<String, Object> map1Dup = new OverlayMap<>(innerMap);
		final OverlayMap<String, Object> map2 = new OverlayMap<>(innerMap);
		final OverlayMap<String, Object> map3 = new OverlayMap<>(map1);
		final OverlayMap<String, Object> map4 = new OverlayMap<>(innerMap2);

		map2.put("a", 2);

		Assert.assertEquals(map1.hashCode(), map1Dup.hashCode());
		Assert.assertEquals(map1, map1);
		Assert.assertEquals(map1, map1Dup);

		if (map1.equals(null) || map1.equals(new Object()) || map1.equals(map2) || map1.equals(map3) || map1.equals(map4)) {
			Assert.fail("OverlayMap equals not implemented properly.");
		}
	}

}
