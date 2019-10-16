package horseshoe;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
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

	private static final class LoadContext {
		private final Context userContext;
		private final PersistentStack<Loader> loaders = new PersistentStack<>();

		public LoadContext(final Context userContext) {
			this.userContext = userContext;
		}
	}

	private static final class Delimiter {
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

	private static final Template EMPTY_TEMPLATE = new Template();
	private static final Template RECURSIVE_TEMPLATE_DETECTED = new Template();

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

		context.loaders.push(loader);
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
					new LoadException(context.loaders, "Unexpected end of stream, unmatched section start tag: \"" + resolvers.peek().toString() + "\"");
				}

				// Check for empty last line of template, so that indentation is not applied
				if (textBeforeStandaloneTag != null && ONLY_WHITESPACE.matcher(textBeforeStandaloneTag.getLastLine()).matches()) {
					textBeforeStandaloneTag.ignoreLastLine();
				}

				context.loaders.pop();
				return templateActions;
			}

			// Parse the expression
			final CharSequence expression = loader.next(loader.peek("{") ? delimiter.unescapedEnd : delimiter.end);

			if (expression.length() != 0) {
				switch (expression.charAt(0)) {
				case '!': // Comments are completely ignored
					break;

				case '>': { // Load partial
					final Template partial = loadPartial(context, CharSequenceUtils.trim(expression, 1, expression.length()).toString());
					final List<Action> actions;

					if (partial == RECURSIVE_TEMPLATE_DETECTED) {
						if (resolvers.isEmpty()) {
							new LoadException(context.loaders, "Partial recursion detected outside of section");
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
							throw new LoadException(context.loaders, "Repeat section without prior section");
						}

						resolvers.push();
					} else { // Start a new section
						resolvers.push(Expression.load(context.userContext, sectionExpression, resolvers.size()));
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
							throw new LoadException(context.loaders, "Section else tag outside section start tag");
						}

						actionStack.pop();
					} else { // Start a new inverted section
						resolvers.push(Expression.load(context.userContext, sectionExpression, resolvers.size()));
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
						throw new LoadException(context.loaders, "Section close tag without matching section start tag");
					}

					final Expression resolver = resolvers.pop();

					if (sectionExpression.length() != 0 && !resolver.matches(sectionExpression)) {
						throw new LoadException(context.loaders, "Unmatched section start tag, expecting: \"" + resolver.toString() + "\"");
					}

					actionStack.pop();
					break;
				}

				case '=': { // Set delimiter
					final Matcher matcher = SET_DELIMITER.matcher(expression);

					if (!matcher.matches()) {
						throw new LoadException(context.loaders, "Invalid set delimiter tag");
					}

					delimiter = new Delimiter(matcher.group(1), matcher.group(2));
					break;
				}

				case '{':
				case '&': {
					final CharSequence sectionExpression = CharSequenceUtils.trim(expression, 1, expression.length());

					textBeforeStandaloneTag = null; // Content tags cannot be stand-alone tags
					actionStack.peek().add(new RenderDynamicContent(Expression.load(context.userContext, sectionExpression, resolvers.size()), false));
					break;
				}

				default: {
					final CharSequence sectionExpression = CharSequenceUtils.trim(expression, 0, expression.length());

					textBeforeStandaloneTag = null; // Content tags cannot be stand-alone tags
					actionStack.peek().add(new RenderDynamicContent(Expression.load(context.userContext, sectionExpression, resolvers.size()), true));
					break;
				}
				}
			}

			if (!loader.hasNext()) {
				throw new LoadException(context.loaders, "Unexpected end of stream, unclosed tag");
			}

			// Advance past the end delimiter
			loader.advanceInternalPointer(expression.length());
		}
	}

	/**
	 * Loads a partial by name.
	 *
	 * @param name the name of the partial
	 * @return The loaded partial
	 * @throws LoadException if an error is encountered while loading the partial
	 */
	private static Template loadPartial(final LoadContext context, final String name) throws LoadException {
		// First, try to load the partial from an existing template
		Template found = context.userContext.partials.get(name);

		if (found == null) {
			// Next, try to load the partial from an internal string
			final String templateText = context.userContext.getStringPartials().get(name);

			if (templateText != null) {
				context.userContext.partials.put(name, RECURSIVE_TEMPLATE_DETECTED);
				found = new Template(name, templateText, context.userContext);
				context.userContext.partials.put(name, found);
			} else {
				// Lastly, try to load the partial from file
				final Path currentDirectoryFile = Paths.get(name);

				// Try to load the partial from the current directory
				if (currentDirectoryFile.toFile().isFile()) {
					context.userContext.partials.put(name, RECURSIVE_TEMPLATE_DETECTED);
					found = new Template(currentDirectoryFile, context.userContext);
					context.userContext.partials.put(name, found);
					return found;
				}

				// Try to load the partial from the list of include directories
				for (final Path directory : context.userContext.getIncludeDirectories()) {
					final Path file = directory.resolve(name);

					if (file.toFile().isFile()) {
						context.userContext.partials.put(name, RECURSIVE_TEMPLATE_DETECTED);
						found = new Template(file, context.userContext);
						context.userContext.partials.put(name, found);
						return found;
					}
				}

				if (found == null) {
					if (context.userContext.getThrowOnPartialNotFound()) {
						throw new LoadException(context.loaders, "Partial not found: " + name);
					} else {
						return EMPTY_TEMPLATE;
					}
				}
			}
		}

		return found;
	}

	private final List<Action> actions;

	private Template() {
		this.actions = new ArrayList<>();
	}

	public Template(final String name, final CharSequence value, final Context context) throws LoadException {
		try (final Loader loader = new Loader(name, value)) {
			this.actions = load(new LoadContext(context), loader);
		} catch (final IOException e) {
			// This should never happen, since we are loading from a string
			throw new RuntimeException("An internal error occurred while loading a template from a character sequence", e);
		}
	}

	public Template(final Path file, final Context context) throws LoadException {
		final LoadContext loadContext = new LoadContext(context);

		try (final Loader loader = new Loader(file.toString(), file, context.getCharset())) {
			this.actions = load(loadContext, loader);
		} catch (final IOException e) {
			throw new LoadException(loadContext.loaders, "File could not be opened (" + e.getMessage() + ")");
		}
	}

	public Template(final String name, final Reader reader, final Context context) throws LoadException, IOException {
		try (final Loader loader = new Loader(name, reader)) {
			this.actions = load(new LoadContext(context), loader);
		}
	}

	/**
	 * Renders the template to the specified writer using the specified context
	 *
	 * @param context the context used while rendering
	 * @param writer the writer used to render the template
	 * @throws IOException if an error occurs while writing to the writer
	 */
	public void render(final Context context, final Writer writer) throws IOException {
		final RenderContext renderContext = new RenderContext(context);

		renderContext.getSectionData().push(context.getGlobalData());
		renderContext.getIndentation().push("");

		for (final Action action : actions) {
			action.perform(renderContext, writer);
		}

		renderContext.getIndentation().pop();
		renderContext.getSectionData().pop();
	}

}
