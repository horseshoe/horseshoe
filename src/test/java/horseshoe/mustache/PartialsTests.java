package horseshoe.mustache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import horseshoe.Helper;

import org.junit.jupiter.api.Test;

class PartialsTests {

	@Test
	void test() throws horseshoe.LoadException, java.io.IOException {
		/* Basic Behavior - The greater-than operator should expand to the named partial. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{>text}}\"", Helper.loadMap(), Helper.loadMap("text", "from partial"), "\"from partial\""));

		/* Failed Lookup - The empty string should be used when the named partial is not found. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{>text}}\"", Helper.loadMap(), Helper.loadMap(), "\"\""));

		/* Context - The greater-than operator should operate within the current context. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{>partial}}\"", Helper.loadMap("text", "content"), Helper.loadMap("partial", "*{{text}}*"), "\"*content*\""));

		/* Recursion - The greater-than operator should properly recurse. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("{{>node}}", Helper.loadMap("content", "X", "nodes", Helper.loadList(Helper.loadMap("content", "Y", "nodes", Helper.loadList()))), Helper.loadMap("node", "{{content}}<{{#nodes}}{{>node}}{{/nodes}}>"), "X<Y<>>"));

		/* Surrounding Whitespace - The greater-than operator should not alter surrounding whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("| {{>partial}} |", Helper.loadMap(), Helper.loadMap("partial", "\t|\t"), "| \t|\t |"));

		/* Inline Indentation - Whitespace should be left untouched. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("  {{data}}  {{> partial}}\n", Helper.loadMap("data", "|"), Helper.loadMap("partial", ">\n>"), "  |  >\n>\n"));

		/* Standalone Line Endings - "\r\n" should be considered a newline for standalone tags. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("|\r\n{{>partial}}\r\n|", Helper.loadMap(), Helper.loadMap("partial", ">"), "|\r\n>|"));

		/* Standalone Without Previous Line - Standalone tags should not require a newline to precede them. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("  {{>partial}}\n>", Helper.loadMap(), Helper.loadMap("partial", ">\n>"), "  >\n  >>"));

		/* Standalone Without Newline - Standalone tags should not require a newline to follow them. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest(">\n  {{>partial}}", Helper.loadMap(), Helper.loadMap("partial", ">\n>"), ">\n  >\n  >"));

		/* Standalone Indentation - Each line of the partial should be indented before rendering. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\\\n {{>partial}}\n/\n", Helper.loadMap("content", "<\n->"), Helper.loadMap("partial", "|\n{{{content}}}\n|\n"), "\\\n |\n <\n->\n |\n/\n"));

		/* Padding Whitespace - Superfluous in-tag whitespace should be ignored. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("|{{> partial }}|", Helper.loadMap("boolean", true), Helper.loadMap("partial", "[]"), "|[]|"));
	}

}
