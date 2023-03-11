package horseshoe;

/**
 * Options that affect file modification settings for {@link BufferedFileUpdateStream}s.
 */
public enum FileModification {

	/**
	 * The file contents will only be modified if the contents changes.
	 */
	UPDATE,

	/**
	 * The file contents will be updated by appending to the end of the existing file.
	 */
	APPEND,

	/**
	 * The file will be truncated and the contents replaced.
	 */
	OVERWRITE

}
