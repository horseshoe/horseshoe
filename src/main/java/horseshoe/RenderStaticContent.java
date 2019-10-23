package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import horseshoe.internal.ParsedLine;

final class RenderStaticContent implements Action {

	private static final ParsedLine EMPTY_LINES[] = new ParsedLine[0];

	private final ParsedLine lines[];
	private boolean ignoreFirstLine = false;
	private boolean ignoreLastLine = false;

	/**
	 * Creates a new render static content action using the specified lines. The list of lines must contain at least one line.
	 *
	 * @param lines the list of lines
	 */
	RenderStaticContent(final List<ParsedLine> lines) {
		this.lines = lines.toArray(EMPTY_LINES);
	}

	@Override
	public void perform(final RenderContext context, final Writer writer) throws IOException {
		final String indentation = context.getIndentation().peek();

		// No indentation on first line
		if (!ignoreFirstLine && lines.length > 1) {
			final ParsedLine line = lines[0];

			writer.write(line.getLine());
			writer.write(context.getSettings().getLineEnding() == null ? line.getEnding() : context.getSettings().getLineEnding());
		}

		// Indent all remaining lines
		for (int i = 1; i < lines.length - 1; i++ ) {
			final ParsedLine line = lines[i];

			if (!line.getLine().isEmpty()) {
				writer.write(indentation);
				writer.write(line.getLine());
			}

			writer.write(context.getSettings().getLineEnding() == null ? line.getEnding() : context.getSettings().getLineEnding());
		}

		// Skip line ending on the last line
		if (!ignoreLastLine) {
			writer.write(indentation);
			writer.write(lines[lines.length - 1].getLine());
		}
	}

	String getFirstLine() {
		return lines[0].getLine();
	}

	String getLastLine() {
		return lines[lines.length - 1].getLine();
	}

	void ignoreFirstLine() {
		ignoreFirstLine = true;
	}

	void ignoreLastLine() {
		ignoreLastLine = true;
	}

	boolean isMultiline() {
		return lines.length > 1;
	}

}
