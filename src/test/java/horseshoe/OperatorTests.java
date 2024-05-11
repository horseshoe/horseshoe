package horseshoe;

import static horseshoe.Operator.CALL;
import static horseshoe.Operator.LEFT_EXPRESSION;
import static horseshoe.Operator.RIGHT_EXPRESSION;
import static horseshoe.Operator.X_RIGHT_EXPRESSIONS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class OperatorTests {

	private static String escapeHTML(final String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("|", "&#124;");
	}

	private static String escapeMarkdown(final String value) {
		return escapeHTML(value).replaceAll("[-\\\\`*_{}\\[\\]()+.!|~]|(?<!&)#", "\\\\$0");
	}

	private static String escapeMarkdownTable(final String value) {
		return value.replaceAll("[\\\\`|]", "\\\\$0");
	}

	@Test
	void generateOperatorTable() throws Exception {
		final String tableHeader = "| Precedence | Operators | Associativity |";
		final StringBuilder operationTable = new StringBuilder(tableHeader).append(System.lineSeparator())
				.append(tableHeader.replaceAll("[A-za-z]", "-")).append(System.lineSeparator());

		final String separator = ", <br>";
		final StringBuilder sb = new StringBuilder();
		final Iterator<Operator> it = Operator.getAll().iterator();

		if (it.hasNext()) {
			Operator nextOperator = it.next();

			while (true) {
				final Operator operator = nextOperator;
				final String operatorOutput = "`" + escapeMarkdownTable(operator.getString()) + "`";
				sb.append(separator);

				if (operator.has(LEFT_EXPRESSION) || operator.has(CALL)) {
					sb.append("a ").append(operatorOutput);

					if (operator.has(X_RIGHT_EXPRESSIONS)) {
						sb.append(" b\\*");
					} else if (operator.has(RIGHT_EXPRESSION)) {
						sb.append(" b");
					}
				} else if (operator.has(X_RIGHT_EXPRESSIONS)) {
					sb.append(operatorOutput).append(" a\\*");
				} else if (operator.has(RIGHT_EXPRESSION)) {
					sb.append(operatorOutput).append(" a");
				} else {
					sb.append(operatorOutput);
				}

				if (operator.getClosingString() != null) {
					sb.append(" `").append(escapeMarkdownTable(operator.getClosingString())).append("`");
				}

				sb.append(" \\(").append(escapeMarkdown(operator.getDescription())).append("\\)");

				if (it.hasNext()) {
					nextOperator = it.next();

					if (operator.getPrecedence() != nextOperator.getPrecedence()) {
						operationTable.append("| " + operator.getPrecedence() + " | " + sb.substring(separator.length()) + " | " + (operator.isLeftAssociative() ? "Left&nbsp;to&nbsp;right" : "Right&nbsp;to&nbsp;left") + " |").append(System.lineSeparator());
						sb.setLength(0);
					}
				} else {
					operationTable.append("| " + operator.getPrecedence() + " | " + sb.substring(separator.length()) + " | " + (operator.isLeftAssociative() ? "Left&nbsp;to&nbsp;right" : "Right&nbsp;to&nbsp;left") + " |").append(System.lineSeparator());
					break;
				}
			}
		}

		final String tableText = operationTable.append(System.lineSeparator()).toString();

		// System.out.println(" Operation Table:");
		// System.out.print(tableText);

		// Replace the operation table text
		final Path readmeFile = Paths.get("README.md");
		final String oldReadme = new String(Files.readAllBytes(readmeFile), StandardCharsets.UTF_8);
		final int tableStart = oldReadme.indexOf(tableHeader);

		if (tableStart < 0) {
			fail("Operation table not found in " + readmeFile);
		}

		final Matcher tableMatcher = Pattern.compile("(?<=[\\n\\r])[^\\n\\r|]").matcher(oldReadme);
		final int endOfTable = tableMatcher.find(tableStart + 1) ? tableMatcher.start() : oldReadme.length();
		final String newReadme = oldReadme.substring(0, tableStart) + tableText + oldReadme.substring(endOfTable);

		if (!tableText.equals(oldReadme.substring(tableStart, endOfTable).replaceAll("\\r?\\n", System.lineSeparator()))) {
			Files.write(readmeFile, newReadme.getBytes(StandardCharsets.UTF_8));
			fail("Operation table updated, rerun to verify");
		}
	}

	@Test
	void testOperatorMethods() {
		final Operator plus = Operator.get("+", true);
		assertThrows(UnsupportedOperationException.class, () -> plus.withLocalBindingIndex(-1));
		assertThrows(UnsupportedOperationException.class, () -> plus.withRightExpressions(2));
		assertEquals("m: 2", Operator.createCall("m", true).withRightExpressions(2).toString());
	}

}
