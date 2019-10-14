package horseshoe;

import java.util.ArrayList;
import java.util.List;

class Section {

	private final List<Action> actions = new ArrayList<>();
	private final List<Action> invertedActions = new ArrayList<>();

	public Section() {
	}

	public List<Action> getActions() {
		return actions;
	}

	public List<Action> getInvertedActions() {
		return invertedActions;
	}

}