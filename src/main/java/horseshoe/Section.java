package horseshoe;

import java.util.ArrayList;
import java.util.Objects;

final class Section {

	private final Section parent;
	private final ArrayList<Section> children = new ArrayList<>();
	private final String name;
	private final Object location;
	private final Expression expression;
	private final String annotation;
	private final boolean isInvisible;
	private final boolean isLocalPartial;
	private boolean cacheResult = false;
	private boolean useCache = false;
	private final ArrayList<Renderer> renderList = new ArrayList<>();
	private final ArrayList<Renderer> invertedRenderList = new ArrayList<>();

	/**
	 * Creates a new repeated section with the specified parent.
	 *
	 * @param parent the parent of the section
	 * @param location the location of the section
	 * @return the repeated section
	 */
	static Section repeat(final Section parent, final Object location) {
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
				throw new IllegalStateException("Cannot repeat child of section " + repeatedSection + ", expecting exactly 1 child section");
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
	 * @param isLocalPartial true if the section is a local partial, otherwise false
	 */
	Section(final Section parent, final String name, final Object location, final Expression expression, final String annotation, final boolean isInvisible, final boolean isLocalPartial) {
		this.parent = parent;
		this.name = Objects.requireNonNull(name, "Encountered null section name");
		this.location = location;
		this.expression = expression;
		this.annotation = annotation;
		this.isInvisible = isInvisible;
		this.isLocalPartial = isLocalPartial;

		if (parent != null) {
			parent.children.add(this);
		}
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
	Section(final Section parent, final String name, final Object location, final Expression expression, final String annotation, final boolean isInvisible) {
		this(parent, name, location, expression, annotation, isInvisible, false);
	}

	/**
	 * Creates a new section using the specified expression.
	 *
	 * @param parent the parent of the section, or null if the section is a top-level section
	 * @param location the location of the section
	 * @param expression the expression for the section
	 */
	Section(final Section parent, final Object location, final Expression expression) {
		this(parent, expression.toString(), location, expression, null, false);
	}

	/**
	 * Checks if the section should cache the result of the expression for later use.
	 *
	 * @return true if the section should cache the result of the expression for later use
	 */
	boolean cacheResult() {
		return cacheResult;
	}

	/**
	 * Gets the name of the annotation for the section.
	 *
	 * @return the name of the annotation for the section, or null if no annotation exists
	 */
	String getAnnotation() {
		return annotation;
	}

	/**
	 * Gets the expression associated with the section.
	 *
	 * @return the expression associated with the section, or null if one does not exist
	 */
	Expression getExpression() {
		return expression;
	}

	/**
	 * Gets the list of inverted renderers associated with the section.
	 *
	 * @return the the list of inverted renderers associated with the section
	 */
	ArrayList<Renderer> getInvertedRenderList() {
		return invertedRenderList;
	}

	/**
	 * Gets the name of the section.
	 *
	 * @return the name of the section
	 */
	String getName() {
		return name;
	}

	/**
	 * Gets the list of renderers associated with the section.
	 *
	 * @return the list of renderers associated with the section
	 */
	ArrayList<Renderer> getRenderList() {
		return renderList;
	}

	/**
	 * Checks the invisibility of the section.
	 *
	 * @return true if the section is invisible, otherwise false
	 */
	boolean isInvisible() {
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
	 * Checks if the section is a local partial.
	 *
	 * @return true if the section is a local partial, otherwise false
	 */
	boolean isLocalPartial() {
		return isLocalPartial;
	}

	/**
	 * Checks if the section should use a cached result rather than the result of an expression.
	 *
	 * @return true if the section should use a cached result rather than the result of an expression
	 */
	boolean useCache() {
		return useCache;
	}

	@Override
	public String toString() {
		if (location == null || location.toString().equals(name)) {
			return "\"" + name + "\"";
		} else if (name.isEmpty()) {
			return location.toString();
		}

		return "\"" + name + "\" (" + location + ")";
	}

}
