package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.Settings;

public final class Expression {

	private static final AtomicInteger DYN_INDEX = new AtomicInteger();
	private static final Operator NAVIGATE_OPERATOR = Operator.get(".");

	// Reflected Methods
	private static final Method IDENTIFIER_GET_VALUE;
	private static final Method IDENTIFIER_GET_VALUE_BACKREACH;
	private static final Method IDENTIFIER_GET_VALUE_METHOD;
	private static final Method PERSISTENT_STACK_PEEK;

	static {
		try {
			IDENTIFIER_GET_VALUE = Identifier.class.getMethod("getValue", Object.class);
			IDENTIFIER_GET_VALUE_BACKREACH = Identifier.class.getMethod("getValue", PersistentStack.class, Settings.ContextAccess.class);
			IDENTIFIER_GET_VALUE_METHOD = Identifier.class.getMethod("getValue", Object.class, Object[].class);
			PERSISTENT_STACK_PEEK = PersistentStack.class.getMethod("peek", int.class);
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException("Bad reflection operation: " + e.getMessage(), e);
		}
	}

	// The patterns used for parsing the grammar
	private static final Pattern IDENTIFIER_BACKREACH_PATTERN = Pattern.compile("\\s*([.][.]/)+");
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\s*(\\.\\.)?((\\.?[A-Za-z_\\$][A-Za-z0-9_\\$]*|`\\.?([^`\\\\]|\\\\[`\\\\])+`)[(]?)");
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("\\s*[0-9][0-9]*[.][0-9]+");
	private static final Pattern LONG_PATTERN = Pattern.compile("\\s*([0-9](x[0-9A-Fa-f]+|[0-9]*))");
	private static final Pattern STRING_PATTERN = Pattern.compile("\\s*\"([^\"\\\\]|\\\\[\\\\\"'btnfr]|\\\\x[0-9A-Fa-f]{1,8}|\\\\u[0-9A-Fa-f]{4}|\\\\U[0-9A-Fa-f]{8})*\"");
	private static final Pattern OPERATOR_PATTERN;

	static {
		final StringBuilder sb = new StringBuilder("\\s*(");

		// Quote each operator as part of the operator pattern
		for (final Operator operator : Operator.getAll()) {
			sb.append(Pattern.quote(operator.getString())).append('|');

			if (operator.getClosingString() != null) {
				sb.append(Pattern.quote(operator.getClosingString())).append('|');
			}
		}

		sb.setLength(sb.length() - 1);
		OPERATOR_PATTERN = Pattern.compile(sb.append(")").toString());
	}

	private static class Output {
		public final LinkedHashMap<Class<?>, MethodBuilder> builders = new LinkedHashMap<>();

		public Output(final MethodBuilder builder, final Class<?> type) {
			builders.put(type, builder);
		}

		/**
		 * Converts the output to an object, combining all builders into the single builder specified. All existing builders are cleared.
		 *
		 * @param builder the builder used to combine all other builders
		 * @return the combined builder that was passed to the method
		 */
		public MethodBuilder convertToObject(final MethodBuilder builder) {
			final MethodBuilder.Label label = builder.newLabel();

			for (final Iterator<Entry<Class<?>, MethodBuilder>> it = builders.entrySet().iterator(); it.hasNext(); ) {
				final Entry<Class<?>, MethodBuilder> entry = it.next();

				builder.append(entry.getValue());

				if (entry.getKey().isPrimitive()) {
					builder.addPrimitiveConversion(entry.getKey(), Object.class);
				}

				if (it.hasNext()) {
					builder.addBranch(GOTO, label);
				}
			}

			builder.updateLabel(label);
			return builder;
		}
	}

	public static abstract class Evaluator {
		public abstract Object evaluate(final Identifier identifiers[], final PersistentStack<Object> context, final Settings.ContextAccess access, final List<String> errors) throws ReflectiveOperationException;
	}

	/**
	 * Finishes the expression, returning one of the possible code paths as an object.
	 *
	 * @param builders the list of possible paths based on class of the item on the stack
	 * @return the final method builder
	 */
	public MethodBuilder finish(final LinkedHashMap<Class<?>, MethodBuilder> builders) {
		MethodBuilder mb = null;

		for (final Entry<Class<?>, MethodBuilder> entry : builders.entrySet()) {
			if (mb == null) {
				mb = entry.getValue();
			} else {
				mb.append(entry.getValue());
			}

			if (entry.getKey().isPrimitive()) {
				mb.addPrimitiveConversion(entry.getKey(), Object.class);
			}

			mb.addCode(ARETURN);
		}

		return mb == null ? logError(new MethodBuilder(), "No paths could return a value").addCode(ACONST_NULL, ARETURN) : mb;
	}

	/**
	 * Generates the appropriate instructions to log an error.
	 *
	 * @param mb the builder to use for logging the error
	 * @param error the error to log
	 * @return the method builder
	 */
	private static MethodBuilder logError(final MethodBuilder mb, final String error) {
		try {
			return mb.addCode(ALOAD, (byte)4).pushConstant(error).addInvoke(List.class.getMethod("add", Object.class));
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Processes the operation
	 *
	 * @param mb
	 * @param identifiers
	 * @param outputStack
	 * @param operator
	 * @throws ReflectiveOperationException
	 */
	private static void processOperation(final HashMap<Identifier, Integer> identifiers, final PersistentStack<Output> outputStack, final Operator operator) throws ReflectiveOperationException {
		// Check for a method call
		if (operator.has(Operator.METHOD_CALL)) {
			final MethodBuilder mb = outputStack.peek(operator.getRightExpressions()).builders.get(Object.class);

			if (operator.getRightExpressions() == 0) {
				mb.addCode(ACONST_NULL);
			} else {
				mb.pushNewObject(Object.class, operator.getRightExpressions());
			}

			// Convert all parameters to objects and stores them in the array
			for (int i = 0; i < operator.getRightExpressions(); i++) {
				outputStack.peek(operator.getRightExpressions() - 1 - i).convertToObject(mb.addCode(DUP).pushConstant(i)).addCode(AASTORE);
			}

			outputStack.pop(1 + operator.getRightExpressions()).push(new Output(mb.addInvoke(IDENTIFIER_GET_VALUE_METHOD), Object.class));
			return;
		}

		switch (operator.getString()) {
		case ".": break;
		case ",": break;

		default:
			break;
		}
	}

	private final String originalString;
	private final Identifier identifiers[];
	private final Evaluator evaluator;
	private final List<String> errors = new ArrayList<>();

	/**
	 * Creates a new expression.
	 *
	 * @param expressionString the advanced expression string
	 * @param simpleExpression true to parse as a simple expression, false to parse as an advanced expression
	 * @param maxBackreach the maximum backreach allowed by the expression
	 */
	public Expression(final CharSequence expressionString, final boolean simpleExpression, final int maxBackreach) throws Exception {
		final HashMap<Identifier, Integer> identifiers = new HashMap<>();
		final PersistentStack<Output> outputStack = new PersistentStack<>();

		this.originalString = expressionString.toString();

		if (".".equals(this.originalString)) {
			outputStack.push(new Output(new MethodBuilder().addCode(ALOAD_2).pushConstant(0).addInvoke(PERSISTENT_STACK_PEEK), Object.class));
		} else if (simpleExpression) {
			final MethodBuilder mb = new MethodBuilder();
			final String names[] = originalString.split("\\.", -1);

			// Load the identifiers
			for (int i = 0; i < names.length; i++) {
				final Identifier identifier = new Identifier(names[i].trim());
				Integer index = identifiers.get(identifier);

				if (index == null) {
					index = identifiers.size();
					identifiers.put(identifier, index);
				}

				if (i != 0) {
					// Create a new output formed by invoking identifiers[index].getValue(object)
					mb.addCode(ALOAD_1).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE);
				} else {
					// Create a new output formed by invoking identifiers[index].getValue(context, access)
					mb.addCode(ALOAD_1).pushConstant(index).addCode(AALOAD, ALOAD_2, ALOAD_3).addInvoke(IDENTIFIER_GET_VALUE_BACKREACH);
				}
			}

			outputStack.push(new Output(mb, Object.class));
		} else {
			// Tokenize the entire expression, using the shunting yard algorithm
			final PersistentStack<Operator> operatorStack = new PersistentStack<>();
			final int length = expressionString.length();
			Operator lastOperator = Operator.get("(");

			// Loop through all input
			nextToken:
			for (final Matcher matcher = IDENTIFIER_BACKREACH_PATTERN.matcher(expressionString); !matcher.hitEnd(); matcher.region(matcher.end(), length)) {
				final boolean hasLeftExpression = (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION | Operator.X_RIGHT_EXPRESSIONS));
				int backreach = 0;

				// Check for backreach
				if (matcher.usePattern(IDENTIFIER_BACKREACH_PATTERN).lookingAt()) {
					if (hasLeftExpression || lastOperator == NAVIGATE_OPERATOR) {
						throw new Exception("Unexpected backreach in expression at offset " + matcher.start(1));
					}

					backreach = matcher.group(1).length() / 3;

					if (backreach > maxBackreach) {
						throw new Exception("Backreach too far (max: " + maxBackreach + ") in expression at offset " + matcher.start(1));
					}

					matcher.region(matcher.end(), length);
				}

				// Check for identifier or literals
				if (!hasLeftExpression && matcher.usePattern(IDENTIFIER_PATTERN).lookingAt()) { // Identifier
					final String match = matcher.group(2);

					// Check for keywords that look like literals
					if ("true".equals(match) && backreach == 0 && lastOperator != NAVIGATE_OPERATOR) {
						outputStack.push(new Output(new MethodBuilder().pushConstant(1), boolean.class));
					} else if ("false".equals(match) && backreach == 0 && lastOperator != NAVIGATE_OPERATOR) {
						outputStack.push(new Output(new MethodBuilder().pushConstant(0), boolean.class));
					} else if (match.endsWith("(")) { // Method
						final String name = match.startsWith("`") ? match.substring(1, match.length() - 2).replaceAll("\\\\(.)", "\\1") : match.substring(0, match.length() - 1);
						final Identifier identifier = new Identifier(backreach, name, true);
						Integer index = identifiers.get(identifier);

						if (index == null) {
							index = identifiers.size();
							identifiers.put(identifier, index);
						}

						if (lastOperator == NAVIGATE_OPERATOR) {
							operatorStack.pop();

							// Create a new output formed by invoking identifiers[index].getValue(object, ...)
							outputStack.push(new Output(outputStack.pop().convertToObject(new MethodBuilder()).addCode(ALOAD_1).pushConstant(index).addCode(AALOAD, SWAP), Object.class));
						} else {
							// Create a new output formed by invoking identifiers[index].getValue(context.peek(backreach), ...)
							outputStack.push(new Output(new MethodBuilder().addCode(ALOAD_1).pushConstant(index).addCode(AALOAD, ALOAD_2).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK), Object.class));
						}

						lastOperator = operatorStack.push(Operator.createMethod(name));
						continue nextToken;
					} else if (".".equals(match)) {
						// Create a new output formed by invoking context.peek(backreach)
						outputStack.push(new Output(new MethodBuilder().addCode(ALOAD_2).pushConstant(backreach).addInvoke(PERSISTENT_STACK_PEEK), Object.class));
					} else { // Identifier
						final String name = match.startsWith("`") ? match.substring(1, match.length() - 1).replaceAll("\\\\(.)", "\\1") : match;
						final Identifier identifier = new Identifier(backreach, name, false);
						Integer index = identifiers.get(identifier);

						if (index == null) {
							index = identifiers.size();
							identifiers.put(identifier, index);
						}

						if (lastOperator == NAVIGATE_OPERATOR) {
							operatorStack.pop();

							// Create a new output formed by invoking identifiers[index].getValue(object)
							outputStack.push(new Output(outputStack.pop().convertToObject(new MethodBuilder()).addCode(ALOAD_1).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE), Object.class));
						} else {
							// Create a new output formed by invoking identifiers[index].getValue(context, access)
							outputStack.push(new Output(new MethodBuilder().addCode(ALOAD_1).pushConstant(index).addCode(AALOAD, ALOAD_2, ALOAD_3).addInvoke(IDENTIFIER_GET_VALUE_BACKREACH), Object.class));
						}
					}
				} else if (backreach != 0 || lastOperator == NAVIGATE_OPERATOR) { // Any backreach must have an identifier associated with it, and identifiers must follow the member selection operator
					throw new Exception("Invalid identifier in expression at offset " + matcher.start(1)); // TODO: Match failed, so start() throws exception
				} else if (matcher.hitEnd()) {
					break;
				} else if (matcher.usePattern(DOUBLE_PATTERN).lookingAt()) { // Double literal
					outputStack.push(new Output(new MethodBuilder().pushConstant(Double.parseDouble(matcher.group(1))), double.class));
				} else if (matcher.usePattern(LONG_PATTERN).lookingAt()) { // Long literal
					final String literal = matcher.group(1);
					final long value = Long.parseLong(literal, literal.contains("x") ? 16 : 10);

					if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
						outputStack.push(new Output(new MethodBuilder().pushConstant(value), long.class));
					} else {
						outputStack.push(new Output(new MethodBuilder().pushConstant((int)value), int.class));
					}
				} else if (matcher.usePattern(STRING_PATTERN).lookingAt()) { // String literal
					final String literal = matcher.group(1);
					int backslash = 0;

					if (literal == null) {
						outputStack.push(new Output(new MethodBuilder().pushConstant(""), String.class));
					} else if ((backslash = literal.indexOf('\\')) < 0) {
						outputStack.push(new Output(new MethodBuilder().pushConstant(literal), String.class));
					} else { // Find escape sequences and replace them with the proper character sequences
						final StringBuilder sb = new StringBuilder(literal.length());
						int start = 0;

						do {
							sb.append(literal, start, backslash);

							if (backslash + 1 >= literal.length()) {
								throw new Exception("Invalid '\\' at end of string literal in expression at offset " + matcher.start(1));
							}

							switch (literal.charAt(backslash + 1)) {
							case '\\': sb.append('\\'); break;
							case '"':  sb.append('\"'); break;
							case '\'': sb.append('\''); break;
							case 'b':  sb.append('\b'); break;
							case 't':  sb.append('\t'); break;
							case 'n':  sb.append('\n'); break;
							case 'f':  sb.append('\f'); break;
							case 'r':  sb.append('\r'); break;

							case 'x':
								int codePoint = 0;

								for (int i = 0; i < 8 && backslash + 2 < literal.length(); i++, backslash++) {
									final int digit = literal.charAt(backslash + 2);

									if (!((digit >= '0' && digit <= '9') || (digit >= 'A' && digit <= 'F') || (digit >= 'a' && digit <= 'f'))) {
										break;
									}
									codePoint = codePoint * 16 + digit;
								}

								sb.append(Character.toChars(codePoint));
								break;

							case 'u':
								sb.append(Character.toChars(Integer.parseInt(literal.substring(backslash + 2, backslash + 6), 16)));
								backslash += 4;
								break;

							case 'U':
								sb.append(Character.toChars(Integer.parseInt(literal.substring(backslash + 2, backslash + 10), 16)));
								backslash += 8;
								break;

							default: throw new Exception("Invalid character (" + literal.charAt(backslash + 1) + ") following '\\' in string literal (offset: " + backslash + ") in expression at offset " + matcher.start(1));
							}

							start = backslash + 2;
						} while ((backslash = literal.indexOf('\\', start)) >= 0);

						outputStack.push(new Output(new MethodBuilder().pushConstant(sb.append(literal, start, literal.length()).toString()), String.class));
					}
				} else if (matcher.usePattern(OPERATOR_PATTERN).lookingAt()) { // Operator
					final String token = matcher.group(1);
					Operator operator = Operator.get(token);

					for (operator = Operator.get(token); operator != null && hasLeftExpression != operator.has(Operator.LEFT_EXPRESSION); ) {
						operator = operator.next();
					}

					if (operator != null) {
						// Shunting-yard Algorithm
						while (!operatorStack.isEmpty() && !operatorStack.peek().has(Operator.X_RIGHT_EXPRESSIONS) && !"(".equals(operatorStack.peek().getString()) && (operatorStack.peek().getPrecedence() < operator.getPrecedence() || (operatorStack.peek().getPrecedence() == operator.getPrecedence() && operator.isLeftAssociative()))) {
							if (operatorStack.peek().getClosingString() != null) {
								throw new Exception("Unexpected '" + token + "' in expression at offset " + matcher.start(1) + ", expecting '" + operatorStack.peek().getClosingString() + "'");
							}

							processOperation(identifiers, outputStack, operatorStack.pop());
						}

						if (!operatorStack.isEmpty() && operatorStack.peek().has(Operator.X_RIGHT_EXPRESSIONS) && ",".equals(operator.getString())) {
							if (!hasLeftExpression) { // Check for invalid and empty expressions
								throw new Exception("Unexpected '" + token + "' in expression at offset " + matcher.start(1));
							}

							operatorStack.peek().addRightExpression();
						} else {
							operatorStack.push(operator);
						}

						lastOperator = operator;
						continue nextToken;
					}

					// Check if this token ends an expression on the stack
					while (!operatorStack.isEmpty()) {
						if (operatorStack.peek().getClosingString() != null) {
							final Operator closedOperator = operatorStack.pop();

							if (!hasLeftExpression) { // Check for invalid and empty expressions
								throw new Exception("Unexpected '" + token + "' in expression at offset " + matcher.start(1));
							} else if (!closedOperator.getClosingString().equals(token)) {
								throw new Exception("Unexpected '" + token + "' in expression at offset " + matcher.start(1) + ", expecting '" + closedOperator.getClosingString() + "'");
							}

							// If the token is not a parenthetical, process the operation
							if (closedOperator.has(Operator.X_RIGHT_EXPRESSIONS)) {
								closedOperator.addRightExpression().getRightExpressions(); // TODO: assign to identifier
								processOperation(identifiers, outputStack, closedOperator);
							} else if (closedOperator.has(Operator.LEFT_EXPRESSION)) {
								processOperation(identifiers, outputStack, closedOperator);
							}

							lastOperator = closedOperator.getClosingOperator();
							continue nextToken;
						}

						processOperation(identifiers, outputStack, operatorStack.pop());
					}

					throw new Exception("Unexpected '" + token + "' in expression at offset " + matcher.start(1));
				} else {
					throw new Exception("Unrecognized identifier in expression at offset " + matcher.regionStart());
				}

				// Check for multiple consecutive identifiers or literals
				if (hasLeftExpression) {
					throw new Exception("Unexpected '" + matcher.group(1) + "' in expression at offset " + matcher.start(1) + ", expecting operator");
				}

				lastOperator = null;
			}

			// Push everything to the output queue
			while (!operatorStack.isEmpty()) {
				if (operatorStack.peek().getClosingString() != null) {
					throw new Exception("Unexpected end of expression, expecting '" + operatorStack.peek().getClosingString() + "' (unmatched '" + operatorStack.peek().getString() + "')");
				}

				processOperation(identifiers, outputStack, operatorStack.pop());
			}
		}

		this.identifiers = new Identifier[identifiers.size()];
		this.evaluator = finish(outputStack.pop().builders).load(Expression.class.getPackage().getName() + ".dyn.Expression_" + DYN_INDEX.getAndIncrement(), Evaluator.class, Expression.class.getClassLoader()).getConstructor().newInstance();
		assert outputStack.isEmpty();

		for (final Entry<Identifier, Integer> entry : identifiers.entrySet()) {
			this.identifiers[entry.getValue()] = entry.getKey();
		}
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof Expression) {
			return originalString.equals(((Expression)object).originalString);
		}

		return false;
	}

	/**
	 * Evaluates the expression using the given context, global data, and access
	 *
	 * @param context the context to use for evaluating the expression
	 * @return the evaluated expression or null if the expression could not be evaluated
	 */
	public Object evaluate(final PersistentStack<Object> context, final Settings.ContextAccess access) {
		try {
			return evaluator.evaluate(identifiers, context, access, errors);
		} catch (final Throwable t) { // Don't let any exceptions escape
			errors.add(t.getMessage());
		}

		return null;
	}

	/**
	 * Gets a list of errors from the expression.
	 *
	 * @return the list of errors for the expression
	 */
	public List<String> getErrors() {
		return errors;
	}

	@Override
	public int hashCode() {
		return originalString.hashCode();
	}

	@Override
	public String toString() {
		return originalString;
	}

}
