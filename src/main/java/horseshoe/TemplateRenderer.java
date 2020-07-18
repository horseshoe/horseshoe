package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

final class TemplateRenderer implements Renderer {

	private final List<Renderer> container;
	private final int containerIndex;
	private final Template template;
	private final String indentation;

	private static final class AllLineIndentationSectionPartialRenderer implements Renderer {
		private final String indentation;

		public AllLineIndentationSectionPartialRenderer(final String indentation) {
			this.indentation = indentation;
		}

		@Override
		public void render(final RenderContext context, final Writer writer) throws IOException {
			context.getIndentation().push(context.getIndentation().peek() + indentation);

			for (final Renderer action : context.getSectionPartials().peek().getRenderList()) {
				action.render(context, writer);
			}

			context.getIndentation().pop();
		}
	}

	private static final class FirstLineIndentationSectionPartialRenderer implements Renderer {
		private final String indentation;

		public FirstLineIndentationSectionPartialRenderer(final String indentation) {
			this.indentation = indentation;
		}

		@Override
		public void render(final RenderContext context, final Writer writer) throws IOException {
			writer.write(indentation);
			context.getIndentation().push("");

			for (final Renderer action : context.getSectionPartials().peek().getRenderList()) {
				action.render(context, writer);
			}

			context.getIndentation().pop();
		}
	}

	private static final class NoIndentationSectionPartialRenderer implements Renderer {
		@Override
		public void render(final RenderContext context, final Writer writer) throws IOException {
			context.getIndentation().push("");

			for (final Renderer action : context.getSectionPartials().peek().getRenderList()) {
				action.render(context, writer);
			}

			context.getIndentation().pop();
		}
	}

	private static final class AllLineIndentationRenderer implements Renderer {
		private final Template template;
		private final String indentation;

		public AllLineIndentationRenderer(final Template template, final String indentation) {
			this.template = template;
			this.indentation = indentation;
		}

		@Override
		public void render(final RenderContext context, final Writer writer) throws IOException {
			context.getIndentation().push(context.getIndentation().peek() + indentation);

			for (final Renderer action : template.getRenderList()) {
				action.render(context, writer);
			}

			context.getIndentation().pop();
		}
	}

	private static final class FirstLineIndentationRenderer implements Renderer {
		private final Template template;
		private final String indentation;

		public FirstLineIndentationRenderer(final Template template, final String indentation) {
			this.template = template;
			this.indentation = indentation;
		}

		@Override
		public void render(final RenderContext context, final Writer writer) throws IOException {
			writer.write(indentation);
			context.getIndentation().push("");

			for (final Renderer action : template.getRenderList()) {
				action.render(context, writer);
			}

			context.getIndentation().pop();
		}
	}

	private static final class NoIndentationRenderer implements Renderer {
		private final Template template;

		public NoIndentationRenderer(final Template template) {
			this.template = template;
		}

		@Override
		public void render(final RenderContext context, final Writer writer) throws IOException {
			context.getIndentation().push("");

			for (final Renderer action : template.getRenderList()) {
				action.render(context, writer);
			}

			context.getIndentation().pop();
		}
	}

	/**
	 * Creates a new render template action.
	 *
	 * @param template the template to render
	 * @param priorStaticContent the static content just prior to the template (for partial indentation)
	 */
	public TemplateRenderer(final List<Renderer> container, final Template template, final String indentation) {
		this.container = container;
		this.containerIndex = container.size();
		this.template = template;
		this.indentation = indentation;

		container.add(this);
	}

	@Override
	public void render(final RenderContext context, final Writer writer) throws IOException {
		final Renderer newRenderer;
		final Renderer nextRenderer;

		if (template == null) {
			if (indentation == null) {
				newRenderer = new NoIndentationSectionPartialRenderer();
			} else if (container.size() <= containerIndex + 1 || ((nextRenderer = container.get(containerIndex + 1)) instanceof StaticContentRenderer &&
					((StaticContentRenderer)nextRenderer).followsStandaloneTag())) {
				newRenderer = new AllLineIndentationSectionPartialRenderer(indentation);
			} else {
				newRenderer = new FirstLineIndentationSectionPartialRenderer(indentation);
			}
		} else if (indentation == null) {
			newRenderer = new NoIndentationRenderer(template);
		} else if (container.size() <= containerIndex + 1 || ((nextRenderer = container.get(containerIndex + 1)) instanceof StaticContentRenderer &&
				((StaticContentRenderer)nextRenderer).followsStandaloneTag())) {
			newRenderer = new AllLineIndentationRenderer(template, indentation);
		} else {
			newRenderer = new FirstLineIndentationRenderer(template, indentation);
		}

		container.set(containerIndex, newRenderer);
		newRenderer.render(context, writer);
	}

}
