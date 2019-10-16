package horseshoe;

import java.util.ArrayList;
import java.util.List;

final class Section {

	private final List<Action> actions = new ArrayList<>();
	private final List<Action> invertedActions = new ArrayList<>();

	/**
	 * Creates a new section
	 */
	public Section() {
	}

	/**
	 * Gets the actions associated with the segment.
	 *
	 * @return the actions associated with the segment
	 */
	public List<Action> getActions() {
		return actions;
	}

	/**
	 * Gets the inverted actions associated with the segment.
	 *
	 * @return the inverted actions associated with the segment
	 */
	public List<Action> getInvertedActions() {
		return invertedActions;
	}

}