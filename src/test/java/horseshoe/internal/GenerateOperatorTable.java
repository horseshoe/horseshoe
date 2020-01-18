package horseshoe.internal;

import static horseshoe.internal.Operator.LEFT_EXPRESSION;
import static horseshoe.internal.Operator.METHOD_CALL;
import static horseshoe.internal.Operator.RIGHT_EXPRESSION;
import static horseshoe.internal.Operator.X_RIGHT_EXPRESSIONS;

import org.junit.Test;

public class GenerateOperatorTable {

	@Test
	public void generateOperationTable() throws Exception {
		System.out.println(" Operation Table:");
		System.out.println("| Precedence | Operators | Associativity |");
		System.out.println("| ---------- | --------- | ------------- |");

		Operator previousOperator = null;
		final String separator = ", <br>";
		final StringBuilder sb = new StringBuilder();

		for (final Operator operator : Operator.getAll()) {
			if (previousOperator != null && operator.getPrecedence() != previousOperator.getPrecedence()) {
				System.out.println("| " + previousOperator.getPrecedence() + " | " + sb.substring(separator.length()) + " | " + (previousOperator.isLeftAssociative() ? "Left-to-right" : "Right-to-left") + " |");

				sb.setLength(0);
			}

			final String operatorOutput = "<code>" + operator.getString().replace("|", "&#124;") + "</code>";
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
				sb.append("<code>").append(operator.getClosingString().replace("|", "&#124;")).append("</code>");
			}

			sb.append(" (").append(operator.getDescription()).append(")");

			previousOperator = operator;
		}

		System.out.println("| " + previousOperator.getPrecedence() + " | " + sb.substring(separator.length()) + " | " + (previousOperator.isLeftAssociative() ? "Left-to-right" : "Right-to-left") + " |");
		System.out.println();
	}

}
