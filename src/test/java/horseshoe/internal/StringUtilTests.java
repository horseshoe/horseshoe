package horseshoe.internal;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilTests {

	@Test
	public void testTrim() {
		final String allWhitespaceString = " \t\n\r \u0085\u00A0\u202F"; // Escapes are space characters

		Assert.assertEquals("", StringUtils.trim(allWhitespaceString, 0, allWhitespaceString.length()));
		Assert.assertEquals("a", StringUtils.trim("\u202F\u202Fa\u202F\u202F", 0, 5)); // Escapes are space characters
	}

	private static void testNewLine(final String value, final int start, final int end) {
		final StringUtils.Range range = StringUtils.findNewLine(value, 0, value.length());

		Assert.assertEquals(start, range.start);
		Assert.assertEquals(end, range.end);
	}

	@Test
	public void testNewLine() {
		Assert.assertNull(StringUtils.findNewLine("aa", 0, 2));
		Assert.assertNull(StringUtils.findNewLine("a\u2027a", 0, 2)); // Escape is a non-newline character
		Assert.assertNull(StringUtils.findNewLine("a\u202Aa", 0, 2)); // Escape is a non-newline character

		testNewLine("a\na", 1, 2);
		testNewLine("a\n\na", 1, 2);
		testNewLine("a\n\ra", 1, 2);
		testNewLine("a\r\na", 1, 3);
		testNewLine("a\r", 1, 2);
		testNewLine("a\ra\n", 1, 2);
		testNewLine("a\u2028a", 1, 2); // Escape is a newline character
		testNewLine("a\u2029a", 1, 2); // Escape is a newline character
	}

}
