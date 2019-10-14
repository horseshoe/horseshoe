package horseshoe;

import java.io.PrintStream;
import java.util.List;

class RenderPartial implements Action {

	private final List<Action> actions;
	private final String indentation;

	/**
	 * Creates a new render partial action
	 *
	 * @param actions the list of actions for the partial
	 * @param indentation the indentation for the partial
	 */
	RenderPartial(final List<Action> actions, final String indentation) {
		this.actions = actions;
		this.indentation = indentation;
	}

	@Override
	public void perform(final RenderContext context, final PrintStream stream) {
		final String newIndentation = context.getIndentation().peek() + indentation;

		context.getIndentation().push(newIndentation);
		stream.print(newIndentation); // Always indent the first line manually

		for (final Action action : actions) {
			action.perform(context, stream);
		}

		context.getIndentation().pop();
	}

}
