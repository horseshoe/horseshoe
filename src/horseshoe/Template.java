package horseshoe;

import horseshoe.actions.IAction;
import horseshoe.actions.RenderText;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template {

	private static class Stack<T> extends java.util.ArrayList<T> {
		private class Iterator implements java.util.Iterator<T> {
			int i = size();

			@Override
			public boolean hasNext() {
				return i != 0;
			}

			@Override
			public T next() {
				return get(--i);
			}
		}

		@Override
		public java.util.Iterator<T> iterator() {
			return new Iterator();
		}

		public T peek() {
			return get(size() - 1);
		}

		public T pop() {
			return remove(size() - 1);
		}

		public T push(T obj) {
			add(obj);
			return obj;
		}
	}

	private static class Delimiter {
		private static final String DEFAULT_START = "{{";
		private static final String DEFAULT_END = "}}";
		private static final Pattern DEFAULT_START_PATTERN = Pattern.compile(Pattern.quote(DEFAULT_START));
		private static final Pattern DEFAULT_END_PATTERN = Pattern.compile(Pattern.quote(DEFAULT_END));

		private final String start;
		private final String end;
		private final Pattern startPattern;
		private final Pattern endPattern;

		public Delimiter() {
			this.start = DEFAULT_START;
			this.end = DEFAULT_END;
			this.startPattern = DEFAULT_START_PATTERN;
			this.endPattern = DEFAULT_END_PATTERN;
		}

		public Delimiter(final String start, final String end) {
			this.start = start;
			this.end = end;
			this.startPattern = Pattern.compile(Pattern.quote(start));
			this.endPattern = Pattern.compile(Pattern.quote(end));
		}
	}

	private static class ParseContext {
		public final Stack<FileFrame> fileStack = new Stack<>();
		public final Stack<Section> sectionStack = new Stack<>();

		public ParseContext(Path firstFile) {
			fileStack.push(new FileFrame(firstFile));
			sectionStack.push(new Section(null));
		}
	}

	private static class Section {
		private final String name;
		private final List<IAction> actions = new ArrayList<>();

		public Section(String name) {
			this.name = name;
		}
	}

	private final Pattern ONLY_WHITESPACE_PATTERN = Pattern.compile("\\s*");
	private final Pattern SET_DELIMITER_PATTERN = Pattern.compile("=\\s*([^\\s]+)\\s+([^\\s]+)\\s*=");

	private final List<IAction> actions;

	public Template(final Path file, final Charset charset) throws ParseException {
		final ParseContext context = new ParseContext(file);

		try (final Scanner scanner = new Scanner(file, charset.name())) {
			if (!load(scanner, context)) {
				throw new ParseException(context.fileStack, "Unexpected end of file, unmatched start tag");
			}
		} catch (IOException e) {
			throw new ParseException(context.fileStack, "File could not be opened (" + e.getMessage() + ")");
		}

		this.actions = context.sectionStack.pop().actions;
	}

	public Template(final InputStream stream, final Charset charset) throws ParseException {
		final ParseContext context = new ParseContext(null);

		try (final Scanner scanner = new Scanner(stream, charset.name())) {
			if (!load(scanner, context)) {
				throw new ParseException(context.fileStack, "Unexpected end of stream, unmatched start tag");
			}
		}

		this.actions = context.sectionStack.pop().actions;
	}

	private boolean load(final Scanner scanner, final ParseContext context) throws ParseException {
		final FileFrame fileFrame = context.fileStack.peek();
		Section section = context.sectionStack.peek();
		Delimiter delimiter = new Delimiter();
		Delimiter activeDelimiter = delimiter;
		RenderText previousText = null;

		fileFrame.initialize();

		// Parse all tags
		for (scanner.useDelimiter(delimiter.startPattern); scanner.hasNext(); scanner.skip(activeDelimiter.endPattern)) {
			// Get text before this tag
			RenderText currentText = null;
			final String text = scanner.next();

			if (!text.isEmpty()) {
				final String[] lines = fileFrame.advance(text);
				currentText = new RenderText(lines);

				// Check for standalone tags
				if (previousText != null && lines.length > 1 && (previousText.isMultiline() || previousText == section.actions.get(0)) &&
						ONLY_WHITESPACE_PATTERN.matcher(previousText.getLastLine()).matches() &&
						ONLY_WHITESPACE_PATTERN.matcher(lines[0]).matches()) {
					currentText.ignoreFirstLine();
					previousText.ignoreLastLine();
				}

				section.actions.add(currentText);
			}

			if (!scanner.useDelimiter(delimiter.endPattern).hasNext()) {
				return true;
			}

			previousText = currentText;
			activeDelimiter = delimiter;
			fileFrame.advance(delimiter.start);

			// Parse the expression
			final String expression = scanner.skip(delimiter.startPattern).next();

			if (!expression.isEmpty()) {
				switch (expression.charAt(0)) {
				case '!': // Comments are completely ignored
					break;

				case '#': // Start a new section, or repeat the previous section
					break;
				case '^':
					break;
				case '/':
					break;

				case '=': // Set delimiter
					final Matcher matcher = SET_DELIMITER_PATTERN.matcher(expression);

					if (!matcher.matches()) {
						throw new ParseException(context.fileStack, "Invalid set delimiter tag");
					}

					delimiter = new Delimiter(matcher.group(1), matcher.group(2));
					break;

				default:
					previousText = null;
					throw new ParseException(context.fileStack, "Unrecognized tag starting with '" + expression.charAt(0) + "'");
				}
			}

			if (!scanner.useDelimiter(delimiter.startPattern).hasNext()) {
				return false;
			}

			// Advance past the end delimiter
			fileFrame.advance(expression + activeDelimiter.end);
		}

		if (previousText != null && ONLY_WHITESPACE_PATTERN.matcher(previousText.getLastLine()).matches()) {
			previousText.ignoreLastLine();
		}

		return true;
	}

	public void render(final Context context, final PrintStream stream) {
		for (final IAction action : actions) {
			action.perform(context, stream);
		}
	}

}
