package horseshoe.mustache;

import horseshoe.Helper;

import org.junit.Test;

public class Inverted {

	@Test
	public void test() throws horseshoe.LoadException, java.io.IOException {
		/* Falsey - Falsey sections should have their contents rendered. */
		Helper.executeMustacheTest("\"{{^boolean}}This should be rendered.{{/boolean}}\"", Helper.loadMap("boolean", false), Helper.loadMap(), "\"This should be rendered.\"");

		/* Truthy - Truthy sections should have their contents omitted. */
		Helper.executeMustacheTest("\"{{^boolean}}This should not be rendered.{{/boolean}}\"", Helper.loadMap("boolean", true), Helper.loadMap(), "\"\"");

		/* Context - Objects and hashes should behave like truthy values. */
		Helper.executeMustacheTest("\"{{^context}}Hi {{name}}.{{/context}}\"", Helper.loadMap("context", Helper.loadMap("name", "Joe")), Helper.loadMap(), "\"\"");

		/* List - Lists should behave like truthy values. */
		Helper.executeMustacheTest("\"{{^list}}{{n}}{{/list}}\"", Helper.loadMap("list", Helper.loadList(Helper.loadMap("n", "1"), Helper.loadMap("n", "2"), Helper.loadMap("n", "3"))), Helper.loadMap(), "\"\"");

		/* Empty List - Empty lists should behave like falsey values. */
		Helper.executeMustacheTest("\"{{^list}}Yay lists!{{/list}}\"", Helper.loadMap("list", Helper.loadList()), Helper.loadMap(), "\"Yay lists!\"");

		/* Doubled - Multiple inverted sections per template should be permitted. */
		Helper.executeMustacheTest("{{^bool}}\n* first\n{{/bool}}\n* {{two}}\n{{^bool}}\n* third\n{{/bool}}\n", Helper.loadMap("bool", false, "two", "second"), Helper.loadMap(), "* first\n* second\n* third\n");

		/* Nested (Falsey) - Nested falsey sections should have their contents rendered. */
		Helper.executeMustacheTest("| A {{^bool}}B {{^bool}}C{{/bool}} D{{/bool}} E |", Helper.loadMap("bool", false), Helper.loadMap(), "| A B C D E |");

		/* Nested (Truthy) - Nested truthy sections should be omitted. */
		Helper.executeMustacheTest("| A {{^bool}}B {{^bool}}C{{/bool}} D{{/bool}} E |", Helper.loadMap("bool", true), Helper.loadMap(), "| A  E |");

		/* Context Misses - Failed context lookups should be considered falsey. */
		Helper.executeMustacheTest("[{{^missing}}Cannot find key 'missing'!{{/missing}}]", Helper.loadMap(), Helper.loadMap(), "[Cannot find key 'missing'!]");

		/* Dotted Names - Truthy - Dotted names should be valid for Inverted Section tags. */
		Helper.executeMustacheTest("\"{{^a.b.c}}Not Here{{/a.b.c}}\" == \"\"", Helper.loadMap("a", Helper.loadMap("b", Helper.loadMap("c", true))), Helper.loadMap(), "\"\" == \"\"");

		/* Dotted Names - Falsey - Dotted names should be valid for Inverted Section tags. */
		Helper.executeMustacheTest("\"{{^a.b.c}}Not Here{{/a.b.c}}\" == \"Not Here\"", Helper.loadMap("a", Helper.loadMap("b", Helper.loadMap("c", false))), Helper.loadMap(), "\"Not Here\" == \"Not Here\"");

		/* Dotted Names - Broken Chains - Dotted names that cannot be resolved should be considered falsey. */
		Helper.executeMustacheTest("\"{{^a.b.c}}Not Here{{/a.b.c}}\" == \"Not Here\"", Helper.loadMap("a", Helper.loadMap()), Helper.loadMap(), "\"Not Here\" == \"Not Here\"");

		/* Surrounding Whitespace - Inverted sections should not alter surrounding whitespace. */
		Helper.executeMustacheTest(" | {{^boolean}}\t|\t{{/boolean}} | \n", Helper.loadMap("boolean", false), Helper.loadMap(), " | \t|\t | \n");

		/* Internal Whitespace - Inverted should not alter internal whitespace. */
		Helper.executeMustacheTest(" | {{^boolean}} {{! Important Whitespace }}\n {{/boolean}} | \n", Helper.loadMap("boolean", false), Helper.loadMap(), " |  \n  | \n");

		/* Indented Inline Sections - Single-line sections should not alter surrounding whitespace. */
		Helper.executeMustacheTest(" {{^boolean}}NO{{/boolean}}\n {{^boolean}}WAY{{/boolean}}\n", Helper.loadMap("boolean", false), Helper.loadMap(), " NO\n WAY\n");

		/* Standalone Lines - Standalone lines should be removed from the template. */
		Helper.executeMustacheTest("| This Is\n{{^boolean}}\n|\n{{/boolean}}\n| A Line\n", Helper.loadMap("boolean", false), Helper.loadMap(), "| This Is\n|\n| A Line\n");

		/* Standalone Indented Lines - Standalone indented lines should be removed from the template. */
		Helper.executeMustacheTest("| This Is\n  {{^boolean}}\n|\n  {{/boolean}}\n| A Line\n", Helper.loadMap("boolean", false), Helper.loadMap(), "| This Is\n|\n| A Line\n");

		/* Standalone Line Endings - "\r\n" should be considered a newline for standalone tags. */
		Helper.executeMustacheTest("|\r\n{{^boolean}}\r\n{{/boolean}}\r\n|", Helper.loadMap("boolean", false), Helper.loadMap(), "|\r\n|");

		/* Standalone Without Previous Line - Standalone tags should not require a newline to precede them. */
		Helper.executeMustacheTest("  {{^boolean}}\n^{{/boolean}}\n/", Helper.loadMap("boolean", false), Helper.loadMap(), "^\n/");

		/* Standalone Without Newline - Standalone tags should not require a newline to follow them. */
		Helper.executeMustacheTest("^{{^boolean}}\n/\n  {{/boolean}}", Helper.loadMap("boolean", false), Helper.loadMap(), "^\n/\n");

		/* Padding - Superfluous in-tag whitespace should be ignored. */
		Helper.executeMustacheTest("|{{^ boolean }}={{/ boolean }}|", Helper.loadMap("boolean", false), Helper.loadMap(), "|=|");
	}

}
