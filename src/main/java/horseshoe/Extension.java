package horseshoe;

/**
 * Options that affect Horseshoe extensions to core Mustache functionality.
 */
public enum Extension {

	/**
	 * An exception will be thrown when a partial is not found rather than silently ignored.
	 */
	THROW_ON_PARTIAL_NOT_FOUND,

	/**
	 * Tags will be parsed as Horseshoe expressions rather than Mustache interpolated variables.
	 */
	EXPRESSIONS,

	/**
	 * End tags ({@code {{/}}}) can be empty rather than required to match the text in start tags.
	 */
	EMPTY_END_TAGS,

	/**
	 * End tags ({@code {{/ [SectionName] }}}) that do not match the text in start tags will be treated as Horseshoe expressions. Enabling this may result in confusing parse exceptions when loading malformed templates.
	 */
	SMART_END_TAGS,

	/**
	 * Empty section tags ({@code {{#}}}) will be interpreted to mean repeat the previous section at the same scope.
	 */
	REPEATED_SECTIONS,

	/**
	 * Empty inverted sections tags ({@code {{^}}}) will be considered else tags and will be rendered whenever the section currently in scope is not rendered.
	 */
	ELSE_TAGS,

	/**
	 * In-line partial tags ({@code {{< [PartialName] }}}) will be loaded and can be used later in a template.
	 */
	INLINE_PARTIALS,

	/**
	 * Annotation section tags ({@code {{# @[AnnotationName] }}}) can be used in templates.
	 */
	ANNOTATIONS

}
