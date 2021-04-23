package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import horseshoe.internal.Expression;

class TemplateRenderer extends TagRenderer {

	private final Template template;
	private final String indentation;
	private final Expression expression;

	TemplateRenderer(final Template template, final String indentation, final Expression expression) {
		this.template = template;
		this.indentation = indentation;
		this.expression = expression;
	}

	TemplateRenderer(final Template template, final String indentation) {
		this(template, indentation, null);
	}

	TemplateRenderer(final String indentation, final boolean standalone, final TemplateRenderer deferred) {
		this(deferred.template, indentation, deferred.expression);
		setStandalone(standalone);
	}

	@Override
	public void render(final RenderContext context, final Writer writer) throws IOException {
		final Template renderTemplate = template == null ? context.getSectionPartials().peek() : template;
		final int bindingsSize = renderTemplate.getBindings().size();
		final Object[] bindings = bindingsSize == 0 ? null : new Object[bindingsSize];
		boolean popData = false;

		if (expression != null) {
			final Object[] args = (Object[]) expression.evaluate(context);

			if (args == null) {
				return;
			} else if (args.length > 0) {
				context.getSectionData().push(new SectionRenderData(args[0]));
				popData = true;
			}

			if (bindings != null) {
				System.arraycopy(args, 0, bindings, 0, Math.min(args.length, renderTemplate.getParameters().size()));
			}
		}

		if (indentation == null) {
			context.getIndentation().push("");
		} else if (isStandalone()) {
			context.getIndentation().push(context.getIndentation().peek() + indentation);
		} else {
			writer.write(indentation);
			context.getIndentation().push("");
		}

		if (bindings != null) {
			final Stack<Object[]> templateBindings = context.getTemplateBindings(renderTemplate.getIndex()).push(bindings);

			renderActions(renderTemplate.getSection().getRenderList(), context, writer);
			templateBindings.pop();
		} else {
			renderActions(renderTemplate.getSection().getRenderList(), context, writer);
		}

		if (popData) {
			context.getSectionData().pop();
		}

		context.getIndentation().pop();
	}

	/**
	 * Renders all actions using the specified context and writer.
	 *
	 * @param actions the actions to render
	 * @param context the render context used when rendering the actions
	 * @param writer the writer to use for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private static void renderActions(final List<Renderer> actions, final RenderContext context, final Writer writer) throws IOException {
		for (final Renderer action : actions) {
			action.render(context, writer);
		}
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
