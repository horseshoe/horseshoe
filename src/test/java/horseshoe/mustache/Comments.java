package horseshoe.mustache;

import org.junit.Test;

import horseshoe.Helper;

public class Comments {

	@Test
	public void test() throws horseshoe.LoadException, java.io.IOException {
		/* Inline - Comment blocks should be removed from the template. */
		Helper.executeMustacheTest("12345{{! Comment Block! }}67890", Helper.loadMap(), Helper.loadMap(), "1234567890");

		/* Multiline - Multiline comments should be permitted. */
		Helper.executeMustacheTest("12345{{!\n  This is a\n  multi-line comment...\n}}67890\n", Helper.loadMap(), Helper.loadMap(), "1234567890\n");

		/* Standalone - All standalone comment lines should be removed. */
		Helper.executeMustacheTest("Begin.\n{{! Comment Block! }}\nEnd.\n", Helper.loadMap(), Helper.loadMap(), "Begin.\nEnd.\n");

		/* Indented Standalone - All standalone comment lines should be removed. */
		Helper.executeMustacheTest("Begin.\n  {{! Indented Comment Block! }}\nEnd.\n", Helper.loadMap(), Helper.loadMap(), "Begin.\nEnd.\n");

		/* Standalone Line Endings - "\r\n" should be considered a newline for standalone tags. */
		Helper.executeMustacheTest("|\r\n{{! Standalone Comment }}\r\n|", Helper.loadMap(), Helper.loadMap(), "|\r\n|");

		/* Standalone Without Previous Line - Standalone tags should not require a newline to precede them. */
		Helper.executeMustacheTest("  {{! I'm Still Standalone }}\n!", Helper.loadMap(), Helper.loadMap(), "!");

		/* Standalone Without Newline - Standalone tags should not require a newline to follow them. */
		Helper.executeMustacheTest("!\n  {{! I'm Still Standalone }}", Helper.loadMap(), Helper.loadMap(), "!\n");

		/* Multiline Standalone - All standalone comment lines should be removed. */
		Helper.executeMustacheTest("Begin.\n{{!\nSomething's going on here...\n}}\nEnd.\n", Helper.loadMap(), Helper.loadMap(), "Begin.\nEnd.\n");

		/* Indented Multiline Standalone - All standalone comment lines should be removed. */
		Helper.executeMustacheTest("Begin.\n  {{!\n    Something's going on here...\n  }}\nEnd.\n", Helper.loadMap(), Helper.loadMap(), "Begin.\nEnd.\n");

		/* Indented Inline - Inline comments should not strip whitespace */
		Helper.executeMustacheTest("  12 {{! 34 }}\n", Helper.loadMap(), Helper.loadMap(), "  12 \n");

		/* Surrounding Whitespace - Comment removal should preserve surrounding whitespace. */
		Helper.executeMustacheTest("12345 {{! Comment Block! }} 67890", Helper.loadMap(), Helper.loadMap(), "12345  67890");
	}

}
