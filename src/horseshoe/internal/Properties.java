package horseshoe.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Properties {

	private static final Matcher JAVA_VERSION_MATCHER = Pattern.compile("(1\\.)?([0-9]+\\.?[0-9]*)").matcher(System.getProperty("java.version"));

	static {
		JAVA_VERSION_MATCHER.find();
	}

	public static final double JAVA_VERSION = Double.parseDouble(JAVA_VERSION_MATCHER.group(2));

}
