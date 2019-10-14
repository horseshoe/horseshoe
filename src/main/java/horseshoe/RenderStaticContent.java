package horseshoe;

import java.io.PrintStream;
import java.util.List;

import horseshoe.internal.ParsedLine;

class RenderStaticContent implements Action {

	private final List<ParsedLine> lines;
	private boolean ignoreFirstLine = false;
	private boolean ignoreLastLine = false;

	RenderStaticContent(final List<ParsedLine> lines) {
		this.lines = lines;
	}

	@Override
	public void perform(final RenderContext context, final PrintStream stream) {
		for (int i = (ignoreFirstLine ? 1 : 0); i < lines.size() - 1; i++ ) {
			final ParsedLine line = lines.get(i);
			stream.print(lines.get(i).getLine() + (context.getLineEnding() == null ? line.getEnding() : context.getLineEnding()));
		}

		if (!ignoreLastLine) {
			stream.print(lines.get(lines.size() - 1).getLine());
		}
	}

	String getFirstLine() {
		return lines.get(0).getLine();
	}

	String getLastLine() {
		return lines.get(lines.size() - 1).getLine();
	}

	void ignoreFirstLine() {
		ignoreFirstLine = true;
	}

	void ignoreLastLine() {
		ignoreLastLine = true;
	}

	boolean isMultiline() {
		return lines.size() > 1;
	}

}
