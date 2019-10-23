package horseshoe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.CharSequenceUtils;
import horseshoe.internal.Loader;
import horseshoe.internal.ParsedLine;
import horseshoe.internal.PersistentStack;

public class TemplateLoader {

	private static final class LoadContext {

		private final Settings settings;
		private final PersistentStack<Loader> loaders = new PersistentStack<>();

		public LoadContext(final Settings settings) {
			this.settings = settings;
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

	private final Map<String, Template> templates = new HashMap<>();
	private final Map<String, Loader> templateLoaders = new HashMap<>();
	private final List<Path> includeDirectories = new ArrayList<>();

	/**
	 * Creates a template loader using the specified include directories.
	 *
	 * @param includeDirectories the list of directories used to locate template (partial) files included from another template
	 */
	public TemplateLoader(final Iterable<? extends Path> includeDirectories) {
		for (final Path path : includeDirectories) {
			this.includeDirectories.add(path);
		}
	}

	/**
	 * Creates a default template loader. The default list of include directories contains only the current directory.
	 */
	public TemplateLoader() {
		this(Collections.singletonList(Paths.get(".")));
	}

	/**
	 * Gets the list of directories used to locate partial files included in a template. The list of string partials is always searched first.
	 *
	 * @return the list of directories used to locate partial files included in a template
	 */
	public List<Path> getIncludeDirectories() {
		return includeDirectories;
	}

	/**
	 * Adds templates from strings. Any templates already loaded are ignored.
	 *
	 * @param templates a map of template names to template strings
	 * @return this loader
	 */
	public TemplateLoader add(final Map<? extends String, ? extends CharSequence> templates) {
		for (final Entry<? extends String, ? extends CharSequence> entry : templates.entrySet()) {
			add(entry.getKey(), entry.getValue());
		}

		return this;
	}

	/**
	 * Adds a template from a string. If the template is already loaded, this has no effect.
	 *
	 * @param name the name of the template
	 * @param value the string value to load as a template
	 * @return this loader
	 */
	public TemplateLoader add(final String name, final CharSequence value) {
		if (!templates.containsKey(name)) {
			final Loader loader = templateLoaders.put(name, new Loader(name, value));

			if (loader != null) {
				loader.close();
			}
		}

		return this;
	}

	/**
	 * Adds a template from a file. If the template is already loaded, this has no effect.
	 *
	 * @param name the name of the template
	 * @param file the file to load as a template
	 * @return this loader
	 * @throws FileNotFoundException if the file does not exist
	 */
	public TemplateLoader add(final String name, final Path file, final Charset charset) throws FileNotFoundException {
		if (!templates.containsKey(name)) {
			final Loader loader = templateLoaders.put(name, new Loader(name, file, charset));

			if (loader != null) {
				loader.close();
			}
		}

		return this;
	}

	/**
	 * Adds a template using a reader. If the template is already loaded, this has no effect on the internal state of the template loader and the reader is closed.
	 *
	 * @param name the name of the template
	 * @param reader the reader to use to load as a template
	 * @return this loader
	 */
	public TemplateLoader add(final String name, final Reader reader) {
		if (!templates.containsKey(name)) {
			final Loader loader = templateLoaders.put(name, new Loader(name, reader));

			if (loader != null) {
				loader.close();
			}
		} else {
			try {
				reader.close();
			} catch (final IOException e) {
			}
		}

		return this;
	}

	/**
	 * Closes any open readers. If no open readers exist, this has no effect.
	 */
	public void close() {
		for (final Loader loader : templateLoaders.values()) {
			loader.close();
		}

		templateLoaders.clear();
	}

	/**
	 * Loads a template from a string. If the template is already loaded, this has no effect.
	 *
	 * @param name the name of the template
	 * @param value the string value to load as a template
	 * @param settings the settings used to load the template
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final String name, final CharSequence value, final Settings settings) throws LoadException {
		return add(name, value).load(name, settings);
	}

	/**
	 * Loads a template from a file. If the template is already loaded, this has no effect.
	 *
	 * @param name the name of the template
	 * @param file the file to load as a template
	 * @param settings the settings used to load the template
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws FileNotFoundException if the file does not exist
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final String name, final Path file, final Charset charset, final Settings settings) throws FileNotFoundException, LoadException {
		return add(name, file, charset).load(name, settings);
	}

	/**
	 * Loads a template using a reader. If the template is already loaded, this has no effect on the internal state of the template loader and the reader is closed.
	 *
	 * @param name the name of the template
	 * @param reader the reader to use to load as a template
	 * @param settings the settings used to load the template
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final String name, final Reader reader, final Settings settings) throws LoadException {
		return add(name, reader).load(name, settings);
	}

	/**
	 * Loads the specified template using the specified settings. If the template is already loaded the settings are ignored.
	 *
	 * @param name the name of the template to load
	 * @param settings the settings used to load the template
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final String name, final Settings settings) throws LoadException {
		return load(name, new LoadContext(settings));
	}

	/**
	 * Loads the specified template using the specified settings. If the template is already loaded the settings are ignored.
	 *
	 * @param name the name of the template to load
	 * @param context the context used to load the template
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	private Template load(final String name, final LoadContext context) throws LoadException {
		Template template = templates.get(name);

		if (template == null) {
			template = new Template(name);
			templates.put(name, template);

			Loader loader = templateLoaders.remove(name);

			try {
				if (loader == null) {
					// Try to load the template from the list of include directories
					for (final Path directory : includeDirectories) {
						final Path file = directory.resolve(name);

						if (file.toFile().isFile()) {
							loader = new Loader(name, file, context.settings.getCharset());
							break;
						}
					}

					if (loader == null) {
						if (context.settings.getThrowOnTemplateNotFound()) {
							throw new LoadException(context.loaders, "Template \"" + name + "\" could not be found");
						} else {
							return template; // Return empty template, per mustache spec
						}
					}
				}

				loadTemplate(template, context, loader);
			} catch (final IOException e) {
				if (context.settings.getThrowOnTemplateNotFound()) {
					throw new LoadException(context.loaders, "Template \"" + name + "\" could not be loaded due to an I/O error: " + e.getMessage(), e);
				}
			} finally {
				if (loader != null) {
					loader.close();
				}
			}
		}

		return template;
	}

	/**
	 * Loads the list of actions to be performed when rendering the template
	 *
	 * @param context the context used to load the template
	 * @param loader the item being loaded
	 * @return a list of actions to be performed by when rendering the associated template
	 * @throws LoadException if an error is encountered while loading the template
	 * @throws IOException if an error is encountered while reading from a file or stream
	 */
	private void loadTemplate(final Template template, final LoadContext context, final Loader loader) throws LoadException, IOException {
		final PersistentStack<Expression> resolvers = new PersistentStack<>();
		final PersistentStack<List<Action>> actionStack = new PersistentStack<>();

		Delimiter delimiter = new Delimiter();
		RenderStaticContent textBeforeStandaloneTag = new RenderStaticContent(new ArrayList<>(Arrays.asList(new ParsedLine("", ""))));

		context.loaders.push(loader);
		actionStack.push(template.getActions());

		// Parse all tags
		while (true) {
			// Get text before this tag
			final CharSequence freeText = loader.next(delimiter.start);
			final int length = freeText.length();

			if (length == 0) {
				if (!template.getActions().isEmpty()) {
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
				if (currentText.isMultiline() || template.getActions().size() == 1) {
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
				return;
			}

			// Parse the expression
			final CharSequence expression = loader.next(loader.peek("{") ? delimiter.unescapedEnd : delimiter.end);

			if (!loader.hasNext()) {
				throw new LoadException(context.loaders, "Unexpected end of stream, unclosed tag");
			}

			if (expression.length() != 0) {
				try {
					switch (expression.charAt(0)) {
					case '!': // Comments are completely ignored
						break;

					case '>': { // Load partial
						final Template partial = load(CharSequenceUtils.trim(expression, 1, expression.length()).toString(), context);

						if (textBeforeStandaloneTag != null && ONLY_WHITESPACE.matcher(textBeforeStandaloneTag.getLastLine()).matches()) {
							textBeforeStandaloneTag.ignoreLastLine();
							actionStack.peek().add(new RenderTemplate(partial, textBeforeStandaloneTag.getLastLine()));
						} else {
							actionStack.peek().add(new RenderTemplate(partial, ""));
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
							resolvers.push(Expression.load(sectionExpression, resolvers.size()));
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
							resolvers.push(Expression.load(sectionExpression, resolvers.size()));
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

						if (sectionExpression.length() != 0 && !resolver.exactlyMatches(sectionExpression)) {
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
						actionStack.peek().add(new RenderDynamicContent(Expression.load(sectionExpression, resolvers.size()), false));
						break;
					}

					default: {
						final CharSequence sectionExpression = CharSequenceUtils.trim(expression, 0, expression.length());

						textBeforeStandaloneTag = null; // Content tags cannot be stand-alone tags
						actionStack.peek().add(new RenderDynamicContent(Expression.load(sectionExpression, resolvers.size()), true));
						break;
					}
					}
				} catch (final Exception e) {
					throw new LoadException(context.loaders, "Invalid expression: " + expression.subSequence(1, expression.length()), e);
				}
			}

			// Advance past the end delimiter
			loader.advanceInternalPointer(expression.length());
		}
	}

}
