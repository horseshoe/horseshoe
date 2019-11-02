package horseshoe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Section {

	private final Expression expression;
	private final String writerName;
	private final Map<String, Template> localPartials;
	private final List<Action> actions = new ArrayList<>();
	private final List<Action> invertedActions = new ArrayList<>();

	/**
	 * Creates a new section using the specified expression and specified writer.
	 *
	 * @param expression the expression for the section
	 * @param writerName the name of the writer to use for the section, or null to use the current writer
	 */
	public Section(final Expression expression, final String writerName, final Map<String, Template> localPartials) {
		this.expression = expression;
		this.writerName = writerName;
		this.localPartials = new HashMap<>(localPartials);
	}

	/**
	 * Creates a new section using the specified expression.
	 *
	 * @param expression the expression for the section
	 */
	public Section(final Expression expression, final Map<String, Template> localPartials) {
		this(expression, null, localPartials);
	}

	/**
	 * Creates a new section using the specified writer.
	 *
	 * @param writerName the name of the writer to use for the section, or null to use the current writer
	 * @throws Exception
	 */
	public Section(final CharSequence writerName, final int maxBackreach, final Map<String, Template> localPartials) throws Exception {
		this(Expression.newEmptyExpression(writerName.toString()), writerName.toString(), localPartials); // TODO: fix
	}

	/**
	 * Creates a new section using an empty expression.
	 */
	public Section(final String name, final Map<String, Template> localPartials) {
		this(Expression.newEmptyExpression(name), null, localPartials);
	}

	/**
	 * Gets the actions associated with the section.
	 *
	 * @return the actions associated with the section
	 */
	public List<Action> getActions() {
		return actions;
	}

	/**
	 * Gets the expression associated with the section.
	 *
	 * @return the expression associated with the section, or null if one does not exist
	 */
	public Expression getExpression() {
		return expression;
	}

	/**
	 * Gets the inverted actions associated with the section.
	 *
	 * @return the inverted actions associated with the section
	 */
	public List<Action> getInvertedActions() {
		return invertedActions;
	}

	/**
	 * Gets the template for the section.
	 *
	 * @return the template for the section, or null if one does not exist
	 */
	public Map<String, Template> getLocalPartials() {
		return localPartials;
	}

	/**
	 * Gets the name of the writer to use for the section.
	 *
	 * @return the name of the writer to use for the section, or null if one does not exist
	 */
	public String getWriterName() {
		return writerName;
	}

	/**
	 * Checks if the specified character sequence matches this section.
	 *
	 * @param value the character sequence to check
	 * @return true if the specified character sequence matches this section, otherwise false
	 */
	public boolean matches(final CharSequence value) {
		if (expression != null) {
			return expression.exactlyMatches(value);
		} else if (writerName != null) {
			return writerName.equals(value);
		}

		return false;
	}

}