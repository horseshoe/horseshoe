package horseshoe;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.CacheList;
import horseshoe.internal.Expression;
import horseshoe.internal.ExpressionParseState;
import horseshoe.internal.Identifier;
import horseshoe.internal.ParsedLine;
import horseshoe.internal.Utilities;
import horseshoe.internal.Utilities.TrimmedString;

/**
 * The TemplateLoader class is used to load any number of {@link Template}s before rendering. Various properties can be configured to load templates with different settings.
 */
public class TemplateLoader {

	private static final Pattern SET_DELIMITER_PATTERN = Pattern.compile("=\\s*(?<start>[^\\s]+)\\s+(?<end>[^\\s]+)\\s*=", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern ANNOTATION_PATTERN = Pattern.compile("(?<name>@" + Identifier.PATTERN + ")\\s*(?:[(]\\s*(?<arguments>.*)\\s*[)]\\s*)?", Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);

	private static final String IDENTIFIER_OPTIONAL_PARENS = Identifier.PATTERN + "(?:\\s*[(]\\s*[)])?";
	private static final Pattern LOAD_PARTIAL_PATTERN = Pattern.compile("\\s*(?:(?<nameOnly>[^|]+?)\\s*|(?<partial>[^\\s].*?)\\s*[|]\\s*(?<imports>|[*]|" + IDENTIFIER_OPTIONAL_PARENS + "(?:\\s*,\\s*" + IDENTIFIER_OPTIONAL_PARENS + ")*)\\s*)?", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile(Identifier.PATTERN, Pattern.UNICODE_CHARACTER_CLASS);

	private final Map<Object, Template> templates = new HashMap<>();
	private final List<Path> includeDirectories = new ArrayList<>();
	private Charset charset = StandardCharsets.UTF_8;
	private boolean preventPartialPathTraversal = true;
	private EnumSet<Extension> extensions = EnumSet.allOf(Extension.class);

	private static final class Delimiter {

		private final String start;
		private final String end;
		private final String unescapedEnd;

		public Delimiter(final String start, final String end) {
			this.start = start;
			this.end = end;
			this.unescapedEnd = "}" + end;
		}

	}

	private final class Loadable {

		private final Template template;
		private final Loader loader;

		public Loadable(final Template template, final Loader loader) {
			this.template = template;
			this.loader = loader;
		}

		public Template load() throws IOException, LoadException {
			try {
				return loadTemplate(template, loader, new Stack<Loader>());
			} finally {
				loader.close();
			}
		}

	}

	private static final class LoadState {

		public static final int TAG_CAN_BE_STANDALONE = 0;
		public static final int TAG_CANT_BE_STANDALONE = 1;
		public static final int TAG_CHECK_TAIL_STANDALONE = 2;

		private final Loader loader;
		private final Stack<Section> sections = new Stack<>();
		private final Map<Identifier, Identifier> allIdentifiers = new HashMap<>();
		private final CacheList<String> bindings = new CacheList<>();
		private final Map<String, Expression> expressionCache = new HashMap<>();
		private final Stack<List<Renderer>> renderLists = new Stack<>();
		private Delimiter delimiter = new Delimiter("{{", "}}");
		private List<ParsedLine> priorStaticText = new ArrayList<>();
		private int standaloneStatus = TAG_CANT_BE_STANDALONE;
		private int tagCount = 0;

		public LoadState(final Loader loader) {
			this.loader = loader;
			this.priorStaticText.add(new ParsedLine("", ""));
		}

		/**
		 * Create a new expression checking the cache to see if it already exists.
		 *
		 * @param location the location of the expression
		 * @param expression the trimmed expression string
		 * @param extensions the set of extensions currently in use
		 * @return the new expression or a cached copy of the expression with updated location
		 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading the expression
		 */
		private Expression createExpression(final Object location, final ExpressionParseState parseState, final EnumSet<Extension> extensions) throws ReflectiveOperationException {
			final Expression cachedExpression = expressionCache.get(parseState.getExpressionString());
			final Expression newExpression = new Expression(cachedExpression, location, parseState, extensions.contains(Extension.EXPRESSIONS));

			expressionCache.put(parseState.getExpressionString(), newExpression);
			return newExpression;
		}

		/**
		 * Create a new expression parser from the load state.
		 *
		 * @param expression the trimmed expression string
		 * @return the new expression parser
		 */
		private ExpressionParseState createExpressionParser(final TrimmedString expression) {
			return new ExpressionParseState(expression.start, expression.string, sections.peek().getNamedExpressions(), allIdentifiers, bindings);
		}

		/**
		 * Checks if the given lines could be the tail end of a standalone tag.
		 *
		 * @param lines the lines of the static text
		 * @return true if the lines could be the tail end of a standalone tag, otherwise false
		 */
		private boolean isStandaloneTagTail(final List<ParsedLine> lines) {
			final int size = lines.size();

			if (size > 1) {
				return Utilities.isWhitespace(lines.get(0).getLine());
			}

			return !loader.hasNext() && Utilities.isWhitespace(lines.get(0).getLine());
		}

		/**
		 * Removes the leading whitespace from the beginning of a potential standalone tag.
		 *
		 * @return the leading whitespace
		 */
		private String removeStandaloneTagHead() {
			final int size = priorStaticText.size();
			final String removedWhitespace;

			if (size > 1 || tagCount == 1) {
				removedWhitespace = priorStaticText.get(size - 1).getLine();

				if (Utilities.isWhitespace(removedWhitespace)) {
					priorStaticText.set(size - 1, new ParsedLine(null, ""));
					return removedWhitespace;
				}
			}

			return null;
		}

	}

	/**
	 * Creates a new template loader that is mustache-compatible.
	 *
	 * @return the new mustache loader
	 */
	public static TemplateLoader newMustacheLoader() {
		return new TemplateLoader().setExtensions(EnumSet.noneOf(Extension.class));
	}

	/**
	 * Creates a default template loader.
	 */
	public TemplateLoader() {
	}

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
	 * Gets the loaded template matching the specified identifier.
	 *
	 * @param identifier the identifier of the template
	 * @return the loaded template, or null if the template could not be found
	 */
	public final Template get(final Object identifier) {
		return templates.get(identifier);
	}

	/**
	 * Gets the character set used for loading additional templates.
	 *
	 * @return the character set used for loading additional templates
	 */
	public final Charset getCharset() {
		return charset;
	}

	/**
	 * Gets the list of directories used to locate partial files included in a template. The list of string partials is always searched first.
	 *
	 * @return the list of directories used to locate partial files included in a template
	 */
	public final List<Path> getIncludeDirectories() {
		return includeDirectories;
	}

	/**
	 * Gets whether or not traversing paths ("/..." or "../...") is prevented when loading partials.
	 *
	 * @return true if traversing paths is prevented when loading partials, otherwise false
	 */
	public final boolean getPreventPartialPathTraversal() {
		return preventPartialPathTraversal;
	}

	/**
	 * Gets the Horseshoe extensions that are enabled.
	 *
	 * @return the set of enabled Horseshoe extensions
	 */
	public final EnumSet<Extension> getExtensions() {
		return extensions;
	}

	/**
	 * Loads a named template from a string. The template is loaded and cached in the loader.
	 *
	 * @param name the name of the template
	 * @param value the string value to load as a template, not a filename
	 * @return the loaded template
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final Template load(final String name, final String value) throws LoadException {
		try (final Loader loader = new Loader(name, value)) {
			return loadTemplate(new Template(name, name), loader, new Stack<Loader>());
		} catch (final IOException e) {
			throw new AssertionError("Loader should never throw " + e.getClass().getName(), e);
		}
	}

	/**
	 * Loads a template using a reader. The template is loaded and cached in the loader.
	 *
	 * @param name the name of the template
	 * @param reader the reader to use to load as a template
	 * @return the loaded template
	 * @throws IOException if an I/O exception is encountered while reading from the reader
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final Template load(final String name, final Reader reader) throws IOException, LoadException {
		try (final Loader loader = new Loader(name, reader)) {
			return loadTemplate(new Template(name, name), loader, new Stack<Loader>());
		}
	}

	/**
	 * Loads a template from a file. The template is loaded and cached in the loader.
	 *
	 * @param file the file to load as a template
	 * @param charset the character encoding to use when loading the template from the file
	 * @return the loaded template
	 * @throws IOException if an I/O exception is encountered while opening or reading the file
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final Template load(final Path file, final Charset charset) throws IOException, LoadException {
		final Path absoluteFile = file.toAbsolutePath().normalize();

		try (final Loader loader = new Loader(absoluteFile, charset)) {
			return loadTemplate(new Template(file.toString(), absoluteFile), loader, new Stack<Loader>());
		}
	}

	/**
	 * Loads a template from a file. The template is loaded and cached in the loader.
	 *
	 * @param file the file to load as a template
	 * @return the loaded template
	 * @throws IOException if an I/O exception is encountered while opening or reading the file
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final Template load(final Path file) throws IOException, LoadException {
		return load(file, charset);
	}

	/**
	 * Loads an anonymous template from a string.
	 *
	 * @param value the string value to load as a template
	 * @return the loaded template
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final Template load(final String value) throws LoadException {
		return load(null, value);
	}

	/**
	 * Loads an anonymous template using a reader.
	 *
	 * @param reader the reader to use to load as a template
	 * @return the loaded template
	 * @throws IOException if an I/O exception is encountered while reading from the reader
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final Template load(final Reader reader) throws IOException, LoadException {
		return load(null, reader);
	}

	/**
	 * Loads multiple templates via Reader or String, returning the last template loaded. All templates are created before loading begins, enabling recursive calls among the templates.
	 *
	 * @param toLoad the templates to load, with all templates being preceded by a name, except the last template
	 * @return the last loaded template
	 * @throws IOException if an I/O exception is encountered while reading from a reader
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final Template load(final Object... toLoad) throws IOException, LoadException {
		if (toLoad.length == 0) {
			throw new IllegalArgumentException("At least one template must be specified");
		}

		final List<Loadable> loadables = new ArrayList<>();
		Template lastTemplate = null;
		int i;

		for (i = 0; i < toLoad.length - 1; ) {
			if (toLoad[i] != null && !(toLoad[i] instanceof String)) {
				throw new IllegalArgumentException("Expecting template name at index " + i + ", found " + toLoad[i].getClass().getName());
			}

			final String name = (String)toLoad[i];
			final Template template = new Template(name, name);
			lastTemplate = template;

			if (!templates.containsKey(name)) {
				putTemplate(name, template);
			}

			if (toLoad[i + 1] instanceof String) {
				loadables.add(new Loadable(template, new Loader(name, (String)toLoad[i + 1])));
				i += 2;
			} else if (toLoad[i + 1] instanceof Reader) {
				loadables.add(new Loadable(template, new Loader(name, (Reader)toLoad[i + 1])));
				i += 2;
			} else {
				throw new IllegalArgumentException("Expecting template at index " + (i + 1) + ", found " + (toLoad[i + 1] == null ? "null" : toLoad[i + 1].getClass().getName()));
			}
		}

		for (final Loadable loadable : loadables) {
			loadable.load();
		}

		if (i == toLoad.length) {
			return lastTemplate;
		} else if (toLoad[i] instanceof String) {
			return load((String)toLoad[i]);
		} else if (toLoad[i] instanceof Reader) {
			return load((Reader)toLoad[i]);
		}

		throw new IllegalArgumentException("Expecting template at index " + i + ", found " + (toLoad[i] == null ? "null" : toLoad[i].getClass().getName()));
	}

	/**
	 * Loads the specified template using the specified settings. If the template is already loaded, the previously loaded instance is returned and all settings are ignored.
	 *
	 * @param name the name of the template to load
	 * @param loaders the stack of items being loaded
	 * @return the loaded template
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	private Template loadPartial(final String name, final Stack<Loader> loaders) throws LoadException {
		try {
			// Try to use a cached template
			final Template cachedTemplate = get(name);

			if (cachedTemplate != null) {
				return cachedTemplate;
			}

			// Load the template
			final Template loadedTemplate = tryLoad(name, loaders);

			if (loadedTemplate != null) {
				return loadedTemplate;
			}
		} catch (final IOException | RuntimeException e) {
			// This probably indicates a file access error, so re-throw as a load exception
			throw new LoadException(loaders, "Template \"" + name + "\" could not be loaded: " + e.getMessage(), e);
		}

		// Throw an exception or warn if the template could not be found
		if (extensions.contains(Extension.THROW_ON_PARTIAL_NOT_FOUND)) {
			throw new LoadException(loaders, "Template \"" + name + "\" could not be found");
		}

		Template.LOGGER.log(Level.WARNING, "Template \"{0}\" could not be found", name);

		// Return empty template, per mustache spec
		return new Template(name, null);
	}

	/**
	 * Loads the given partial tag.
	 *
	 * @param loaders the current stack of loaders
	 * @param state the load state of the template
	 * @param tag the string representation of the tag
	 * @return the name of the loaded partial
	 * @throws LoadException if an error is encountered while loading the partial
	 */
	private String loadPartial(final Stack<Loader> loaders, final LoadState state, final String tag) throws LoadException {
		final Matcher matcher;
		String indentation = null;

		if (tag.length() > 1 && tag.charAt(1) == '>') { // >> uses indentation on first line, no trailing new line
			matcher = LOAD_PARTIAL_PATTERN.matcher(tag).region(2, tag.length());
			state.standaloneStatus = LoadState.TAG_CHECK_TAIL_STANDALONE;
		} else {
			matcher = LOAD_PARTIAL_PATTERN.matcher(tag).region(1, tag.length());
			indentation = state.removeStandaloneTagHead();

			// 1) Standalone tag uses indentation for each line, no trailing new line
			// 2) Non-standalone tag uses indentation on first line, with trailing new line
			if (indentation == null) {
				state.standaloneStatus = LoadState.TAG_CANT_BE_STANDALONE;
			} else {
				state.standaloneStatus = LoadState.TAG_CHECK_TAIL_STANDALONE;
			}
		}

		// Check if the tag matches the load partial pattern
		if (!matcher.matches()) {
			throw new LoadException(loaders, "Invalid load partial tag");
		}

		final String imports = matcher.group("imports");
		final String name = matcher.group(imports == null ? "nameOnly" : "partial");

		// Check for anonymous partials
		if (name == null) {
			TemplateRenderer.create(state.renderLists.peek(), null, indentation);
			return null;
		}

		// Load the partial and any associated imports
		final Template localPartial = state.sections.peek().getLocalPartials().get(name);
		final Template partial = (localPartial != null ? localPartial : loadPartial(name, loaders));

		if ("*".equals(imports)) {
			state.sections.peek().getNamedExpressions().putAll(partial.getSection().getNamedExpressions());
		} else if (imports != null) {
			for (final Matcher identifiers = IDENTIFIER_PATTERN.matcher(imports); identifiers.find(); ) {
				final String expressionName = identifiers.group();
				final Expression expression = partial.getSection().getNamedExpressions().get(expressionName);

				if (expression == null) {
					throw new LoadException(loaders, "Expression \"" + expressionName + "\" not found in partial");
				}

				state.sections.peek().getNamedExpressions().put(expressionName, expression);
			}
		}

		TemplateRenderer.create(state.renderLists.peek(), partial, indentation);
		return name;
	}

	/**
	 * Tries to load the specified template by file. If the template is already loaded, the previously loaded instance is returned and all settings are ignored.
	 *
	 * @param name the name of the template to load
	 * @param absoluteFile the absolute, normalized template file to load
	 * @param loaders the stack of items being loaded
	 * @return the loader if the file can be loaded, otherwise null
	 * @throws IOException if there is an IOException when loading from a file
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	private Template loadTemplate(final String name, final Path absoluteFile, final Stack<Loader> loaders) throws IOException, LoadException {
		final Template cachedTemplate = get(absoluteFile);

		if (cachedTemplate != null) {
			return cachedTemplate;
		}

		// Ensure the file exists and access is not prevented via partial path traversal before loading from file
		if (!absoluteFile.toFile().isFile()) {
			return null;
		} else if (getPreventPartialPathTraversal() && (loaders.isEmpty() || loaders.peekBase().getFile() == null || !absoluteFile.startsWith(loaders.peekBase().getFile().getParent()))) {
			boolean matches = false;

			// Check if the file begins with the directory of one of the include directories
			for (final Path directory : includeDirectories) {
				if (absoluteFile.startsWith(directory.toAbsolutePath().normalize())) {
					matches = true;
					break;
				}
			}

			if (!matches) {
				return null;
			}
		}

		try (Loader loader = new Loader(absoluteFile, charset)) {
			return loadTemplate(new Template(name, absoluteFile), loader, loaders);
		}
	}

	/**
	 * Loads the list of actions to be performed when rendering the template.
	 *
	 * @param template the template to load
	 * @param loader the item being loaded
	 * @param loaders the stack of items being loaded
	 * @return the loaded template
	 * @throws IOException if an error is encountered while reading from a file or stream
	 * @throws LoadException if an error is encountered while loading the template
	 */
	protected final Template loadTemplate(final Template template, final Loader loader, final Stack<Loader> loaders) throws IOException, LoadException {
		putTemplate(template.getIdentifier(), template);
		Template.LOGGER.log(Level.FINE, "Loading template {0}", template.getSection());

		final LoadState state = new LoadState(loader);

		loaders.push(loader);
		state.sections.push(template.getSection());
		state.renderLists.push(template.getRenderList());

		// Parse all tags
		while (true) {
			// Get text before this tag
			final List<ParsedLine> lines = loader.nextLines(state.delimiter.start);

			if ((state.standaloneStatus == LoadState.TAG_CAN_BE_STANDALONE && state.isStandaloneTagTail(lines) && state.removeStandaloneTagHead() != null) ||
					(state.standaloneStatus == LoadState.TAG_CHECK_TAIL_STANDALONE && state.isStandaloneTagTail(lines))) {
				StaticContentRenderer.create(state.renderLists.peek(), lines, true, false);
			} else {
				StaticContentRenderer.create(state.renderLists.peek(), lines, false, state.renderLists.peek().isEmpty());
			}

			if (!loader.hasNext()) {
				break;
			}

			// Parse the tag
			final String tag = loader.next(loader.checkNext("{") ? state.delimiter.unescapedEnd : state.delimiter.end);

			if (!loader.hasNext()) {
				throw new LoadException(loaders, "Incomplete tag at end of input");
			}

			state.tagCount++;
			state.standaloneStatus = LoadState.TAG_CAN_BE_STANDALONE;
			state.priorStaticText = lines;

			try {
				processTag(loaders, state, tag);
			} catch (final LoadException e) {
				throw e;
			} catch (final Exception e) {
				throw new LoadException(loaders, "Invalid tag \"" + tag + "\": " + e.getMessage(), e);
			}
		}

		// Pop off the top section (should be the template root section) and verify that the section stack is empty
		final Section topSection = state.sections.pop();

		if (!state.sections.isEmpty()) {
			throw new LoadException(loaders, "Missing section close tag for section " + topSection + " at end of input");
		}

		Template.LOGGER.log(Level.FINE, "Loaded template {0} containing {1} tags", new Object[] { template.getSection(), state.tagCount });
		loaders.pop();
		return template;
	}

	/**
	 * Processes the given tag.
	 *
	 * @param loaders the current stack of loaders
	 * @param state the load state of the template
	 * @param tag the string representation of the tag
	 * @throws LoadException if an error is encountered while loading the template
	 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading an expression
	 */
	private void processTag(final Stack<Loader> loaders, final LoadState state, final String tag) throws LoadException, ReflectiveOperationException {
		if (tag.length() == 0) {
			return;
		}

		switch (tag.charAt(0)) {
			case '!': // Comments are completely ignored
				return;

			case '<': // Local partial
				if (extensions.contains(Extension.INLINE_PARTIALS)) {
					final String name = Utilities.trim(tag, 1).string;
					final Template partial = new Template(name, state.loader.toLocation());

					state.sections.peek().getLocalPartials().put(name, partial);
					state.sections.push(partial.getSection().inheritFrom(state.sections.peek()));
					state.renderLists.push(partial.getRenderList());
					return;
				}

				break;

			case '>':
				loadPartial(loaders, state, tag);
				return;

			case '#': {
				if (tag.length() > 1 && tag.charAt(1) == '>') { // Section Partial
					final int index = state.renderLists.peek().size();
					state.renderLists.peek().add(null);

					final Template partial = new Template(loadPartial(loaders, state, tag.substring(1)), state.loader.toLocation());
					state.renderLists.peek().set(index, new Renderer() {
						@Override
						public void render(final RenderContext context, final Writer writer) throws IOException {
							context.getSectionPartials().push(partial);
						}
					});
					state.renderLists.peek().add(new Renderer() {
						@Override
						public void render(final RenderContext context, final Writer writer) throws IOException {
							context.getSectionPartials().pop();
						}
					});

					state.sections.push(partial.getSection());
					state.renderLists.push(partial.getRenderList());
					return;
				}

				// Start a new section, or repeat the previous section
				final TrimmedString expression = Utilities.trim(tag, 1);

				if (expression.string.isEmpty() && extensions.contains(Extension.REPEATED_SECTIONS)) { // Repeat the previous section
					state.sections.push(Section.repeat(state.sections.peek(), state.loader.toLocation()));
				} else if (!expression.string.isEmpty() && expression.string.charAt(0) == '@' && extensions.contains(Extension.ANNOTATIONS)) { // Annotation section
					final Matcher annotation = ANNOTATION_PATTERN.matcher(tag).region(expression.start, tag.length());

					if (!annotation.matches()) {
						throw new LoadException(loaders, "Invalid annotation format");
					}

					final String sectionName = annotation.group("name");
					final String parameters = annotation.group("arguments");
					final Object location = state.loader.toLocation();

					// Load the annotation arguments
					state.sections.push(new Section(state.sections.peek(), sectionName, location, parameters == null ? null : state.createExpression(location, state.createExpressionParser(new TrimmedString(annotation.start(2), parameters)), extensions), sectionName.substring(1), true));
				} else { // Start a new section
					final Object location = state.loader.toLocation();

					state.sections.push(new Section(state.sections.peek(), location, state.createExpression(location, state.createExpressionParser(expression), extensions)));
				}

				// Add a new render section action
				state.renderLists.peek().add(SectionRenderer.FACTORY.create(state.sections.peek()));
				state.renderLists.push(state.sections.peek().getRenderList());
				return;
			}

			case '^': { // Start a new inverted section, or else block for the current section
				final TrimmedString expression = Utilities.trim(tag, 1);

				if ((expression.string.isEmpty() || expression.string.startsWith("#")) && extensions.contains(Extension.ELSE_TAGS)) { // Else block for the current section
					if (state.sections.peek().getExpression() == null && state.sections.peek().getAnnotation() == null || state.sections.peek().getRenderList() != state.renderLists.pop()) {
						throw new LoadException(loaders, "Section else tag outside section start tag");
					}

					final Section previousSection = state.sections.peek();

					if (expression.string.isEmpty()) { // "else" tag
						state.renderLists.push(previousSection.getInvertedRenderList());
					} else { // "else if" tag
						final Object location = state.loader.toLocation();

						state.sections.pop(1).push(new Section(state.sections.peek(), location, state.createExpression(location, state.createExpressionParser(Utilities.trim(tag, 2)), extensions)));
						previousSection.getInvertedRenderList().add(SectionRenderer.FACTORY.create(state.sections.peek()));
						state.renderLists.push(state.sections.peek().getRenderList());
					}
				} else { // Start a new inverted section
					final Object location = state.loader.toLocation();

					state.sections.push(new Section(state.sections.peek(), location, state.createExpression(location, state.createExpressionParser(expression), extensions)));
					state.renderLists.peek().add(SectionRenderer.FACTORY.create(state.sections.peek()));
					state.renderLists.push(state.sections.peek().getInvertedRenderList());
				}

				return;
			}

			case '/': { // Close the current section
				final TrimmedString expression = Utilities.trim(tag, 1);
				final Section section = state.sections.peek();

				if (state.sections.size() <= 1) { // There should always be at least one section on the stack (the template root section)
					throw new LoadException(loaders, "Section close tag without matching section start tag");
				} else if ((!expression.string.isEmpty() || !extensions.contains(Extension.EMPTY_END_TAGS)) && !section.getName().contentEquals(expression.string)) {
					if (!expression.string.isEmpty() && extensions.contains(Extension.SMART_END_TAGS)) {
						break;
					}

					throw new LoadException(loaders, "Section close tag mismatch, expecting close tag for section " + section);
				}

				if (state.sections.pop().getParent() == null) { // Null parent means top-level, which indicates an inline partial
					state.removeStandaloneTagHead();
					state.standaloneStatus = LoadState.TAG_CHECK_TAIL_STANDALONE;
				}

				state.renderLists.pop();
				return;
			}

			case '=': { // Set delimiter
				final Matcher matcher = SET_DELIMITER_PATTERN.matcher(tag);

				if (!matcher.matches()) {
					throw new LoadException(loaders, "Invalid set delimiter tag");
				}

				state.delimiter = new Delimiter(matcher.group("start"), matcher.group("end"));
				return;
			}

			case '{': // Unescaped content tag
			case '&':
				state.standaloneStatus = LoadState.TAG_CANT_BE_STANDALONE; // Content tags cannot be stand-alone tags
				state.renderLists.peek().add(new DynamicContentRenderer(state.createExpression(state.loader.toLocation(), state.createExpressionParser(Utilities.trim(tag, 1)), extensions), false));
				return;

			default:
				break;
		}

		// Load the expression
		final ExpressionParseState parseState = state.createExpressionParser(Utilities.trim(tag, 0));
		final Expression expression = state.createExpression(state.loader.toLocation(), parseState, extensions);

		if (parseState.returnsValue()) {
			// Default to parsing as a dynamic content tag
			state.standaloneStatus = LoadState.TAG_CANT_BE_STANDALONE; // Content tags cannot be stand-alone tags
			state.renderLists.peek().add(new DynamicContentRenderer(expression, true));
		}
	}

	/**
	 * Loads a template from a string and puts it in the cache of the loader.
	 *
	 * @param name the name of the template
	 * @param value the string value to load as a template
	 * @return this loader
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final TemplateLoader put(final String name, final String value) throws LoadException {
		load(name, value);
		return this;
	}

	/**
	 * Loads a template from a file and puts it in the cache of the loader.
	 *
	 * @param file the file to load as a template
	 * @param charset the character encoding to use when loading the template from the file
	 * @return this loader
	 * @throws IOException if the file failed to load
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final TemplateLoader put(final Path file, final Charset charset) throws IOException, LoadException {
		load(file, charset);
		return this;
	}

	/**
	 * Loads a template from a file and puts it in the cache of the loader.
	 *
	 * @param file the file to load as a template
	 * @return this loader
	 * @throws IOException if the file failed to load
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final TemplateLoader put(final Path file) throws IOException, LoadException {
		return put(file, charset);
	}

	/**
	 * Loads a template using a reader and puts it in the cache of the loader.
	 *
	 * @param name the name of the template
	 * @param reader the reader to use to load as a template
	 * @return this loader
	 * @throws IOException if an I/O exception is encountered while reading from the reader
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public final TemplateLoader put(final String name, final Reader reader) throws IOException, LoadException {
		load(name, reader);
		return this;
	}

	/**
	 * Puts a template into the loader.
	 *
	 * @param template the template to put into the loader
	 * @return this loader
	 */
	public final TemplateLoader put(final Template template) {
		putTemplate(template.getIdentifier(), template);
		return this;
	}

	/**
	 * Puts a template by name into the loader. All future references to the specified name will use the specified template.
	 *
	 * @param name the name identifying the template
	 * @param template the template to put into the loader
	 * @return this loader
	 */
	public final TemplateLoader put(final String name, final Template template) {
		putTemplate(name, template);
		return this;
	}

	/**
	 * Puts a template by file into the loader. All future references to the specified file will use the specified template.
	 *
	 * @param file the file identifying the template
	 * @param template the template to put into the loader
	 * @return this loader
	 */
	public final TemplateLoader put(final Path file, final Template template) {
		putTemplate(file.toAbsolutePath().normalize(), template);
		return this;
	}

	/**
	 * Puts a template using the specified identifier into the loader. All future references to the specified identifier will use the specified template.
	 *
	 * @param identifier the identifier for the template
	 * @param template the template to put into the loader
	 */
	protected final void putTemplate(final Object identifier, final Template template) {
		if (identifier != null) {
			templates.put(identifier, template);
		}
	}

	/**
	 * Sets the character set used for loading additional templates.
	 *
	 * @param charset the character set used for loading additional templates
	 * @return this object
	 */
	public final TemplateLoader setCharset(final Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * Sets the enabled Horseshoe extensions. The extensions to be enabled should be bit-wise OR'd together.
	 *
	 * @param extensions the bit-wise OR of the enabled Horseshoe extensions
	 * @return this object
	 */
	public final TemplateLoader setExtensions(final EnumSet<Extension> extensions) {
		this.extensions = extensions;
		return this;
	}

	/**
	 * Sets whether or not traversing paths ("/..." or "../...") is prevented when loading partials.
	 *
	 * @param preventPartialPathTraversal true to prevent traversing paths when loading partials, otherwise false
	 * @return this object
	 */
	public final TemplateLoader setPreventPartialPathTraversal(final boolean preventPartialPathTraversal) {
		this.preventPartialPathTraversal = preventPartialPathTraversal;
		return this;
	}

	/**
	 * Tries to load the specified template using the specified settings.
	 *
	 * @param name the name of the template to load
	 * @param loaders the stack of items being loaded
	 * @return the loaded template
	 * @throws IOException if there is an IOException when loading from a file
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	protected Template tryLoad(final String name, final Stack<Loader> loaders) throws IOException, LoadException {
		// Try to load the template by file
		final Path file = Paths.get(name);

		if (file.isAbsolute()) {
			return loadTemplate(name, file, loaders);
		}

		// Try to load the template relative to the current template
		if (!loaders.isEmpty() && loaders.peek().getFile() != null) {
			final Template relativeTemplate = loadTemplate(name, loaders.peek().getFile().resolveSibling(file).toAbsolutePath().normalize(), loaders);

			if (relativeTemplate != null) {
				return relativeTemplate;
			}
		}

		// Try to load the template from the list of include directories
		for (final Path directory : includeDirectories) {
			final Template includeTemplate = loadTemplate(name, directory.resolve(file).toAbsolutePath().normalize(), loaders);

			if (includeTemplate != null) {
				return includeTemplate;
			}
		}

		return null;
	}

}
