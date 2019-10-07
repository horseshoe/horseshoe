package horseshoe;

import java.nio.file.Path;
import java.util.regex.Pattern;

public class FileFrame {

	public static final Pattern NEW_LINES = Pattern.compile("\\r\\n?|\\n");

	private final Path file;
	private int line = 0;
	private int column = 0;

	public FileFrame(final Path file) {
		this.file = file;
	}

	@Override
	public String toString() {
		return (file == null ? "[InputStream]" : file.toString()) + (line > 0 ? ", line " + line : "") + (column > 0 ? ", column " + column : "");
	}

	public FileFrame initialize() {
		line = 1;
		column = 1;
		return this;
	}

	public String[] advance(String value) {
		final String[] lines = NEW_LINES.split(value, -1);

		if (lines.length > 1) {
			line += lines.length - 1;
			column = lines[lines.length - 1].length() + 1;
		} else {
			column += value.length();
		}

		return lines;
	}

}
