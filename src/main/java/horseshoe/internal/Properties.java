package horseshoe.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Properties {

	private static final Matcher JAVA_VERSION_MATCHER = Pattern.compile("(?:1[.])?(?<majorDotMinor>[0-9]+[.]?[0-9]*)").matcher(System.getProperty("java.version"));

	/**
	 * The version of Java being run in the form MAJOR.MINOR (e.g. 7.0 for Java 7).
	 */
	public static final double JAVA_VERSION = JAVA_VERSION_MATCHER.find() ? Double.parseDouble(JAVA_VERSION_MATCHER.group("majorDotMinor")) : 7.0;

	private Properties() {
	}

}
