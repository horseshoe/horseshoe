package horseshoe;

import java.util.ArrayList;
import java.util.List;

final class Section {

	private final String writerName;
	private final List<Action> actions = new ArrayList<>();
	private final List<Action> invertedActions = new ArrayList<>();

	/**
	 * Creates a new section using the specified writer.
	 *
	 * @param writerName the name of the writer to use for the section
	 */
	public Section(final String writerName) {
		this.writerName = writerName;
	}

	/**
	 * Creates a new section using the default writer.
	 */
	public Section() {
		this(null);
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
	 * Gets the inverted actions associated with the section.
	 *
	 * @return the inverted actions associated with the section
	 */
	public List<Action> getInvertedActions() {
		return invertedActions;
	}

	/**
	 * Gets the name of the writer to use for the section.
	 *
	 * @return the actions associated with the section
	 */
	public String getWriterName() {
		return writerName;
	}

}