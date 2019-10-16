package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

final class RenderPartial implements Action {

	private final List<Action> actions;
	private final String indentation;

	/**
	 * Creates a new render partial action
	 *
	 * @param actions the list of actions for the partial
	 * @param indentation the indentation for the partial
	 */
	public RenderPartial(final List<Action> actions, final String indentation) {
		this.actions = actions;
		this.indentation = indentation;
	}

	@Override
	public void perform(final RenderContext context, final Writer writer) throws IOException {
		final String newIndentation = context.getIndentation().peek() + indentation;

		context.getIndentation().push(newIndentation);
		writer.write(newIndentation); // Always indent the first line manually

		for (final Action action : actions) {
			action.perform(context, writer);
		}

		context.getIndentation().pop();
	}

}
