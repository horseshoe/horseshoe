package horseshoe;

import static horseshoe.bytecode.MethodBuilder.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.ExpressionParseState.Evaluation;
import horseshoe.Utilities.TrimmedString;
import horseshoe.bytecode.BytecodeContainer;
import horseshoe.bytecode.MethodBuilder;
import horseshoe.bytecode.MethodBuilder.Label;
import horseshoe.util.Accessor;
import horseshoe.util.Evaluable;
import horseshoe.util.Identifier;
import horseshoe.util.Operands;
import horseshoe.util.SectionRenderData;
import horseshoe.util.Streamable;

/**
 * {@link Expression}s are evaluated to populate output content when a {@link Template} is rendered.
 * They are created when a {@link Template} is parsed and cannot be directly instantiated.
 */
public final class Expression {

	private static final byte LOAD_EXPRESSIONS = ALOAD_1;
	private static final byte LOAD_IDENTIFIERS = ALOAD_2;
	private static final byte LOAD_CONTEXT = ALOAD_3;
	private static final byte[] LOAD_ARGUMENTS = new byte[] { ALOAD, 4 };
	private static final int FIRST_LOCAL = 5;

	private static final Expression[] EMPTY_EXPRESSIONS = { };
	private static final Identifier[] EMPTY_IDENTIFIERS = { };
	private static final Operator START_OPERATOR = Operator.get("(", false);

	private static final Expression EXPRESSION_BEING_CREATED = new Expression();

	private static final Map<BytecodeContainer, Expression> cache = new HashMap<>();
	private static long cacheIndex = 0;

	// Reflected Methods
	private static final Method ARRAY_LIST_ADD = getMethod(ArrayList.class, "add", Object.class);
	private static final Constructor<?> ARRAY_LIST_CTOR_INT = getConstructor(ArrayList.class, int.class);
	private static final Method ACCESSOR_LOOKUP = getMethod(Accessor.class, "lookup", Object.class, Object.class, boolean.class);
	private static final Method ACCESSOR_LOOKUP_RANGE = getMethod(Accessor.class, "lookupRange", Object.class, Comparable.class, Object.class, boolean.class);
	private static final Field ACCESSOR_TO_BEGINNING = getField(Accessor.class, "TO_BEGINNING");
	private static final Method EXPRESSION_EVALUATE = getMethod(Expression.class, "evaluate", RenderContext.class, Object[].class);
	private static final Constructor<?> HALT_EXCEPTION_CTOR_STRING = getConstructor(HaltRenderingException.class, String.class);
	private static final Method IDENTIFIER_FIND_OBJECT = getMethod(Identifier.class, "findObject", RenderContext.class);
	private static final Method IDENTIFIER_FIND_VALUE = getMethod(Identifier.class, "findValue", RenderContext.class, int.class);
	private static final Method IDENTIFIER_FIND_VALUE_METHOD = getMethod(Identifier.class, "findValue", RenderContext.class, int.class, Object[].class);
	private static final Method IDENTIFIER_GET_VALUE = getMethod(Identifier.class, "getValue", Object.class, Object.class, boolean.class);
	private static final Method IDENTIFIER_GET_VALUE_METHOD = getMethod(Identifier.class, "getValue", Object.class, Object.class, boolean.class, Object[].class);
	private static final Field IDENTIFIER_NULL_ORIGINAL_CONTEXT = getField(Identifier.class, "NULL_ORIGINAL_CONTEXT");
	private static final Method IDENTIFIER_PEEK_STACK = getMethod(Identifier.class, "peekStack", Stack.class, int.class, String.class);
	private static final Method ITERATOR_HAS_NEXT = getMethod(Iterator.class, "hasNext");
	private static final Method ITERATOR_NEXT = getMethod(Iterator.class, "next");
	private static final Constructor<?> LINKED_HASH_MAP_CTOR_INT = getConstructor(LinkedHashMap.class, int.class);
	private static final Method LINKED_HASH_MAP_PUT = getMethod(LinkedHashMap.class, "put", Object.class, Object.class);
	private static final Method LINKED_HASH_SET_ADD = getMethod(LinkedHashSet.class, "add", Object.class);
	private static final Constructor<?> LINKED_HASH_SET_CTOR_INT = getConstructor(LinkedHashSet.class, int.class);
	private static final Method MAP_GET = getMethod(Map.class, "get", Object.class);
	private static final Method OBJECT_EQUALS = getMethod(Object.class, "equals", Object.class);
	private static final Method OBJECT_GET_CLASS = getMethod(Object.class, "getClass");
	private static final Method OBJECT_HASH_CODE = getMethod(Object.class, "hashCode");
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
	private static final Method OPERANDS_REQUIRE_NON_NULL_TO_STRING = getMethod(Operands.class, "requireNonNullToString", Object.class);
	private static final Method OPERANDS_SHIFT_LEFT = getMethod(Operands.class, "shiftLeft", Number.class, Number.class);
	private static final Method OPERANDS_SHIFT_RIGHT = getMethod(Operands.class, "shiftRight", Number.class, Number.class);
	private static final Method OPERANDS_SHIFT_RIGHT_ZERO = getMethod(Operands.class, "shiftRightZero", Number.class, Number.class);
	private static final Method OPERANDS_SUBTRACT = getMethod(Operands.class, "subtract", Object.class, Object.class);
	private static final Method OPERANDS_XOR = getMethod(Operands.class, "xor", Number.class, Number.class);
	private static final Method PATTERN_COMPILE = getMethod(Pattern.class, "compile", String.class, int.class);
	private static final Method RENDER_CONTEXT_GET_ANNOTATION_MAP = getMethod(RenderContext.class, "getAnnotationMap");
	private static final Method RENDER_CONTEXT_GET_INDENTATION = getMethod(RenderContext.class, "getIndentation");
	private static final Method RENDER_CONTEXT_GET_SECTION_DATA = getMethod(RenderContext.class, "getSectionData");
	private static final Method RENDER_CONTEXT_GET_TEMPLATE_BINDINGS = getMethod(RenderContext.class, "getTemplateBindings", int.class);
	private static final Constructor<SectionRenderData> SECTION_RENDER_DATA_CTOR_DATA = getConstructor(SectionRenderData.class, Object.class);
	private static final Field SECTION_RENDER_DATA_DATA = getField(SectionRenderData.class, "data");
	private static final Field SECTION_RENDER_DATA_HAS_NEXT = getField(SectionRenderData.class, "hasNext");
	private static final Field SECTION_RENDER_DATA_INDEX = getField(SectionRenderData.class, "index");
	private static final Method SECTION_RENDERER_CREATE_ANNOTATION_DATA = getMethod(SectionRenderer.class, "createAnnotationData", AnnotationHandler.class, Object[].class);
	private static final Method STACK_PEEK = getMethod(Stack.class, "peek");
	private static final Method STACK_PEEK_BASE = getMethod(Stack.class, "peekBase");
	private static final Method STACK_POP = getMethod(Stack.class, "pop");
	private static final Method STACK_PUSH_OBJECT = getMethod(Stack.class, "push", Object.class);
	private static final Method STREAMABLE_ADD = getMethod(Streamable.class, "add", Object.class);
	private static final Method STREAMABLE_FLAT_ADD_ARRAY = getMethod(Streamable.class, "flatAdd", Object[].class);
	private static final Method STREAMABLE_FLAT_ADD_ITERABLE = getMethod(Streamable.class, "flatAdd", Iterable.class);
	private static final Method STREAMABLE_OF_UNKNOWN = getMethod(Streamable.class, "ofUnknown", Object.class);
	private static final Method STREAMABLE_STREAM = getMethod(Streamable.class, "stream");
	private static final Method STRING_BUILDER_APPEND_STRING = getMethod(StringBuilder.class, "append", String.class);
	private static final Method STRING_BUILDER_APPEND_OBJECT = getMethod(StringBuilder.class, "append", Object.class);
	private static final Constructor<?> STRING_BUILDER_INIT_STRING = getConstructor(StringBuilder.class, String.class);
	private static final Method STRING_VALUE_OF = getMethod(String.class, "valueOf", Object.class);

	// The patterns used for parsing the grammar
	static final String SINGLE_COMMENT_PATTERN = "(?:/(?s:/[^\\n\\x0B\\x0C\\r\\u0085\\u2028\\u2029]*|[*].*?[*]/)\\s*)";
	static final Pattern COMMENT_PATTERN = Pattern.compile(SINGLE_COMMENT_PATTERN + "++", Pattern.UNICODE_CHARACTER_CLASS);
	static final Pattern COMMENTS_PATTERN = Pattern.compile(SINGLE_COMMENT_PATTERN + "*+", Pattern.UNICODE_CHARACTER_CLASS);

	private static final Pattern DOUBLE_PATTERN = Pattern.compile("(?<double>[-+]Infinity|[-+]?(?:\\d[\\d_']*[fFdD]|(?:\\d[\\d_']*[.]?[eE][-+]?[\\d_']+|\\d[\\d_']*[.][\\d_']+(?:[eE][-+]?[\\d_']+)?|0[xX](?:[\\dA-Fa-f_']+[.]?|[\\dA-Fa-f_']+[.][\\dA-Fa-f_']+)[pP][-+]?[\\d_']+)[fFdD]?))\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(?<identifier>" + Identifier.PATTERN + "|`(?:[^`]|``)++`|[.][.]|[.])\\s*" + COMMENTS_PATTERN + "(?<isMethod>[(])?\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_SHORT_PATTERN = Pattern.compile("(?<identifier>" + Identifier.PATTERN + ")", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_WITH_PREFIX_PATTERN;
	private static final Pattern LONG_PATTERN = Pattern.compile("(?:(?<hexsign>[-+]?)0[xX](?<hexadecimal>[\\dA-Fa-f_']+)|(?<decimal>[-+]?\\d[\\d_']*))(?<isLong>[lL])?\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern NAMED_EXPRESSION_PARAMETER_PATTERN = Pattern.compile("(?:,\\s*" + COMMENTS_PATTERN + "(?:(?<parameter>" + Identifier.PATTERN + ")\\s*" + COMMENTS_PATTERN + ")?)", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern NAMED_EXPRESSION_PATTERN = Pattern.compile(COMMENTS_PATTERN + "(?<name>" + Identifier.PATTERN + ")\\s*" + COMMENTS_PATTERN + "(?:[(]\\s*" + COMMENTS_PATTERN + "(?:(?<firstParameter>[.]|" + Identifier.PATTERN + ")\\s*" + COMMENTS_PATTERN + ")?(?<remainingParameters>" + NAMED_EXPRESSION_PARAMETER_PATTERN + "+)?[)]\\s*" + COMMENTS_PATTERN + "[-=]>|(?<assignment>:=))\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern OPERATOR_PATTERN;
	private static final Pattern REGEX_PATTERN = Pattern.compile("~/(?<nounicode>\\(\\?-U\\))?(?<regex>(?:[^/\\\\]|\\\\.)*+)/\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern STRING_PATTERN = Pattern.compile("(?:\"(?<doubleString>(?:[^\"\\\\]|\\\\[\\\\\"`'btnfr${}0]|\\\\x[\\dA-Fa-f]|\\\\u[\\dA-Fa-f]{4}|\\\\U[\\dA-Fa-f]{8})*+)\"|'(?<singleString>(?:[^']|'')*+)')\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern END_OF_STRING_TEMPLATE_PATTERN = Pattern.compile("(?:[^}\\\\]|\\\\.)++(?=})");
	private static final Pattern TRAILING_IDENTIFIER_PATTERN = Pattern.compile(COMMENTS_PATTERN + "(((?<name>" + Identifier.PATTERN + ")\\s*" + COMMENTS_PATTERN + ")?[-=]>\\s*)?", Pattern.UNICODE_CHARACTER_CLASS);

	private static final byte[] CHAR_VALUE = new byte[] {
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, -1, -1, -1,
			-1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,
			-1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1
	};

	private final Object location;
	private final String originalString;
	private final String name;
	private final String callName;
	private final Expression[] expressions;
	private final Identifier[] identifiers;
	private final Evaluable evaluable;

	static {
		final StringBuilder allOperators = new StringBuilder();
		final StringBuilder assignmentOperators = new StringBuilder();
		final TreeSet<Operator> patterns = new TreeSet<>((o1, o2) -> {
			final int lengthDiff = o2.getString().length() - o1.getString().length();
			return lengthDiff == 0 ? o1.getString().compareTo(o2.getString()) : lengthDiff;
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
			if (pattern.has(Operator.ASSIGNMENT)) {
				assignmentOperators.append('|').append(Pattern.quote(pattern.getString()));
			} else {
				allOperators.append(Pattern.quote(pattern.getString())).append('|');
			}
		}

		// Add comma as a separator
		final String nonAssignmentOperators = allOperators.append(',').toString();

		IDENTIFIER_WITH_PREFIX_PATTERN = Pattern.compile("(?:(?<backreach>[.]?[/\\\\]|(?:[.][.][/\\\\])+)?(?<internal>[.](?![.]))?|(?<isAnnotation>@)?)" + IDENTIFIER_PATTERN + "(?:" + COMMENTS_PATTERN + "(?!" + nonAssignmentOperators + ")(?<assignment>" + assignmentOperators.substring(1) + ")\\s*)?", Pattern.UNICODE_CHARACTER_CLASS);
		OPERATOR_PATTERN = Pattern.compile("(?<operator>" + nonAssignmentOperators + ")\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	}

	/**
	 * Adds a return to the expression. This method adds the necessary code if the result is assigned to a template binding.
	 *
	 * @param mb the method builder used to add the return statement
	 * @param state the parse state for the expression
	 * @param operand the operand to return from the expression
	 * @return the method builder used to add the return statement
	 */
	private static MethodBuilder addReturn(final MethodBuilder mb, final ExpressionParseState state, final Operand operand) {
		if (state.getBindingName() != null) {
			final TemplateBinding templateBinding = state.getOrAddTemplateBinding(state.getBindingName());

			return mb.addCode(LOAD_CONTEXT).pushConstant(templateBinding.getTemplateIndex()).addInvoke(RENDER_CONTEXT_GET_TEMPLATE_BINDINGS).addInvoke(STACK_PEEK).addCast(Object[].class).pushConstant(templateBinding.getIndex()).append(operand.toObject()).addCode(AASTORE, ACONST_NULL).addFlowBreakingCode(ARETURN, 0);
		} else {
			return mb.append(operand.toObject()).addFlowBreakingCode(ARETURN, 0);
		}
	}

	/**
	 * Gets the class name used for a new expression. The class name is a fixed length, so that equivalent methods generate bytecode of the same length.
	 *
	 * @return the class name used for a new expression
	 */
	private static String className() {
		final String number = "000000000000000" + Long.toHexString(cacheIndex);

		return Expression.class.getPackage().getName() + ".Expression_" + number.substring(number.length() - 10);
	}

	/**
	 * Counts the number of ternaries on the operator stack.
	 *
	 * @param state the parse state
	 * @param precedence the precedence level of ternary operators
	 * @return the number of ternary operations at the current level in the operator stack
	 */
	private static int countTernaries(final ExpressionParseState state, final int precedence) {
		int ternaries = 0;

		for (final Operator stackOperator : state.getOperators()) {
			if (stackOperator.getPrecedence() != precedence) {
				return ternaries;
			} else if ("?".equals(stackOperator.getString())) {
				ternaries++;

				if (ternaries > 0) {
					return ternaries;
				}
			} else if (":".equals(stackOperator.getString())) {
				ternaries--;
			}
		}

		return ternaries;
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
					if (matcher.group("isAnnotation") != null) {
						if (!state.getExtensions().contains(Extension.ANNOTATIONS)) {
							throw new IllegalStateException("Invalid identifier \"" + identifierText + "\"");
						}

						final String name = identifierText.startsWith("`") ? identifierText.substring(1, identifierText.length() - 1).replace("``", "`") : identifierText;

						if (!isMethod) {
							final Label endLabel = new Label();
							state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_ANNOTATION_MAP).pushConstant(name).addInvoke(MAP_GET).addCode(DUP).addBranch(IFNULL, endLabel).addCast(AnnotationHandler.class).addCode(ACONST_NULL).addInvoke(SECTION_RENDERER_CREATE_ANNOTATION_DATA).updateLabel(endLabel)));
							return null;
						}

						final String annotation = "@" + name;

						if (matcher.start() == 0) {
							state.setCallName(annotation);
						}

						return state.getOperators().push(Operator.createCall(annotation, false)).peek();
					}

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
					throw new IllegalStateException("Invalid identifier with prefix \"" + backreachString + "\" (index " + state.getIndex(matcher) + ")");
				}
			} else { // All other backreach ('./', '../', '../../', etc.)
				backreach = backreachString.length() / 3;
			}
		}

		// Process the identifier
		if (".".equals(identifierText)) {
			if (isRoot) {
				state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_PEEK_BASE).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true)));
			} else {
				state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).pushConstant(identifierText).addInvoke(IDENTIFIER_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true)));
			}
		} else if ("..".equals(identifierText)) {
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach + 1).pushConstant(identifierText).addInvoke(IDENTIFIER_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true)));
		} else if (isInternal) { // Everything that starts with "." is considered internal
			final String internalIdentifier = "." + identifierText;

			switch (internalIdentifier) {
				case ".hasNext":
					state.getOperands().push(new Operand(boolean.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).pushConstant(internalIdentifier).addInvoke(IDENTIFIER_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_HAS_NEXT, true)));
					break;
				case ".indentation":
					state.getOperands().push(new Operand(String.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_INDENTATION).pushConstant(backreach).pushConstant(internalIdentifier).addInvoke(IDENTIFIER_PEEK_STACK)));
					break;
				case ".index":
					state.getOperands().push(new Operand(int.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).pushConstant(internalIdentifier).addInvoke(IDENTIFIER_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_INDEX, true)));
					break;
				case ".isFirst":
					state.getOperands().push(new Operand(int.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).pushConstant(internalIdentifier).addInvoke(IDENTIFIER_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_INDEX, true)));
					state.getOperators().push(Operator.get("!", false));
					processOperation(state);
					break;
				default:
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
					break;
			}
		} else if (isMethod) { // Process the method call
			final String name = identifierText.startsWith("`") ? identifierText.substring(1, identifierText.length() - 1).replace("``", "`") : identifierText;

			// Methods are formed by pushing multiple operands on the stack:
			//  1) Cleanup code after the invocation, possibly null
			//  2) Code to invoke the call using the identifier when the stack looks like: identifier, target object, argument array
			//  3) Context object used as first argument to the call
			//  4) Target object on which the call was invoked, which may be a duplicate of the context object
			//  5) Parameters are pushed on later
			if (lastNavigation) {
				// Create a new invocation of identifier.getValue(object, null, ignoreFailures, arguments)
				final MethodBuilder objectBuilder = state.getOperands().pop().toObject();
				final MethodBuilder call = new MethodBuilder().addCode(ACONST_NULL, SWAP).pushConstant(lastOperator.has(Operator.IGNORE_FAILURES)).addCode(SWAP).addInvoke(IDENTIFIER_GET_VALUE_METHOD);

				if (lastOperator.has(Operator.SAFE) || lastOperator.has(Operator.IGNORE_FAILURES)) {
					state.getOperands().push(new Operand(Object.class, new MethodBuilder()));
					objectBuilder.addCode(DUP).addBranch(IFNULL, state.getOperands().peek().builder.newLabel());
				} else {
					state.getOperands().push(null);
				}

				state.getOperands().push(new Operand(Object.class, call));
				state.getOperands().push(new Operand(Object.class, objectBuilder));
				state.getOperands().push(state.getOperands().peek());
				return state.getOperators().push(Operator.createCall(name, true)).peek();
			} else {
				// Create a new invocation of identifier.getValue(object, Identifier.NULL_ORIGINAL_CONTEXT, false, arguments) or identifier.findValue(context, backreach, arguments)
				state.getOperands().push(null);

				if (isRoot) {
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addFieldAccess(IDENTIFIER_NULL_ORIGINAL_CONTEXT, true).addCode(SWAP).pushConstant(false).addCode(SWAP).addInvoke(IDENTIFIER_GET_VALUE_METHOD)));
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_PEEK_BASE).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true)));
					state.getOperands().push(state.getOperands().peek());
				} else {
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().pushConstant(backreach).addCode(SWAP).addInvoke(IDENTIFIER_FIND_VALUE_METHOD)));
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT)));

					if (backreach >= 0) {
						state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).pushConstant(name).addInvoke(IDENTIFIER_PEEK_STACK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true))); // Identifier.peekStack(context.getSectionData(), backreach, name).data
					} else {
						if (matcher.start() == 0) {
							state.setCallName(name);
						}

						state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(IDENTIFIER_FIND_OBJECT)));
					}
				}

				return state.getOperators().push(Operator.createCall(name, backreach >= 0)).peek();
			}
		} else { // Process the identifier
			final String name = identifierText.startsWith("`") ? identifierText.substring(1, identifierText.length() - 1).replace("``", "`") : identifierText;

			if (lastNavigation) { // Follows a '.', so not a local or template binding
				final Identifier identifier = state.getCachedIdentifier(new Identifier(name));
				final int index = state.getIdentifiers().getOrAdd(identifier);
				final Operator operator = state.getOperators().pop();

				if (operator.has(Operator.SAFE)) {
					// Create a new output formed by invoking identifiers[index].getValue(object, null, ignoreFailures)
					final Label end = state.getOperands().peek().builder.newLabel();
					state.getOperands().push(new Operand(Object.class, identifier, state.getOperands().pop().toObject().addCode(DUP).addBranch(IFNULL, end).addCode(LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP, ACONST_NULL).pushConstant(operator.has(Operator.IGNORE_FAILURES)).addInvoke(IDENTIFIER_GET_VALUE).updateLabel(end)));
				} else {
					state.getOperands().push(new Operand(Object.class, identifier, state.getOperands().pop().toObject().addCode(LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP, ACONST_NULL).pushConstant(operator.has(Operator.IGNORE_FAILURES)).addInvoke(IDENTIFIER_GET_VALUE)));
				}
			} else { // Check for a local or template binding
				final Operator operator = Operator.get(matcher.group("assignment"), true);

				// Look-ahead for an assignment operation
				if (operator != null) {
					if (backreach >= 0) {
						throw new IllegalStateException("Invalid assignment to non-local variable (index " + state.getIndex(matcher) + ")");
					}

					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addAccess(ASTORE, FIRST_LOCAL + state.getLocalBindings().getOrAdd(name))));
					return state.getOperators().push(operator).peek();
				} else { // Resolve the identifier
					parseSimpleIdentifier(state, name, backreach, isRoot);
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
			initializeLocalBindings.addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_PEEK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true).addAccess(ASTORE, FIRST_LOCAL + state.getLocalBindings().getOrAdd(firstParameter));
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

				initializeLocalBindings.addCode(LOAD_ARGUMENTS).addBranch(IFNULL, ifNull).pushConstant(i).addCode(LOAD_ARGUMENTS).addCode(ARRAYLENGTH).addBranch(IF_ICMPGE, ifNull)
					.addCode(LOAD_ARGUMENTS).pushConstant(i).addCode(AALOAD).addGoto(end, 1)
					.updateLabel(ifNull).addCode(ACONST_NULL).updateLabel(end).addAccess(ASTORE, FIRST_LOCAL + state.getLocalBindings().getOrAdd(name));
			}
		}
	}

	/**
	 * Parse a simple identifier.
	 *
	 * @param state the parse state
	 * @param name the name of the identifier
	 * @param backreach the backreach for the identifier, or less than 0 to indicate an unspecified backreach
	 * @param isRoot true if the identifier is prefixed with a root indicator
	 */
	private static void parseSimpleIdentifier(final ExpressionParseState state, final String name, final int backreach, final boolean isRoot) {
		final Integer bindingIndex;
		final TemplateBinding templateBinding;

		if (backreach < 0 && (bindingIndex = state.getLocalBindings().get(name)) != null) { // Check for a local binding
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().addAccess(ALOAD, FIRST_LOCAL + bindingIndex)));
		} else if (backreach < 0 && (templateBinding = state.getTemplateBinding(name)) != null) { // Check for a template binding
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).pushConstant(templateBinding.getTemplateIndex()).addInvoke(RENDER_CONTEXT_GET_TEMPLATE_BINDINGS).addInvoke(STACK_PEEK).addCast(Object[].class).pushConstant(templateBinding.getIndex()).addCode(AALOAD)));
		} else { // Resolve the identifier
			final Identifier identifier = state.getCachedIdentifier(new Identifier(name));
			final int index = state.getIdentifiers().getOrAdd(identifier);

			if (isRoot) { // Create invocation identifiers[index].getValue(context.getSectionData().peekBase().data, NULL_ORIGINAL_CONTEXT, false)
				state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_PEEK_BASE).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true).addFieldAccess(IDENTIFIER_NULL_ORIGINAL_CONTEXT, true).pushConstant(false).addInvoke(IDENTIFIER_GET_VALUE)));
			} else { // Create invocation identifiers[index].findValue(context, backreach)
				state.getOperands().push(new Operand(Object.class, backreach >= 0 ? null : identifier, new MethodBuilder().addCode(LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(LOAD_CONTEXT).pushConstant(backreach).addInvoke(IDENTIFIER_FIND_VALUE)));
			}
		}
	}

	/**
	 * Parse a string literal, substituting character escape sequences as necessary, and adding it to the parse state.
	 *
	 * @param state the parse state
	 * @param matcher the matcher with the current string literal matched
	 */
	private static void parseStringLiteral(final ExpressionParseState state, final Matcher matcher) {
		final MethodBuilder builder = new MethodBuilder();
		final String singleString = matcher.group("singleString");

		if (singleString != null) {
			state.getOperands().push(new Operand(String.class, builder.pushConstant(singleString.replace("''", "'"))));
			return;
		}

		final String string = matcher.group("doubleString");
		int interpolation = string.indexOf('$'); // Stores the index of the next "$" or closing interpolation "}"
		int backslash = string.indexOf('\\'); // Stores the index of the next "\"

		if (interpolation < 0 && backslash < 0) {
			state.getOperands().push(new Operand(String.class, builder.pushConstant(string)));
			return;
		}

		// Find escape sequences and process string interpolation
		final StringBuilder sb = new StringBuilder(string.length());
		final int stringStart = state.getIndex(matcher) + 1; // + 1 for '"' character
		int start = 0;
		int interpolationStart = 0;

		do {
			if (Integer.compareUnsigned(backslash, interpolation) < 0) { // Found escape sequence
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

				backslash = string.indexOf('\\', start);
			} else if (interpolation < start) { // "\$"
				interpolation = string.indexOf('$', start);
			} else if (string.charAt(interpolation) == '}') { // End of "${...}" expression
				final TrimmedString expression = Utilities.trim(sb.append(string, start, interpolation).toString(), 0);
				final ExpressionParseState templateState = state.createNestedState(interpolationStart + expression.start, expression.string);

				sb.setLength(0);
				processExpression(templateState, COMMENT_PATTERN.matcher(expression.string), START_OPERATOR, expression.string.length());
				builder.append(templateState.getOperands().pop().toObject()).addInvoke(STRING_BUILDER_APPEND_OBJECT);
				assert templateState.getOperands().isEmpty();
				start = interpolation + 1;
				interpolation = string.indexOf('$', start);
			} else if (interpolation + 1 >= string.length()) { // Ends with "$"
				break;
			} else { // Found "$"
				if (builder.isEmpty()) {
					builder.pushNewObject(StringBuilder.class).addCode(DUP).pushConstant(sb.append(string, start, interpolation).toString()).addInvoke(STRING_BUILDER_INIT_STRING);
				} else {
					builder.pushConstant(sb.append(string, start, interpolation).toString()).addInvoke(STRING_BUILDER_APPEND_STRING);
				}

				sb.setLength(0);
				final Matcher interpolationMatcher = IDENTIFIER_SHORT_PATTERN.matcher(string);

				if (string.charAt(interpolation + 1) == '{') { // Start of "${...}" expression
					if (!interpolationMatcher.usePattern(END_OF_STRING_TEMPLATE_PATTERN).region(interpolation + 2, string.length()).lookingAt()) {
						throw new IllegalStateException("Invalid string interpolation expression \"" + string.substring(interpolation) + "\"");
					}

					start = interpolation + 2;
					interpolationStart = stringStart + start;
					interpolation = interpolationMatcher.end();
				} else if (interpolationMatcher.region(interpolation + 1, string.length()).lookingAt()) { // Found "$[identifier]"
					parseSimpleIdentifier(state, interpolationMatcher.group(), Identifier.UNSTATED_BACKREACH, false);
					builder.append(state.getOperands().pop().builder).addInvoke(STRING_BUILDER_APPEND_OBJECT);
					start = interpolationMatcher.end();
					interpolation = string.indexOf('$', start);
				} else { // Stray "$", e.g. "$50.00"
					start = interpolation;
					interpolation = string.indexOf('$', start + 1);
				}
			}
		} while (interpolation >= 0 || backslash >= 0);

		final String lastString = sb.append(string, start, string.length()).toString();

		if (builder.isEmpty()) {
			state.getOperands().push(new Operand(String.class, builder.pushConstant(lastString)));
		} else {
			state.getOperands().push(new Operand(String.class, builder.pushConstant(lastString).addInvoke(STRING_BUILDER_APPEND_STRING).addInvoke(OBJECT_TO_STRING)));
		}
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
			final byte value = CHAR_VALUE[string.charAt(i)];

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
			throw new IllegalStateException("Invalid identifier (index " + state.getIndex(matcher) + ")");
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
			parseStringLiteral(state, matcher);
		} else if (!hasLeftExpression && matcher.usePattern(REGEX_PATTERN).lookingAt()) { // Regular expression literal ("nounicode" hack is to support broken Java runtime library implementations)
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().pushConstant(Pattern.compile(matcher.group("regex")).pattern()).pushConstant(matcher.group("nounicode") == null ? Pattern.UNICODE_CHARACTER_CLASS : 0).addInvoke(PATTERN_COMPILE)));
		} else if (matcher.usePattern(OPERATOR_PATTERN).lookingAt()) { // Operator
			return processOperator(state, matcher, lastOperator, hasLeftExpression);
		} else {
			final String start = state.getExpressionString().length() - matcher.regionStart() > 10 ? state.getExpressionString().substring(matcher.regionStart(), matcher.regionStart() + 7) + "..." : state.getExpressionString().substring(matcher.regionStart());
			throw new IllegalStateException("Unrecognized operand \"" + start + "\" (index " + state.getIndex(matcher) + ")");
		}

		return null;
	}

	/**
	 * Processes a call, which may be either a named expression call or a method call. The operand stack will be updated with the results of evaluating the operator.
	 *
	 * @param state the parse state
	 * @param operator the call operator
	 */
	private static void processCall(final ExpressionParseState state, final Operator operator) {
		final int parameterCount = operator.getRightExpressions();
		final Expression namedExpression;

		if (operator.getString().isEmpty()) { // Parsing the expression as a method call, so return array with arguments
			final MethodBuilder mb = new MethodBuilder().pushNewObject(Object.class, parameterCount);

			for (int i = 0; i < parameterCount; i++) {
				mb.addCode(DUP).pushConstant(i).append(state.getOperands().peek(parameterCount - 1 - i).toObject()).addCode(AASTORE);
			}

			state.getOperands().pop(parameterCount).push(new Operand(Object.class, mb));
			return;
		} else if (operator.getString().startsWith("@")) { // Parsing Annotation
			final Label endLabel = new Label();
			final MethodBuilder mb = new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_ANNOTATION_MAP).pushConstant(operator.getString().substring(1)).addInvoke(MAP_GET).addCode(DUP).addBranch(IFNULL, endLabel).addCast(AnnotationHandler.class).pushNewObject(Object.class, parameterCount);

			for (int i = 0; i < parameterCount; i++) {
				mb.addCode(DUP).pushConstant(i).append(state.getOperands().peek(parameterCount - 1 - i).toObject()).addCode(AASTORE);
			}

			state.getOperands().pop(parameterCount).push(new Operand(Object.class, mb.addInvoke(SECTION_RENDERER_CREATE_ANNOTATION_DATA).updateLabel(endLabel)));
			return;
		} else if (operator.has(Operator.KNOWN_OBJECT) || (namedExpression = state.getNamedExpressions().get(operator.getString())) == null) {
			processMethodCall(state, operator);
			return;
		}

		// Process the named expression
		final int index = state.getExpressions().getOrAdd(namedExpression);
		final MethodBuilder expressionResult = new MethodBuilder().addCode(LOAD_EXPRESSIONS).pushConstant(index).addCode(AALOAD).addCode(LOAD_CONTEXT);

		// Load the arguments
		if (parameterCount > 1) {
			expressionResult.append(state.getOperands().peek(parameterCount - 1).toObject()).pushNewObject(Object.class, parameterCount - 1);
		} else if (parameterCount > 0) {
			expressionResult.append(state.getOperands().peek(parameterCount - 1).toObject()).addCode(ACONST_NULL);
		} else {
			expressionResult.addCode(ACONST_NULL);
		}

		for (int i = 0; i < parameterCount - 1; i++) {
			expressionResult.addCode(DUP).pushConstant(i).append(state.getOperands().peek(parameterCount - 2 - i).toObject()).addCode(AASTORE);
		}

		// Evaluate the expression (if at least one parameter, then first push the first parameter onto the context stack and pop it off after)
		if (parameterCount > 0) {
			expressionResult.addCode(SWAP).addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addCode(SWAP).pushNewObject(SectionRenderData.class).addCode(DUP_X1, SWAP).addInvoke(SECTION_RENDER_DATA_CTOR_DATA).addInvoke(STACK_PUSH_OBJECT).addCode(POP)
					.addInvoke(EXPRESSION_EVALUATE).addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_POP).addCode(POP);
		} else {
			expressionResult.addInvoke(EXPRESSION_EVALUATE);
		}

		state.getOperands().pop(parameterCount + 4).push(new Operand(Object.class, expressionResult));
	}

	/**
	 * Processes an expression.
	 *
	 * @param state the parse state
	 * @param matcher the matcher used to process the expression
	 * @param lastOperator the last operator that was parsed
	 * @param end the ending index of the expression in the matcher
	 */
	private static void processExpression(final ExpressionParseState state, final Matcher matcher, final Operator lastOperator, final int end) {
		Operator previousOperator = lastOperator;

		// Loop through all tokens
		for (; matcher.regionStart() < end; matcher.region(matcher.end(), end)) {
			previousOperator = parseToken(state, matcher, previousOperator);
		}

		// Push everything to the output queue
		while (!state.getOperators().isEmpty()) {
			Operator operator = state.getOperators().pop();

			if (operator.getClosingString() != null) {
				throw new IllegalStateException("Unexpected end of expression, expecting \"" + operator.getClosingString() + "\" (unmatched \"" + operator.getString() + "\")");
			} else if (operator.has(Operator.X_RIGHT_EXPRESSIONS) && (previousOperator == null || !previousOperator.has(Operator.RIGHT_EXPRESSION | Operator.X_RIGHT_EXPRESSIONS))) { // Process multi-argument operators, but allow trailing commas
				operator = operator.addRightExpression();
			} else if (previousOperator != null && previousOperator.has(Operator.RIGHT_EXPRESSION)) {
				if (previousOperator.has(Operator.IGNORE_TRAILING)) {
					previousOperator = null;
					continue;
				} else {
					throw new IllegalStateException("Unexpected end of expression");
				}
			}

			state.getOperators().push(operator);
			processOperation(state);
		}

		if (state.getOperands().isEmpty()) {
			throw new IllegalStateException("Unexpected empty expression");
		}
	}

	/**
	 * Processes the method call. The operand stack will be updated with the results of evaluating the operator.
	 *
	 * @param state the parse state
	 * @param operator the method call operator
	 */
	private static void processMethodCall(final ExpressionParseState state, final Operator operator) {
		final int parameterCount = operator.getRightExpressions();

		// Check Object methods
		final Method method;

		switch (operator.getString()) {
			case "equals":
				method = OBJECT_EQUALS;
				break;
			case "getClass":
				method = OBJECT_GET_CLASS;
				break;
			case "hashCode":
				method = OBJECT_HASH_CODE;
				break;
			case "toString":
				method = OBJECT_TO_STRING;
				break;
			default:
				method = null;
				break;
		}

		if (method != null && parameterCount == method.getParameterCount()) {
			final MethodBuilder methodResult = state.getOperands().peek(parameterCount).builder;

			for (int i = 0; i < parameterCount; i++) {
				methodResult.append(state.getOperands().peek(parameterCount - 1 - i).toObject());
			}

			final Operand end = state.getOperands().pop(parameterCount + 3).pop();
			final Class<?> returnType = Operand.ALLOWED_TYPES.contains(method.getReturnType()) ? method.getReturnType() : Object.class;

			if (end != null) { // Safe navigation, so the result must be object
				state.getOperands().push(new Operand(Object.class, new Operand(returnType, methodResult.addInvoke(method)).toObject().append(end.builder)));
			} else {
				state.getOperands().push(new Operand(returnType, methodResult.addInvoke(method)));
			}

			return;
		}

		// Create the identifier, then get and invoke the appropriate method
		final int index = state.getIdentifiers().getOrAdd(state.getCachedIdentifier(new Identifier(operator.getString(), parameterCount)));
		final MethodBuilder methodResult = state.getOperands().peek(parameterCount + 1).builder.addCode(LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP);

		if (parameterCount == 0) {
			methodResult.addCode(ACONST_NULL);
		} else {
			methodResult.pushNewObject(Object.class, parameterCount);
		}

		// Convert all parameters to objects and store them in the array
		for (int i = 0; i < parameterCount; i++) {
			methodResult.addCode(DUP).pushConstant(i).append(state.getOperands().peek(parameterCount - 1 - i).toObject()).addCode(AASTORE);
		}

		final MethodBuilder call = state.getOperands().pop(parameterCount + 2).pop().builder;
		final Operand end = state.getOperands().pop();
		state.getOperands().push(new Operand(Object.class, methodResult.append(call)));

		if (end != null) {
			state.getOperands().peek().builder.append(end.builder);
		}
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
		} else if (operator.has(Operator.CALL)) { // Check for a call
			processCall(state, operator);
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
						throw new IllegalStateException("Unexpected \"" + operator + "\" operator applied to " + subject.type.getName() + ", expecting list, map, set, string, or array type value");
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
							builder.addCode(DUP).append(first.toObject()).append(state.getOperands().peek(i + --pairs).toObject()).addInvoke(LINKED_HASH_MAP_PUT).addCode(POP);
						} else {
							builder.addCode(DUP).append(first.toObject()).addCode(DUP).addInvoke(LINKED_HASH_MAP_PUT).addCode(POP);
						}
					}

					state.getOperands().pop(operator.getRightExpressions() + totalPairs).push(new Operand(Object.class, builder));
				} else if ("[".equals(operator.getString())) { // Create a list
					final MethodBuilder builder = new MethodBuilder().pushNewObject(ArrayList.class).addCode(DUP).pushConstant(operator.getRightExpressions()).addInvoke(ARRAY_LIST_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						builder.addCode(DUP).append(state.getOperands().peek(i).toObject()).addInvoke(ARRAY_LIST_ADD).addCode(POP);
					}

					state.getOperands().pop(operator.getRightExpressions()).push(new Operand(Object.class, builder));
				} else { // Create a set
					final MethodBuilder builder = new MethodBuilder().pushNewObject(LinkedHashSet.class).addCode(DUP).pushConstant((operator.getRightExpressions() * 4 + 2) / 3).addInvoke(LINKED_HASH_SET_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						builder.addCode(DUP).append(state.getOperands().peek(i).toObject()).addInvoke(LINKED_HASH_SET_ADD).addCode(POP);
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
					state.getOperands().push(left).peek().builder.append(right.toObject()).addInvoke(OPERANDS_REQUIRE_NON_NULL_TO_STRING).addInvoke(STRING_BUILDER_APPEND_STRING);
				} else if (String.class.equals(left.type) || String.class.equals(right.type) || StringBuilder.class.equals(right.type)) {
					state.getOperands().push(new Operand(StringBuilder.class, left.toObject().pushNewObject(StringBuilder.class).addCode(DUP_X1, SWAP).addInvoke(STRING_VALUE_OF).addInvoke(STRING_BUILDER_INIT_STRING).append(right.toObject()).addInvoke(OPERANDS_REQUIRE_NON_NULL_TO_STRING).addInvoke(STRING_BUILDER_APPEND_STRING)));
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
			case "??": {
				final Label end = left.builder.newLabel();

				state.getOperands().push(new Operand(Object.class, left.toObject().addCode(DUP).addBranch(IFNONNULL, end).addCode(POP).append(right.toObject()).updateLabel(end)));
				break;
			}
			case "?:": {
				final Label end = left.builder.newLabel();

				state.getOperands().push(new Operand(Object.class, new Operand(Object.class, left.toObject().addCode(DUP)).toBoolean().addBranch(IFNE, end).addCode(POP).append(right.toObject()).updateLabel(end)));
				break;
			}

			case "!?": {
				final Label end = left.builder.newLabel();

				state.getOperands().push(new Operand(Object.class, left.toObject().addCode(DUP).addBranch(IFNULL, end).addCode(POP).append(right.toObject()).updateLabel(end)));
				break;
			}
			case "!:": {
				final Label end = left.builder.newLabel();

				state.getOperands().push(new Operand(Object.class, new Operand(Object.class, left.toObject().addCode(ACONST_NULL, SWAP)).toBoolean().addBranch(IFEQ, end).addCode(POP).append(right.toObject()).updateLabel(end)));
				break;
			}

			case "?": {
				if (!Entry.class.equals(right.type)) {
					throw new IllegalStateException("Incomplete ternary operator, missing \":\"");
				}

				assert !state.getOperands().isEmpty();

				final Label isFalse = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				state.getOperands().push(new Operand(Object.class, state.getOperands().pop().toBoolean().addBranch(IFEQ, isFalse).append(left.builder).addGoto(end, 1).updateLabel(isFalse).append(right.builder).updateLabel(end)));
				break;
			}

			case ":":
				state.getOperands().push(new Operand(Entry.class, left.toObject()));

				if (countTernaries(state, operator.getPrecedence()) > 0) { // Process everything up to the ternary
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
				final Label startOfLoop = new Label();
				final Label endOfLoop = new Label();

				state.getOperands().push(new Operand(Object.class, left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addCode(DUP).addInvoke(STREAMABLE_STREAM).updateLabel(startOfLoop).addCode(DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP2).addInvoke(ITERATOR_NEXT).addAccess(ASTORE, operator.getLocalBindingIndex())
						.append(right.toObject()).addInvoke(STREAMABLE_ADD).addGoto(startOfLoop, 0).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#|": { // Flat remap
				final Label startOfLoop = new Label();
				final Label endOfLoop = new Label();
				final Label notNull = new Label();
				final Label notIterable = new Label();
				final Label notArray = new Label();

				state.getOperands().push(new Operand(Object.class, left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addCode(DUP).addInvoke(STREAMABLE_STREAM).updateLabel(startOfLoop).addCode(DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP2).addInvoke(ITERATOR_NEXT).addAccess(ASTORE, operator.getLocalBindingIndex())
						.append(right.toObject()).addCode(DUP).addBranch(IFNONNULL, notNull).addCode(POP2).addGoto(startOfLoop, -2)
						.updateLabel(notNull).addCode(DUP).addInstanceOfCheck(Iterable.class).addBranch(IFEQ, notIterable).addCast(Iterable.class).addInvoke(STREAMABLE_FLAT_ADD_ITERABLE).addGoto(startOfLoop, -2)
						.updateLabel(notIterable).addCode(DUP).addInstanceOfCheck(Object[].class).addBranch(IFEQ, notArray).addCast(Object[].class).addInvoke(STREAMABLE_FLAT_ADD_ARRAY).addGoto(startOfLoop, -2)
						.updateLabel(notArray).addThrow(IllegalArgumentException.class, "Illegal flat map result, must be null, Iterable, or array", 2).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#?": { // Filter
				final Label startOfLoop = new Label();
				final Label readdObject = new Label();
				final Label endOfLoop = new Label();

				state.getOperands().push(new Operand(Object.class, left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addCode(DUP).addInvoke(STREAMABLE_STREAM).updateLabel(startOfLoop).addCode(DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP2).addInvoke(ITERATOR_NEXT).addCode(DUP).addAccess(ASTORE, operator.getLocalBindingIndex())
						.append(right.toBoolean()).addBranch(IFNE, readdObject).addCode(POP2).addGoto(startOfLoop, -2)
						.updateLabel(readdObject).addInvoke(STREAMABLE_ADD).addGoto(startOfLoop, 0).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#<": { // Reduction
				final Label startOfLoop = new Label();
				final Label endOfLoop = new Label();

				state.getOperands().push(new Operand(Object.class, left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addInvoke(STREAMABLE_STREAM).addCode(ACONST_NULL).updateLabel(startOfLoop).addCode(SWAP, DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP).addInvoke(ITERATOR_NEXT).addAccess(ASTORE, operator.getLocalBindingIndex()).addCode(SWAP, POP)
						.append(right.toObject()).addGoto(startOfLoop, 0).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#^": // Return
				state.getOperands().push(new Operand(Object.class, addReturn(new MethodBuilder(), state, right).addCode(ACONST_NULL)));
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
					state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).pushConstant(right.identifier.getName()).addInvoke(OPERANDS_GET_CLASS)));
				} else {
					final Label isNull = right.builder.newLabel();
					state.getOperands().push(new Operand(Object.class, right.toObject().addCode(DUP).addBranch(IFNULL, isNull).addInvoke(OBJECT_TO_STRING).addCode(LOAD_CONTEXT).addCode(SWAP).addInvoke(OPERANDS_GET_CLASS).updateLabel(isNull)));
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

			// Check for operators with multiple comma-separated values (collections, method calls, etc.)
			if (!state.getOperators().isEmpty() && state.getOperators().peek().has(Operator.X_RIGHT_EXPRESSIONS) && ",".equals(token)) {
				state.getOperators().push(state.getOperators().pop().addRightExpression());
				return operator;
			} else if (state.getOperators().isEmpty() && state.isCall()) {
				throw new IllegalStateException("Unexpected \"" + token + "\", expecting end of expression (index " + state.getIndex(matcher) + ")");
			} else if (operator.has(Operator.TRAILING_IDENTIFIER)) { // Check for streaming operations w/ 'x -> ...'
				matcher.region(matcher.end(), matcher.regionEnd()).usePattern(TRAILING_IDENTIFIER_PATTERN).lookingAt();
				final String name = matcher.group("name");

				if (name != null) {
					state.getOperators().push(operator.withLocalBindingIndex(FIRST_LOCAL + state.getLocalBindings().getOrAdd(name)));
					return operator;
				}
			} else if (state.getOperators().isEmpty()) {
				state.setCallName(null);
			}

			state.getOperators().push(operator);
			return operator;
		} else if (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION) || lastOperator.getString().startsWith(":")) { // Check if this token ends an expression on the stack
			if (lastOperator != null && !lastOperator.has(Operator.CALL) && lastOperator.getString().startsWith(":")) { // ":" can have an optional right expression
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
						throw new IllegalStateException("Unexpected \"" + token + "\", expecting \"" + closedOperator.getClosingString() + "\" (index " + state.getIndex(matcher) + ")");
					}

					state.getOperators().push(closedOperator);
					processOperation(state);
					return closedOperator.getClosingOperator();
				}

				state.getOperators().push(closedOperator);
				processOperation(state);
			}
		}

		throw new IllegalStateException("Unexpected \"" + token + "\" (index " + state.getIndex(matcher) + ")");
	}

	/**
	 * Creates a new expression.
	 *
	 * @param location the location of the expression
	 * @param state the parse state for the expression
	 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading the expression
	 */
	static Expression create(final Object location, final ExpressionParseState state) throws ReflectiveOperationException {
		final MethodBuilder mb = new MethodBuilder();
		String expressionName = null;

		if (".".equals(state.getExpressionString())) {
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_PEEK).addCast(SectionRenderData.class).addFieldAccess(SECTION_RENDER_DATA_DATA, true)));
		} else if (!state.getExtensions().contains(Extension.EXPRESSIONS)) {
			final String[] names = Pattern.compile("\\s*[.]\\s*", Pattern.UNICODE_CHARACTER_CLASS).split(state.getExpressionString(), -1);

			// Push a new operand formed by invoking identifiers[index].getValue(context, backreach, access)
			state.getOperands().push(new Operand(Object.class, new MethodBuilder().addCode(LOAD_IDENTIFIERS).pushConstant(state.getIdentifiers().getOrAdd(state.getCachedIdentifier(new Identifier(names[0])))).addCode(AALOAD).addCode(LOAD_CONTEXT).pushConstant(Identifier.UNSTATED_BACKREACH).addInvoke(IDENTIFIER_FIND_VALUE)));

			// Load the identifiers and invoke identifiers[index].getValue(object, null, false)
			for (int i = 1; i < names.length; i++) {
				state.getOperands().peek().builder.addCode(LOAD_IDENTIFIERS).pushConstant(state.getIdentifiers().getOrAdd(state.getCachedIdentifier(new Identifier(names[i])))).addCode(AALOAD, SWAP, ACONST_NULL).pushConstant(false).addInvoke(IDENTIFIER_GET_VALUE);
			}
		} else { // Tokenize the entire expression, using the shunting yard algorithm
			int initializeBindingsStart = 0;
			final int end = state.getExpressionString().length();
			final Matcher matcher = NAMED_EXPRESSION_PATTERN.matcher(state.getExpressionString());
			Operator lastOperator = START_OPERATOR;

			if (matcher.lookingAt()) {
				if (matcher.group("assignment") != null) {
					state.setBindingName(matcher.group("name"));
					state.setEvaluation(Evaluation.EVALUATE);
				} else {
					expressionName = matcher.group("name");
					state.setEvaluation(Evaluation.NO_EVALUATION);
					parseNamedExpressionSignature(state, mb, matcher);
					initializeBindingsStart = state.getLocalBindings().size();

					if (state.getNamedExpressions().putIfAbsent(expressionName, EXPRESSION_BEING_CREATED) != null) {
						throw new IllegalStateException("Expression \"" + expressionName + "\" is already defined");
					}
				}

				matcher.region(matcher.end(), end);
			} else if (state.isCall()) {
				lastOperator = Operator.createCall("", false);
				state.getOperators().push(lastOperator);
				matcher.region(1, end);
			}

			// Process the expression
			processExpression(state, matcher, lastOperator, end);

			// Initialize all local bindings to null
			for (int i = initializeBindingsStart; i < state.getLocalBindings().size(); i++) {
				mb.addCode(ACONST_NULL).addAccess(ASTORE, FIRST_LOCAL + i);
			}
		}

		addReturn(mb, state, state.getOperands().pop());
		assert state.getOperands().isEmpty();
		final Expression cachedExpression;

		synchronized (cache) {
			final String name = className();
			final BytecodeContainer bytecode = mb.build(name, Evaluable.class);
			cachedExpression = cache.get(bytecode);

			if (cachedExpression == null) {
				final Expression expression = new Expression(location, expressionName, state, null,
						MethodBuilder.<Evaluable>createClass(name, Expression.class.getClassLoader(), Expression.class.getProtectionDomain(), bytecode.getBytecode()).getConstructor().newInstance());

				cache.put(bytecode, expression);
				cacheIndex++;
				return expression;
			}
		}

		// Check if any other items in the cached expression match
		return new Expression(location, expressionName, state, cachedExpression, cachedExpression.evaluable);
	}

	/**
	 * Creates a new empty expression that cannot be evaluated.
	 */
	private Expression() {
		this.location = null;
		this.originalString = "";
		this.name = null;
		this.callName = null;
		this.expressions = EMPTY_EXPRESSIONS;
		this.identifiers = EMPTY_IDENTIFIERS;
		this.evaluable = null;
	}

	/**
	 * Creates a new expression from a location, an optional expression name, a parse state, an optional cached expression to use for loading, and an evaluable object.
	 *
	 * @param location the location of the expression
	 * @param expressionName the optional name of the expression
	 * @param state the parse state of the expression
	 * @param cache the optional cached expression
	 * @param evaluable the evaluable associated with the expression
	 */
	private Expression(final Object location, final String expressionName, final ExpressionParseState state, final Expression cache, final Evaluable evaluable) {
		this.location = location;
		this.originalString = state.getExpressionString();
		this.name = expressionName;
		this.callName = state.getCallName();
		this.expressions = (cache != null && state.getExpressions().equalsArray(cache.expressions) ? cache.expressions : state.getExpressions().toArray(Expression.class, EMPTY_EXPRESSIONS));
		this.identifiers = (cache != null && state.getIdentifiers().equalsArray(cache.identifiers) ? cache.identifiers : state.getIdentifiers().toArray(Identifier.class, EMPTY_IDENTIFIERS));
		this.evaluable = evaluable;

		// Update self-expression reference
		if (expressionName != null) {
			state.getNamedExpressions().put(expressionName, this);

			for (int i = 0; i < expressions.length; i++) {
				if (expressions[i] == EXPRESSION_BEING_CREATED) {
					expressions[i] = this;
					break;
				}
			}
		}
	}

	/**
	 * Gets the call name of the expression.
	 *
	 * @return the call name of the expression
	 */
	String getCallName() {
		return callName;
	}

	/**
	 * Gets the name of the expression.
	 *
	 * @return the name of the expression
	 */
	String getName() {
		return name;
	}

	@Override
	public boolean equals(final Object obj) {
		return this == obj ||
			(obj instanceof Expression && originalString.equals(((Expression) obj).originalString) && Objects.equals(location, ((Expression) obj).location));
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
			context.getSettings().getErrorHandler().onError(e, () -> "Failed to evaluate expression \"" + originalString + "\" (" + location + (e.getMessage() == null ? ")" : "): " + e.getMessage()), this);
		}

		return null;
	}

	/**
	 * Gets the evaluable used by the expression.
	 *
	 * @return the evaluable used by the expression
	 */
	Evaluable getEvaluable() {
		return evaluable;
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
