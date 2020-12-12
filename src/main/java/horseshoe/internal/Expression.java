package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.RenderContext;
import horseshoe.SectionRenderData;
import horseshoe.Stack;
import horseshoe.internal.MethodBuilder.Label;

public final class Expression {

	private static final AtomicInteger DYN_INDEX = new AtomicInteger();

	// Reflected Methods
	private static final Constructor<?> ARRAY_LIST_CTOR_INT = getConstructor(ArrayList.class, int.class);
	private static final Method ACCESSOR_LOOKUP = getMethod(Accessor.class, "lookup", Object.class, Object.class, boolean.class);
	private static final Method ACCESSOR_LOOKUP_RANGE = getMethod(Accessor.class, "lookupRange", Object.class, Comparable.class, Object.class, boolean.class);
	private static final Field ACCESSOR_TO_BEGINNING = getField(Accessor.class, "TO_BEGINNING");
	private static final Method EXPRESSION_EVALUATE = getMethod(Expression.class, "evaluate", RenderContext.class, Object[].class);
	private static final Method EXPRESSION_PEEK_STACK = getMethod(Expression.class, "peekStack", Stack.class, int.class, String.class);
	private static final Constructor<?> HALT_EXCEPTION_CTOR_STRING = getConstructor(HaltRenderingException.class, String.class);
	private static final Method IDENTIFIER_FIND_VALUE = getMethod(Identifier.class, "findValue", RenderContext.class, int.class);
	private static final Method IDENTIFIER_FIND_VALUE_METHOD = getMethod(Identifier.class, "findValue", RenderContext.class, int.class, Object[].class);
	private static final Method IDENTIFIER_GET_ROOT_VALUE = getMethod(Identifier.class, "getRootValue", RenderContext.class);
	private static final Method IDENTIFIER_GET_ROOT_VALUE_METHOD = getMethod(Identifier.class, "getRootValue", RenderContext.class, Object[].class);
	private static final Method IDENTIFIER_GET_VALUE = getMethod(Identifier.class, "getValue", Object.class, boolean.class);
	private static final Method IDENTIFIER_GET_VALUE_METHOD = getMethod(Identifier.class, "getValue", Object.class, boolean.class, Object[].class);
	private static final Method ITERABLE_ITERATOR = getMethod(Iterable.class, "iterator");
	private static final Method ITERATOR_HAS_NEXT = getMethod(Iterator.class, "hasNext");
	private static final Method ITERATOR_NEXT = getMethod(Iterator.class, "next");
	private static final Constructor<?> LINKED_HASH_MAP_CTOR_INT = getConstructor(LinkedHashMap.class, int.class);
	private static final Constructor<?> LINKED_HASH_SET_CTOR_INT = getConstructor(LinkedHashSet.class, int.class);
	private static final Method LIST_ADD = getMethod(List.class, "add", Object.class);
	private static final Method MAP_PUT = getMethod(Map.class, "put", Object.class, Object.class);
	private static final Method OBJECT_TO_STRING = getMethod(Object.class, "toString");
	private static final Method OPERANDS_ADD = getMethod(Operands.class, "add", Object.class, Object.class);
	private static final Method OPERANDS_AND = getMethod(Operands.class, "and", Number.class, Number.class);
	private static final Method OPERANDS_COMPARE = getMethod(Operands.class, "compare", boolean.class, Object.class, Object.class);
	private static final Method OPERANDS_CREATE_RANGE = getMethod(Operands.class, "createRange", Number.class, Number.class, boolean.class);
	private static final Method OPERANDS_DIVIDE = getMethod(Operands.class, "divide", Number.class, Number.class);
	private static final Method OPERANDS_EXPONENTIATE = getMethod(Operands.class, "exponentiate", Number.class, Number.class);
	private static final Method OPERANDS_FIND_PATTERN = getMethod(Operands.class, "findPattern", Object.class, Object.class);
	private static final Method OPERANDS_GET_CLASS = getMethod(Operands.class, "getClass", RenderContext.class, String.class);
	private static final Method OPERANDS_IS_IN = getMethod(Operands.class, "isIn", Object.class, Object.class);
	private static final Method OPERANDS_MATCHES_PATTERN = getMethod(Operands.class, "matchesPattern", Object.class, Object.class);
	private static final Method OPERANDS_MULTIPLY = getMethod(Operands.class, "multiply", Number.class, Number.class);
	private static final Method OPERANDS_MODULO = getMethod(Operands.class, "modulo", Number.class, Number.class);
	private static final Method OPERANDS_NEGATE = getMethod(Operands.class, "negate", Number.class);
	private static final Method OPERANDS_NOT = getMethod(Operands.class, "not", Number.class);
	private static final Method OPERANDS_OR = getMethod(Operands.class, "or", Number.class, Number.class);
	private static final Method OPERANDS_SHIFT_LEFT = getMethod(Operands.class, "shiftLeft", Number.class, Number.class);
	private static final Method OPERANDS_SHIFT_RIGHT = getMethod(Operands.class, "shiftRight", Number.class, Number.class);
	private static final Method OPERANDS_SHIFT_RIGHT_ZERO = getMethod(Operands.class, "shiftRightZero", Number.class, Number.class);
	private static final Method OPERANDS_SUBTRACT = getMethod(Operands.class, "subtract", Object.class, Object.class);
	private static final Method OPERANDS_XOR = getMethod(Operands.class, "xor", Number.class, Number.class);
	private static final Method PATTERN_COMPILE = getMethod(Pattern.class, "compile", String.class, int.class);
	private static final Method RENDER_CONTEXT_GET_INDENTATION = getMethod(RenderContext.class, "getIndentation");
	private static final Method RENDER_CONTEXT_GET_SECTION_DATA = getMethod(RenderContext.class, "getSectionData");
	private static final Constructor<SectionRenderData> SECTION_RENDER_DATA_CTOR_DATA = getConstructor(SectionRenderData.class, Object.class);
	private static final Field SECTION_RENDER_DATA_DATA = getField(SectionRenderData.class, "data");
	private static final Field SECTION_RENDER_DATA_HAS_NEXT = getField(SectionRenderData.class, "hasNext");
	private static final Field SECTION_RENDER_DATA_INDEX = getField(SectionRenderData.class, "index");
	private static final Method SET_ADD = getMethod(Set.class, "add", Object.class);
	private static final Method STACK_PEEK = getMethod(Stack.class, "peek", int.class);
	private static final Method STACK_PEEK_BASE = getMethod(Stack.class, "peekBase");
	private static final Method STACK_POP = getMethod(Stack.class, "pop");
	private static final Method STACK_PUSH_OBJECT = getMethod(Stack.class, "push", Object.class);
	private static final Method STREAMABLE_ADD = getMethod(Streamable.class, "add", Object.class);
	private static final Method STREAMABLE_FLAT_ADD_ARRAY = getMethod(Streamable.class, "flatAdd", Object[].class);
	private static final Method STREAMABLE_FLAT_ADD_ITERABLE = getMethod(Streamable.class, "flatAdd", Iterable.class);
	private static final Method STREAMABLE_OF_UNKNOWN = getMethod(Streamable.class, "ofUnknown", Object.class);
	private static final Method STRING_BUILDER_APPEND_OBJECT = getMethod(StringBuilder.class, "append", Object.class);
	private static final Constructor<?> STRING_BUILDER_INIT_STRING = getConstructor(StringBuilder.class, String.class);
	private static final Method STRING_VALUE_OF = getMethod(String.class, "valueOf", Object.class);

	// The patterns used for parsing the grammar
	private static final Pattern COMMENT_PATTERN = Pattern.compile("(?:/(?s:/[^\\n\\x0B\\x0C\\r\\u0085\\u2028\\u2029]*|[*].*?[*]/)\\s*)", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("(?<double>[-+]Infinity|[-+]?(?:[0-9][0-9_']*[fFdD]|(?:[0-9][0-9_']*[.]?[eE][-+]?[0-9_']+|[0-9][0-9_']*[.][0-9_']+(?:[eE][-+]?[0-9_']+)?|0[xX](?:[0-9A-Fa-f_']+[.]?|[0-9A-Fa-f_']+[.][0-9A-Fa-f_']+)[pP][-+]?[0-9_']+)[fFdD]?))\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(?<identifier>" + Identifier.PATTERN + "|`(?:[^`]|``)+`|[.][.]|[.])\\s*" + COMMENT_PATTERN + "*(?<isMethod>[(])?\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_WITH_PREFIX_PATTERN;
	private static final Pattern LONG_PATTERN = Pattern.compile("(?:(?<hexsign>[-+]?)0[xX](?<hexadecimal>[0-9A-Fa-f_']+)|(?<decimal>[-+]?[0-9][0-9_']*))(?<isLong>[lL])?\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern NAMED_EXPRESSION_PARAMETER_PATTERN = Pattern.compile("(?:,\\s*" + COMMENT_PATTERN + "*(?:(?<parameter>" + Identifier.PATTERN + ")\\s*" + COMMENT_PATTERN + "*)?)", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern NAMED_EXPRESSION_PATTERN = Pattern.compile(COMMENT_PATTERN + "*(?<name>" + Identifier.PATTERN + ")\\s*" + COMMENT_PATTERN + "*(?:[(]\\s*" + COMMENT_PATTERN + "*(?:(?<firstParameter>[.]|" + Identifier.PATTERN + ")\\s*" + COMMENT_PATTERN + "*)?(?<remainingParameters>" + NAMED_EXPRESSION_PARAMETER_PATTERN + "+)?[)]\\s*" + COMMENT_PATTERN + "*)?[-=]>\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern OPERATOR_PATTERN;
	private static final Pattern REGEX_PATTERN = Pattern.compile("~/(?<nounicode>\\(\\?-U\\))?(?<regex>(?:[^/\\\\]|\\\\.)*)/\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern STRING_PATTERN = Pattern.compile("(?:\"(?<string>(?:[^\"\\\\]|\\\\[\\\\\"'btnfr{}0]|\\\\x[0-9A-Fa-f]|\\\\u[0-9A-Fa-f]{4}|\\\\U[0-9A-Fa-f]{8})*)\"|'(?<unescapedString>(?:[^']|'')*)')\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern TRAILING_IDENTIFIER_PATTERN = Pattern.compile(COMMENT_PATTERN + "*(((?<name>" + Identifier.PATTERN + ")\\s*" + COMMENT_PATTERN + "*)?[-=]>\\s*)?", Pattern.UNICODE_CHARACTER_CLASS);

	private static final byte[] CHAR_VALUE = new byte[] {
			127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
			127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,   0,   1,   2,   3,   4,   5,   6,   7,   8,   9, 127, 127, 127, 127, 127, 127,
			127,  10,  11,  12,  13,  14,  15,  16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,  32,  33,  34,  35, 127, 127, 127, 127, 127,
			127,  10,  11,  12,  13,  14,  15,  16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,  32,  33,  34,  35, 127, 127, 127, 127, 127
	};

	private static final Expression[] EMPTY_EXPRESSIONS = { };
	private static final Identifier[] EMPTY_IDENTIFIERS = { };

	private final Object location;
	private final String originalString;
	private final Expression[] expressions;
	private final Identifier[] identifiers;
	private final Evaluable evaluable;
	private final boolean isNamed;

	public abstract static class Evaluable {
		private static final byte LOAD_EXPRESSIONS = ALOAD_1;
		private static final byte LOAD_IDENTIFIERS = ALOAD_2;
		private static final byte LOAD_CONTEXT = ALOAD_3;
		private static final byte[] LOAD_ARGUMENTS = new byte[] { ALOAD, 4 };
		private static final int FIRST_LOCAL = 5;

		/**
		 * Evaluates the object using the specified parameters.
		 *
		 * @param expressions the expressions used to evaluate the object
		 * @param identifiers the identifiers used to evaluate the object
		 * @param context the context used to evaluate the object
		 * @param arguments the arguments used to evaluate the object
		 * @return the result of evaluating the object
		 * @throws Exception if an error occurs while evaluating the expression
		 */
		public abstract Object evaluate(final Expression[] expressions, final Identifier[] identifiers, final RenderContext context, final Object[] arguments) throws Exception;
	}

	static {
		final StringBuilder allOperators = new StringBuilder();
		final StringBuilder assignmentOperators = new StringBuilder();
		final Set<Operator> patterns = new TreeSet<>(new Comparator<Operator>() {
			@Override
			public int compare(final Operator o1, final Operator o2) {
				final int lengthDiff = o2.getString().length() - o1.getString().length();
				return lengthDiff == 0 ? o1.getString().compareTo(o2.getString()) : lengthDiff;
			}
		});

		// Get each operator, ordered by length
		for (final Operator operator : Operator.getAll()) {
			patterns.add(operator);

			if (operator.getClosingString() != null) {
				patterns.add(operator.getClosingOperator());
			}
		}

		// Create the patterns
		for (final Operator pattern : patterns) {
			allOperators.append(Pattern.quote(pattern.getString())).append('|');

			if (pattern.has(Operator.ASSIGNMENT)) {
				assignmentOperators.append('|').append(Pattern.quote(pattern.getString()));
			}
		}

		// Add comma as a separator
		IDENTIFIER_WITH_PREFIX_PATTERN = Pattern.compile("(?:(?<backreach>[.]?[/\\\\]|(?:[.][.][/\\\\])+)?)(?<internal>[.](?![.]))?" + IDENTIFIER_PATTERN + "(?=" + COMMENT_PATTERN + "*(?<assignment>" + assignmentOperators.substring(1) + ")(?:[^=]|$))?", Pattern.UNICODE_CHARACTER_CLASS);
		OPERATOR_PATTERN = Pattern.compile("(?<operator>" + allOperators.append(",)\\s*").toString(), Pattern.UNICODE_CHARACTER_CLASS);
	}

	/**
	 * Parses an identifier from the given matcher and returns the last operator (null or method call).
	 *
	 * @param state the parse state
	 * @param matcher the matcher used to match the next token
	 * @param lastOperator the last operator that was parsed
	 * @param lastNavigation true if the last operator was a navigate operator, otherwise false
	 * @return the last operator, which may be either null or a method call operator
	 */
	private static Operator parseIdentifier(final ExpressionParseState state, final Matcher matcher, final Operator lastOperator, final boolean lastNavigation) {
		final String identifierText = matcher.group("identifier");
		final boolean isMethod = matcher.group("isMethod") != null;
		int backreach = 0;
		boolean isRoot = false;
		boolean isInternal = false;

		// Check for additional identifier properties
		if (!lastNavigation) {
			final String backreachString = matcher.group("backreach");
			isInternal = matcher.group("internal") != null;

			if (backreachString == null) {
				if (!isInternal) {
					// Check for literals
					switch (identifierText) {
						case "true":
							state.getOperands().push(new Operand(boolean.class, new MethodBuilder().pushConstant(1)));
							return null;
						case "false":
							state.getOperands().push(new Operand(boolean.class, new MethodBuilder().pushConstant(0)));
							return null;
						case "null":
							state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
							return null;
						case ".": // Skip the unstated backreach for these
						case "..":
							break;
						default:
							backreach = Identifier.UNSTATED_BACKREACH;
							break;
					}
				}
			} else if (backreachString.length() == 1) { // Check for root ('/', '\')
				isRoot = true;

				if (isInternal || (identifierText.length() > 1 && identifierText.charAt(0) == '.')) {
					throw new IllegalArgumentException("Invalid identifier with prefix \"" + backreachString + "\" (index " + state.getIndex(matcher) + ")");
				}
			} else { // All other backreach ('./', '../', '../../', etc.)
				backreach = backreachString.length() / 3;
			}
		}

		// Process the identifier
		if (".".equals(identifierText)) {
			if (isRoot) {
				state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_PEEK_BASE).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true)));
			} else {
				state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).pushConstant(identifierText).addInvoke(EXPRESSION_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true)));
			}
		} else if ("..".equals(identifierText)) {
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach + 1).pushConstant(identifierText).addInvoke(EXPRESSION_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true)));
		} else if (isInternal) { // Everything that starts with "." is considered internal
			final String internalIdentifier = "." + identifierText;

			switch (internalIdentifier) {
				case ".hasNext":
					state.getOperands().push(new Operand(boolean.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).pushConstant(internalIdentifier).addInvoke(EXPRESSION_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_HAS_NEXT, true)));
					break;
				case ".indentation":
					state.getOperands().push(new Operand(String.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_INDENTATION).pushConstant(backreach).pushConstant(internalIdentifier).addInvoke(EXPRESSION_PEEK_STACK)));
					break;
				case ".index":
					state.getOperands().push(new Operand(int.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).pushConstant(internalIdentifier).addInvoke(EXPRESSION_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_INDEX, true)));
					break;
				case ".isFirst":
					state.getOperands().push(new Operand(int.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).pushConstant(internalIdentifier).addInvoke(EXPRESSION_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_INDEX, true)));
					state.getOperators().push(Operator.get("!", false));
					processOperation(state);
					break;
				default:
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
					break;
			}
		} else if (isMethod) { // Process the method call
			final String name = identifierText.startsWith("`") ? identifierText.substring(1, identifierText.length() - 1).replace("``", "`") : identifierText;

			if (lastNavigation) {
				// Create a new output formed by invoking identifiers[index].getValue(object, ...)
				final MethodBuilder objectBuilder = state.getOperands().pop().toObject();
				final Label skipFunc = objectBuilder.newLabel();

				state.getOperands().push(new Operand(Object.class, new MethodBuilder().addInvoke(IDENTIFIER_GET_VALUE_METHOD).updateLabel(skipFunc)));

				if (lastOperator.has(Operator.SAFE)) {
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(SWAP).pushConstant(lastOperator.has(Operator.IGNORE_FAILURES))));
					state.getOperands().push(new Operand(Object.class, objectBuilder.addCode(DUP).addBranch(IFNULL, skipFunc)));
				} else {
					state.getOperands().push(new Operand(Object.class, objectBuilder.pushConstant(lastOperator.has(Operator.IGNORE_FAILURES))));
					state.getOperands().push(new Operand(Object.class, new MethodBuilder()));
				}

				return state.getOperators().push(Operator.createMethod(name, true)).peek();
			} else {
				// Create a new output formed by invoking identifiers[index].getRootValue(context, ...) or identifiers[index].findValue(context, backreach, ...)
				if (isRoot) {
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addInvoke(IDENTIFIER_GET_ROOT_VALUE_METHOD)));
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT)));
				} else {
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addInvoke(IDENTIFIER_FIND_VALUE_METHOD)));
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).pushConstant(backreach)));
				}

				state.getOperands().push(new Operand(Object.class, new MethodBuilder()));
				return state.getOperators().push(Operator.createMethod(name, backreach >= 0)).peek();
			}
		} else { // Process the identifier
			final String name = identifierText.startsWith("`") ? identifierText.substring(1, identifierText.length() - 1).replace("``", "`") : identifierText;

			if (lastNavigation) {
				final Identifier identifier = new Identifier(name);
				final int index = state.getIdentifiers().getOrAdd(identifier);
				final Operator operator = state.getOperators().pop();

				if (operator.has(Operator.SAFE)) {
					// Create a new output formed by invoking identifiers[index].getValue(object)
					final Label end = state.getOperands().peek().builder.newLabel();
					state.getOperands().push(new Operand(Object.class, identifier, state.getOperands().pop().toObject().addCode(DUP).addBranch(IFNULL, end).addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).pushConstant(operator.has(Operator.IGNORE_FAILURES)).addInvoke(IDENTIFIER_GET_VALUE).updateLabel(end)));
				} else {
					state.getOperands().push(new Operand(Object.class, identifier, state.getOperands().pop().toObject().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).pushConstant(operator.has(Operator.IGNORE_FAILURES)).addInvoke(IDENTIFIER_GET_VALUE)));
				}
			} else {
				final Integer localBindingIndex;
				final Operator operator = Operator.get(matcher.group("assignment"), true);

				// Look-ahead for an assignment operation
				if (operator != null && operator.has(Operator.ASSIGNMENT)) {
					if (backreach >= 0) {
						throw new IllegalArgumentException("Invalid assignment to non-local variable (index " + state.getIndex(matcher) + ")");
					}

					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addAccess(ASTORE, Evaluable.FIRST_LOCAL + state.getLocalBindings().getOrAdd(name))));
				} else if (backreach < 0 && (localBindingIndex = state.getLocalBindings().get(name)) != null) { // Check for a local binding
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addAccess(ALOAD, Evaluable.FIRST_LOCAL + localBindingIndex.intValue())));
				} else { // Resolve the identifier
					final Identifier identifier = new Identifier(name);
					final int index = state.getIdentifiers().getOrAdd(identifier);

					// Create a new output formed by invoking identifiers[index].getRootValue(context) or identifiers[index].findValue(context, backreach)
					if (isRoot) {
						state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).addInvoke(IDENTIFIER_GET_ROOT_VALUE)));
					} else {
						state.getOperands().push(new Operand(Object.class, backreach >= 0 ? null : identifier, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).pushConstant(backreach).addInvoke(IDENTIFIER_FIND_VALUE)));
					}
				}
			}
		}

		return null;
	}

	/**
	 * Parse a named expression signature.
	 *
	 * @param state the parse state
	 * @param initializeLocalBindings the method builder used to initialize the local bindings
	 * @param matcher the matcher pointing to the named expression signature
	 */
	private static void parseNamedExpressionSignature(final ExpressionParseState state, final MethodBuilder initializeLocalBindings, final Matcher matcher) {
		final String firstParameter = matcher.group("firstParameter");
		final String remainingParameters = matcher.group("remainingParameters");

		if (firstParameter != null && !".".equals(firstParameter)) {
			initializeLocalBindings.addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(0).addInvoke(STACK_PEEK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true).addAccess(ASTORE, Evaluable.FIRST_LOCAL + state.getLocalBindings().getOrAdd(firstParameter));
		}

		if (remainingParameters == null) {
			return;
		}

		// Parse the parameters
		int i = 0;

		for (final Matcher parameterMatcher = NAMED_EXPRESSION_PARAMETER_PATTERN.matcher(remainingParameters); parameterMatcher.lookingAt(); i++, parameterMatcher.region(parameterMatcher.end(), remainingParameters.length())) {
			final String name = parameterMatcher.group("parameter");

			if (name != null) {
				if (state.getLocalBindings().get(name) != null) {
					throw new IllegalStateException("Duplicate parameter \"" + name + "\" in named expression");
				}

				// Load the local bindings
				final Label ifNull = initializeLocalBindings.newLabel();
				final Label end = initializeLocalBindings.newLabel();

				initializeLocalBindings.addCode(Evaluable.LOAD_ARGUMENTS).addBranch(IFNULL, ifNull).pushConstant(i).addCode(Evaluable.LOAD_ARGUMENTS).addCode(ARRAYLENGTH).addBranch(IF_ICMPGE, ifNull)
					.addCode(Evaluable.LOAD_ARGUMENTS).pushConstant(i).addCode(AALOAD).addGoto(end, 1)
					.updateLabel(ifNull).addCode(ACONST_NULL).updateLabel(end).addAccess(ASTORE, Evaluable.FIRST_LOCAL + state.getLocalBindings().getOrAdd(name));
			}
		}
	}

	/**
	 * Parse a string literal, substituting character escape sequences as necessary.
	 *
	 * @param matcher the matcher with the current string literal matched
	 * @return the parsed string representation of the string literal
	 */
	private static String parseStringLiteral(final Matcher matcher) {
		final String string = matcher.group("string");
		int backslash = 0;

		if (string == null || (backslash = string.indexOf('\\')) < 0) {
			return string == null ? matcher.group("unescapedString").replace("''", "'") : string;
		}

		// Find escape sequences and replace them with the proper character
		final StringBuilder sb = new StringBuilder(string.length());
		int start = 0;

		do {
			sb.append(string, start, backslash);
			assert backslash + 1 < string.length() : "Invalid string literal accepted with trailing \"\\\""; // According to the pattern, a backslash must be followed by a character
			start = backslash + 2;

			switch (string.charAt(backslash + 1)) {
				case 'b':
					sb.append('\b');
					break;
				case 't':
					sb.append('\t');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'f':
					sb.append('\f');
					break;
				case 'r':
					sb.append('\r');
					break;
				case '0': // Only allow zero, not the octal escape of C, C++, Java, etc.
					sb.appendCodePoint(0);
					break;
				case 'x':
					start = parseStringLiteralHex(sb, string, start);
					break;
				case 'u':
					assert backslash + 5 < string.length() : "Invalid string literal accepted with trailing \"\\u\""; // According to the pattern, \\u must be followed by 4 digits
					sb.appendCodePoint(Integer.parseInt(string.substring(backslash + 2, backslash + 6), 16));
					start = backslash + 6;
					break;
				case 'U':
					assert backslash + 9 < string.length() : "Invalid string literal accepted with trailing \"\\U\""; // According to the pattern, \\U must be followed by 8 digits
					sb.appendCodePoint(Integer.parseInt(string.substring(backslash + 2, backslash + 10), 16));
					start = backslash + 10;
					break;
				default:
					sb.append(string.charAt(backslash + 1));
					break;
			}
		} while ((backslash = string.indexOf('\\', start)) >= 0);

		return sb.append(string, start, string.length()).toString();
	}

	/**
	 * Parse a string literal hex escape of arbitrary length, and append the code point using a string builder.
	 *
	 * @param sb the string builder used to build the string
	 * @param string the string literal being parsed
	 * @param index the index of the first hex character in the string literal
	 * @return the index after the hex escape
	 */
	private static int parseStringLiteralHex(final StringBuilder sb, final String string, final int index) {
		int codePoint = 0;
		int i = index;

		for (; i < string.length() && string.charAt(i) < CHAR_VALUE.length; i++) {
			final int value = CHAR_VALUE[string.charAt(i)];

			if ((value & 0xF0) != 0) {
				break;
			}

			codePoint = codePoint * 16 + value;
		}

		sb.appendCodePoint(codePoint);
		return i;
	}

	/**
	 * Parses a token from the given matcher and returns the last operator.
	 *
	 * @param state the parse state
	 * @param matcher the matcher used to match the next token
	 * @param lastOperator the last operator that was parsed
	 * @return the last operator, which may be a newly processed operator or the operator passed into the method
	 */
	private static Operator parseToken(final ExpressionParseState state, final Matcher matcher, final Operator lastOperator) {
		// Skip comments
		if (matcher.usePattern(COMMENT_PATTERN).lookingAt()) {
			return lastOperator;
		}

		final boolean hasLeftExpression = (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION | Operator.X_RIGHT_EXPRESSIONS));
		final boolean lastNavigation = (lastOperator != null && lastOperator.has(Operator.NAVIGATION));

		// Check for identifier or literal
		if (!hasLeftExpression && matcher.usePattern(lastNavigation ? IDENTIFIER_PATTERN : IDENTIFIER_WITH_PREFIX_PATTERN).lookingAt()) { // Identifier
			return parseIdentifier(state, matcher, lastOperator, lastNavigation);
		} else if (lastNavigation) { // An identifier must follow the navigation operator
			throw new IllegalArgumentException("Invalid identifier (index " + state.getIndex(matcher) + ")");
		} else if (!hasLeftExpression && matcher.usePattern(DOUBLE_PATTERN).lookingAt()) { // Double literal
			state.getOperands().push(new Operand(double.class, new MethodBuilder().pushConstant(Double.parseDouble(matcher.group("double").replace("_", "").replace("'", "")))));
		} else if (!hasLeftExpression && matcher.usePattern(LONG_PATTERN).lookingAt()) { // Long literal
			final String decimal = matcher.group("decimal");
			final long value = decimal == null ? Long.parseLong(matcher.group("hexsign") + matcher.group("hexadecimal").replace("_", "").replace("'", ""), 16) : Long.parseLong(decimal.replace("_", "").replace("'", ""));

			if (matcher.group("isLong") != null || (int)value != value) {
				state.getOperands().push(new Operand(long.class, new MethodBuilder().pushConstant(value)));
			} else {
				state.getOperands().push(new Operand(int.class, new MethodBuilder().pushConstant((int)value)));
			}
		} else if (!hasLeftExpression && matcher.usePattern(STRING_PATTERN).lookingAt()) { // String literal
			state.getOperands().push(new Operand(String.class, new MethodBuilder().pushConstant(parseStringLiteral(matcher))));
		} else if (!hasLeftExpression && matcher.usePattern(REGEX_PATTERN).lookingAt()) { // Regular expression literal ("nounicode" hack is to support broken Java runtime library implementations)
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().pushConstant(Pattern.compile(matcher.group("regex")).pattern()).pushConstant(matcher.group("nounicode") == null ? Pattern.UNICODE_CHARACTER_CLASS : 0).addInvoke(PATTERN_COMPILE)));
		} else if (matcher.usePattern(OPERATOR_PATTERN).lookingAt()) { // Operator
			return processOperator(state, matcher, lastOperator, hasLeftExpression);
		} else {
			final String start = state.getTrimmedString().length() - matcher.regionStart() > 10 ? state.getTrimmedString().substring(matcher.regionStart(), matcher.regionStart() + 7) + "..." : state.getTrimmedString().substring(matcher.regionStart());
			throw new IllegalArgumentException("Unrecognized operand \"" + start + "\" (index " + state.getIndex(matcher) + ")");
		}

		return null;
	}

	/**
	 * Peeks into a stack, but throws a human-readable exception if the depth is out-of-range.
	 *
	 * @param <T> the type of items in the stack
	 * @param stack the stack to peek into
	 * @param depth the depth to peek into the stack
	 * @param name the name of the identifier to report in the string
	 * @return the item at the specified point in the stack
	 */
	public static <T> T peekStack(final Stack<T> stack, final int depth, final String name) {
		if (depth >= stack.size()) {
			throw new IndexOutOfBoundsException("Failed to get \"" + name + "\" at depth " + depth + ", max depth available in current scope is " + (stack.size() - 1));
		}

		return stack.peek(depth);
	}

	/**
	 * Processes the method call. The operand stack will be updated with the results of evaluating the operator.
	 *
	 * @param state the parse state
	 * @param operator the method call operator
	 */
	private static void processMethodCall(final ExpressionParseState state, final Operator operator) {
		final int parameterCount = operator.getRightExpressions();
		final Expression namedExpression;

		if (operator.has(Operator.KNOWN_OBJECT) || (namedExpression = state.getNamedExpressions().get(operator.getString())) == null) {
			// Create the identifier, then get and invoke the appropriate method
			final int index = state.getIdentifiers().getOrAdd(new Identifier(operator.getString(), parameterCount));
			final MethodBuilder methodResult = state.getOperands().peek(parameterCount).builder.addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).append(state.getOperands().peek(parameterCount + 1).builder);

			if (parameterCount == 0) {
				methodResult.addCode(ACONST_NULL);
			} else {
				methodResult.pushNewObject(Object.class, parameterCount);

				// Convert all parameters to objects and store them in the array
				for (int i = 0; i < parameterCount; i++) {
					methodResult.addCode(DUP).pushConstant(i).append(state.getOperands().peek(parameterCount - 1 - i).toObject()).addCode(AASTORE);
				}
			}

			state.getOperands().push(new Operand(Object.class, methodResult.append(state.getOperands().pop(parameterCount + 2).pop().builder)));
			return;
		}

		// Process the named expression
		final MethodBuilder expressionResult = new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA);
		final int index = state.getExpressions().getOrAdd(namedExpression);

		// Load the context
		if (parameterCount == 0) {
			expressionResult.addCode(DUP).pushConstant(0).addInvoke(STACK_PEEK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true);
		} else {
			expressionResult.append(state.getOperands().peek(parameterCount - 1).toObject());
		}

		expressionResult.pushNewObject(SectionRenderData.class).addCode(DUP_X1, SWAP).addInvoke(SECTION_RENDER_DATA_CTOR_DATA).addInvoke(STACK_PUSH_OBJECT).addCode(POP).addCode(Evaluable.LOAD_EXPRESSIONS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT);

		// Load the arguments
		if (parameterCount > 1) {
			expressionResult.pushNewObject(Object.class, parameterCount - 1);

			for (int i = 0; i < parameterCount - 1; i++) {
				expressionResult.addCode(DUP).pushConstant(i).append(state.getOperands().peek(parameterCount - 2 - i).toObject()).addCode(AASTORE);
			}
		} else {
			expressionResult.addCode(ACONST_NULL);
		}

		// Evaluate the expression
		expressionResult.addInvoke(EXPRESSION_EVALUATE).addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_POP).addCode(POP);

		state.getOperands().pop(parameterCount + 3).push(new Operand(Object.class, expressionResult));
	}

	/**
	 * Processes the specified operation. The operand stack will be updated with the results of evaluating the operator.
	 *
	 * @param state the parse state
	 */
	private static void processOperation(final ExpressionParseState state) {
		final Operator operator = state.getOperators().pop();
		final Operand right;

		if (operator.has(Operator.NAVIGATION)) { // Navigation operator is handled during parsing
			return;
		} else if (operator.has(Operator.METHOD_CALL)) { // Check for a method call
			processMethodCall(state, operator);
			return;
		} else if (operator.has(Operator.RIGHT_EXPRESSION)) {
			right = state.getOperands().pop();
		} else {
			right = null;
		}

		final Operand left = operator.has(Operator.LEFT_EXPRESSION) ? state.getOperands().pop() : null;

		switch (operator.getString()) {
			// List / Set / Array / Map / String Lookup Operations
			case  "[": case  "[?":
			case "?[": case "?[?":
				if (left != null) {
					final Operand subject = Entry.class.equals(right.type) ? state.getOperands().pop() : left;

					if (subject.type.isPrimitive() || Number.class.isAssignableFrom(subject.type)) {
						throw new IllegalArgumentException("Unexpected \"" + operator + "\" operator applied to " + subject.type.getName() + ", expecting list, map, set, string, or array type value");
					}

					final Label end = subject.builder.newLabel();

					if (operator.has(Operator.SAFE)) {
						subject.builder.addCode(DUP).addBranch(IFNULL, end);
					}

					if (Entry.class.equals(right.type)) {
						state.getOperands().push(new Operand(Object.class, subject.builder.append(left.toObject()).append(right.toObject()).pushConstant(operator.has(Operator.IGNORE_FAILURES)).addInvoke(ACCESSOR_LOOKUP_RANGE).updateLabel(end)));
					} else {
						state.getOperands().push(new Operand(Object.class, subject.builder.append(right.toObject()).pushConstant(operator.has(Operator.IGNORE_FAILURES)).addInvoke(ACCESSOR_LOOKUP).updateLabel(end)));
					}

					break;
				} // Intentional fall-through if left is null
			case "{": {
				int pairs = 0;

				// Find the number of pairs
				for (int i = 0; i < operator.getRightExpressions(); i++) {
					if (Entry.class.equals(state.getOperands().peek(i + pairs).type)) {
						pairs++;
						assert Entry.class.equals(state.getOperands().peek(i + pairs).type) : Entry.class + " must occur in pairs on the operand stack";
					}
				}

				if (pairs > 0) { // Create a map
					final int totalPairs = pairs;
					final MethodBuilder builder = new MethodBuilder().pushNewObject(LinkedHashMap.class).addCode(DUP).pushConstant((operator.getRightExpressions() * 4 + 2) / 3).addInvoke(LINKED_HASH_MAP_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						final Operand first = state.getOperands().peek(i + pairs);

						if (Entry.class.equals(first.type)) {
							builder.addCode(DUP).append(first.toObject()).append(state.getOperands().peek(i + --pairs).toObject()).addInvoke(MAP_PUT).addCode(POP);
						} else {
							builder.addCode(DUP).append(first.toObject()).addCode(DUP).addInvoke(MAP_PUT).addCode(POP);
						}
					}

					state.getOperands().pop(operator.getRightExpressions() + totalPairs).push(new Operand(Object.class, builder));
				} else if ("[".equals(operator.getString())) { // Create a list
					final MethodBuilder builder = new MethodBuilder().pushNewObject(ArrayList.class).addCode(DUP).pushConstant(operator.getRightExpressions()).addInvoke(ARRAY_LIST_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						builder.addCode(DUP).append(state.getOperands().peek(i).toObject()).addInvoke(LIST_ADD).addCode(POP);
					}

					state.getOperands().pop(operator.getRightExpressions()).push(new Operand(Object.class, builder));
				} else { // Create a set
					final MethodBuilder builder = new MethodBuilder().pushNewObject(LinkedHashSet.class).addCode(DUP).pushConstant((operator.getRightExpressions() * 4 + 2) / 3).addInvoke(LINKED_HASH_SET_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						builder.addCode(DUP).append(state.getOperands().peek(i).toObject()).addInvoke(SET_ADD).addCode(POP);
					}

					state.getOperands().pop(operator.getRightExpressions()).push(new Operand(Object.class, builder));
				}

				break;
			}
			case "[:]":
				state.getOperands().push(new Operand(Object.class, new MethodBuilder().pushNewObject(LinkedHashMap.class).addCode(DUP).pushConstant(8).addInvoke(LINKED_HASH_MAP_CTOR_INT)));
				break;
			case "..":
			case "..<":
				state.getOperands().push(new Operand(Object.class, left.toNumeric(false).append(right.toNumeric(false)).pushConstant(operator.getString().endsWith("<")).addInvoke(OPERANDS_CREATE_RANGE)));
				break;

			// Math Operations
			case "**":
				state.getOperands().push(new Operand(Double.class, left.toNumeric(true).append(right.toNumeric(true)).addInvoke(OPERANDS_EXPONENTIATE)));
				break;
			case "+":
				if (left == null) { // Unary +, basically do nothing except require a number
					state.getOperands().push(new Operand(Double.class, right.toNumeric(true)));
				} else if (StringBuilder.class.equals(left.type)) { // Check for string concatenation
					state.getOperands().push(left).peek().builder.append(right.toObject()).addInvoke(STRING_BUILDER_APPEND_OBJECT);
				} else if (String.class.equals(left.type) || String.class.equals(right.type) || StringBuilder.class.equals(right.type)) {
					state.getOperands().push(new Operand(StringBuilder.class, left.toObject().pushNewObject(StringBuilder.class).addCode(DUP_X1, SWAP).addInvoke(STRING_VALUE_OF).addInvoke(STRING_BUILDER_INIT_STRING).append(right.toObject()).addInvoke(STRING_BUILDER_APPEND_OBJECT)));
				} else { // String concatenation, mathematical addition, collection addition, or invalid
					state.getOperands().push(new Operand(Object.class, left.toObject().append(right.toObject()).addInvoke(OPERANDS_ADD)));
				}

				break;
			case "-":
				if (left == null) {
					state.getOperands().push(new Operand(Double.class, right.toNumeric(true).addInvoke(OPERANDS_NEGATE)));
				} else {
					state.getOperands().push(new Operand(Object.class, left.toObject().append(right.toObject()).addInvoke(OPERANDS_SUBTRACT)));
				}

				break;
			case "*":
				state.getOperands().push(new Operand(Double.class, left.toNumeric(true).append(right.toNumeric(true)).addInvoke(OPERANDS_MULTIPLY)));
				break;
			case "/":
				state.getOperands().push(new Operand(Double.class, left.toNumeric(true).append(right.toNumeric(true)).addInvoke(OPERANDS_DIVIDE)));
				break;
			case "%":
				state.getOperands().push(new Operand(Double.class, left.toNumeric(true).append(right.toNumeric(true)).addInvoke(OPERANDS_MODULO)));
				break;

			// Integral Operations
			case "~":
				state.getOperands().push(new Operand(Integer.class, right.toNumeric(false).addInvoke(OPERANDS_NOT)));
				break;
			case "<<":
				state.getOperands().push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_SHIFT_LEFT)));
				break;
			case ">>":
				state.getOperands().push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_SHIFT_RIGHT)));
				break;
			case ">>>":
				state.getOperands().push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_SHIFT_RIGHT_ZERO)));
				break;
			case "&":
				state.getOperands().push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_AND)));
				break;
			case "^":
				state.getOperands().push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_XOR)));
				break;
			case "|":
				state.getOperands().push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_OR)));
				break;

			// Logical Operators
			case "!": {
				final Label notZero = right.builder.newLabel();
				final Label end = right.builder.newLabel();

				state.getOperands().push(new Operand(boolean.class, right.toBoolean().addBranch(IFNE, notZero).addCode(ICONST_1).addGoto(end, 1).updateLabel(notZero).addCode(ICONST_0).updateLabel(end)));
				break;
			}
			case "&&": {
				final Label notZero = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				state.getOperands().push(new Operand(boolean.class, left.toBoolean().addBranch(IFNE, notZero).addCode(ICONST_0).addGoto(end, 1).updateLabel(notZero).append(right.toBoolean()).updateLabel(end)));
				break;
			}
			case "||": {
				final Label zero = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				state.getOperands().push(new Operand(boolean.class, left.toBoolean().addBranch(IFEQ, zero).addCode(ICONST_1).addGoto(end, 1).updateLabel(zero).append(right.toBoolean()).updateLabel(end)));
				break;
			}

			// Comparison Operators
			case "<=>":
				state.getOperands().push(new Operand(int.class, new MethodBuilder().pushConstant(false).append(left.toObject()).append(right.toObject()).addInvoke(OPERANDS_COMPARE)));
				break;
			case "<=":
				state.getOperands().push(left.execCompareOp(right, IFLE));
				break;
			case ">=":
				state.getOperands().push(left.execCompareOp(right, IFGE));
				break;
			case "<":
				state.getOperands().push(left.execCompareOp(right, IFLT));
				break;
			case ">":
				state.getOperands().push(left.execCompareOp(right, IFGT));
				break;
			case "==":
				state.getOperands().push(left.execCompareOp(right, IFEQ));
				break;
			case "!=":
				state.getOperands().push(left.execCompareOp(right, IFNE));
				break;

			case "=~":
				state.getOperands().push(new Operand(boolean.class, new MethodBuilder().append(left.toObject()).append(right.toObject()).addInvoke(OPERANDS_FIND_PATTERN)));
				break;
			case "==~":
				state.getOperands().push(new Operand(boolean.class, new MethodBuilder().append(left.toObject()).append(right.toObject()).addInvoke(OPERANDS_MATCHES_PATTERN)));
				break;

			// Membership Operator
			case "in":
				state.getOperands().push(new Operand(boolean.class, new MethodBuilder().append(left.toObject()).append(right.toObject()).addInvoke(OPERANDS_IS_IN)));
				break;

			// Ternary Operations
			case "??":
			case "?:": {
				final Label end = left.builder.newLabel();

				state.getOperands().push(new Operand(Object.class, left.toObject().addCode(DUP).addBranch(IFNONNULL, end).addCode(POP).append(right.toObject()).updateLabel(end)));
				break;
			}
			case "?": {
				if (!Entry.class.equals(right.type)) {
					throw new IllegalArgumentException("Incomplete ternary operator, missing \":\"");
				}

				assert !state.getOperands().isEmpty();

				final Label isFalse = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				state.getOperands().push(new Operand(Object.class, state.getOperands().pop().toBoolean().addBranch(IFEQ, isFalse).append(left.builder).addGoto(end, 1).updateLabel(isFalse).append(right.builder).updateLabel(end)));
				break;
			}

			case ":":
				state.getOperands().push(new Operand(Entry.class, left.toObject()));

				int ternaries = 0;

				for (final Operator stackOperator : state.getOperators()) {
					if (stackOperator.getPrecedence() != operator.getPrecedence()) {
						break;
					} else if ("?".equals(stackOperator.getString())) {
						ternaries++;
					} else if (":".equals(stackOperator.getString())) {
						ternaries--;
					}
				}

				if (ternaries > 0) { // Process everything up to the ternary
					while (!"?".equals(state.getOperators().peek().getString())) {
						processOperation(state);
					}

					state.getOperands().push(new Operand(Entry.class, right.toObject()));
					processOperation(state);
				} else { // Check if the colon should be part of a pair or a map
					state.getOperands().push(new Operand(Entry.class, right.toObject()));

					if (state.getOperators().isEmpty() || !state.getOperators().peek().has(Operator.ALLOW_PAIRS)) {
						state.getOperators().push(Operator.get(",", true).withRightExpressions(-1));
					}
				}

				break;

			case ";":
				state.getOperands().push(new Operand(right.type, left.builder.addCode(long.class.equals(left.type) || double.class.equals(left.type) ? POP2 : POP).append(right.builder)));
				break;

			case ",":
				state.getOperands().push(left);
				state.getOperators().push(Operator.get("[", false).withRightExpressions(operator.getRightExpressions() + 1));
				processOperation(state);
				break;

			// Streaming Operations
			case "#>":
			case "#.": { // Remap
				final MethodBuilder mb = left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addCode(DUP).addInvoke(ITERABLE_ITERATOR);
				final Label startOfLoop = mb.newLabel();
				final Label endOfLoop = mb.newLabel();

				state.getOperands().push(new Operand(Object.class, mb.addCode(DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP2).addInvoke(ITERATOR_NEXT).addAccess(ASTORE, operator.getLocalBindingIndex())
						.append(right.toObject()).addInvoke(STREAMABLE_ADD).addGoto(startOfLoop, 0).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#|": { // Flat remap
				final MethodBuilder mb = left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addCode(DUP).addInvoke(ITERABLE_ITERATOR);
				final Label startOfLoop = mb.newLabel();
				final Label endOfLoop = mb.newLabel();
				final Label notNull = mb.newLabel();
				final Label notIterable = mb.newLabel();
				final Label notArray = mb.newLabel();

				state.getOperands().push(new Operand(Object.class, mb.addCode(DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP2).addInvoke(ITERATOR_NEXT).addAccess(ASTORE, operator.getLocalBindingIndex())
						.append(right.toObject()).addCode(DUP).addBranch(IFNONNULL, notNull).addCode(POP2).addGoto(startOfLoop, -2)
						.updateLabel(notNull).addCode(DUP).addInstanceOfCheck(Iterable.class).addBranch(IFEQ, notIterable).addCast(Iterable.class).addInvoke(STREAMABLE_FLAT_ADD_ITERABLE).addGoto(startOfLoop, -2)
						.updateLabel(notIterable).addCode(DUP).addInstanceOfCheck(Object[].class).addBranch(IFEQ, notArray).addCast(Object[].class).addInvoke(STREAMABLE_FLAT_ADD_ARRAY).addGoto(startOfLoop, -2)
						.updateLabel(notArray).addThrow(IllegalArgumentException.class, "Illegal flat map result, must be null, Iterable, or array", 2).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#?": { // Filter
				final MethodBuilder mb = left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addCode(DUP).addInvoke(ITERABLE_ITERATOR);
				final Label startOfLoop = mb.newLabel();
				final Label readdObject = mb.newLabel();
				final Label endOfLoop = mb.newLabel();

				state.getOperands().push(new Operand(Object.class, mb.addCode(DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP2).addInvoke(ITERATOR_NEXT).addCode(DUP).addAccess(ASTORE, operator.getLocalBindingIndex())
						.append(right.toBoolean()).addBranch(IFNE, readdObject).addCode(POP2).addGoto(startOfLoop, -2)
						.updateLabel(readdObject).addInvoke(STREAMABLE_ADD).addGoto(startOfLoop, 0).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#<": { // Reduction
				final MethodBuilder mb = left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addInvoke(ITERABLE_ITERATOR).addCode(ACONST_NULL);
				final Label startOfLoop = mb.newLabel();
				final Label endOfLoop = mb.newLabel();

				state.getOperands().push(new Operand(Object.class, mb.addCode(SWAP, DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP).addInvoke(ITERATOR_NEXT).addAccess(ASTORE, operator.getLocalBindingIndex()).addCode(SWAP, POP)
						.append(right.toObject()).addGoto(startOfLoop, 0).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#^": // Return
				state.getOperands().push(new Operand(Object.class, right.toObject().addFlowBreakingCode(ARETURN, 0).addCode(ACONST_NULL)));
				break;

			case "(":
				state.getOperands().push(right);
				break;

			case "=":
				state.getOperands().push(new Operand(Object.class, right.toObject().addCode(DUP).append(left.builder)));
				break;

			case "\u2620": case "~:<": // Die
				state.getOperands().push(new Operand(Object.class, right.toObject().addInvoke(STRING_VALUE_OF).pushNewObject(HaltRenderingException.class).addCode(DUP_X1, SWAP).addInvoke(HALT_EXCEPTION_CTOR_STRING).addFlowBreakingCode(ATHROW, 0).addCode(ACONST_NULL)));
				break;

			case "~@": // Get class
				if (right.identifier != null) {
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).pushConstant(right.identifier.getName()).addInvoke(OPERANDS_GET_CLASS)));
				} else {
					final Label isNull = right.builder.newLabel();
					state.getOperands().push(new Operand(Object.class, right.toObject().addCode(DUP).addBranch(IFNULL, isNull).addInvoke(OBJECT_TO_STRING).addCode(Evaluable.LOAD_CONTEXT).addCode(SWAP).addInvoke(OPERANDS_GET_CLASS).updateLabel(isNull)));
				}

				break;

			default:
				throw new IllegalStateException("Unrecognized operator \"" + operator + "\"");
		}
	}

	/**
	 * Processes the current operator from the given matcher and returns it.
	 *
	 * @param state the parse state
	 * @param matcher the matcher used to match the next token
	 * @param lastOperator the last operator that was parsed
	 * @param hasLeftExpression true if the operator being processed has a left expression
	 * @return the newly processed operator
	 */
	private static Operator processOperator(final ExpressionParseState state, final Matcher matcher, final Operator lastOperator, final boolean hasLeftExpression) {
		final String token = matcher.group("operator");
		final Operator operator = Operator.get(token, hasLeftExpression);

		if (operator != null) {
			// Shunting-yard Algorithm
			while (!state.getOperators().isEmpty() && !state.getOperators().peek().isContainer() &&
					((hasLeftExpression && state.getOperators().peek().getPrecedence() < operator.getPrecedence()) ||
						(state.getOperators().peek().getPrecedence() == operator.getPrecedence() && operator.isLeftAssociative()))) {
				processOperation(state);
			}

			if (!state.getOperators().isEmpty() && state.getOperators().peek().has(Operator.X_RIGHT_EXPRESSIONS) && ",".equals(token)) {
				state.getOperators().push(state.getOperators().pop().addRightExpression());
				return operator;
			} else if (operator.has(Operator.TRAILING_IDENTIFIER)) {
				matcher.region(matcher.end(), matcher.regionEnd()).usePattern(TRAILING_IDENTIFIER_PATTERN).lookingAt();
				final String name = matcher.group("name");

				if (name != null) {
					state.getOperators().push(operator.withLocalBindingIndex(Evaluable.FIRST_LOCAL + state.getLocalBindings().getOrAdd(name)));
					return operator;
				}
			}

			state.getOperators().push(operator);
			return operator;
		} else if (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION) || lastOperator.getString().startsWith(":")) { // Check if this token ends an expression on the stack
			if (lastOperator != null && lastOperator.getString().startsWith(":")) { // ":" can have an optional right expression
				if (":<".equals(lastOperator.getString())) {
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addFieldAccess(ACCESSOR_TO_BEGINNING, true)));
					state.getOperators().pop(1).push(Operator.get(":", true));
				} else {
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
				}
			}

			while (!state.getOperators().isEmpty()) {
				Operator closedOperator = state.getOperators().pop();

				if (closedOperator.has(Operator.X_RIGHT_EXPRESSIONS) && hasLeftExpression) { // Process multi-argument operators
					closedOperator = closedOperator.addRightExpression();
				}

				if (closedOperator.getClosingString() != null) {
					if (!closedOperator.getClosingString().equals(token)) { // Check for a mismatched closing string
						throw new IllegalArgumentException("Unexpected \"" + token + "\", expecting \"" + closedOperator.getClosingString() + "\" (index " + state.getIndex(matcher) + ")");
					}

					state.getOperators().push(closedOperator);
					processOperation(state);
					return closedOperator.getClosingOperator();
				}

				state.getOperators().push(closedOperator);
				processOperation(state);
			}
		}

		throw new IllegalArgumentException("Unexpected \"" + token + "\" (index " + state.getIndex(matcher) + ")");
	}

	/**
	 * Creates a new expression, using the cached expression if applicable.
	 *
	 * @param cachedExpression a cached copy of the expression that can be leveraged to improve performance if applicable.
	 * @param location the location of the expression
	 * @param startIndex the starting index of the expression within the given scope
	 * @param expression the trimmed expression string (starting at {@code expressionStart})
	 * @param namedExpressions the map used to lookup named expressions
	 * @param horseshoeExpressions true to parse as a horseshoe expression, false to parse as a Mustache variable list
	 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading the expression
	 */
	public Expression(final Expression cachedExpression, final Object location, final int startIndex, final String expression, final Map<String, Expression> namedExpressions, final boolean horseshoeExpressions) throws ReflectiveOperationException {
		final ExpressionParseState state = new ExpressionParseState(startIndex, expression, namedExpressions);
		final MethodBuilder mb = new MethodBuilder();
		boolean named = false;

		this.location = location;
		this.originalString = state.getTrimmedString();

		if (".".equals(originalString)) {
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(0).addInvoke(STACK_PEEK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true)));
		} else if (!horseshoeExpressions) {
			final String[] names = Pattern.compile("\\s*[.]\\s*", Pattern.UNICODE_CHARACTER_CLASS).split(originalString, -1);

			// Push a new operand formed by invoking identifiers[index].getValue(context, backreach, access)
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(state.getIdentifiers().getOrAdd(new Identifier(names[0]))).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).pushConstant(Identifier.UNSTATED_BACKREACH).addInvoke(IDENTIFIER_FIND_VALUE)));

			// Load the identifiers and invoke identifiers[index].getValue(object)
			for (int i = 1; i < names.length; i++) {
				state.getOperands().peek().builder.addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(state.getIdentifiers().getOrAdd(new Identifier(names[i]))).addCode(AALOAD, SWAP).pushConstant(false).addInvoke(IDENTIFIER_GET_VALUE);
			}
		} else { // Tokenize the entire expression, using the shunting yard algorithm
			int initializeBindingsStart = 0;
			final int end = state.getTrimmedString().length();
			final Matcher matcher = NAMED_EXPRESSION_PATTERN.matcher(state.getTrimmedString());

			if (matcher.lookingAt()) {
				state.getNamedExpressions().put(matcher.group("name"), this);
				named = true;
				parseNamedExpressionSignature(state, mb, matcher);
				initializeBindingsStart = state.getLocalBindings().size();
				matcher.region(matcher.end(), end);
			}

			Operator lastOperator = Operator.get("(", false);

			// Loop through all tokens
			for (; matcher.regionStart() < end; matcher.region(matcher.end(), end)) {
				lastOperator = parseToken(state, matcher, lastOperator);
			}

			// Push everything to the output queue
			while (!state.getOperators().isEmpty()) {
				Operator operator = state.getOperators().pop();

				if (operator.getClosingString() != null) {
					throw new IllegalArgumentException("Unexpected end of expression, expecting \"" + operator.getClosingString() + "\" (unmatched \"" + operator.getString() + "\")");
				} else if (operator.has(Operator.X_RIGHT_EXPRESSIONS) && (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION | Operator.X_RIGHT_EXPRESSIONS))) { // Process multi-argument operators, but allow trailing commas
					operator = operator.addRightExpression();
				} else if (lastOperator != null && lastOperator.has(Operator.RIGHT_EXPRESSION)) {
					if (lastOperator.has(Operator.IGNORE_TRAILING)) {
						lastOperator = null;
						continue;
					} else {
						throw new IllegalArgumentException("Unexpected end of expression");
					}
				}

				state.getOperators().push(operator);
				processOperation(state);
			}

			// Initialize all local bindings to null
			for (int i = initializeBindingsStart; i < state.getLocalBindings().size(); i++) {
				mb.addCode(ACONST_NULL).addAccess(ASTORE, Evaluable.FIRST_LOCAL + i);
			}
		}

		if (state.getOperands().isEmpty()) {
			throw new IllegalArgumentException("Unexpected empty expression");
		}

		// Check for match to the cached expression
		if (cachedExpression != null && state.getExpressions().equalsArray(cachedExpression.expressions) && state.getIdentifiers().equalsArray(cachedExpression.identifiers)) {
			assert cachedExpression.originalString.equals(originalString) : "Invalid cached expression \"" + cachedExpression + "\" does not match parsed expression \"" + originalString + "\"";
			this.expressions = cachedExpression.expressions;
			this.identifiers = cachedExpression.identifiers;
			this.evaluable = cachedExpression.evaluable;
			this.isNamed = named;
			return;
		}

		// Populate all the class data
		this.expressions = state.getExpressions().isEmpty() ? EMPTY_EXPRESSIONS : state.getExpressions().toArray(Expression.class);
		this.identifiers = state.getIdentifiers().isEmpty() ? EMPTY_IDENTIFIERS : state.getIdentifiers().toArray(Identifier.class);
		this.evaluable = mb.append(state.getOperands().pop().toObject()).addFlowBreakingCode(ARETURN, 0).build(Expression.class.getPackage().getName() + ".Expression_" + DYN_INDEX.getAndIncrement(), Evaluable.class, Expression.class.getClassLoader()).getConstructor().newInstance();
		this.isNamed = named;
		assert state.getOperands().isEmpty();
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof Expression) {
			return originalString.equals(((Expression)object).originalString) && location.equals(((Expression)object).location);
		}

		return false;
	}

	/**
	 * Evaluates the expression using a new default render context.
	 *
	 * @return the evaluated expression or null if the expression could not be evaluated
	 */
	public Object evaluate() {
		return evaluate(new RenderContext());
	}

	/**
	 * Evaluates the expression using the given render context and the specified arguments.
	 *
	 * @param context the render context used to evaluate the object
	 * @param arguments the arguments used to evaluate the object
	 * @return the evaluated expression or null if the expression could not be evaluated
	 */
	public Object evaluate(final RenderContext context, final Object... arguments) {
		try {
			return evaluable.evaluate(expressions, identifiers, context, arguments);
		} catch (final HaltRenderingException e) {
			throw e;
		} catch (final Exception | LinkageError e) { // Don't let any exceptions escape
			if (e.getMessage() == null) {
				context.getSettings().getLogger().log(Level.WARNING, e, "Failed to evaluate expression \"{0}\" ({1})", originalString, location);
			} else {
				context.getSettings().getLogger().log(Level.WARNING, e, "Failed to evaluate expression \"{0}\" ({1}): {2}", originalString, location, e.getMessage());
			}
		}

		return null;
	}

	@Override
	public int hashCode() {
		return originalString.hashCode();
	}

	/**
	 * Checks if the expression is named.
	 *
	 * @return true if the expression is named, otherwise false
	 */
	public boolean isNamed() {
		return isNamed;
	}

	@Override
	public String toString() {
		return originalString;
	}

}
