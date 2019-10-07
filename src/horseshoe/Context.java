package horseshoe;

import java.util.LinkedHashMap;
import java.util.Map;

public class Context {

	private Map<String, Object> map = new LinkedHashMap<>();

	public Context() {
	}

	public Context(String key, Object value) {
		put(key, value);
	}

	public Context put(String key, Object value) {
		map.put(key, value);
		return this;
	}

}
