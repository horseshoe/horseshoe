package horseshoe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.Expression;
import horseshoe.internal.Identifier;
import horseshoe.internal.ParsedLine;
import horseshoe.internal.PersistentStack;
import horseshoe.internal.StringUtils;

/**
 * The TemplateLoader class is used to load any number of {@link Template}s before rendering. Various properties can be configured to load templates with different settings.
 */
public class TemplateLoader {

	private static final Pattern SET_DELIMITER_PATTERN = Pattern.compile("=\\s*(?<start>[^\\s]+)\\s+(?<end>[^\\s]+)\\s*=", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern ANNOTATION_PATTERN = Pattern.compile("(?<name>@" + Identifier.PATTERN + ")\\s*(?:\\(\\s*(?<parameters>.*)\\s*\\)\\s*)?", Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern NAMED_EXPRESSION_PATTERN = Pattern.compile("(?<name>" + Identifier.PATTERN + ")\\s*(?:\\(\\s*\\)\\s*)?[-=]>\\s*", Pattern.UNICODE_CHARACTER_CLASS);

	private final Map<Object, Template> templates = new HashMap<>();
	private final Map<Object, Loader> loaderMap = new HashMap<>();
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

	private static final class LoadState {

		public static final int TAG_CAN_BE_STANDALONE = 0;
		public static final int TAG_CANT_BE_STANDALONE = 1;
		public static final int TAG_CHECK_TAIL_STANDALONE = 2;

		private final Loader loader;
		private final PersistentStack<Section> sections = new PersistentStack<>();
		private final PersistentStack<List<Renderer>> renderLists = new PersistentStack<>();
		private Delimiter delimiter = new Delimiter("{{", "}}");
		private List<ParsedLine> priorStaticText = new ArrayList<>();
		private int standaloneStatus = TAG_CANT_BE_STANDALONE;
		private int tagCount = 0;

		public LoadState(final Loader loader) {
			this.loader = loader;
			this.priorStaticText.add(new ParsedLine("", ""));
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
				return StringUtils.isWhitespace(lines.get(0).getLine());
			}

			return !loader.hasNext() && StringUtils.isWhitespace(lines.get(0).getLine());
		}

		/**
		 * Removes the leading whitespace from the beginning of a potential standalone tag.
		 *
		 * @return the leading whitespace
		 */
		private String removeStandaloneTagHead() {
			final int size = priorStaticText.size();
			final String removedWhitespace;

			if ((size > 1 || tagCount == 1) && StringUtils.isWhitespace(removedWhitespace = priorStaticText.get(size - 1).getLine())) {
				priorStaticText.set(size - 1, new ParsedLine(null, ""));
				return removedWhitespace;
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
	 * Adds templates from strings. The templates will not be loaded until they are referenced by another template being loaded or the {@link #load(String)} method is called with the specified template name. Any templates already loaded are ignored.
	 *
	 * @param templates a map of template names to template strings
	 * @return this loader
	 */
	public TemplateLoader add(final Map<String, String> templates) {
		for (final Entry<String, String> entry : templates.entrySet()) {
			add(entry.getKey(), entry.getValue());
		}

		return this;
	}

	/**
	 * Adds a template from a string. The template will not be loaded until it is referenced by another template being loaded or the {@link #load(String)} method is called with the specified template name. If the template is already loaded, this has no effect.
	 *
	 * @param name the name of the template
	 * @param value the string value to load as a template
	 * @return this loader
	 */
	public TemplateLoader add(final String name, final String value) {
		if (!templates.containsKey(name)) {
			final Loader loader = loaderMap.put(name, new Loader(name, value));

			if (loader != null) {
				loader.close();
			}
		}

		return this;
	}

	/**
	 * Adds a template to the loader. If the template already exists in the template loader, then this has no effect.
	 *
	 * @param template the template to add to the loader
	 * @return this loader
	 */
	public TemplateLoader add(final Template template) {
		if (!templates.containsKey(template.getIdentifier())) {
			return put(template);
		}

		return this;
	}

	/**
	 * Adds a template from a file. The template will not be loaded until it is referenced by another template being loaded or the {@link #load(Path)} method is called with the specified template name. If the template is already loaded, this has no effect.
	 *
	 * @param file the file to load as a template
	 * @param charset the character set to use when loading the file
	 * @return this loader
	 * @throws FileNotFoundException if the file does not exist
	 */
	public TemplateLoader add(final Path file, final Charset charset) throws FileNotFoundException {
		final Path absoluteFile = file.toAbsolutePath().normalize();

		if (!templates.containsKey(absoluteFile)) {
			final Loader loader = loaderMap.put(absoluteFile, new Loader(absoluteFile, charset));

			if (loader != null) {
				loader.close();
			}
		}

		return this;
	}

	/**
	 * Adds a template from a file. The template will not be loaded until it is referenced by another template being loaded or the {@link #load(String)} method is called with the specified template name. If the template is already loaded, this has no effect.
	 *
	 * @param file the file to load as a template
	 * @return this loader
	 * @throws FileNotFoundException if the file does not exist
	 */
	public TemplateLoader add(final Path file) throws FileNotFoundException {
		return add(file, charset);
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
			final Loader loader = loaderMap.put(name, new Loader(name, reader));

			if (loader != null) {
				loader.close();
			}
		} else {
			try {
				reader.close();
			} catch (final IOException e) {
				Template.LOGGER.log(Level.WARNING, "Failed to close reader for template \"" + name + "\"", e);
			}
		}

		return this;
	}

	/**
	 * Closes any open readers. If no open readers exist, this has no effect. This can be used to cleanup after a load exception occurs.
	 */
	public void close() {
		for (final Loader loader : loaderMap.values()) {
			loader.close();
		}

		loaderMap.clear();
	}

	/**
	 * Gets the character set used for loading additional templates.
	 *
	 * @return the character set used for loading additional templates
	 */
	public Charset getCharset() {
		return charset;
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
	 * Gets whether or not traversing paths ("/..." or "../...") is prevented when loading partials.
	 *
	 * @return true if traversing paths is prevented when loading partials, otherwise false
	 */
	public boolean getPreventPartialPathTraversal() {
		return preventPartialPathTraversal;
	}

	/**
	 * Gets the Horseshoe extensions that are enabled.
	 *
	 * @return the set of enabled Horseshoe extensions
	 */
	public EnumSet<Extension> getExtensions() {
		return extensions;
	}

	/**
	 * Loads a template from a string. If the template is already loaded, this has no effect.
	 *
	 * @param name the name of the template
	 * @param value the string value to load as a template
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final String name, final String value) throws LoadException {
		return add(name, value).load(name);
	}

	/**
	 * Loads a template from a file. If the template is already loaded, this has no effect.
	 *
	 * @param file the file to load as a template
	 * @param charset the character encoding to use when loading the template from the file
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws IOException if the file failed to load
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final Path file, final Charset charset) throws IOException, LoadException {
		final Path absoluteFile = file.toAbsolutePath().normalize();

		// Try to load via a cached template
		final Template cachedTemplate = templates.get(absoluteFile);

		if (cachedTemplate != null) {
			return cachedTemplate;
		}

		// Load via a cached loader
		add(absoluteFile, charset);

		try (final Loader loader = loaderMap.remove(absoluteFile)) {
			final Template newTemplate = new Template(file.toString(), absoluteFile);

			templates.put(absoluteFile, newTemplate);
			return loadTemplate(newTemplate, new PersistentStack<Loader>(), loader);
		}
	}

	/**
	 * Loads a template from a file. If the template is already loaded, this has no effect.
	 *
	 * @param file the file to load as a template
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws IOException if the file failed to load
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final Path file) throws IOException, LoadException {
		return load(file, charset);
	}

	/**
	 * Loads a template using a reader. If the template is already loaded, this has no effect on the internal state of the template loader and the reader is closed.
	 *
	 * @param name the name of the template
	 * @param reader the reader to use to load as a template
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final String name, final Reader reader) throws LoadException {
		return add(name, reader).load(name);
	}

	/**
	 * Loads the specified template using the specified settings. If the template is already loaded the settings are ignored.
	 *
	 * @param name the name of the template to load
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final String name) throws LoadException {
		return load(name, new PersistentStack<Loader>());
	}

	/**
	 * Loads the specified template using the specified settings. If the template is already loaded, the previously loaded instance is returned and all settings are ignored.
	 *
	 * @param name the name of the template to load
	 * @param loaders the stack of items being loaded
	 * @return the loaded template
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	private Template load(final String name, final PersistentStack<Loader> loaders) throws LoadException {
		try {
			// Try to load the template by name
			final Template cachedTemplate = templates.get(name);

			if (cachedTemplate != null) {
				return cachedTemplate;
			}

			try (final Loader loader = loaderMap.remove(name)) {
				if (loader != null) {
					final Template newTemplate = new Template(name, name);

					templates.put(name, newTemplate);
					return loadTemplate(newTemplate, loaders, loader);
				}
			}

			// Try to load the template by file
			final Path file = Paths.get(name);

			if (file.isAbsolute()) {
				final Template absoluteTemplate = loadTemplate(name, file, loaders);

				if (absoluteTemplate != null) {
					return absoluteTemplate;
				}
			} else {
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
	 * Tries to load the specified template by file. If the template is already loaded, the previously loaded instance is returned and all settings are ignored.
	 *
	 * @param name the name of the template to load
	 * @param absoluteFile the absolute, normalized template file to load
	 * @param loaders the stack of items being loaded
	 * @return the loader if the file can be loaded, otherwise null
	 * @throws IOException if there is an IOException when loading from a file
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	private Template loadTemplate(final String name, final Path absoluteFile, final PersistentStack<Loader> loaders) throws IOException, LoadException {
		final Template cachedTemplate = templates.get(absoluteFile);

		if (cachedTemplate != null) {
			return cachedTemplate;
		}

		try (final Loader loader = loaderMap.remove(absoluteFile)) {
			if (loader != null) {
				final Template newTemplate = new Template(name, absoluteFile);

				templates.put(absoluteFile, newTemplate);
				return loadTemplate(newTemplate, loaders, loader);
			}
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
			final Template newTemplate = new Template(name, absoluteFile);

			templates.put(absoluteFile, newTemplate);
			return loadTemplate(newTemplate, loaders, loader);
		}
	}

	/**
	 * Loads the list of actions to be performed when rendering the template.
	 *
	 * @param template the template to load
	 * @param loaders the stack of items being loaded
	 * @param loader the item being loaded
	 * @return the loaded template
	 * @throws LoadException if an error is encountered while loading the template
	 * @throws IOException if an error is encountered while reading from a file or stream
	 */
	private Template loadTemplate(final Template template, final PersistentStack<Loader> loaders, final Loader loader) throws LoadException, IOException {
		Template.LOGGER.log(Level.FINE, "Loading template {0}", template.getSection());

		final LoadState state = new LoadState(loader);

		loaders.push(loader);
		state.sections.push(template.getSection());
		state.renderLists.push(template.getActions());

		// Parse all tags
		while (true) {
			// Get text before this tag
			final List<ParsedLine> lines = loader.nextLines(state.delimiter.start);

			if ((state.standaloneStatus == LoadState.TAG_CAN_BE_STANDALONE && state.isStandaloneTagTail(lines) && state.removeStandaloneTagHead() != null) ||
					(state.standaloneStatus == LoadState.TAG_CHECK_TAIL_STANDALONE && state.isStandaloneTagTail(lines))) {
				new StaticContentRenderer(state.renderLists.peek(), lines, true, false);
			} else {
				new StaticContentRenderer(state.renderLists.peek(), lines, false, state.renderLists.peek().isEmpty());
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
			throw new LoadException(loaders, "Unmatched section tag " + topSection + " at end of input");
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
	private void processTag(final PersistentStack<Loader> loaders, final LoadState state, final String tag) throws LoadException, ReflectiveOperationException {
		if (tag.length() == 0) {
			return;
		}

		switch (tag.charAt(0)) {
			case '!': // Comments are completely ignored
				return;

			case '<': // Local partial
				if (extensions.contains(Extension.INLINE_PARTIALS)) {
					final String name = StringUtils.trim(tag, 1, tag.length());
					final Template partial = new Template(name, state.loader.toLocation());

					partial.getSection().getLocalPartials().put(name, partial);
					state.sections.peek().getLocalPartials().put(name, partial);
					state.sections.push(partial.getSection());
					state.renderLists.push(partial.getActions());
					return;
				}

				break;

			case '>': { // Load partial
				final String name;
				String indentation = null;

				if (tag.length() > 1 && tag.charAt(1) == '>') { // >> uses indentation on first line, no trailing new line
					state.standaloneStatus = LoadState.TAG_CHECK_TAIL_STANDALONE;
					name = StringUtils.trim(tag, 2, tag.length());
				} else {
					name = StringUtils.trim(tag, 1, tag.length());
					indentation = state.removeStandaloneTagHead();

					// 1) Standalone tag uses indentation for each line, no trailing new line
					// 2) Non-standalone tag uses indentation on first line, with trailing new line
					if (indentation == null) {
						state.standaloneStatus = LoadState.TAG_CANT_BE_STANDALONE;
					} else {
						state.standaloneStatus = LoadState.TAG_CHECK_TAIL_STANDALONE;
					}
				}

				final Template localPartial = state.sections.peek().getLocalPartials().get(name);
				final Template partial = (localPartial != null ? localPartial : load(name, loaders));

				state.sections.peek().getNamedExpressions().putAll(partial.getSection().getNamedExpressions());
				new TemplateRenderer(state.renderLists.peek(), partial, indentation);
				return;
			}

			case '#': { // Start a new section, or repeat the previous section
				final String expression = StringUtils.trim(tag, 1, tag.length());

				if (expression.length() == 0 && extensions.contains(Extension.REPEATED_SECTIONS)) { // Repeat the previous section
					state.sections.push(Section.repeat(state.sections.peek(), state.loader.toLocation()));
				} else if (expression.length() != 0 && expression.charAt(0) == '@' && extensions.contains(Extension.ANNOTATIONS)) { // Annotation section
					final Matcher annotation = ANNOTATION_PATTERN.matcher(expression);

					if (!annotation.matches()) {
						throw new LoadException(loaders, "Invalid annotation format");
					}

					final String sectionName = annotation.group("name");
					final String parameters = annotation.group("parameters");
					final Object location = state.loader.toLocation();

					// Load the annotation arguments
					state.sections.push(new Section(state.sections.peek(), sectionName, location, parameters == null ? null : new Expression(location, null, parameters, state.sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS)), sectionName.substring(1), true));
				} else { // Start a new section
					final Object location = state.loader.toLocation();

					state.sections.push(new Section(state.sections.peek(), location, new Expression(location, null, expression, state.sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS))));
				}

				// Add a new render section action
				state.renderLists.peek().add(SectionRenderer.FACTORY.create(state.sections.peek()));
				state.renderLists.push(state.sections.peek().getRenderList());
				return;
			}

			case '^': { // Start a new inverted section, or else block for the current section
				final String expression = StringUtils.trim(tag, 1, tag.length());
				final SectionRenderer renderer;

				if (expression.length() == 0 && extensions.contains(Extension.ELSE_TAGS)) { // Else block for the current section
					if (state.sections.peek().getExpression() == null && state.sections.peek().getAnnotation() == null || state.sections.peek().getRenderList() != state.renderLists.pop()) {
						throw new LoadException(loaders, "Section else tag outside section start tag");
					}

					renderer = (SectionRenderer)state.renderLists.peek().get(state.renderLists.peek().size() - 1);
				} else { // Start a new inverted section
					final Object location = state.loader.toLocation();

					state.sections.push(new Section(state.sections.peek(), location, new Expression(location, null, expression, state.sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS))));
					renderer = SectionRenderer.FACTORY.create(state.sections.peek());
					state.renderLists.peek().add(renderer);
				}

				// Grab the inverted action list from the section
				state.renderLists.push(renderer.getSection().getInvertedRenderList());
				return;
			}

			case '/': { // Close the current section
				final String expression = StringUtils.trim(tag, 1, tag.length());
				final Section section = state.sections.peek();

				if (state.sections.size() <= 1) { // There should always be at least one section on the stack (the template root section)
					throw new LoadException(loaders, "Section close tag without matching section start tag");
				} else if ((expression.length() > 0 || !extensions.contains(Extension.EMPTY_END_TAGS)) && !section.getName().contentEquals(expression)) {
					if (expression.length() > 0 && extensions.contains(Extension.SMART_END_TAGS)) {
						break;
					}

					throw new LoadException(loaders, "Unclosed section, expecting close tag for section " + section);
				}

				state.sections.pop();
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
				state.renderLists.peek().add(new DynamicContentRenderer(new Expression(state.loader.toLocation(), null, StringUtils.trim(tag, 1, tag.length()), state.sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS)), false));
				return;

			default:
				// Check for a named expression
				if (extensions.containsAll(EnumSet.of(Extension.EXPRESSIONS, Extension.NAMED_EXPRESSIONS))) {
					final String expression = StringUtils.trim(tag, 0, tag.length());
					final Matcher namedExpression = NAMED_EXPRESSION_PATTERN.matcher(expression);

					if (namedExpression.lookingAt()) {
						new Expression(state.loader.toLocation(), namedExpression.group("name"), expression.substring(namedExpression.end(), expression.length()), state.sections.peek().getNamedExpressions(), true);
						return;
					}
				}

				break;
		}

		// Default to parsing as a dynamic content tag
		state.standaloneStatus = LoadState.TAG_CANT_BE_STANDALONE; // Content tags cannot be stand-alone tags
		state.renderLists.peek().add(new DynamicContentRenderer(new Expression(state.loader.toLocation(), null, StringUtils.trim(tag, 0, tag.length()), state.sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS)), true));
	}

	/**
	 * Puts a template into the loader.
	 *
	 * @param template the template to put into the loader
	 * @return this loader
	 */
	public TemplateLoader put(final Template template) {
		templates.put(template.getIdentifier(), template);
		return this;
	}

	/**
	 * Puts a template by name into the loader. All future references to the specified name will use the specified template.
	 *
	 * @param name the name identifying the template
	 * @param template the template to put into the loader
	 * @return this loader
	 */
	public TemplateLoader put(final String name, final Template template) {
		templates.put(name, template);
		return this;
	}

	/**
	 * Puts a template by file into the loader. All future references to the specified file will use the specified template.
	 *
	 * @param file the file identifying the template
	 * @param template the template to put into the loader
	 * @return this loader
	 */
	public TemplateLoader put(final Path file, final Template template) {
		templates.put(file.toAbsolutePath().normalize(), template);
		return this;
	}

	/**
	 * Sets the character set used for loading additional templates.
	 *
	 * @param charset the character set used for loading additional templates
	 * @return this object
	 */
	public TemplateLoader setCharset(final Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * Sets the enabled Horseshoe extensions. The extensions to be enabled should be bit-wise OR'd together.
	 *
	 * @param extensions the bit-wise OR of the enabled Horseshoe extensions
	 * @return this object
	 */
	public TemplateLoader setExtensions(final EnumSet<Extension> extensions) {
		this.extensions = extensions;
		return this;
	}

	/**
	 * Sets whether or not traversing paths ("/..." or "../...") is prevented when loading partials.
	 *
	 * @param preventPartialPathTraversal true to prevent traversing paths when loading partials, otherwise false
	 * @return this object
	 */
	public TemplateLoader setPreventPartialPathTraversal(final boolean preventPartialPathTraversal) {
		this.preventPartialPathTraversal = preventPartialPathTraversal;
		return this;
	}

}
