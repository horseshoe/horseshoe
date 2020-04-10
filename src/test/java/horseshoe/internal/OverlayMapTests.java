package horseshoe.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

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

		assertEquals("a", map.get("A"));
		assertEquals(1, map.get("b"));
		assertEquals(null, map.get("B"));
		assertEquals("a", map.get("A"));

		assertNotEquals(null, map.toString());
		assertNotEquals(null, map2.toString());

		map.put("a", 2);
		map.put("b", null);

		assertEquals(2, map.get("a"));
		assertEquals(2, map2.get("a"));
		assertEquals(0, innerMap.get("a"));
		assertEquals(null, map.get("b"));
		assertEquals(null, map2.get("b"));
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

		assertEquals(map1.hashCode(), map1Dup.hashCode());
		assertTrue(map1.equals(map1));
		assertTrue(map1.equals(map1Dup));
		assertFalse(map1.equals(null));
		assertFalse(map1.equals(new Object()));
		assertFalse(map1.equals(map2));
		assertFalse(map1.equals(map3));
		assertFalse(map1.equals(map4));
	}

}
