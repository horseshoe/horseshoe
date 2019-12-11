package horseshoe;

import java.io.IOException;
import java.io.Writer;

final class TemplateRenderer implements Action {

	private final Template template;
	private final StaticContentRenderer priorStaticContent;

	/**
	 * Creates a new render template action
	 *
	 * @param template the template to render
	 * @param priorStaticContent the static content just prior to the template (for partial indentation)
	 */
	public TemplateRenderer(final Template template, final StaticContentRenderer priorStaticContent) {
		this.template = template;
		this.priorStaticContent = priorStaticContent;
	}

	@Override
	public void perform(final RenderContext context, final Writer writer) throws IOException {
		if (priorStaticContent.isLastLineIgnored()) { // This is a stand-alone tag, so use the indentation before the tag and write out the indentation for the first line, since it is not written out as part of the prior static content.
			final String indentation = context.getIndentation().peek() + priorStaticContent.getLastLine();

			context.getIndentation().push(indentation);
		} else { // This is not a stand-alone tag, so don't use the indentation (also, the indentation for the first line is already written).
			context.getIndentation().push(context.getIndentation().peek());
		}

		for (final Action action : template.getActions()) {
			action.perform(context, writer);
		}

		context.getIndentation().pop();
	}

}
