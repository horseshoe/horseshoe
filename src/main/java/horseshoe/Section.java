package horseshoe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import horseshoe.internal.Expression;
import horseshoe.internal.OverlayMap;

final class Section {

	private static final int INITIAL_CHILDREN_CAPACITY = 4; // Likely not many nested sections, so don't initialize the full capacity
	private static final int INITIAL_INVERTED_ACTIONS_CAPACITY = 4; // Likely doesn't exist, or used for error reporting, so don't initialize the full capacity

	private final Section parent;
	private final List<Section> children = new ArrayList<>(INITIAL_CHILDREN_CAPACITY);
	private final String name;
	private final String location;
	private final Expression expression;
	private final String annotation;
	private final boolean isInvisible;
	private boolean cacheResult = false;
	private boolean useCache = false;
	private final Map<String, Expression> namedExpressions;
	private final Map<String, Template> localPartials;
	private final List<Action> actions = new ArrayList<>();
	private final List<Action> invertedActions = new ArrayList<>(INITIAL_INVERTED_ACTIONS_CAPACITY);

	/**
	 * Creates a new repeated section with the specified parent.
	 *
	 * @param parent the parent of the section
	 * @param location the location of the section
	 * @return the repeated section
	 */
	public static Section repeat(final Section parent, final String location) {
		Section repeatContainer = parent;
		int nested = 0;
		int skipChildrenSize = 0;

		// Create the repeated section, here is the algorithm:
		//  1) Starting with the parent section, traverse the ancestors until we find a container with at least one previous child section
		//  2) Traverse down all single descendants the appropriate number of times ignoring invisible sections (annotations, top-level)
		//  3) If the repeated section is not nested, use the cache, otherwise use the expression of the repeated section
		for (; repeatContainer.children.size() == skipChildrenSize; skipChildrenSize = 1, repeatContainer = repeatContainer.parent) {
			if (repeatContainer.isRepeat()) {
				nested++;
			} else if (!repeatContainer.isInvisible() || repeatContainer.parent == null) {
				throw new IllegalStateException("Cannot repeat section, no previous section exists");
			}
		}

		Section repeatedSection = repeatContainer.children.get(repeatContainer.children.size() - 1 - skipChildrenSize);

		for (int skippedChildren = nested; repeatedSection.isInvisible() || skippedChildren-- > 0; repeatedSection = repeatedSection.children.get(0)) {
			if (repeatedSection.children.size() != 1) {
				throw new IllegalStateException("Cannot repeat child of section \"" + repeatedSection.getName() + "\", expecting exactly 1 child section");
			}
		}

		final Section newSection = new Section(parent, "", location, repeatedSection.getExpression(), null, false);

		if (nested == 0) {
			repeatedSection.cacheResult = true;
			newSection.useCache = true;
		}

		return newSection;
	}

	/**
	 * Creates a new section using the specified expression and specified writer.
	 *
	 * @param parent the parent of the section, or null if the section is a top-level section
	 * @param name the name for the section
	 * @param location the location of the section
	 * @param expression the expression for the section
	 * @param annotation the name of the annotation for the section, or null if no annotation exists
	 * @param isInvisible true if the section is not visible to backreach, otherwise false
	 */
	public Section(final Section parent, final String name, final String location, final Expression expression, final String annotation, final boolean isInvisible) {
		this.parent = parent;
		this.name = Objects.requireNonNull(name, "Encountered null section name");
		this.location = Objects.requireNonNull(location, "Encountered null location");
		this.expression = expression;
		this.annotation = annotation;
		this.isInvisible = isInvisible;

		if (parent == null) {
			this.namedExpressions = new HashMap<>();
			this.localPartials = new HashMap<>();
		} else {
			this.namedExpressions = new OverlayMap<>(parent.namedExpressions);
			this.localPartials = new OverlayMap<>(parent.localPartials);
			parent.children.add(this);
		}
	}

	/**
	 * Creates a new section using the specified expression.
	 *
	 * @param parent the parent of the section, or null if the section is a top-level section
	 * @param location the location of the section
	 * @param expression the expression for the section
	 */
	public Section(final Section parent, final String location, final Expression expression) {
		this(parent, expression.toString(), location, expression, null, false);
	}

	/**
	 * Checks if the section should cache the result of the expression for later use.
	 *
	 * @return true if the section should cache the result of the expression for later use
	 */
	public boolean cacheResult() {
		return cacheResult;
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
	 * Gets the location where the section begins.
	 *
	 * @return the location where the section begins
	 */
	public String getLocation() {
		return location;
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
	 * Gets the map of named expressions associated with the section.
	 *
	 * @return the map of named expressions associated with the section
	 */
	public Map<String, Expression> getNamedExpressions() {
		return namedExpressions;
	}

	/**
	 * Checks the invisibility of the section.
	 *
	 * @return true if the section is invisible, otherwise false
	 */
	public boolean isInvisible() {
		return isInvisible;
	}

	/**
	 * Checks if the section is a repeat of another section.
	 *
	 * @return true if the section is a repeat of another section, otherwise false
	 */
	private boolean isRepeat() {
		return name.isEmpty();
	}

	/**
	 * Checks if the section should use a cached result rather than the result of an expression.
	 *
	 * @return true if the section should use a cached result rather than the result of an expression
	 */
	public boolean useCache() {
		return useCache;
	}

}
