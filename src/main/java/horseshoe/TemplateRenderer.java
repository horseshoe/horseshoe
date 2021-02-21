package horseshoe;

import java.io.IOException;
import java.io.Writer;

final class TemplateRenderer extends StandaloneRenderer {

	private final Template template;
	private final String indentation;

	TemplateRenderer(final Template template, final String indentation) {
		this.template = template;
		this.indentation = indentation;
	}

	@Override
	public void render(final RenderContext context, final Writer writer) throws IOException {
		if (indentation == null) {
			context.getIndentation().push("");
		} else if (isStandalone()) {
			context.getIndentation().push(context.getIndentation().peek() + indentation);
		} else {
			writer.write(indentation);
			context.getIndentation().push("");
		}

		for (final Renderer action : template == null ? context.getSectionPartials().peek().getRenderList() : template.getRenderList()) {
			action.render(context, writer);
		}

		context.getIndentation().pop();
	}

	/**
	 * Gets the template associated with this renderer.
	 *
	 * @return the template associated with this renderer
	 */
	Template getTemplate() {
		return template;
	}

}
