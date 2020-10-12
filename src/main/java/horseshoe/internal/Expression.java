package horseshoe.internal;

import static horseshoe.internal.MethodBuilder.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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
import horseshoe.SectionRenderer;
import horseshoe.Stack;
import horseshoe.internal.MethodBuilder.Label;

public final class Expression {

	private static final AtomicInteger DYN_INDEX = new AtomicInteger();
	private static final int FIRST_LOCAL_BINDING_INDEX = Evaluable.FIRST_LOCAL_INDEX + 2; // The first few indices are reserved for use internally by this class

	// Reflected Methods
	private static final Constructor<?> ARRAY_LIST_CTOR_INT;
	private static final Method ACCESSOR_LOOKUP;
	private static final Method ACCESSOR_LOOKUP_RANGE;
	private static final Method EXPRESSION_EVALUATE;
	private static final Constructor<?> HALT_EXCEPTION_CTOR_STRING;
	private static final Method IDENTIFIER_FIND_VALUE;
	private static final Method IDENTIFIER_FIND_VALUE_METHOD;
	private static final Method IDENTIFIER_GET_ROOT_VALUE;
	private static final Method IDENTIFIER_GET_VALUE;
	private static final Method IDENTIFIER_GET_VALUE_METHOD;
	private static final Method ITERABLE_ITERATOR;
	private static final Method ITERATOR_HAS_NEXT;
	private static final Method ITERATOR_NEXT;
	private static final Constructor<?> LINKED_HASH_MAP_CTOR_INT;
	private static final Constructor<?> LINKED_HASH_SET_CTOR_INT;
	private static final Method LIST_ADD;
	private static final Method MAP_PUT;
	private static final Method OBJECT_TO_STRING;
	private static final Method OPERANDS_ADD;
	private static final Method OPERANDS_AND;
	private static final Method OPERANDS_COMPARE;
	private static final Method OPERANDS_DIVIDE;
	private static final Method OPERANDS_EXPONENTIATE;
	private static final Method OPERANDS_FIND_PATTERN;
	private static final Method OPERANDS_GET_CLASS;
	private static final Method OPERANDS_MATCHES_PATTERN;
	private static final Method OPERANDS_MULTIPLY;
	private static final Method OPERANDS_MODULO;
	private static final Method OPERANDS_NEGATE;
	private static final Method OPERANDS_NOT;
	private static final Method OPERANDS_OR;
	private static final Method OPERANDS_SHIFT_LEFT;
	private static final Method OPERANDS_SHIFT_RIGHT;
	private static final Method OPERANDS_SHIFT_RIGHT_ZERO;
	private static final Method OPERANDS_SUBTRACT;
	private static final Method OPERANDS_XOR;
	private static final Method PATTERN_COMPILE;
	private static final Method RENDER_CONTEXT_GET_INDENTATION;
	private static final Method RENDER_CONTEXT_GET_SECTION_DATA;
	private static final Method RENDER_CONTEXT_GET_SECTION_RENDERERS;
	private static final Method SECTION_RENDERER_GET_INDEX;
	private static final Method SECTION_RENDERER_HAS_NEXT;
	private static final Method SET_ADD;
	private static final Method STACK_PEEK;
	private static final Method STACK_PEEK_BASE;
	private static final Method STACK_POP;
	private static final Method STACK_PUSH_OBJECT;
	private static final Method STREAMABLE_ADD;
	private static final Method STREAMABLE_FLAT_ADD_ARRAY;
	private static final Method STREAMABLE_FLAT_ADD_ITERABLE;
	private static final Method STREAMABLE_OF_UNKNOWN;
	private static final Method STRING_BUILDER_APPEND_OBJECT;
	private static final Constructor<?> STRING_BUILDER_INIT_STRING;
	private static final Method STRING_VALUE_OF;

	// The patterns used for parsing the grammar
	private static final Pattern COMMENT_PATTERN = Pattern.compile("(?:/(?s:/[^\\n\\x0B\\x0C\\r\\u0085\\u2028\\u2029]*|[*].*?[*]/)\\s*)", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("(?<double>[-+]Infinity|[-+]?(?:[0-9]+[fFdD]|(?:[0-9]+[.]?[eE][-+]?[0-9]+|[0-9]+[.][0-9]+(?:[eE][-+]?[0-9]+)?|0[xX](?:[0-9A-Fa-f]+[.]?|[0-9A-Fa-f]+[.][0-9A-Fa-f]+)[pP][-+]?[0-9]+)[fFdD]?))\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(?<identifier>" + Identifier.PATTERN + "|`(?:[^`]|``)+`|[.][.]|[.])\\s*" + COMMENT_PATTERN + "*(?<isMethod>[(])?\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_WITH_PREFIX_PATTERN;
	private static final Pattern LONG_PATTERN = Pattern.compile("(?:(?<hexsign>[-+]?)0[xX](?<hexadecimal>[0-9A-Fa-f]+)|(?<decimal>[-+]?[0-9]+))(?<isLong>[lL])?\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern NAMED_EXPRESSION_PARAMETER_PATTERN = Pattern.compile("(?:,\\s*" + COMMENT_PATTERN + "*(?:(?<parameter>" + Identifier.PATTERN + ")\\s*" + COMMENT_PATTERN + "*)?)", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern NAMED_EXPRESSION_PATTERN = Pattern.compile(COMMENT_PATTERN + "*(?<name>" + Identifier.PATTERN + ")\\s*" + COMMENT_PATTERN + "*(?:[(]\\s*" + COMMENT_PATTERN + "*(?:(?<firstParameter>[.]|" + Identifier.PATTERN + ")\\s*" + COMMENT_PATTERN + "*)?(?<remainingParameters>" + NAMED_EXPRESSION_PARAMETER_PATTERN + "+)?[)]\\s*" + COMMENT_PATTERN + "*)?[-=]>\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern OPERATOR_PATTERN;
	private static final Pattern REGEX_PATTERN = Pattern.compile("~/(?<nounicode>\\(\\?-U\\))?(?<regex>(?:[^/\\\\]|\\\\.)*)/\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern STRING_PATTERN = Pattern.compile("(?:\"(?<string>(?:[^\"\\\\]|\\\\[\\\\\"'btnfr{}0]|\\\\x[0-9A-Fa-f]|\\\\u[0-9A-Fa-f]{4}|\\\\U[0-9A-Fa-f]{8})*)\"|'(?<unescapedString>(?:[^']|'')*)')\\s*", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern TRAILING_IDENTIFIER_PATTERN = Pattern.compile(COMMENT_PATTERN + "*(?<name>" + Identifier.PATTERN + ")\\s*" + COMMENT_PATTERN + "*[-=]>\\s*", Pattern.UNICODE_CHARACTER_CLASS);

	private static final byte[] CHAR_VALUE = new byte[] {
			127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
			127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127,   0,   1,   2,   3,   4,   5,   6,   7,   8,   9, 127, 127, 127, 127, 127, 127,
			127,  10,  11,  12,  13,  14,  15,  16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,  32,  33,  34,  35, 127, 127, 127, 127, 127,
			127,  10,  11,  12,  13,  14,  15,  16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,  32,  33,  34,  35, 127, 127, 127, 127, 127
	};

	private final Object location;
	private final String originalString;
	private final Expression[] expressions;
	private final Identifier[] identifiers;
	private final Evaluable evaluable;
	private final boolean isNamed;

	private static final class ParseState {
		final int startIndex;
		final String trimmedString;
		final Map<String, Expression> namedExpressions;
		final Map<Expression, Integer> expressions = new LinkedHashMap<>();
		final Map<Identifier, Integer> identifiers = new LinkedHashMap<>();
		final Map<String, Integer> localBindings = new LinkedHashMap<>();
		final Stack<Operand> operands = new Stack<>();
		final Stack<Operator> operators = new Stack<>();

		private ParseState(final int startIndex, final String trimmedString, final Map<String, Expression> namedExpressions) {
			this.startIndex = startIndex;
			this.trimmedString = trimmedString;
			this.namedExpressions = namedExpressions;
		}

		/**
		 * Creates a local binding if one does not exist and returns the resulting local index.
		 *
		 * @param name the name of the binding
		 * @return the local index of the binding
		 */
		int createLocalBinding(final String name) {
			Integer index = localBindings.get(name);

			if (index == null) {
				index = FIRST_LOCAL_BINDING_INDEX + localBindings.size();
				localBindings.put(name, index);
			}

			return index;
		}

		/**
		 * Gets the index for the specified identifier. If the identifier does not exist in the map, a new entry will be created and that index will be returned.
		 *
		 * @param identifier the identifier to locate
		 * @return the index of the specified identifier
		 */
		int getIdentifierIndex(final Identifier identifier) {
			Integer index = identifiers.get(identifier);

			if (index == null) {
				index = identifiers.size();
				identifiers.put(identifier, index);
			}

			return index;
		}

		/**
		 * Gets a local binding index. If one does not exist with the specified name, returns null.
		 *
		 * @param name the name of the binding
		 * @return the local index of the binding, or null if one does not exist
		 */
		Integer getLocalBinding(final String name) {
			return localBindings.get(name);
		}

		/**
		 * Gets the index of the matcher within the tag.
		 *
		 * @return the index of the matcher within the tag
		 */
		int getIndex(final Matcher matcher) {
			return startIndex + matcher.regionStart();
		}

		/**
		 * Initializes the local bindings for this expression.
		 *
		 * @param mb the method builder used to initialize the local bindings
		 * @param start the starting index where initialization begins
		 */
		public void initializeLocalBindings(final MethodBuilder mb, final int start) {
			for (int i = start; i < localBindings.size(); i++) {
				mb.addCode(ACONST_NULL).addAccess(ASTORE, FIRST_LOCAL_BINDING_INDEX + i);
			}
		}
	}

	public abstract static class Evaluable {
		private static final byte LOAD_EXPRESSIONS = ALOAD_1;
		private static final byte LOAD_IDENTIFIERS = ALOAD_2;
		private static final byte LOAD_CONTEXT = ALOAD_3;
		private static final byte[] LOAD_ARGUMENTS = new byte[] { ALOAD, 4 };
		private static final int FIRST_LOCAL_INDEX = 5;

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
		try {
			ARRAY_LIST_CTOR_INT = ArrayList.class.getConstructor(int.class);
			ACCESSOR_LOOKUP = Accessor.class.getMethod("lookup", Object.class, Object.class);
			ACCESSOR_LOOKUP_RANGE = Accessor.class.getMethod("lookupRange", Object.class, Object.class, Object.class);
			EXPRESSION_EVALUATE = Expression.class.getMethod("evaluate", RenderContext.class, Object[].class);
			HALT_EXCEPTION_CTOR_STRING = HaltRenderingException.class.getConstructor(String.class);
			IDENTIFIER_FIND_VALUE = Identifier.class.getMethod("findValue", RenderContext.class, int.class);
			IDENTIFIER_FIND_VALUE_METHOD = Identifier.class.getMethod("findValue", RenderContext.class, int.class, Object[].class);
			IDENTIFIER_GET_ROOT_VALUE = Identifier.class.getMethod("getRootValue", RenderContext.class);
			IDENTIFIER_GET_VALUE = Identifier.class.getMethod("getValue", Object.class);
			IDENTIFIER_GET_VALUE_METHOD = Identifier.class.getMethod("getValue", Object.class, Object[].class);
			ITERABLE_ITERATOR = Iterable.class.getMethod("iterator");
			ITERATOR_HAS_NEXT = Iterator.class.getMethod("hasNext");
			ITERATOR_NEXT = Iterator.class.getMethod("next");
			LINKED_HASH_MAP_CTOR_INT = LinkedHashMap.class.getConstructor(int.class);
			LINKED_HASH_SET_CTOR_INT = LinkedHashSet.class.getConstructor(int.class);
			LIST_ADD = List.class.getMethod("add", Object.class);
			MAP_PUT = Map.class.getMethod("put", Object.class, Object.class);
			OBJECT_TO_STRING = Object.class.getMethod("toString");
			OPERANDS_ADD = Operands.class.getMethod("add", Object.class, Object.class);
			OPERANDS_AND = Operands.class.getMethod("and", Number.class, Number.class);
			OPERANDS_COMPARE = Operands.class.getMethod("compare", boolean.class, Object.class, Object.class);
			OPERANDS_DIVIDE = Operands.class.getMethod("divide", Number.class, Number.class);
			OPERANDS_EXPONENTIATE = Operands.class.getMethod("exponentiate", Number.class, Number.class);
			OPERANDS_FIND_PATTERN = Operands.class.getMethod("findPattern", Object.class, Object.class);
			OPERANDS_GET_CLASS = Operands.class.getMethod("getClass", RenderContext.class, String.class);
			OPERANDS_MATCHES_PATTERN = Operands.class.getMethod("matchesPattern", Object.class, Object.class);
			OPERANDS_MULTIPLY = Operands.class.getMethod("multiply", Number.class, Number.class);
			OPERANDS_MODULO = Operands.class.getMethod("modulo", Number.class, Number.class);
			OPERANDS_NEGATE = Operands.class.getMethod("negate", Number.class);
			OPERANDS_NOT = Operands.class.getMethod("not", Number.class);
			OPERANDS_OR = Operands.class.getMethod("or", Number.class, Number.class);
			OPERANDS_SHIFT_LEFT = Operands.class.getMethod("shiftLeft", Number.class, Number.class);
			OPERANDS_SHIFT_RIGHT = Operands.class.getMethod("shiftRight", Number.class, Number.class);
			OPERANDS_SHIFT_RIGHT_ZERO = Operands.class.getMethod("shiftRightZero", Number.class, Number.class);
			OPERANDS_SUBTRACT = Operands.class.getMethod("subtract", Object.class, Object.class);
			OPERANDS_XOR = Operands.class.getMethod("xor", Number.class, Number.class);
			PATTERN_COMPILE = Pattern.class.getMethod("compile", String.class, int.class);
			RENDER_CONTEXT_GET_INDENTATION = RenderContext.class.getMethod("getIndentation");
			RENDER_CONTEXT_GET_SECTION_DATA = RenderContext.class.getMethod("getSectionData");
			RENDER_CONTEXT_GET_SECTION_RENDERERS = RenderContext.class.getMethod("getSectionRenderers");
			SECTION_RENDERER_GET_INDEX = SectionRenderer.class.getMethod("getIndex");
			SECTION_RENDERER_HAS_NEXT = SectionRenderer.class.getMethod("hasNext");
			SET_ADD = Set.class.getMethod("add", Object.class);
			STACK_PEEK = Stack.class.getMethod("peek", int.class);
			STACK_PEEK_BASE = Stack.class.getMethod("peekBase");
			STACK_POP = Stack.class.getMethod("pop");
			STACK_PUSH_OBJECT = Stack.class.getMethod("push", Object.class);
			STREAMABLE_ADD = Streamable.class.getMethod("add", Object.class);
			STREAMABLE_FLAT_ADD_ARRAY = Streamable.class.getMethod("flatAdd", Object[].class);
			STREAMABLE_FLAT_ADD_ITERABLE = Streamable.class.getMethod("flatAdd", Iterable.class);
			STREAMABLE_OF_UNKNOWN = Streamable.class.getMethod("ofUnknown", Object.class);
			STRING_BUILDER_APPEND_OBJECT = StringBuilder.class.getMethod("append", Object.class);
			STRING_BUILDER_INIT_STRING = StringBuilder.class.getConstructor(String.class);
			STRING_VALUE_OF = String.class.getMethod("valueOf", Object.class);
		} catch (final ReflectiveOperationException e) {
			throw new ExceptionInInitializerError("Failed to get required class member: " + e.getMessage());
		}
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
		IDENTIFIER_WITH_PREFIX_PATTERN = Pattern.compile("(?:(?<prefix>[/\\\\])|(?<backreach>(?:[.][.][/\\\\])+)|(?<current>[.][/\\\\])?)(?<internal>[.](?![.]))?" + IDENTIFIER_PATTERN + "(?=" + COMMENT_PATTERN + "*(?<assignment>" + assignmentOperators.substring(1) + ")(?:[^=]|$))?", Pattern.UNICODE_CHARACTER_CLASS);
		OPERATOR_PATTERN = Pattern.compile("(?<operator>" + allOperators.append(",)\\s*").toString(), Pattern.UNICODE_CHARACTER_CLASS);
	}

	/**
	 * Checks if a collection contains the same items as an array.
	 *
	 * @param <T> the type of item being compared
	 * @param list1 the collection of items to compare
	 * @param list2 the array of items to compare
	 * @return true if list1 is empty and list2 is null or if the lists contain the same items in the same order, otherwise false
	 */
	private static <T> boolean equalLists(final Collection<T> list1, final T[] list2) {
		if (list2 == null) {
			return list1.isEmpty();
		}

		final int size = list1.size();
		int i = 0;

		if (size != list2.length) {
			return false;
		}

		for (final Iterator<T> it = list1.iterator(); it.hasNext(); i++) {
			if (!it.next().equals(list2[i])) {
				return false;
			}
		}

		return true;
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
	private static Operator parseIdentifier(final ParseState state, final Matcher matcher, final Operator lastOperator, final boolean lastNavigation) {
		final String identifierText = matcher.group("identifier");
		final boolean isMethod = matcher.group("isMethod") != null;
		int backreach = 0;
		boolean isRoot = false;
		boolean unstatedBackreach = false;
		boolean isInternal = false;

		// Check for additional identifier properties
		if (!lastNavigation) {
			isInternal = matcher.group("internal") != null;
			final String prefixString = matcher.group("prefix");

			if (prefixString != null) { // Currently only '/' and '\' are allowed as prefixes
				isRoot = true;

				if (isInternal || isMethod || (identifierText.startsWith(".") && !".".equals(identifierText))) {
					throw new IllegalArgumentException("Invalid identifier with prefix \"" + prefixString + "\" (index " + state.getIndex(matcher) + ")");
				}
			} else {
				final String backreachString = matcher.group("backreach");

				if (backreachString != null) {
					backreach = backreachString.length() / 3;
				} else if (matcher.group("current") == null) {
					// Check for literals
					switch (identifierText) {
						case "true":
							state.operands.push(new Operand(boolean.class, new MethodBuilder().pushConstant(1)));
							return null;
						case "false":
							state.operands.push(new Operand(boolean.class, new MethodBuilder().pushConstant(0)));
							return null;
						case "null":
							state.operands.push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
							return null;
						default:
							unstatedBackreach = true;
							break;
					}
				}
			}
		}

		// Process the identifier
		if (".".equals(identifierText)) {
			if (isRoot) {
				state.operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_PEEK_BASE)));
			} else {
				state.operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach).addInvoke(STACK_PEEK)));
			}
		} else if ("..".equals(identifierText)) {
			state.operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(backreach + 1).addInvoke(STACK_PEEK)));
		} else if (isInternal) { // Everything that starts with "." is considered internal
			switch (identifierText) {
				case "hasNext":
					state.operands.push(new Operand(boolean.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_RENDERERS).pushConstant(backreach).addInvoke(STACK_PEEK).addCast(SectionRenderer.class).addInvoke(SECTION_RENDERER_HAS_NEXT)));
					break;
				case "indentation":
					state.operands.push(new Operand(String.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_INDENTATION).pushConstant(backreach).addInvoke(STACK_PEEK)));
					break;
				case "index":
					state.operands.push(new Operand(int.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_RENDERERS).pushConstant(backreach).addInvoke(STACK_PEEK).addCast(SectionRenderer.class).addInvoke(SECTION_RENDERER_GET_INDEX)));
					break;
				case "isFirst":
					state.operands.push(new Operand(int.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_RENDERERS).pushConstant(backreach).addInvoke(STACK_PEEK).addCast(SectionRenderer.class).addInvoke(SECTION_RENDERER_GET_INDEX)));
					state.operators.push(Operator.get("!", false));
					processOperation(state);
					break;
				default:
					state.operands.push(new Operand(Object.class, new MethodBuilder().addCode(ACONST_NULL)));
					break;
			}
		} else if (isMethod) { // Process the method call
			final String name = identifierText.startsWith("`") ? identifierText.substring(1, identifierText.length() - 1).replace("``", "`") : identifierText;

			if (lastNavigation) {
				// Create a new output formed by invoking identifiers[index].getValue(object, ...)
				final MethodBuilder objectBuilder = state.operands.pop().toObject();
				final Label skipFunc = objectBuilder.newLabel();

				state.operands.push(new Operand(Object.class, new MethodBuilder().addInvoke(IDENTIFIER_GET_VALUE_METHOD).updateLabel(skipFunc)));

				if (lastOperator.has(Operator.SAFE)) {
					state.operands.push(new Operand(Object.class, new MethodBuilder().addCode(SWAP)));
					state.operands.push(new Operand(Object.class, objectBuilder.addCode(DUP).addBranch(IFNULL, skipFunc)));
				} else {
					state.operands.push(new Operand(Object.class, objectBuilder));
					state.operands.push(new Operand(Object.class, new MethodBuilder()));
				}

				return state.operators.push(Operator.createMethod(name, true)).peek();
			} else {
				// Create a new output formed by invoking identifiers[index].findValue(context, backreach, ...)
				state.operands.push(new Operand(Object.class, new MethodBuilder().addInvoke(IDENTIFIER_FIND_VALUE_METHOD)));
				state.operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).pushConstant(unstatedBackreach ? Identifier.UNSTATED_BACKREACH : backreach)));
				state.operands.push(new Operand(Object.class, new MethodBuilder()));
				return state.operators.push(Operator.createMethod(name, !unstatedBackreach)).peek();
			}
		} else { // Process the identifier
			final String name = identifierText.startsWith("`") ? identifierText.substring(1, identifierText.length() - 1).replace("``", "`") : identifierText;

			if (lastNavigation) {
				final Identifier identifier = new Identifier(name);
				final int index = state.getIdentifierIndex(identifier);

				if (state.operators.pop().has(Operator.SAFE)) {
					// Create a new output formed by invoking identifiers[index].getValue(object)
					final Label end = state.operands.peek().builder.newLabel();
					state.operands.push(new Operand(Object.class, identifier, state.operands.pop().toObject().addCode(DUP).addBranch(IFNULL, end).addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE).updateLabel(end)));
				} else {
					state.operands.push(new Operand(Object.class, identifier, state.operands.pop().toObject().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE)));
				}
			} else {
				final Integer localBindingIndex = unstatedBackreach ? state.getLocalBinding(name) : null;
				final Operator operator = Operator.get(matcher.group("assignment"), true);

				// Look-ahead for an assignment operation
				if (operator != null && operator.has(Operator.ASSIGNMENT)) {
					if (!unstatedBackreach) {
						throw new IllegalArgumentException("Invalid assignment to non-local variable (index " + state.getIndex(matcher) + ")");
					}

					state.operands.push(new Operand(Object.class, new MethodBuilder().addAccess(ASTORE, state.createLocalBinding(name))));
				} else if (localBindingIndex != null) { // Check for a local binding
					state.operands.push(new Operand(Object.class, new MethodBuilder().addAccess(ALOAD, localBindingIndex)));
				} else { // Resolve the identifier
					final Identifier identifier = new Identifier(name);
					final int index = state.getIdentifierIndex(identifier);

					// Create a new output formed by invoking identifiers[index].getRootValue(context, access) or identifiers[index].findValue(context, backreach, access)
					if (isRoot) {
						state.operands.push(new Operand(Object.class, identifier, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).addInvoke(IDENTIFIER_GET_ROOT_VALUE)));
					} else {
						state.operands.push(new Operand(Object.class, identifier, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).pushConstant(unstatedBackreach ? Identifier.UNSTATED_BACKREACH : backreach).addInvoke(IDENTIFIER_FIND_VALUE)));
					}
				}
			}
		}

		return null;
	}

	/**
	 * Parse a named expression heading.
	 *
	 * @param state the parse state
	 * @param initializeLocalBindings the method builder used to initialize the local bindings
	 * @param matcher the matcher pointing to the named expression heading
	 */
	private static void parseNamedExpressionHeading(final ParseState state, final MethodBuilder initializeLocalBindings, final Matcher matcher) {
		final String firstParameter = matcher.group("firstParameter");
		final String remainingParameters = matcher.group("remainingParameters");

		if (firstParameter != null && !".".equals(firstParameter)) {
			initializeLocalBindings.addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(0).addInvoke(STACK_PEEK).addAccess(ASTORE, state.createLocalBinding(firstParameter));
		}

		if (remainingParameters == null) {
			return;
		}

		// Parse the parameters
		int i = 0;

		for (final Matcher parameterMatcher = NAMED_EXPRESSION_PARAMETER_PATTERN.matcher(remainingParameters); parameterMatcher.lookingAt(); i++, parameterMatcher.region(parameterMatcher.end(), remainingParameters.length())) {
			final String name = parameterMatcher.group("parameter");

			if (name != null) {
				if (state.getLocalBinding(name) != null) {
					throw new IllegalStateException("Duplicate parameter \"" + name + "\" in named expression");
				}

				// Load the local bindings
				final Label ifNull = initializeLocalBindings.newLabel();
				final Label end = initializeLocalBindings.newLabel();

				initializeLocalBindings.addCode(Evaluable.LOAD_ARGUMENTS).addBranch(IFNULL, ifNull).pushConstant(i).addCode(Evaluable.LOAD_ARGUMENTS).addCode(ARRAYLENGTH).addBranch(IF_ICMPGE, ifNull)
					.addCode(Evaluable.LOAD_ARGUMENTS).pushConstant(i).addCode(AALOAD).addGoto(end, 1)
					.updateLabel(ifNull).addCode(ACONST_NULL).updateLabel(end).addAccess(ASTORE, state.createLocalBinding(name));
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
	private static Operator parseToken(final ParseState state, final Matcher matcher, final Operator lastOperator) {
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
			state.operands.push(new Operand(double.class, new MethodBuilder().pushConstant(Double.parseDouble(matcher.group("double")))));
		} else if (!hasLeftExpression && matcher.usePattern(LONG_PATTERN).lookingAt()) { // Long literal
			final String decimal = matcher.group("decimal");
			final long value = decimal == null ? Long.parseLong(matcher.group("hexsign") + matcher.group("hexadecimal"), 16) : Long.parseLong(decimal);

			if (matcher.group("isLong") != null || (int)value != value) {
				state.operands.push(new Operand(long.class, new MethodBuilder().pushConstant(value)));
			} else {
				state.operands.push(new Operand(int.class, new MethodBuilder().pushConstant((int)value)));
			}
		} else if (!hasLeftExpression && matcher.usePattern(STRING_PATTERN).lookingAt()) { // String literal
			state.operands.push(new Operand(String.class, new MethodBuilder().pushConstant(parseStringLiteral(matcher))));
		} else if (!hasLeftExpression && matcher.usePattern(REGEX_PATTERN).lookingAt()) { // Regular expression literal ("nounicode" hack is to support broken Java runtime library implementations)
			state.operands.push(new Operand(Object.class, new MethodBuilder().pushConstant(Pattern.compile(matcher.group("regex")).pattern()).pushConstant(matcher.group("nounicode") == null ? Pattern.UNICODE_CHARACTER_CLASS : 0).addInvoke(PATTERN_COMPILE)));
		} else if (matcher.usePattern(OPERATOR_PATTERN).lookingAt()) { // Operator
			final String token = matcher.group("operator");
			final Operator operator = Operator.get(token, hasLeftExpression);

			if (operator != null) {
				// Shunting-yard Algorithm
				while (!state.operators.isEmpty() && !state.operators.peek().isContainer() && (state.operators.peek().getPrecedence() < operator.getPrecedence() || (state.operators.peek().getPrecedence() == operator.getPrecedence() && operator.isLeftAssociative()))) {
					processOperation(state);
				}

				if (!state.operators.isEmpty() && state.operators.peek().has(Operator.X_RIGHT_EXPRESSIONS) && ",".equals(token)) {
					state.operators.push(state.operators.pop().addRightExpression());
				} else if (operator.has(Operator.TRAILING_IDENTIFIER)) {
					if (!matcher.region(matcher.end(), matcher.regionEnd()).usePattern(TRAILING_IDENTIFIER_PATTERN).lookingAt()) {
						throw new IllegalArgumentException("Missing identifier after \"" + operator + "\" operator (index " + state.getIndex(matcher) + ")");
					}

					state.operators.push(operator.withLocalBindingIndex(state.createLocalBinding(matcher.group("name"))));
				} else {
					state.operators.push(operator);
				}

				return operator;
			} else if (lastOperator == null || !lastOperator.has(Operator.RIGHT_EXPRESSION)) { // Check if this token ends an expression on the stack
				while (!state.operators.isEmpty()) {
					Operator closedOperator = state.operators.pop();

					if (closedOperator.has(Operator.X_RIGHT_EXPRESSIONS) && hasLeftExpression) { // Process multi-argument operators
						closedOperator = closedOperator.addRightExpression();
					}

					if (closedOperator.getClosingString() != null) {
						if (!closedOperator.getClosingString().equals(token)) { // Check for a mismatched closing string
							throw new IllegalArgumentException("Unexpected \"" + token + "\", expecting \"" + closedOperator.getClosingString() + "\" (index " + state.getIndex(matcher) + ")");
						}

						state.operators.push(closedOperator);
						processOperation(state);
						return closedOperator.getClosingOperator();
					}

					state.operators.push(closedOperator);
					processOperation(state);
				}
			}

			throw new IllegalArgumentException("Unexpected \"" + token + "\" (index " + state.getIndex(matcher) + ")");
		} else {
			final String start = state.trimmedString.length() - matcher.regionStart() > 10 ? state.trimmedString.substring(matcher.regionStart(), matcher.regionStart() + 7) + "..." : state.trimmedString.substring(matcher.regionStart());
			throw new IllegalArgumentException("Unrecognized operand \"" + start + "\" (index " + state.getIndex(matcher) + ")");
		}

		return null;
	}

	/**
	 * Processes the specified operation. The operand stack will be updated with the results of evaluating the operator.
	 *
	 * @param state the parse state
	 */
	private static void processOperation(final ParseState state) {
		final Operator operator = state.operators.pop();
		final Operand right;

		if (operator.has(Operator.NAVIGATION)) { // Navigation operator is handled during parsing
			return;
		} else if (operator.has(Operator.METHOD_CALL)) { // Check for a method call
			processMethodCall(state, operator);
			return;
		} else if (operator.has(Operator.RIGHT_EXPRESSION)) {
			right = state.operands.pop();
			assert !Entry.class.equals(right.type) || operator.has(Operator.ALLOW_PAIRS) : Entry.class + " cannot be passed to operator";
		} else {
			right = null;
		}

		final Operand left = operator.has(Operator.LEFT_EXPRESSION) ? state.operands.pop() : null;

		switch (operator.getString()) {
			// List / Set / Array / Map / String Lookup Operations
			case "[":
			case "?[":
				if (left != null) {
					final Operand subject = Entry.class.equals(right.type) ? state.operands.pop() : left;

					if (!Object.class.equals(subject.type)) {
						throw new IllegalArgumentException("Unexpected \"" + operator + "\" operator applied to " + subject.type.getName() + ", expecting list, map, set, or array type value");
					}

					final Label end = subject.builder.newLabel();

					if (operator.has(Operator.SAFE)) {
						subject.builder.addCode(DUP).addBranch(IFNULL, end);
					}

					if (Entry.class.equals(right.type)) {
						state.operands.push(new Operand(Object.class, subject.builder.append(left.toObject()).append(right.toObject()).addInvoke(ACCESSOR_LOOKUP_RANGE).updateLabel(end)));
					} else {
						state.operands.push(new Operand(Object.class, subject.builder.append(right.toObject()).addInvoke(ACCESSOR_LOOKUP).updateLabel(end)));
					}

					break;
				} // Intentional fall-through if left is null
			case "{": {
				int pairs = 0;

				// Find the number of pairs
				for (int i = 0; i < operator.getRightExpressions(); i++) {
					if (Entry.class.equals(state.operands.peek(i + pairs).type)) {
						pairs++;
						assert Entry.class.equals(state.operands.peek(i + pairs).type) : Entry.class + " must occur in pairs on the operand stack";
					}
				}

				if (pairs > 0) { // Create a map
					final int totalPairs = pairs;
					final MethodBuilder builder = new MethodBuilder().pushNewObject(LinkedHashMap.class).addCode(DUP).pushConstant((operator.getRightExpressions() * 4 + 2) / 3).addInvoke(LINKED_HASH_MAP_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						final Operand first = state.operands.peek(i + pairs);

						if (Entry.class.equals(first.type)) {
							builder.addCode(DUP).append(first.toObject()).append(state.operands.peek(i + --pairs).toObject()).addInvoke(MAP_PUT).addCode(POP);
						} else {
							builder.addCode(DUP).append(first.toObject()).addCode(DUP).addInvoke(MAP_PUT).addCode(POP);
						}
					}

					state.operands.pop(operator.getRightExpressions() + totalPairs).push(new Operand(Object.class, builder));
				} else if ("[".equals(operator.getString())) { // Create a list
					final MethodBuilder builder = new MethodBuilder().pushNewObject(ArrayList.class).addCode(DUP).pushConstant(operator.getRightExpressions()).addInvoke(ARRAY_LIST_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						builder.addCode(DUP).append(state.operands.peek(i).toObject()).addInvoke(LIST_ADD).addCode(POP);
					}

					state.operands.pop(operator.getRightExpressions()).push(new Operand(Object.class, builder));
				} else { // Create a set
					final MethodBuilder builder = new MethodBuilder().pushNewObject(LinkedHashSet.class).addCode(DUP).pushConstant((operator.getRightExpressions() * 4 + 2) / 3).addInvoke(LINKED_HASH_SET_CTOR_INT);

					for (int i = operator.getRightExpressions() - 1; i >= 0; i--) {
						builder.addCode(DUP).append(state.operands.peek(i).toObject()).addInvoke(SET_ADD).addCode(POP);
					}

					state.operands.pop(operator.getRightExpressions()).push(new Operand(Object.class, builder));
				}

				break;
			}
			case "[:]":
				state.operands.push(new Operand(Object.class, new MethodBuilder().pushNewObject(LinkedHashMap.class).addCode(DUP).pushConstant(8).addInvoke(LINKED_HASH_MAP_CTOR_INT)));
				break;
			case "..": {
				final Label decreasing = left.builder.newLabel();
				final Label increasingLoop = left.builder.newLabel();
				final Label decreasingLoop = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				state.operands.push(new Operand(Object.class, left.toNumeric(false).addPrimitiveConversion(Number.class, int.class).addCode(DUP).addAccess(ISTORE, Evaluable.FIRST_LOCAL_INDEX).append(right.toNumeric(false).addPrimitiveConversion(Number.class, int.class)).addCode(DUP2).addBranch(IF_ICMPGT, decreasing)
						.addCode(SWAP, ISUB, ICONST_1, IADD, NEWARRAY, (byte)10, DUP).addAccess(ASTORE, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(ICONST_0).updateLabel(increasingLoop).addCode(DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(SWAP, DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL_INDEX).addCode(IADD, IASTORE, ICONST_1, IADD, DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(ARRAYLENGTH).addBranch(IF_ICMPLT, increasingLoop).addGoto(end, 0)
						.updateLabel(decreasing).addCode(ISUB, ICONST_1, IADD, NEWARRAY, (byte)10, DUP).addAccess(ASTORE, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(ICONST_0).updateLabel(decreasingLoop).addCode(DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(SWAP, DUP).addAccess(ILOAD, Evaluable.FIRST_LOCAL_INDEX).addCode(SWAP, ISUB, IASTORE, ICONST_1, IADD, DUP).addAccess(ALOAD, Evaluable.FIRST_LOCAL_INDEX + 1).addCode(ARRAYLENGTH).addBranch(IF_ICMPLT, decreasingLoop)
						.updateLabel(end).addCode(POP)));
				break;
			}

			// Math Operations
			case "**":
				state.operands.push(new Operand(Double.class, left.toNumeric(true).append(right.toNumeric(true)).addInvoke(OPERANDS_EXPONENTIATE)));
				break;
			case "+":
				if (left == null) { // Unary +, basically do nothing except require a number
					state.operands.push(new Operand(Double.class, right.toNumeric(true)));
				} else if (StringBuilder.class.equals(left.type)) { // Check for string concatenation
					state.operands.push(left).peek().builder.append(right.toObject()).addInvoke(STRING_BUILDER_APPEND_OBJECT);
				} else if (String.class.equals(left.type) || String.class.equals(right.type) || StringBuilder.class.equals(right.type)) {
					state.operands.push(new Operand(StringBuilder.class, left.toObject().pushNewObject(StringBuilder.class).addCode(DUP_X1, SWAP).addInvoke(STRING_VALUE_OF).addInvoke(STRING_BUILDER_INIT_STRING).append(right.toObject()).addInvoke(STRING_BUILDER_APPEND_OBJECT)));
				} else { // String concatenation, mathematical addition, collection addition, or invalid
					state.operands.push(new Operand(Object.class, left.toObject().append(right.toObject()).addInvoke(OPERANDS_ADD)));
				}

				break;
			case "-":
				if (left == null) {
					state.operands.push(new Operand(Double.class, right.toNumeric(true).addInvoke(OPERANDS_NEGATE)));
				} else {
					state.operands.push(new Operand(Object.class, left.toObject().append(right.toObject()).addInvoke(OPERANDS_SUBTRACT)));
				}

				break;
			case "*":
				state.operands.push(new Operand(Double.class, left.toNumeric(true).append(right.toNumeric(true)).addInvoke(OPERANDS_MULTIPLY)));
				break;
			case "/":
				state.operands.push(new Operand(Double.class, left.toNumeric(true).append(right.toNumeric(true)).addInvoke(OPERANDS_DIVIDE)));
				break;
			case "%":
				state.operands.push(new Operand(Double.class, left.toNumeric(true).append(right.toNumeric(true)).addInvoke(OPERANDS_MODULO)));
				break;

			// Integral Operations
			case "~":
				state.operands.push(new Operand(Integer.class, right.toNumeric(false).addInvoke(OPERANDS_NOT)));
				break;
			case "<<":
				state.operands.push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_SHIFT_LEFT)));
				break;
			case ">>":
				state.operands.push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_SHIFT_RIGHT)));
				break;
			case ">>>":
				state.operands.push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_SHIFT_RIGHT_ZERO)));
				break;
			case "&":
				state.operands.push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_AND)));
				break;
			case "^":
				state.operands.push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_XOR)));
				break;
			case "|":
				state.operands.push(new Operand(Integer.class, left.toNumeric(false).append(right.toNumeric(false)).addInvoke(OPERANDS_OR)));
				break;

			// Logical Operators
			case "!": {
				final Label notZero = right.builder.newLabel();
				final Label end = right.builder.newLabel();

				state.operands.push(new Operand(boolean.class, right.toBoolean().addBranch(IFNE, notZero).addCode(ICONST_1).addGoto(end, 1).updateLabel(notZero).addCode(ICONST_0).updateLabel(end)));
				break;
			}
			case "&&": {
				final Label notZero = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				state.operands.push(new Operand(boolean.class, left.toBoolean().addBranch(IFNE, notZero).addCode(ICONST_0).addGoto(end, 1).updateLabel(notZero).append(right.toBoolean()).updateLabel(end)));
				break;
			}
			case "||": {
				final Label zero = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				state.operands.push(new Operand(boolean.class, left.toBoolean().addBranch(IFEQ, zero).addCode(ICONST_1).addGoto(end, 1).updateLabel(zero).append(right.toBoolean()).updateLabel(end)));
				break;
			}

			// Comparison Operators
			case "<=>":
				state.operands.push(new Operand(int.class, new MethodBuilder().pushConstant(false).append(left.toObject()).append(right.toObject()).addInvoke(OPERANDS_COMPARE)));
				break;
			case "<=":
				state.operands.push(left.execCompareOp(right, IFLE));
				break;
			case ">=":
				state.operands.push(left.execCompareOp(right, IFGE));
				break;
			case "<":
				state.operands.push(left.execCompareOp(right, IFLT));
				break;
			case ">":
				state.operands.push(left.execCompareOp(right, IFGT));
				break;
			case "==":
				state.operands.push(left.execCompareOp(right, IFEQ));
				break;
			case "!=":
				state.operands.push(left.execCompareOp(right, IFNE));
				break;

			case "=~":
				state.operands.push(new Operand(boolean.class, new MethodBuilder().append(left.toObject()).append(right.toObject()).addInvoke(OPERANDS_FIND_PATTERN)));
				break;
			case "==~":
				state.operands.push(new Operand(boolean.class, new MethodBuilder().append(left.toObject()).append(right.toObject()).addInvoke(OPERANDS_MATCHES_PATTERN)));
				break;

			// Ternary Operations
			case "??":
			case "?:": {
				final Label end = left.builder.newLabel();

				state.operands.push(new Operand(Object.class, left.toObject().addCode(DUP).addBranch(IFNONNULL, end).addCode(POP).append(right.toObject()).updateLabel(end)));
				break;
			}
			case "?": {
				if (!Entry.class.equals(right.type)) {
					throw new IllegalArgumentException("Incomplete ternary operator, missing \":\"");
				}

				assert Entry.class.equals(left.type);
				assert !state.operands.isEmpty();

				final Label isFalse = left.builder.newLabel();
				final Label end = left.builder.newLabel();

				state.operands.push(new Operand(Object.class, state.operands.pop().toBoolean().addBranch(IFEQ, isFalse).append(left.builder).addGoto(end, 1).updateLabel(isFalse).append(right.builder).updateLabel(end)));
				break;
			}

			case ":":
				state.operands.push(new Operand(Entry.class, left.toObject())).push(new Operand(Entry.class, right.toObject()));

				if (state.operators.isEmpty() || !state.operators.peek().has(Operator.ALLOW_PAIRS)) {
					state.operators.push(Operator.get(",", true).withRightExpressions(-1));
				}

				break;

			case ";":
				state.operands.push(new Operand(right.type, left.builder.addCode(long.class.equals(left.type) || double.class.equals(left.type) ? POP2 : POP).append(right.builder)));
				break;

			case ",":
				state.operands.push(left);
				state.operators.push(Operator.get("[", false).withRightExpressions(operator.getRightExpressions() + 1));
				processOperation(state);
				break;

			// Streaming Operations
			case "#>":
			case "#.": { // Remap
				final MethodBuilder mb = left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addCode(DUP).addInvoke(ITERABLE_ITERATOR);
				final Label startOfLoop = mb.newLabel();
				final Label endOfLoop = mb.newLabel();

				state.operands.push(new Operand(Object.class, mb.addCode(DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
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

				state.operands.push(new Operand(Object.class, mb.addCode(DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
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

				state.operands.push(new Operand(Object.class, mb.addCode(DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP2).addInvoke(ITERATOR_NEXT).addCode(DUP).addAccess(ASTORE, operator.getLocalBindingIndex())
						.append(right.toBoolean()).addBranch(IFNE, readdObject).addCode(POP2).addGoto(startOfLoop, -2)
						.updateLabel(readdObject).addInvoke(STREAMABLE_ADD).addGoto(startOfLoop, 0).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#<": { // Reduction
				final MethodBuilder mb = left.toObject().addInvoke(STREAMABLE_OF_UNKNOWN).addInvoke(ITERABLE_ITERATOR).addCode(ACONST_NULL);
				final Label startOfLoop = mb.newLabel();
				final Label endOfLoop = mb.newLabel();

				state.operands.push(new Operand(Object.class, mb.addCode(SWAP, DUP).addInvoke(ITERATOR_HAS_NEXT).addBranch(IFEQ, endOfLoop)
						.addCode(DUP).addInvoke(ITERATOR_NEXT).addAccess(ASTORE, operator.getLocalBindingIndex()).addCode(SWAP, POP)
						.append(right.toObject()).addGoto(startOfLoop, 0).updateLabel(endOfLoop).addCode(POP)));
				break;
			}
			case "#^": // Return
				state.operands.push(new Operand(Object.class, right.toObject().addFlowBreakingCode(ARETURN, 0).addCode(ACONST_NULL)));
				break;

			case "(":
				state.operands.push(right);
				break;

			case "=":
				state.operands.push(new Operand(Object.class, right.toObject().addCode(DUP).append(left.builder)));
				break;

			case "\u2620": case "~:<": // Die
				state.operands.push(new Operand(Object.class, right.toObject().addInvoke(STRING_VALUE_OF).pushNewObject(HaltRenderingException.class).addCode(DUP_X1, SWAP).addInvoke(HALT_EXCEPTION_CTOR_STRING).addFlowBreakingCode(ATHROW, 0).addCode(ACONST_NULL)));
				break;

			case "~@": { // Get class
				final Label isNull = right.builder.newLabel();

				if (right.identifier != null) {
					final Label notNull = right.builder.newLabel();
					state.operands.push(new Operand(Object.class, right.toObject().addCode(DUP).addBranch(IFNULL, isNull).addInvoke(OBJECT_TO_STRING).addGoto(notNull, 0).updateLabel(isNull).addCode(POP).pushConstant(right.identifier.getName()).updateLabel(notNull).addCode(Evaluable.LOAD_CONTEXT).addCode(SWAP).addInvoke(OPERANDS_GET_CLASS)));
				} else {
					state.operands.push(new Operand(Object.class, right.toObject().addCode(DUP).addBranch(IFNULL, isNull).addInvoke(OBJECT_TO_STRING).addCode(Evaluable.LOAD_CONTEXT).addCode(SWAP).addInvoke(OPERANDS_GET_CLASS).updateLabel(isNull)));
				}

				break;
			}

			default:
				throw new IllegalStateException("Unrecognized operator \"" + operator + "\"");
		}
	}

	/**
	 * Processes the method call. The operand stack will be updated with the results of evaluating the operator.
	 *
	 * @param state the parse state
	 * @param operator the method call operator
	 */
	private static void processMethodCall(final ParseState state, final Operator operator) {
		final int parameterCount = operator.getRightExpressions();
		final Expression namedExpression;

		if (operator.has(Operator.KNOWN_OBJECT) || (namedExpression = state.namedExpressions.get(operator.getString())) == null) {
			// Create the identifier, then get and invoke the appropriate method
			final int index = state.getIdentifierIndex(new Identifier(operator.getString(), parameterCount));
			final MethodBuilder methodResult = state.operands.peek(parameterCount).builder.addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(index).addCode(AALOAD).append(state.operands.peek(parameterCount + 1).builder);

			if (parameterCount == 0) {
				methodResult.addCode(ACONST_NULL);
			} else {
				methodResult.pushNewObject(Object.class, parameterCount);

				// Convert all parameters to objects and store them in the array
				for (int i = 0; i < parameterCount; i++) {
					methodResult.addCode(DUP).pushConstant(i).append(state.operands.peek(parameterCount - 1 - i).toObject()).addCode(AASTORE);
				}
			}

			state.operands.push(new Operand(Object.class, methodResult.append(state.operands.pop(parameterCount + 2).pop().builder)));
			return;
		}

		// Process the named expression
		final MethodBuilder expressionResult = new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA);
		Integer index = state.expressions.get(namedExpression);

		if (index == null) {
			index = state.expressions.size();
			state.expressions.put(namedExpression, index);
		}

		// Load the context
		if (parameterCount == 0) {
			expressionResult.addCode(DUP).pushConstant(0).addInvoke(STACK_PEEK);
		} else {
			expressionResult.append(state.operands.peek(parameterCount - 1).toObject());
		}

		expressionResult.addInvoke(STACK_PUSH_OBJECT).addCode(POP).addCode(Evaluable.LOAD_EXPRESSIONS).pushConstant(index).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT);

		// Load the arguments
		if (parameterCount > 1) {
			expressionResult.pushNewObject(Object.class, parameterCount - 1);

			for (int i = 0; i < parameterCount - 1; i++) {
				expressionResult.addCode(DUP).pushConstant(i).append(state.operands.peek(parameterCount - 2 - i).toObject()).addCode(AASTORE);
			}
		} else {
			expressionResult.addCode(ACONST_NULL);
		}

		// Evaluate the expression
		expressionResult.addInvoke(EXPRESSION_EVALUATE).addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).addInvoke(STACK_POP).addCode(POP);

		state.operands.pop(parameterCount + 3).push(new Operand(Object.class, expressionResult));
	}

	/**
	 * Creates a new expression.
	 *
	 * @param location the location of the expression
	 * @param expressionString the trimmed, advanced expression string
	 * @param namedExpressions the map used to lookup named expressions
	 * @param horseshoeExpressions true to parse as a horseshoe expression, false to parse as a Mustache variable list
	 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading the expression
	 */
	Expression(final Object location, final String expression, final Map<String, Expression> namedExpressions, final boolean horseshoeExpressions) throws ReflectiveOperationException {
		this(null, location, 0, expression, namedExpressions, horseshoeExpressions);
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
		final ParseState state = new ParseState(startIndex, expression, namedExpressions);
		final MethodBuilder mb = new MethodBuilder();
		boolean named = false;

		this.location = location;
		this.originalString = expression;

		if (".".equals(originalString)) {
			state.operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_CONTEXT).addInvoke(RENDER_CONTEXT_GET_SECTION_DATA).pushConstant(0).addInvoke(STACK_PEEK)));
		} else if (!horseshoeExpressions) {
			final String[] names = Pattern.compile("\\s*[.]\\s*", Pattern.UNICODE_CHARACTER_CLASS).split(originalString, -1);

			// Push a new operand formed by invoking identifiers[index].getValue(context, backreach, access)
			state.operands.push(new Operand(Object.class, new MethodBuilder().addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(state.getIdentifierIndex(new Identifier(names[0]))).addCode(AALOAD).addCode(Evaluable.LOAD_CONTEXT).pushConstant(Identifier.UNSTATED_BACKREACH).addInvoke(IDENTIFIER_FIND_VALUE)));

			// Load the identifiers and invoke identifiers[index].getValue(object)
			for (int i = 1; i < names.length; i++) {
				state.operands.peek().builder.addCode(Evaluable.LOAD_IDENTIFIERS).pushConstant(state.getIdentifierIndex(new Identifier(names[i]))).addCode(AALOAD, SWAP).addInvoke(IDENTIFIER_GET_VALUE);
			}
		} else { // Tokenize the entire expression, using the shunting yard algorithm
			int initializeBindingsStart = 0;
			final int end = expression.length();
			final Matcher matcher = NAMED_EXPRESSION_PATTERN.matcher(expression);

			if (matcher.lookingAt()) {
				namedExpressions.put(matcher.group("name"), this);
				named = true;
				parseNamedExpressionHeading(state, mb, matcher);
				initializeBindingsStart = state.localBindings.size();
				matcher.region(matcher.end(), end);
			}

			Operator lastOperator = Operator.get("(", false);

			// Loop through all tokens
			for (; matcher.regionStart() < end; matcher.region(matcher.end(), end)) {
				lastOperator = parseToken(state, matcher, lastOperator);
			}

			// Push everything to the output queue
			while (!state.operators.isEmpty()) {
				Operator operator = state.operators.pop();

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

				state.operators.push(operator);
				processOperation(state);
			}

			state.initializeLocalBindings(mb, initializeBindingsStart);
		}

		if (state.operands.isEmpty()) {
			throw new IllegalArgumentException("Unexpected empty expression");
		}

		// Check for match to the cached expression
		if (cachedExpression != null && equalLists(state.expressions.keySet(), cachedExpression.expressions) && equalLists(state.identifiers.keySet(), cachedExpression.identifiers)) {
			assert cachedExpression.originalString.equals(originalString) : "Invalid cached expression \"" + cachedExpression + "\" does not match parsed expression \"" + originalString + "\"";
			this.expressions = cachedExpression.expressions;
			this.identifiers = cachedExpression.identifiers;
			this.evaluable = cachedExpression.evaluable;
			this.isNamed = named;
			return;
		}

		// Populate all the expressions
		if (state.expressions.isEmpty()) {
			this.expressions = null;
		} else {
			this.expressions = new Expression[state.expressions.size()];

			for (final Entry<Expression, Integer> entry : state.expressions.entrySet()) {
				this.expressions[entry.getValue()] = entry.getKey();
			}
		}

		// Populate all the identifiers
		if (state.identifiers.isEmpty()) {
			this.identifiers = null;
		} else {
			this.identifiers = new Identifier[state.identifiers.size()];

			for (final Entry<Identifier, Integer> entry : state.identifiers.entrySet()) {
				this.identifiers[entry.getValue()] = entry.getKey();
			}
		}

		// Create the evaluator
		this.evaluable = mb.append(state.operands.pop().toObject()).addFlowBreakingCode(ARETURN, 0).build(Expression.class.getPackage().getName() + ".Expression_" + DYN_INDEX.getAndIncrement(), Evaluable.class, Expression.class.getClassLoader()).getConstructor().newInstance();
		this.isNamed = named;
		assert state.operands.isEmpty();
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof Expression) {
			return originalString.equals(((Expression)object).originalString) && location.equals(((Expression)object).location);
		}

		return false;
	}

	/**
	 * Evaluates the expression using the given render context.
	 *
	 * @param context the render context used to evaluate the object
	 * @return the evaluated expression or null if the expression could not be evaluated
	 */
	public Object evaluate(final RenderContext context) {
		return evaluate(context, null);
	}

	/**
	 * Evaluates the expression using the given render context and the specified arguments.
	 *
	 * @param context the render context used to evaluate the object
	 * @param arguments the arguments used to evaluate the object
	 * @return the evaluated expression or null if the expression could not be evaluated
	 */
	public Object evaluate(final RenderContext context, final Object[] arguments) {
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
