package horseshoe.mustache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import horseshoe.Helper;

import org.junit.jupiter.api.Test;

class SectionsTests {

	@Test
	void test() throws horseshoe.LoadException, java.io.IOException {
		/* Truthy - Truthy sections should have their contents rendered. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#boolean}}This should be rendered.{{/boolean}}\"", Helper.loadMap("boolean", true), Helper.loadMap(), "\"This should be rendered.\""));

		/* Falsey - Falsey sections should have their contents omitted. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#boolean}}This should not be rendered.{{/boolean}}\"", Helper.loadMap("boolean", false), Helper.loadMap(), "\"\""));

		/* Context - Objects and hashes should be pushed onto the context stack. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#context}}Hi {{name}}.{{/context}}\"", Helper.loadMap("context", Helper.loadMap("name", "Joe")), Helper.loadMap(), "\"Hi Joe.\""));

		/* Deeply Nested Contexts - All elements on the context stack should be accessible. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("{{#a}}\n{{one}}\n{{#b}}\n{{one}}{{two}}{{one}}\n{{#c}}\n{{one}}{{two}}{{three}}{{two}}{{one}}\n{{#d}}\n{{one}}{{two}}{{three}}{{four}}{{three}}{{two}}{{one}}\n{{#e}}\n{{one}}{{two}}{{three}}{{four}}{{five}}{{four}}{{three}}{{two}}{{one}}\n{{/e}}\n{{one}}{{two}}{{three}}{{four}}{{three}}{{two}}{{one}}\n{{/d}}\n{{one}}{{two}}{{three}}{{two}}{{one}}\n{{/c}}\n{{one}}{{two}}{{one}}\n{{/b}}\n{{one}}\n{{/a}}\n", Helper.loadMap("a", Helper.loadMap("one", "1"), "b", Helper.loadMap("two", "2"), "c", Helper.loadMap("three", "3"), "d", Helper.loadMap("four", "4"), "e", Helper.loadMap("five", "5")), Helper.loadMap(), "1\n121\n12321\n1234321\n123454321\n1234321\n12321\n121\n1\n"));

		/* List - Lists should be iterated; list items should visit the context stack. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#list}}{{item}}{{/list}}\"", Helper.loadMap("list", Helper.loadList(Helper.loadMap("item", "1"), Helper.loadMap("item", "2"), Helper.loadMap("item", "3"))), Helper.loadMap(), "\"123\""));

		/* Empty List - Empty lists should behave like falsey values. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#list}}Yay lists!{{/list}}\"", Helper.loadMap("list", Helper.loadList()), Helper.loadMap(), "\"\""));

		/* Doubled - Multiple sections per template should be permitted. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("{{#bool}}\n* first\n{{/bool}}\n* {{two}}\n{{#bool}}\n* third\n{{/bool}}\n", Helper.loadMap("bool", true, "two", "second"), Helper.loadMap(), "* first\n* second\n* third\n"));

		/* Nested (Truthy) - Nested truthy sections should have their contents rendered. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("| A {{#bool}}B {{#bool}}C{{/bool}} D{{/bool}} E |", Helper.loadMap("bool", true), Helper.loadMap(), "| A B C D E |"));

		/* Nested (Falsey) - Nested falsey sections should be omitted. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("| A {{#bool}}B {{#bool}}C{{/bool}} D{{/bool}} E |", Helper.loadMap("bool", false), Helper.loadMap(), "| A  E |"));

		/* Context Misses - Failed context lookups should be considered falsey. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("[{{#missing}}Found key 'missing'!{{/missing}}]", Helper.loadMap(), Helper.loadMap(), "[]"));

		/* Implicit Iterator - String - Implicit iterators should directly interpolate strings. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#list}}({{.}}){{/list}}\"", Helper.loadMap("list", Helper.loadList("a", "b", "c", "d", "e")), Helper.loadMap(), "\"(a)(b)(c)(d)(e)\""));

		/* Implicit Iterator - Integer - Implicit iterators should cast integers to strings and interpolate. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#list}}({{.}}){{/list}}\"", Helper.loadMap("list", Helper.loadList("1", "2", "3", "4", "5")), Helper.loadMap(), "\"(1)(2)(3)(4)(5)\""));

		/* Implicit Iterator - Decimal - Implicit iterators should cast decimals to strings and interpolate. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#list}}({{.}}){{/list}}\"", Helper.loadMap("list", Helper.loadList("1.1", "2.2", "3.3", "4.4", "5.5")), Helper.loadMap(), "\"(1.1)(2.2)(3.3)(4.4)(5.5)\""));

		/* Implicit Iterator - Array - Implicit iterators should allow iterating over nested arrays. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#list}}({{#.}}{{.}}{{/.}}){{/list}}\"", Helper.loadMap("list", Helper.loadList(Helper.loadList("1", "2", "3"), Helper.loadList("a", "b", "c"))), Helper.loadMap(), "\"(123)(abc)\""));

		/* Dotted Names - Truthy - Dotted names should be valid for Section tags. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#a.b.c}}Here{{/a.b.c}}\" == \"Here\"", Helper.loadMap("a", Helper.loadMap("b", Helper.loadMap("c", true))), Helper.loadMap(), "\"Here\" == \"Here\""));

		/* Dotted Names - Falsey - Dotted names should be valid for Section tags. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#a.b.c}}Here{{/a.b.c}}\" == \"\"", Helper.loadMap("a", Helper.loadMap("b", Helper.loadMap("c", false))), Helper.loadMap(), "\"\" == \"\""));

		/* Dotted Names - Broken Chains - Dotted names that cannot be resolved should be considered falsey. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#a.b.c}}Here{{/a.b.c}}\" == \"\"", Helper.loadMap("a", Helper.loadMap()), Helper.loadMap(), "\"\" == \"\""));

		/* Surrounding Whitespace - Sections should not alter surrounding whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest(" | {{#boolean}}\t|\t{{/boolean}} | \n", Helper.loadMap("boolean", true), Helper.loadMap(), " | \t|\t | \n"));

		/* Internal Whitespace - Sections should not alter internal whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest(" | {{#boolean}} {{! Important Whitespace }}\n {{/boolean}} | \n", Helper.loadMap("boolean", true), Helper.loadMap(), " |  \n  | \n"));

		/* Indented Inline Sections - Single-line sections should not alter surrounding whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest(" {{#boolean}}YES{{/boolean}}\n {{#boolean}}GOOD{{/boolean}}\n", Helper.loadMap("boolean", true), Helper.loadMap(), " YES\n GOOD\n"));

		/* Standalone Lines - Standalone lines should be removed from the template. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("| This Is\n{{#boolean}}\n|\n{{/boolean}}\n| A Line\n", Helper.loadMap("boolean", true), Helper.loadMap(), "| This Is\n|\n| A Line\n"));

		/* Indented Standalone Lines - Indented standalone lines should be removed from the template. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("| This Is\n  {{#boolean}}\n|\n  {{/boolean}}\n| A Line\n", Helper.loadMap("boolean", true), Helper.loadMap(), "| This Is\n|\n| A Line\n"));

		/* Standalone Line Endings - "\r\n" should be considered a newline for standalone tags. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("|\r\n{{#boolean}}\r\n{{/boolean}}\r\n|", Helper.loadMap("boolean", true), Helper.loadMap(), "|\r\n|"));

		/* Standalone Without Previous Line - Standalone tags should not require a newline to precede them. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("  {{#boolean}}\n#{{/boolean}}\n/", Helper.loadMap("boolean", true), Helper.loadMap(), "#\n/"));

		/* Standalone Without Newline - Standalone tags should not require a newline to follow them. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("#{{#boolean}}\n/\n  {{/boolean}}", Helper.loadMap("boolean", true), Helper.loadMap(), "#\n/\n"));

		/* Padding - Superfluous in-tag whitespace should be ignored. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("|{{# boolean }}={{/ boolean }}|", Helper.loadMap("boolean", true), Helper.loadMap(), "|=|"));
	}

}
