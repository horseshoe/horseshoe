package horseshoe;

/**
 * A Horseshoe extension is an option that extends the core Mustache functionality.
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
	 * Named Horseshoe expression tags ({@code {{[NamedExpression]() -> [Expression]}}}) will be loaded and can be used later in a template. The {@link #EXPRESSIONS} extension must also be enabled for this extension to function properly.
	 */
	NAMED_EXPRESSIONS,

	/**
	 * End tags ({@code {{/}}}) can be empty rather than required to match the text in start tags.
	 */
	EMPTY_END_TAGS,

	/**
	 * End tags ({@code {{/[SectionName]}}}) that do not match the text in start tags will be treated as Horseshoe expressions. Enabling this may result in confusing parse exceptions when loading malformed templates.
	 */
	SMART_END_TAGS,

	/**
	 * Empty inverted sections tags ({@code {{^}}}) will be considered else tags and will be rendered whenever the section currently in scope is not rendered.
	 */
	ELSE_TAGS,

	/**
	 * In-line partial tags ({@code {{<[PartialName]}}}) will be loaded and can be used later in a template.
	 */
	INLINE_PARTIALS,

	/**
	 * Annotation section tags ({@code {{#@[AnnotationName]}}}) can be used in templates.
	 */
	ANNOTATIONS

}
