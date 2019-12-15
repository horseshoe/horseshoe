package horseshoe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import horseshoe.internal.Expression;

final class Section {

	private final String name;
	private final Expression expression;
	private final String annotation;
	private final Map<String, Template> localPartials;
	private final List<Action> actions = new ArrayList<>();
	private final List<Action> invertedActions = new ArrayList<>();

	/**
	 * Creates a new section using the specified expression and specified writer.
	 *
	 * @param name the name for the section
	 * @param expression the expression for the section
	 * @param annotation the name of the annotation for the section, or null if no annotation exists
	 * @param localPartials the local partials for the section
	 */
	public Section(final String name, final Expression expression, final String annotation, final Map<String, Template> localPartials) {
		this.name = name;
		this.expression = expression;
		this.annotation = annotation;
		this.localPartials = new HashMap<>(localPartials);
	}

	/**
	 * Creates a new section using the specified expression.
	 *
	 * @param expression the expression for the section
	 */
	public Section(final Expression expression, final Map<String, Template> localPartials) {
		this(expression.toString(), expression, null, localPartials);
	}

	/**
	 * Creates a new section using an empty expression.
	 */
	public Section(final String name, final Map<String, Template> localPartials) {
		this(name, null, null, localPartials);
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
	 * Gets the name of the annotation for the section.
	 *
	 * @return the name of the annotation for the section, or null if no annotation exists
	 */
	public String getAnnotation() {
		return annotation;
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
	 * Gets the local partials for the section.
	 *
	 * @return the local partials for the section
	 */
	public Map<String, Template> getLocalPartials() {
		return localPartials;
	}

	/**
	 * Gets the name of the section.
	 *
	 * @return the name of the section
	 */
	public String getName() {
		return name;
	}

}
