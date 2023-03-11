package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

final class StaticContentRenderer extends Renderer {

	private final ArrayList<Renderer> container;
	private final int containerIndex;
	private final ArrayList<ParsedLine> lines;
	private final boolean followsStandaloneTag;
	private final boolean indentFirstLine;

	private static final class EmptyRenderer extends Renderer {

		@Override
		void render(final RenderContext context, final Writer writer) throws IOException {
			// No content is rendered
		}

	}

	private static class SingleLineNoIndentRenderer extends Renderer {

		private final String line;

		SingleLineNoIndentRenderer(final String line) {
			this.line = line;
		}

		@Override
		void render(final RenderContext context, final Writer writer) throws IOException {
			writer.write(line);
		}

	}

	private static final class SingleLineRenderer extends SingleLineNoIndentRenderer {

		SingleLineRenderer(final String line) {
			super(line);
		}

		@Override
		void render(final RenderContext context, final Writer writer) throws IOException {
			writer.write(context.getIndentation().peek());
			super.render(context, writer);
		}

	}

	private static class MultiLineNoIndentNoLastRenderer extends Renderer {

		protected final ParsedLine[] lines;

		MultiLineNoIndentNoLastRenderer(final ParsedLine[] lines) {
			this.lines = lines;
		}

		@Override
		void render(final RenderContext context, final Writer writer) throws IOException {
			final String indentation = context.getIndentation().peek();
			final String lineEnding = context.getSettings().getLineEndings();

			writer.write(lines[0].getLine());
			writer.write(lineEnding == null ? lines[0].getEnding() : lineEnding);

			// Write all lines (with indentation)
			for (int i = 1; i < lines.length - 1; i++) {
				final ParsedLine line = lines[i];

				if (!line.getLine().isEmpty()) {
					writer.write(indentation);
					writer.write(line.getLine());
				}

				writer.write(lineEnding == null ? line.getEnding() : lineEnding);
			}
		}

	}

	private static final class MultiLineNoIndentRenderer extends MultiLineNoIndentNoLastRenderer {

		MultiLineNoIndentRenderer(final ParsedLine[] lines) {
			super(lines);
		}

		@Override
		void render(final RenderContext context, final Writer writer) throws IOException {
			super.render(context, writer);

			// Skip line ending on the last line
			writer.write(context.getIndentation().peek());
			writer.write(lines[lines.length - 1].getLine());
		}

	}

	private static class MultiLineNoLastRenderer extends Renderer {

		protected final ParsedLine[] lines;

		MultiLineNoLastRenderer(final ParsedLine[] lines) {
			this.lines = lines;
		}

		@Override
		void render(final RenderContext context, final Writer writer) throws IOException {
			final String indentation = context.getIndentation().peek();
			final String lineEnding = context.getSettings().getLineEndings();

			// Write all lines (with indentation)
			for (int i = 0; i < lines.length - 1; i++) {
				final ParsedLine line = lines[i];

				if (!line.getLine().isEmpty()) {
					writer.write(indentation);
					writer.write(line.getLine());
				}

				writer.write(lineEnding == null ? line.getEnding() : lineEnding);
			}
		}

	}

	private static final class MultiLineRenderer extends MultiLineNoLastRenderer {

		MultiLineRenderer(final ParsedLine[] lines) {
			super(lines);
		}

		@Override
		void render(final RenderContext context, final Writer writer) throws IOException {
			super.render(context, writer);

			// Skip line ending on the last line
			writer.write(context.getIndentation().peek());
			writer.write(lines[lines.length - 1].getLine());
		}

	}

	static StaticContentRenderer create(final ArrayList<Renderer> container, final ArrayList<ParsedLine> lines) {
		final StaticContentRenderer renderer = new StaticContentRenderer(container, lines, true, false);

		container.add(renderer);
		return renderer;
	}

	static StaticContentRenderer create(final ArrayList<Renderer> container, final ArrayList<ParsedLine> lines, final boolean indentFirstLine) {
		final StaticContentRenderer renderer = new StaticContentRenderer(container, lines, false, indentFirstLine);

		container.add(renderer);
		return renderer;
	}

	private StaticContentRenderer(final ArrayList<Renderer> container, final ArrayList<ParsedLine> lines, final boolean followsStandaloneTag, final boolean indentFirstLine) {
		this.container = container;
		this.containerIndex = container.size();
		this.lines = lines;
		this.followsStandaloneTag = followsStandaloneTag;
		this.indentFirstLine = indentFirstLine;
	}

	boolean followsStandaloneTag() {
		return followsStandaloneTag;
	}

	@Override
	void render(final RenderContext context, final Writer writer) throws IOException {
		final Renderer replacementAction;
		final int size = lines.size();
		final int offset;
		final boolean indent;

		if (followsStandaloneTag) {
			offset = 1;
			indent = true;
		} else {
			offset = 0;
			indent = indentFirstLine;
		}

		if (size > offset + 1) {
			final ParsedLine[] lineArray = new ParsedLine[size - offset];

			for (int i = 0; i < lineArray.length; i++) {
				lineArray[i] = lines.get(i + offset);
			}

			final String line = lineArray[lineArray.length - 1].getLine();

			if (line == null || (line.isEmpty() && containerIndex == container.size() - 1)) {
				if (indent) {
					replacementAction = new MultiLineNoLastRenderer(lineArray);
				} else {
					replacementAction = new MultiLineNoIndentNoLastRenderer(lineArray);
				}
			} else if (indent) {
				replacementAction = new MultiLineRenderer(lineArray);
			} else {
				replacementAction = new MultiLineNoIndentRenderer(lineArray);
			}
		} else if (size <= offset || lines.get(offset).getLine() == null) {
			replacementAction = new EmptyRenderer();
		} else if (indent) {
			replacementAction = new SingleLineRenderer(lines.get(offset).getLine());
		} else {
			replacementAction = new SingleLineNoIndentRenderer(lines.get(offset).getLine());
		}

		container.set(containerIndex, replacementAction);
		replacementAction.render(context, writer);
	}

}
