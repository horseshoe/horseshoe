package horseshoe.actions;

import horseshoe.Context;

import java.io.PrintStream;

public class RenderText implements IAction {

	private final String[] lines;
	private boolean ignoreFirstLine = false;
	private boolean ignoreLastLine = false;

	public RenderText(final String[] lines) {
		this.lines = lines;
	}

	@Override
	public void perform(final Context context, final PrintStream stream) {
		for (int i = (ignoreFirstLine ? 1 : 0); i < lines.length - 1; i++ ) {
			stream.println(lines[i]);
		}

		if (!ignoreLastLine) {
			stream.print(lines[lines.length - 1]);
		}
	}

	public String getFirstLine() {
		return lines[0];
	}

	public String getLastLine() {
		return lines[lines.length - 1];
	}

	public void ignoreFirstLine() {
		ignoreFirstLine = true;
	}

	public void ignoreLastLine() {
		ignoreLastLine = true;
	}

	public boolean isMultiline() {
		return lines.length > 1;
	}

}
