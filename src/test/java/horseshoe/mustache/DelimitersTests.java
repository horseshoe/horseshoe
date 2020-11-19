package horseshoe.mustache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import horseshoe.Helper;

import org.junit.jupiter.api.Test;

class DelimitersTests {

	@Test
	void test() throws horseshoe.LoadException, java.io.IOException {
		/* Pair Behavior - The equals sign (used on both sides) should permit delimiter changes. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("{{=<% %>=}}(<%text%>)", Helper.loadMap("text", "Hey!"), Helper.loadMap(), "(Hey!)"));

		/* Special Characters - Characters with special meaning regexen should be valid delimiters. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("({{=[ ]=}}[text])", Helper.loadMap("text", "It worked!"), Helper.loadMap(), "(It worked!)"));

		/* Sections - Delimiters set outside sections should persist. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("[\n{{#section}}\n  {{data}}\n  |data|\n{{/section}}\n\n{{= | | =}}\n|#section|\n  {{data}}\n  |data|\n|/section|\n]\n", Helper.loadMap("section", true, "data", "I got interpolated."), Helper.loadMap(), "[\n  I got interpolated.\n  |data|\n\n  {{data}}\n  I got interpolated.\n]\n"));

		/* Inverted Sections - Delimiters set outside inverted sections should persist. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("[\n{{^section}}\n  {{data}}\n  |data|\n{{/section}}\n\n{{= | | =}}\n|^section|\n  {{data}}\n  |data|\n|/section|\n]\n", Helper.loadMap("section", false, "data", "I got interpolated."), Helper.loadMap(), "[\n  I got interpolated.\n  |data|\n\n  {{data}}\n  I got interpolated.\n]\n"));

		/* Partial Inheritence - Delimiters set in a parent template should not affect a partial. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("[ {{>include}} ]\n{{= | | =}}\n[ |>include| ]\n", Helper.loadMap("value", "yes"), Helper.loadMap("include", ".{{value}}."), "[ .yes. ]\n[ .yes. ]\n"));

		/* Post-Partial Behavior - Delimiters set in a partial should not affect the parent template. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("[ {{>include}} ]\n[ .{{value}}.  .|value|. ]\n", Helper.loadMap("value", "yes"), Helper.loadMap("include", ".{{value}}. {{= | | =}} .|value|."), "[ .yes.  .yes. ]\n[ .yes.  .|value|. ]\n"));

		/* Surrounding Whitespace - Surrounding whitespace should be left untouched. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("| {{=@ @=}} |", Helper.loadMap(), Helper.loadMap(), "|  |"));

		/* Outlying Whitespace (Inline) - Whitespace should be left untouched. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest(" | {{=@ @=}}\n", Helper.loadMap(), Helper.loadMap(), " | \n"));

		/* Standalone Tag - Standalone lines should be removed from the template. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("Begin.\n{{=@ @=}}\nEnd.\n", Helper.loadMap(), Helper.loadMap(), "Begin.\nEnd.\n"));

		/* Indented Standalone Tag - Indented standalone lines should be removed from the template. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("Begin.\n  {{=@ @=}}\nEnd.\n", Helper.loadMap(), Helper.loadMap(), "Begin.\nEnd.\n"));

		/* Standalone Line Endings - "\r\n" should be considered a newline for standalone tags. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("|\r\n{{= @ @ =}}\r\n|", Helper.loadMap(), Helper.loadMap(), "|\r\n|"));

		/* Standalone Without Previous Line - Standalone tags should not require a newline to precede them. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("  {{=@ @=}}\n=", Helper.loadMap(), Helper.loadMap(), "="));

		/* Standalone Without Newline - Standalone tags should not require a newline to follow them. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("=\n  {{=@ @=}}", Helper.loadMap(), Helper.loadMap(), "=\n"));

		/* Pair with Padding - Superfluous in-tag whitespace should be ignored. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("|{{= @   @ =}}|", Helper.loadMap(), Helper.loadMap(), "||"));
	}

}
