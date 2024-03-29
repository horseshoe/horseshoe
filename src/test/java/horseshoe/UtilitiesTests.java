package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class UtilitiesTests {

	@Test
	void testTrim() {
		final String allWhitespaceString = " \t\n\r \u0085\u00A0\u202F"; // Escapes are space characters

		assertEquals("", Utilities.trim(allWhitespaceString, 0).string);
		assertEquals("a", Utilities.trim("\u202F\u202Fa\u202F\u202F", 0).string); // Escapes are space characters
	}

	private static void testNewLine(final String value, final int start, final int end) {
		final Utilities.Range range = Utilities.findNewLine(value, 0, value.length());

		assertEquals(start, range.start);
		assertEquals(end, range.end);
	}

	@Test
	void testNewLine() {
		assertNull(Utilities.findNewLine("aa", 0, 2));
		assertNull(Utilities.findNewLine("a\u2027a", 0, 2)); // Escape is a non-newline character
		assertNull(Utilities.findNewLine("a\u202Aa", 0, 2)); // Escape is a non-newline character

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
