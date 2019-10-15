package horseshoe;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.CharSequenceUtils;
import horseshoe.internal.Loader;
import horseshoe.internal.ParsedLine;
import horseshoe.internal.PersistentStack;

public class Template {

	private static class Delimiter {
		private static final Pattern DEFAULT_START = Pattern.compile(Pattern.quote("{{"));
		private static final Pattern DEFAULT_END = Pattern.compile(Pattern.quote("}}"));
		private static final Pattern DEFAULT_UNESCAPED_END = Pattern.compile(Pattern.quote("}}}"));

		private final Pattern start;
		private final Pattern end;
		private final Pattern unescapedEnd;

		public Delimiter() {
			this.start = DEFAULT_START;
			this.end = DEFAULT_END;
			this.unescapedEnd = DEFAULT_UNESCAPED_END;
		}

		public Delimiter(final String start, final String end) {
			this.start = Pattern.compile(Pattern.quote(start));
			this.end = Pattern.compile(Pattern.quote(end));
			this.unescapedEnd = Pattern.compile(Pattern.quote("}" + end));
		}
	}

	private static final Pattern ONLY_WHITESPACE = Pattern.compile("\\s*");
	private static final Pattern SET_DELIMITER = Pattern.compile("=\\s*([^\\s]+)\\s+([^\\s]+)\\s*=");

	/**
	 * Loads and returns a list of actions to be performed by when rendering the associated template
	 *
	 * @param context the context used to load the template
	 * @param loader the item being loaded
	 * @return a list of actions to be performed by when rendering the associated template
	 * @throws LoadException if an error is encountered while loading the template
	 * @throws IOException if an error is encountered while reading from a file or stream
	 */
	private static List<Action> load(final LoadContext context, final Loader loader) throws LoadException, IOException {
		final List<Action> templateActions = new ArrayList<>();
		final PersistentStack<Expression> resolvers = new PersistentStack<>();
		final PersistentStack<List<Action>> actionStack = new PersistentStack<>();

		Delimiter delimiter = new Delimiter();
		RenderStaticContent textBeforeStandaloneTag = new RenderStaticContent(new ArrayList<>(Arrays.asList(new ParsedLine("", ""))));

		context.pushLoader(loader);
		actionStack.push(templateActions);

		// Parse all tags
		while (true) {
			// Get text before this tag
			final CharSequence freeText = loader.next(delimiter.start);
			final int length = freeText.length();

			if (length == 0) {
				if (!templateActions.isEmpty()) {
					loader.advanceInternalPointer(0);

					// The text can only be following a stand-alone tag if it is at the very end of the template
					if (!loader.hasNext() && textBeforeStandaloneTag != null && ONLY_WHITESPACE.matcher(textBeforeStandaloneTag.getLastLine()).matches()) {
						textBeforeStandaloneTag.ignoreLastLine();
					}

					// Empty text can never be just before a stand-alone tag, unless it is the first content in the template
					textBeforeStandaloneTag = null;
				}
			} else {
				final List<ParsedLine> lines = new ArrayList<>();
				loader.advanceInternalPointer(length, lines);
				final RenderStaticContent currentText = new RenderStaticContent(lines);
				actionStack.peek().add(currentText);

				// Check for stand-alone tags
				if (textBeforeStandaloneTag != null && currentText.isMultiline() &&
						ONLY_WHITESPACE.matcher(textBeforeStandaloneTag.getLastLine()).matches() &&
						ONLY_WHITESPACE.matcher(currentText.getFirstLine()).matches()) {
					textBeforeStandaloneTag.ignoreLastLine();
					currentText.ignoreFirstLine();
				}

				// Use the current text as the basis for future stand-alone tag detection if the currentText is multiline or it is the first line in the template
				if (currentText.isMultiline() || templateActions.size() == 1) {
					textBeforeStandaloneTag = currentText;
				} else {
					textBeforeStandaloneTag = null;
				}
			}

			if (!loader.hasNext()) {
				if (!resolvers.isEmpty()) {
					new LoadException(context.reset(), "Unexpected end of stream, unmatched section start tag: \"" + resolvers.peek().toString() + "\"");
				}

				// Check for empty last line of template, so that indentation is not applied
				if (textBeforeStandaloneTag != null && ONLY_WHITESPACE.matcher(textBeforeStandaloneTag.getLastLine()).matches()) {
					textBeforeStandaloneTag.ignoreLastLine();
				}

				context.popLoader();
				return templateActions;
			}

			// Parse the expression
			final CharSequence expression = loader.next(loader.peek("{") ? delimiter.unescapedEnd : delimiter.end);

			if (expression.length() != 0) {
				switch (expression.charAt(0)) {
				case '!': // Comments are completely ignored
					break;

				case '>': { // Load partial
					final Template partial = context.loadPartial(CharSequenceUtils.trim(expression, 1, expression.length()).toString());
					final List<Action> actions;

					if (partial == LoadContext.RECURSIVE_TEMPLATE_DETECTED) {
						if (resolvers.isEmpty()) {
							new LoadException(context.reset(), "Partial recursion detected outside of section");
						}

						actions = templateActions;
					} else {
						actions = partial.actions;
					}

					if (textBeforeStandaloneTag != null && ONLY_WHITESPACE.matcher(textBeforeStandaloneTag.getLastLine()).matches()) {
						textBeforeStandaloneTag.ignoreLastLine();
						actionStack.peek().add(new RenderPartial(actions, textBeforeStandaloneTag.getLastLine()));
					} else {
						actionStack.peek().add(new RenderPartial(actions, ""));
					}

					break;
				}

				case '#': { // Start a new section, or repeat the previous section
					final CharSequence sectionExpression = CharSequenceUtils.trim(expression, 1, expression.length());

					if (sectionExpression.length() == 0) { // Repeat the previous section
						if (!resolvers.hasPoppedItem()) {
							throw new LoadException(context.reset(), "Repeat section without prior section");
						}

						resolvers.push();
					} else { // Start a new section
						resolvers.push(Expression.load(context, sectionExpression, resolvers.size()));
					}

					// Add a new render section action
					final Section section = new Section();

					actionStack.peek().add(RenderSection.FACTORY.create(resolvers.peek(), section));
					actionStack.push(section.getActions());
					break;
				}

				case '^': { // Start a new inverted section, or else block for the current section
					final CharSequence sectionExpression = CharSequenceUtils.trim(expression, 1, expression.length());

					if (sectionExpression.length() == 0) { // Else block for the current section
						if (resolvers.isEmpty()) {
							throw new LoadException(context.reset(), "Section else tag outside section start tag");
						}

						actionStack.pop();
					} else { // Start a new inverted section
						resolvers.push(Expression.load(context, sectionExpression, resolvers.size()));
						actionStack.peek().add(RenderSection.FACTORY.create(resolvers.peek(), new Section()));
					}

					// Grab the inverted action list from the section
					final List<Action> actions = actionStack.peek();

					actionStack.push(((RenderSection)actions.get(actions.size() - 1)).getSection().getInvertedActions());
					break;
				}

				case '/': { // Close the current section
					final CharSequence sectionExpression = CharSequenceUtils.trim(expression, 1, expression.length());

					if (resolvers.isEmpty()) {
						throw new LoadException(context.reset(), "Section close tag without matching section start tag");
					}

					final Expression resolver = resolvers.pop();

					if (sectionExpression.length() != 0 && !resolver.equals(sectionExpression)) {
						throw new LoadException(context.reset(), "Unmatched section start tag, expecting: \"" + resolver.toString() + "\"");
					}

					actionStack.pop();
					break;
				}

				case '=': { // Set delimiter
					final Matcher matcher = SET_DELIMITER.matcher(expression);

					if (!matcher.matches()) {
						throw new LoadException(context.reset(), "Invalid set delimiter tag");
					}

					delimiter = new Delimiter(matcher.group(1), matcher.group(2));
					break;
				}

				case '{':
				case '&': {
					final CharSequence sectionExpression = CharSequenceUtils.trim(expression, 1, expression.length());

					textBeforeStandaloneTag = null; // Content tags cannot be stand-alone tags
					actionStack.peek().add(new RenderDynamicContent(Expression.load(context, sectionExpression, resolvers.size()), false));
					break;
				}

				default: {
					final CharSequence sectionExpression = CharSequenceUtils.trim(expression, 0, expression.length());

					textBeforeStandaloneTag = null; // Content tags cannot be stand-alone tags
					actionStack.peek().add(new RenderDynamicContent(Expression.load(context, sectionExpression, resolvers.size()), true));
					break;
				}
				}
			}

			if (!loader.hasNext()) {
				throw new LoadException(context.reset(), "Unexpected end of stream, unclosed tag");
			}

			// Advance past the end delimiter
			loader.advanceInternalPointer(expression.length());
		}
	}

	private final List<Action> actions;

	public Template() {
		this.actions = new ArrayList<>();
	}

	public Template(final String name, final CharSequence value, final LoadContext context) throws LoadException {
		try (final Loader loader = new Loader(name, value)) {
			this.actions = load(context, loader);
		} catch (final IOException e) {
			// This should never happen, since we are loading from a string
			throw new RuntimeException("An internal error occurred while loading a template from a character sequence", e);
		}
	}

	public Template(final Path file, final Charset charset, final LoadContext context) throws LoadException {
		try (final Loader loader = new Loader(file.toString(), file, charset)) {
			this.actions = load(context, loader);
		} catch (final IOException e) {
			throw new LoadException(context.reset(), "File could not be opened (" + e.getMessage() + ")");
		}
	}

	public Template(final String name, final InputStream stream, final Charset charset, final LoadContext context) throws LoadException, IOException {
		try (final Loader loader = new Loader(name, stream, charset)) {
			this.actions = load(context, loader);
		}
	}

	public void render(final RenderContext context, final PrintStream stream) {
		context.getSectionData().push(context.getGlobalData());
		context.getIndentation().push("");

		for (final Action action : actions) {
			action.perform(context, stream);
		}

		context.getIndentation().pop();
		context.getSectionData().pop();
	}

}