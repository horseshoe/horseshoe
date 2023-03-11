package horseshoe;

/**
 * Options that affect context access when checking identifiers in expressions.
 */
public enum ContextAccess {

	/**
	 * The context access that allows access to all section scopes when resolving an identifier in an expression.
	 */
	FULL,

	/**
	 * The context access that allows access to the current section scope and the root-level section scope when resolving an identifier in an expression.
	 */
	CURRENT_AND_ROOT,

	/**
	 * The context access that allows access to only the current section scope when resolving an identifier in an expression.
	 */
	CURRENT

}
