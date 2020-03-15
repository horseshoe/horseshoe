package horseshoe.internal;

import static horseshoe.internal.Operator.LEFT_EXPRESSION;
import static horseshoe.internal.Operator.METHOD_CALL;
import static horseshoe.internal.Operator.RIGHT_EXPRESSION;
import static horseshoe.internal.Operator.X_RIGHT_EXPRESSIONS;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class GenerateOperatorTable {

	private static String escapeHTML(final String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("|", "&#124;");
	}

	private static String escapeMarkdown(final String value) {
		return escapeHTML(value).replaceAll("[-\\\\`*_{}\\[\\]()+.!|]|(?<!&)#", "\\\\$0");
	}

	@Test
	public void generateOperationTable() throws Exception {
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
				final String operatorOutput = "<code>" + escapeMarkdown(operator.getString()) + "</code>";
				sb.append(separator);

				if (operator.has(LEFT_EXPRESSION) || operator.has(METHOD_CALL)) {
					sb.append("a").append(operatorOutput);

					if (operator.has(X_RIGHT_EXPRESSIONS)) {
						sb.append("b\\*");
					} else if (operator.has(RIGHT_EXPRESSION)) {
						sb.append("b");
					}
				} else if (operator.has(X_RIGHT_EXPRESSIONS)) {
					sb.append(operatorOutput).append("a\\*");
				} else if (operator.has(RIGHT_EXPRESSION)) {
					sb.append(operatorOutput).append("a");
				} else {
					sb.append(operatorOutput);
				}

				if (operator.getClosingString() != null) {
					sb.append("<code>").append(escapeMarkdown(operator.getClosingString())).append("</code>");
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

		final String tableText = operationTable.toString();

		System.out.println(" Operation Table:");
		System.out.println(tableText);

		// Replace the operation table text
		final Path readmeFile = Paths.get("README.md");
		final String newReadme = new String(Files.readAllBytes(readmeFile), StandardCharsets.UTF_8)
				.replaceFirst("(?<=[\\n\\r])" + Pattern.quote(tableHeader) + "[\\n\\r]+(\\|.*[\\n\\r]+)+", Matcher.quoteReplacement(tableText + System.lineSeparator()));

		Files.write(readmeFile, newReadme.getBytes(StandardCharsets.UTF_8));
	}

}
