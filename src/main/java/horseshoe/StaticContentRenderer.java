package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import horseshoe.internal.ParsedLine;
import horseshoe.internal.RenderContext;

final class StaticContentRenderer implements Action {

	private static final ParsedLine[] EMPTY_LINES = new ParsedLine[0];

	private final ParsedLine[] lines;
	private final boolean indentFirstLine;
	private boolean ignoreFirstLine = false;
	private boolean ignoreLastLine = false;

	/**
	 * Creates a new render static content action using the specified lines. The list of lines must contain at least one line.
	 *
	 * @param lines the list of lines
	 * @param indentFirstLine true to indent the first line, otherwise false
	 */
	StaticContentRenderer(final List<ParsedLine> lines, final boolean indentFirstLine) {
		this.lines = lines.toArray(EMPTY_LINES);
		this.indentFirstLine = indentFirstLine;
	}

	String getFirstLine() {
		return lines[0].getLine();
	}

	String getLastLine() {
		return lines[lines.length - 1].getLine();
	}

	StaticContentRenderer ignoreFirstLine() {
		ignoreFirstLine = true;
		return this;
	}

	StaticContentRenderer ignoreLastLine() {
		ignoreLastLine = true;
		return this;
	}

	boolean isLastLineIgnored() {
		return ignoreLastLine;
	}

	boolean isMultiline() {
		return lines.length > 1;
	}

	@Override
	public void perform(final RenderContext context, final Writer writer) throws IOException {
		if (lines.length == 1) {
			renderSingleLine(context, writer);
			return;
		}

		final String indentation = context.getIndentation().peek();
		final String lineEnding = context.getSettings().getLineEndings();

		// Only write the first line if it is not ignored
		if (!ignoreFirstLine) {
			if (indentFirstLine) {
				writer.write(indentation);
			}

			writer.write(lines[0].getLine());
			writer.write(lineEnding == null ? lines[0].getEnding() : lineEnding);
		}

		// Write all remaining lines (with indentation)
		for (int i = 1; i < lines.length - 1; i++) {
			final ParsedLine line = lines[i];

			if (!line.getLine().isEmpty()) {
				writer.write(indentation);
				writer.write(line.getLine());
			}

			writer.write(lineEnding == null ? line.getEnding() : lineEnding);
		}

		// Skip line ending on the last line
		if (!ignoreLastLine) {
			writer.write(indentation);
			writer.write(lines[lines.length - 1].getLine());
		}
	}

	/**
	 * Renders the single line content.
	 *
	 * @param context the current render context
	 * @param writer the writer used to render the content
	 * @throws IOException if an error occurs while writing to the writer
	 */
	private void renderSingleLine(final RenderContext context, final Writer writer) throws IOException {
		// Only write the line if it is not ignored
		if (!(ignoreFirstLine || ignoreLastLine)) {
			if (indentFirstLine) {
				writer.write(context.getIndentation().peek());
			}

			writer.write(lines[0].getLine());
		}
	}
}
