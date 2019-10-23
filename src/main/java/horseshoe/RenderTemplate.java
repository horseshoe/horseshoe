package horseshoe;

import java.io.IOException;
import java.io.Writer;

final class RenderTemplate implements Action {

	private final Template template;
	private final String indentation;

	/**
	 * Creates a new render template action
	 *
	 * @param template the template to render
	 * @param indentation the indentation for the template
	 */
	public RenderTemplate(final Template template, final String indentation) {
		this.template = template;
		this.indentation = indentation;
	}

	@Override
	public void perform(final RenderContext context, final Writer writer) throws IOException {
		final String newIndentation = context.getIndentation().peek() + indentation;

		context.getIndentation().push(newIndentation);
		writer.write(newIndentation); // Always indent the first line manually

		for (final Action action : template.getActions()) {
			action.perform(context, writer);
		}

		context.getIndentation().pop();
	}

}
