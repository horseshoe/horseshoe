package horseshoe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;

import horseshoe.Utilities.TrimmedString;
import horseshoe.util.Identifier;

final class TemplateLoadState {

	private static final ParsedLine EMPTY_LINE = new ParsedLine("", "");

	static final int TAG_CAN_BE_STANDALONE = 0;
	static final int TAG_CANT_BE_STANDALONE = 1;
	static final int TAG_CHECK_TAIL_STANDALONE = 2;

	private final Loader loader;
	private final Template template;
	private final Stack<Section> sections = new Stack<>();
	private final HashMap<Identifier, Identifier> allIdentifiers = new HashMap<>();

	private final HashMap<String, Expression> namedExpressions = new HashMap<>();
	private final Stack<LinkedHashMap<String, TemplateBinding>> templateBindings = new Stack<>();
	private final Stack<ArrayList<Renderer>> renderLists = new Stack<>();

	private TemplateLoadState.Delimiter delimiter = new TemplateLoadState.Delimiter("{{", "}}");
	private ArrayList<ParsedLine> priorStaticText = new ArrayList<>(Arrays.asList(EMPTY_LINE));
	private int standaloneStatus;
	private int tagCount = 0;

	static final class Delimiter {

		final String start;
		final String end;
		final String unescapedEnd;

		Delimiter(final String start, final String end) {
			this.start = start;
			this.end = end;
			this.unescapedEnd = "}" + end;
		}

	}

	TemplateLoadState(final Loader loader, final Template template) {
		this.loader = loader;
		this.template = template;
		templateBindings.push(template.getBindings());
	}

	/**
	 * Create a new expression checking the cache to see if it already exists.
	 *
	 * @param location the location of the expression
	 * @param parseState the parse state used to create the expression
	 * @return the new expression
	 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading the expression
	 */
	Expression createExpression(final Object location, final ExpressionParseState parseState) throws ReflectiveOperationException {
		final Expression expression = Expression.create(location, parseState);

		if (sections.size() == 1) {
			template.getRootExpressions().put(expression.getName(), expression);
		}

		return expression;
	}

	/**
	 * Create a new expression parser from the load state.
	 *
	 * @param expression the trimmed expression string
	 * @param extensions the set of extensions currently in use
	 * @param isCall true if the expression should be parsed as part of a call invocation (starts "(", ends with ")", returns array object)
	 * @return the new expression parser
	 */
	ExpressionParseState createExpressionParser(final TrimmedString expression, final EnumSet<Extension> extensions, final boolean isCall) {
		return new ExpressionParseState(expression.start, expression.string, extensions, isCall, getNamedExpressions(), allIdentifiers, templateBindings);
	}

	/**
	 * Create a new expression parser from the load state.
	 *
	 * @param expression the trimmed expression string
	 * @param extensions the set of extensions currently in use
	 * @return the new expression parser
	 */
	ExpressionParseState createExpressionParser(final TrimmedString expression, final EnumSet<Extension> extensions) {
		return createExpressionParser(expression, extensions, false);
	}

	/**
	 * Gets the delimiter for the state.
	 *
	 * @return the delimiter for the state
	 */
	TemplateLoadState.Delimiter getDelimiter() {
		return delimiter;
	}

	/**
	 * Gets the named expressions for the state.
	 *
	 * @return the named expressions for the state
	 */
	HashMap<String, Expression> getNamedExpressions() {
		return namedExpressions;
	}

	/**
	 * Gets the sections for the state.
	 *
	 * @return the sections for the state
	 */
	Stack<Section> getSections() {
		return sections;
	}

	/**
	 * Gets the render lists for the state.
	 *
	 * @return the render lists for the state
	 */
	Stack<ArrayList<Renderer>> getRenderLists() {
		return renderLists;
	}

	/**
	 * Gets the standalone status for the state.
	 *
	 * @return the standalone status for the state
	 */
	int getStandaloneStatus() {
		return standaloneStatus;
	}

	/**
	 * Gets the tag count for the state.
	 *
	 * @return the tag count for the state
	 */
	int getTagCount() {
		return tagCount;
	}

	/**
	 * Gets the template being loaded.
	 *
	 * @return the template being loaded
	 */
	Template getTemplate() {
		return template;
	}

	/**
	 * Gets the template bindings for the state.
	 *
	 * @return the template bindings for the state
	 */
	Stack<LinkedHashMap<String, TemplateBinding>> getTemplateBindings() {
		return templateBindings;
	}

	/**
	 * Checks if the given lines could be the tail end of a standalone tag.
	 *
	 * @param lines the lines of the static text
	 * @return true if the lines could be the tail end of a standalone tag, otherwise false
	 */
	boolean isStandaloneTagTail(final ArrayList<ParsedLine> lines) {
		final int size = lines.size();

		if (size > 1) {
			return Utilities.isWhitespace(lines.get(0).getLine());
		}

		return !loader.hasNext() && Utilities.isWhitespace(lines.get(0).getLine());
	}

	/**
	 * Removes the leading whitespace from the beginning of a potential standalone tag.
	 *
	 * @return the leading whitespace
	 */
	String removeStandaloneTagHead() {
		final int size = priorStaticText.size();

		if (size > 1 || tagCount == 1) {
			final String removedWhitespace = priorStaticText.get(size - 1).getLine();

			if (removedWhitespace != null && Utilities.isWhitespace(removedWhitespace)) {
				priorStaticText.set(size - 1, new ParsedLine(null, ""));
				return removedWhitespace;
			}
		}

		return null;
	}

	/**
	 * Sets the delimiter for the state.
	 *
	 * @param delimiter the delimiter for the state
	 * @return this load state
	 */
	TemplateLoadState setDelimiter(final TemplateLoadState.Delimiter delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	/**
	 * Sets the standalone status for the state.
	 *
	 * @param standaloneStatus the standalone status for the state
	 * @return this load state
	 */
	TemplateLoadState setStandaloneStatus(final int standaloneStatus) {
		this.standaloneStatus = standaloneStatus;
		return this;
	}

	/**
	 * Converts the current state to a location.
	 *
	 * @return the location based on the current state
	 */
	Object toLocation() {
		return loader.toLocation();
	}

	/**
	 * Updates the static text for the state, increments the tag count, and resets the standalone status.
	 *
	 * @param staticText the updated prior static text for the state
	 * @return this load state
	 */
	TemplateLoadState updateStaticText(final ArrayList<ParsedLine> staticText) {
		this.tagCount++;
		this.standaloneStatus = TAG_CAN_BE_STANDALONE;
		this.priorStaticText = staticText;
		return this;
	}

}