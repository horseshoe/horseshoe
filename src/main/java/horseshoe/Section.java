package horseshoe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import horseshoe.internal.Expression;
import horseshoe.internal.OverlayMap;

final class Section {

	private final String name;
	private final Expression expression;
	private final String annotation;
	private final boolean isInvisible;
	private final Map<String, Expression> namedExpressions;
	private final Map<String, Template> localPartials;
	private final List<Action> actions = new ArrayList<>();
	private final List<Action> invertedActions = new ArrayList<>();

	/**
	 * Creates a new section using the specified expression and specified writer.
	 *
	 * @param name the name for the section
	 * @param expression the expression for the section
	 * @param annotation the name of the annotation for the section, or null if no annotation exists
	 * @param parent the parent of the section, or null if the section is a top-level section
	 * @param isInvisible true if the section is not visible to backreach, otherwise false
	 */
	public Section(final Section parent, final String name, final Expression expression, final String annotation, final boolean isInvisible) {
		this.name = name;
		this.expression = expression;
		this.annotation = annotation;
		this.isInvisible = isInvisible;

		if (parent == null) {
			this.namedExpressions = new HashMap<>();
			this.localPartials = new HashMap<>();
		} else {
			this.namedExpressions = new OverlayMap<>(parent.namedExpressions);
			this.localPartials = new OverlayMap<>(parent.localPartials);
		}
	}

	/**
	 * Creates a new section using the specified expression.
	 *
	 * @param parent the parent of the section, or null if the section is a top-level section
	 * @param expression the expression for the section
	 */
	public Section(final Section parent, final Expression expression) {
		this(parent, expression.toString(), expression, null, false);
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
	 * Gets the map of named expressions associated with the section.
	 *
	 * @return the map of named expressions associated with the section
	 */
	public Map<String, Expression> getNamedExpressions() {
		return namedExpressions;
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

	/**
	 * Checks the invisibility of the section.
	 *
	 * @return true if the section is invisible, otherwise false
	 */
	public boolean isInvisible() {
		return isInvisible;
	}

}
