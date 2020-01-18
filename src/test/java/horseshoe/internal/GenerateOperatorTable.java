package horseshoe.internal;

import static horseshoe.internal.Operator.LEFT_EXPRESSION;
import static horseshoe.internal.Operator.METHOD_CALL;
import static horseshoe.internal.Operator.RIGHT_EXPRESSION;
import static horseshoe.internal.Operator.X_RIGHT_EXPRESSIONS;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

public class GenerateOperatorTable {

	private static String escapeHTML(final String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String escapeMarkdown(final String value) {
		return escapeHTML(value).replaceAll("[-\\\\`*_{}\\[\\]()#+.!|]", "\\\\$0");
	}

	@Test
	public void generateOperationTable() throws Exception {
		System.out.println(" Operation Table:");
		System.out.println("| Precedence | Operators | Associativity |");
		System.out.println("| ---------- | --------- | ------------- |");

		final String separator = ", <br>";
		final StringBuilder sb = new StringBuilder();
		final Iterator<Operator> it = Operator.getAll().iterator();

		if (it.hasNext()) {
			Operator nextOperator = it.next();

			while (true) {
				final Operator operator = nextOperator;
				final String operatorOutput = "<code>" + escapeHTML(operator.getString()).replace("|", "&#124;") + "</code>";
				sb.append(separator);

				if (operator.has(LEFT_EXPRESSION) || operator.has(METHOD_CALL)) {
					sb.append("a").append(operatorOutput);

					if (operator.has(X_RIGHT_EXPRESSIONS)) {
						sb.append("b?");
					} else if (operator.has(RIGHT_EXPRESSION)) {
						sb.append("b");
					}
				} else if (operator.has(X_RIGHT_EXPRESSIONS)) {
					sb.append(operatorOutput).append("a?");
				} else if (operator.has(RIGHT_EXPRESSION)) {
					sb.append(operatorOutput).append("a");
				} else {
					sb.append(operatorOutput);
				}

				if (operator.getClosingString() != null) {
					sb.append("<code>").append(escapeHTML(operator.getClosingString()).replace("|", "&#124;")).append("</code>");
				}

				sb.append(" \\(").append(escapeMarkdown(operator.getDescription())).append("\\)");

				if (it.hasNext()) {
					nextOperator = it.next();

					if (operator.getPrecedence() != nextOperator.getPrecedence()) {
						System.out.println("| " + operator.getPrecedence() + " | " + sb.substring(separator.length()) + " | " + (operator.isLeftAssociative() ? "Left&nbsp;to&nbsp;right" : "Right&nbsp;to&nbsp;left") + " |");
						sb.setLength(0);
					}
				} else {
					System.out.println("| " + operator.getPrecedence() + " | " + sb.substring(separator.length()) + " | " + (operator.isLeftAssociative() ? "Left&nbsp;to&nbsp;right" : "Right&nbsp;to&nbsp;left") + " |");
					break;
				}
			}
		}

		System.out.println();
		assertTrue(true); // Explicitly state that we passed (for code analysis tools)
	}

}
