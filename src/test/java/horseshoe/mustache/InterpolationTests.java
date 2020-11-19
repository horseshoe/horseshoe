package horseshoe.mustache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import horseshoe.Helper;

import org.junit.jupiter.api.Test;

class InterpolationTests {

	@Test
	void test() throws horseshoe.LoadException, java.io.IOException {
		/* No Interpolation - Mustache-free templates should render as-is. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("Hello from {Mustache}!\n", Helper.loadMap(), Helper.loadMap(), "Hello from {Mustache}!\n"));

		/* Basic Interpolation - Unadorned tags should interpolate content into the template. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("Hello, {{subject}}!\n", Helper.loadMap("subject", "world"), Helper.loadMap(), "Hello, world!\n"));

		/* HTML Escaping - Basic interpolation should be HTML escaped. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("These characters should be HTML escaped: {{forbidden}}\n", Helper.loadMap("forbidden", "& \" < >"), Helper.loadMap(), "These characters should be HTML escaped: &amp; &quot; &lt; &gt;\n"));

		/* Triple Mustache - Triple mustaches should interpolate without HTML escaping. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("These characters should not be HTML escaped: {{{forbidden}}}\n", Helper.loadMap("forbidden", "& \" < >"), Helper.loadMap(), "These characters should not be HTML escaped: & \" < >\n"));

		/* Ampersand - Ampersand should interpolate without HTML escaping. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("These characters should not be HTML escaped: {{&forbidden}}\n", Helper.loadMap("forbidden", "& \" < >"), Helper.loadMap(), "These characters should not be HTML escaped: & \" < >\n"));

		/* Basic Integer Interpolation - Integers should interpolate seamlessly. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{mph}} miles an hour!\"", Helper.loadMap("mph", "85"), Helper.loadMap(), "\"85 miles an hour!\""));

		/* Triple Mustache Integer Interpolation - Integers should interpolate seamlessly. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{{mph}}} miles an hour!\"", Helper.loadMap("mph", "85"), Helper.loadMap(), "\"85 miles an hour!\""));

		/* Ampersand Integer Interpolation - Integers should interpolate seamlessly. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{&mph}} miles an hour!\"", Helper.loadMap("mph", "85"), Helper.loadMap(), "\"85 miles an hour!\""));

		/* Basic Decimal Interpolation - Decimals should interpolate seamlessly with proper significance. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{power}} jiggawatts!\"", Helper.loadMap("power", "1.21"), Helper.loadMap(), "\"1.21 jiggawatts!\""));

		/* Triple Mustache Decimal Interpolation - Decimals should interpolate seamlessly with proper significance. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{{power}}} jiggawatts!\"", Helper.loadMap("power", "1.21"), Helper.loadMap(), "\"1.21 jiggawatts!\""));

		/* Ampersand Decimal Interpolation - Decimals should interpolate seamlessly with proper significance. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{&power}} jiggawatts!\"", Helper.loadMap("power", "1.21"), Helper.loadMap(), "\"1.21 jiggawatts!\""));

		/* Basic Context Miss Interpolation - Failed context lookups should default to empty strings. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("I ({{cannot}}) be seen!", Helper.loadMap(), Helper.loadMap(), "I () be seen!"));

		/* Triple Mustache Context Miss Interpolation - Failed context lookups should default to empty strings. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("I ({{{cannot}}}) be seen!", Helper.loadMap(), Helper.loadMap(), "I () be seen!"));

		/* Ampersand Context Miss Interpolation - Failed context lookups should default to empty strings. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("I ({{&cannot}}) be seen!", Helper.loadMap(), Helper.loadMap(), "I () be seen!"));

		/* Dotted Names - Basic Interpolation - Dotted names should be considered a form of shorthand for sections. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{person.name}}\" == \"{{#person}}{{name}}{{/person}}\"", Helper.loadMap("person", Helper.loadMap("name", "Joe")), Helper.loadMap(), "\"Joe\" == \"Joe\""));

		/* Dotted Names - Triple Mustache Interpolation - Dotted names should be considered a form of shorthand for sections. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{{person.name}}}\" == \"{{#person}}{{{name}}}{{/person}}\"", Helper.loadMap("person", Helper.loadMap("name", "Joe")), Helper.loadMap(), "\"Joe\" == \"Joe\""));

		/* Dotted Names - Ampersand Interpolation - Dotted names should be considered a form of shorthand for sections. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{&person.name}}\" == \"{{#person}}{{&name}}{{/person}}\"", Helper.loadMap("person", Helper.loadMap("name", "Joe")), Helper.loadMap(), "\"Joe\" == \"Joe\""));

		/* Dotted Names - Arbitrary Depth - Dotted names should be functional to any level of nesting. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{a.b.c.d.e.name}}\" == \"Phil\"", Helper.loadMap("a", Helper.loadMap("b", Helper.loadMap("c", Helper.loadMap("d", Helper.loadMap("e", Helper.loadMap("name", "Phil")))))), Helper.loadMap(), "\"Phil\" == \"Phil\""));

		/* Dotted Names - Broken Chains - Any falsey value prior to the last part of the name should yield ''. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{a.b.c}}\" == \"\"", Helper.loadMap("a", Helper.loadMap()), Helper.loadMap(), "\"\" == \"\""));

		/* Dotted Names - Broken Chain Resolution - Each part of a dotted name should resolve only against its parent. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{a.b.c.name}}\" == \"\"", Helper.loadMap("a", Helper.loadMap("b", Helper.loadMap()), "c", Helper.loadMap("name", "Jim")), Helper.loadMap(), "\"\" == \"\""));

		/* Dotted Names - Initial Resolution - The first part of a dotted name should resolve as any other name. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("\"{{#a}}{{b.c.d.e.name}}{{/a}}\" == \"Phil\"", Helper.loadMap("a", Helper.loadMap("b", Helper.loadMap("c", Helper.loadMap("d", Helper.loadMap("e", Helper.loadMap("name", "Phil"))))), "b", Helper.loadMap("c", Helper.loadMap("d", Helper.loadMap("e", Helper.loadMap("name", "Wrong"))))), Helper.loadMap(), "\"Phil\" == \"Phil\""));

		/* Dotted Names - Context Precedence - Dotted names should be resolved against former resolutions. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("{{#a}}{{b.c}}{{/a}}", Helper.loadMap("a", Helper.loadMap("b", Helper.loadMap()), "b", Helper.loadMap("c", "ERROR")), Helper.loadMap(), ""));

		/* Interpolation - Surrounding Whitespace - Interpolation should not alter surrounding whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("| {{string}} |", Helper.loadMap("string", "---"), Helper.loadMap(), "| --- |"));

		/* Triple Mustache - Surrounding Whitespace - Interpolation should not alter surrounding whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("| {{{string}}} |", Helper.loadMap("string", "---"), Helper.loadMap(), "| --- |"));

		/* Ampersand - Surrounding Whitespace - Interpolation should not alter surrounding whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("| {{&string}} |", Helper.loadMap("string", "---"), Helper.loadMap(), "| --- |"));

		/* Interpolation - Standalone - Standalone interpolation should not alter surrounding whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("  {{string}}\n", Helper.loadMap("string", "---"), Helper.loadMap(), "  ---\n"));

		/* Triple Mustache - Standalone - Standalone interpolation should not alter surrounding whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("  {{{string}}}\n", Helper.loadMap("string", "---"), Helper.loadMap(), "  ---\n"));

		/* Ampersand - Standalone - Standalone interpolation should not alter surrounding whitespace. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("  {{&string}}\n", Helper.loadMap("string", "---"), Helper.loadMap(), "  ---\n"));

		/* Interpolation With Padding - Superfluous in-tag whitespace should be ignored. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("|{{ string }}|", Helper.loadMap("string", "---"), Helper.loadMap(), "|---|"));

		/* Triple Mustache With Padding - Superfluous in-tag whitespace should be ignored. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("|{{{ string }}}|", Helper.loadMap("string", "---"), Helper.loadMap(), "|---|"));

		/* Ampersand With Padding - Superfluous in-tag whitespace should be ignored. */
		assertDoesNotThrow(() -> Helper.executeMustacheTest("|{{& string }}|", Helper.loadMap("string", "---"), Helper.loadMap(), "|---|"));
	}

}
