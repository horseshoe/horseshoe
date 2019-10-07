package horseshoe.actions;

import horseshoe.Context;

import java.io.PrintStream;

public interface IAction {
	void perform(final Context context, final PrintStream stream);
}
