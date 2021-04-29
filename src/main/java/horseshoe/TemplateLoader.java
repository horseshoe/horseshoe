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
	private static final Pattern ANNOTATION_PATTERN = Pattern.compile("(?<name>@" + Identifier.PATTERN + ")\\s*" + Expression.COMMENTS_PATTERN + "(?s:(?<arguments>[(].*?[)]\\s*" + Expression.COMMENTS_PATTERN + "))?", Pattern.UNICODE_CHARACTER_CLASS);

	private static final Pattern INLINE_PARTIAL_NAME_PATTERN = Pattern.compile(Expression.COMMENTS_PATTERN + "(?<name>" + Identifier.PATTERN + ")\\s*" + Expression.COMMENTS_PATTERN, Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern INLINE_PARTIAL_PARAMETER_PATTERN = Pattern.compile("\\s*" + Expression.COMMENTS_PATTERN + "(?<parameter>[.]|" + Identifier.PATTERN + ")\\s*" + Expression.COMMENTS_PATTERN, Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern INLINE_PARTIAL_PATTERN = Pattern.compile(INLINE_PARTIAL_NAME_PATTERN + "(?:[(]\\s*" + Expression.COMMENTS_PATTERN + "(?<parameters>(?:[.]|" + Identifier.PATTERN + ")(?:,\\s*" + Expression.COMMENTS_PATTERN + Identifier.PATTERN + ")*+)?\\s*" + Expression.COMMENTS_PATTERN + "[)]\\s*" + Expression.COMMENTS_PATTERN + ")?", Pattern.UNICODE_CHARACTER_CLASS);

	private static final Pattern IDENTIFIER_PARENS_PATTERN = Pattern.compile("(?<name>" + Identifier.PATTERN + ")\\s*" + Expression.COMMENTS_PATTERN + "[(]\\s*" + Expression.COMMENTS_PATTERN + "[)]\\s*" + Expression.COMMENTS_PATTERN, Pattern.UNICODE_CHARACTER_CLASS);
	private static final String IDENTIFIER_PARENS = IDENTIFIER_PARENS_PATTERN.toString().replace("(?<name>", "(?:");
	private static final Pattern PARTIAL_IMPORTS_PATTERN = Pattern.compile("[|]\\s*" + Expression.COMMENTS_PATTERN + "(?<imports>|[*]|" + IDENTIFIER_PARENS + "(?:,\\s*" + Expression.COMMENTS_PATTERN + IDENTIFIER_PARENS + ")*)\\s*" + Expression.COMMENTS_PATTERN + "$", Pattern.UNICODE_CHARACTER_CLASS);

	private final Map<Object, Template> templates = new HashMap<>();
	private final List<Path> includeDirectories = new ArrayList<>();
	private Charset charset = StandardCharsets.UTF_8;
	private boolean preventPartialPathTraversal = true;
	private EnumSet<Extension> extensions = EnumSet.allOf(Extension.class);

	private final class Loadable {

		private final Template template;
		private final Loader loader;

		public Loadable(final Template template, final Loader loader) {
			this.template = template;
			this.loader = loader;
		}

		public Template load() throws IOException, LoadException {
			try {
				return loadTemplate(template, loader, new Stack<>());
			} finally {
				loader.close();
			}
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
			return loadTemplate(new Template(name, name), loader, new Stack<>());
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
			return loadTemplate(new Template(name, name), loader, new Stack<>());
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
			return loadTemplate(new Template(file.toString(), absoluteFile), loader, new Stack<>());
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
	 * @return the template renderer associated with the loaded partial
	 * @throws LoadException if an error is encountered while loading the partial
	 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading an expression
	 */
	private TemplateRenderer loadPartial(final Stack<Loader> loaders, final TemplateLoadState state, final String tag) throws LoadException, ReflectiveOperationException {
		int start;
		int end = tag.length();
		final String indentation;

		if (end > 1 && tag.charAt(1) == '>') { // ">>" uses indentation on first line, no trailing new line
			start = 2;
			indentation = null;
			state.setStandaloneStatus(TemplateLoadState.TAG_CHECK_TAIL_STANDALONE);
		} else {
			start = 1;
			indentation = state.removeStandaloneTagHead();

			// 1) Standalone tag uses indentation for each line, no trailing new line
			// 2) Non-standalone tag uses indentation on first line, with trailing new line
			state.setStandaloneStatus(indentation == null ? TemplateLoadState.TAG_CANT_BE_STANDALONE : TemplateLoadState.TAG_CHECK_TAIL_STANDALONE);
		}

		// Checks for partial invocation: [name]([arguments]), [name]|[named_expressions]
		while (start < end && Character.isSpaceChar(tag.charAt(start))) {
			start++;
		}

		// Check for anonymous partial
		if (start >= end) {
			return new TemplateRenderer(null, indentation);
		}

		// Check for imports
		final Matcher matcher = PARTIAL_IMPORTS_PATTERN.matcher(tag).region(start, end);

		if (matcher.find()) {
			return loadPartial(loaders, state, indentation, tag.substring(start, matcher.start()).trim(), null, matcher.group("imports"));
		}

		final Object location = state.toLocation();
		Expression argumentsExpression = null;

		// Find arguments
		for (int i = start; (i = tag.indexOf('(', i + 1)) >= 0; ) {
			try {
				argumentsExpression = state.createExpression(location, state.createExpressionParser(new TrimmedString(i, tag.substring(i)), true), extensions);
				end = i;
				break;
			} catch (final RuntimeException e) {
				// Ignore and try next match
			}
		}

		final Expression arguments = argumentsExpression;
		final String name = tag.substring(start, end);

		// Check for expression partial: ('a' + 'b')
		if (tag.charAt(start) == '(') {
			final Expression expression = state.createExpression(location, state.createExpressionParser(new TrimmedString(start, name), false), extensions);

			return new TemplateRenderer(new Template(null, "[Deferred]"), null) {
				@Override
				public void render(RenderContext context, Writer writer) throws IOException {
					final Object result = expression.evaluate(context);
					if (result == null) {
						return;
					}
					try {
						final TemplateRenderer renderer = loadPartial(loaders, state, indentation, result.toString(), arguments, null);
						renderer.setStandalone(isStandalone());
						renderer.render(context, writer);
					} catch (final LoadException e) {
						throw new HaltRenderingException("Failed to load partial from expression \"" + name + "\": " + e.getMessage());
					}
				}
			};
		}

		// Load partial by name
		return loadPartial(loaders, state, indentation, name.trim(), arguments, null);
	}

	/**
	 * Loads the given partial by name.
	 *
	 * @param loaders the current stack of loaders
	 * @param state the load state of the template
	 * @param indentation the indentation to apply to the partial
	 * @param name the name of the partial to load
	 * @param arguments the optional expression used to evaluate arguments
	 * @param imports the optional string of imports
	 * @return the template renderer associated with the loaded partial
	 * @throws LoadException if an error is encountered while loading the partial
	 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading an expression
	 */
	private TemplateRenderer loadPartial(final Stack<Loader> loaders, final TemplateLoadState state, final String indentation, final String name, final Expression arguments, final String imports) throws LoadException {
		if (imports == null) {
			final Template localPartial = state.getLocalPartials().get(name);

			if (localPartial != null) {
				return new TemplateRenderer(localPartial, indentation, arguments);
			}
		}

		// Load the partial and any associated imports
		final Template partial = loadPartial(name, loaders);

		if ("*".equals(imports)) {
			state.getNamedExpressions().putAll(partial.getRootExpressions());
		} else if (imports != null) {
			for (final Matcher identifiers = IDENTIFIER_PARENS_PATTERN.matcher(imports); identifiers.find(); ) {
				final String expressionName = identifiers.group("name");
				final Expression expression = partial.getRootExpressions().get(expressionName);

				if (expression == null) {
					throw new LoadException(loaders, "Expression \"" + expressionName + "\" not found in partial");
				}

				state.getNamedExpressions().put(expressionName, expression);
			}
		}

		return new TemplateRenderer(partial, indentation, arguments);
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

		final TemplateLoadState state = new TemplateLoadState(loader, template);

		loaders.push(loader);
		state.getSections().push(template.getSection());
		state.getRenderLists().push(template.getSection().getRenderList());

		// Parse first static content
		List<ParsedLine> lines = loader.nextLines(state.getDelimiter().start);
		StaticContentRenderer.create(state.getRenderLists().peek(), lines, true);

		// Parse all tags
		while (loader.hasNext()) {
			// Parse the tag
			final String tag = loader.next(loader.checkNext("{") ? state.getDelimiter().unescapedEnd : state.getDelimiter().end);
			final TagRenderer previousTagRenderer;

			if (!loader.hasNext()) {
				throw new LoadException(loaders, "Incomplete tag at end of input");
			}

			state.updateStaticText(lines);

			try {
				previousTagRenderer = processTag(loaders, state, tag);
			} catch (final LoadException e) {
				throw e;
			} catch (final Exception e) {
				throw new LoadException(loaders, "Invalid tag \"" + tag + "\": " + e.getMessage(), e);
			}

			// Get text after the tag
			lines = loader.nextLines(state.getDelimiter().start);

			if ((state.getStandaloneStatus() == TemplateLoadState.TAG_CAN_BE_STANDALONE && state.isStandaloneTagTail(lines) && state.removeStandaloneTagHead() != null) ||
					(state.getStandaloneStatus() == TemplateLoadState.TAG_CHECK_TAIL_STANDALONE && state.isStandaloneTagTail(lines))) {
				if (previousTagRenderer != null) {
					previousTagRenderer.setStandalone(true);
				}

				StaticContentRenderer.create(state.getRenderLists().peek(), lines);
			} else {
				StaticContentRenderer.create(state.getRenderLists().peek(), lines, state.getRenderLists().peek().isEmpty() && state.getSections().peek().getParent() == null);
			}
		}

		// Pop off the top section (should be the template root section) and verify that the section stack is empty
		final Section topSection = state.getSections().pop();

		if (!state.getSections().isEmpty()) {
			throw new LoadException(loaders, "Missing section close tag for section " + topSection + " at end of input");
		}

		Template.LOGGER.log(Level.FINE, "Loaded template {0} containing {1} tags", new Object[] { template.getSection(), state.getTagCount() });
		loaders.pop();
		return template;
	}

	/**
	 * Parse an inline partial signature from the specified tag.
	 *
	 * @param tag the tag to parse as an inline partial
	 * @param state the load state of the template
	 * @return the inline partial
	 */
	private static Template parsePartialSignature(final String tag, final TemplateLoadState state) {
		final Matcher matcher = INLINE_PARTIAL_PATTERN.matcher(tag);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("Partial signature is invalid");
		}

		final String parameters = matcher.group("parameters");
		final List<String> parameterNames = new ArrayList<>();

		if (parameters != null) {
			for (final Matcher parameterMatcher = INLINE_PARTIAL_PARAMETER_PATTERN.matcher(parameters); parameterMatcher.find(); ) {
				final String name = parameterMatcher.group("parameter");

				if (parameterNames.contains(name)) {
					throw new IllegalStateException("Duplicate parameter \"" + name + "\" in inline partial signature");
				}

				parameterNames.add(name);
			}
		}

		return new Template(matcher.group("name"), state.toLocation(), state.getTemplateBindings().size(), parameterNames);
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
	private TagRenderer processTag(final Stack<Loader> loaders, final TemplateLoadState state, final String tag) throws LoadException, ReflectiveOperationException {
		if (tag.length() == 0) {
			return null;
		}

		switch (tag.charAt(0)) {
			case '!': // Comments are completely ignored
				return null;

			case '<': // Local partial
				if (extensions.contains(Extension.INLINE_PARTIALS)) {
					final Template partial = parsePartialSignature(Utilities.trim(tag, 1).string, state);

					if (state.getLocalPartials().put(partial.getSection().getName(), partial) != null) {
						throw new IllegalStateException("Inline partial \"" + partial.getSection().getName() + "\" is already defined");
					}

					state.getSections().push(partial.getSection());
					state.getTemplateBindings().push(partial.getBindings());
					state.getRenderLists().push(partial.getSection().getRenderList());
					return null;
				}

				break;

			case '>': {
				final TemplateRenderer renderer = loadPartial(loaders, state, tag);

				state.getRenderLists().peek().add(renderer);
				return renderer;
			}

			case '#': {
				if (tag.length() > 1 && tag.charAt(1) == '>') { // Section Partial
					final TemplateRenderer partialRenderer = loadPartial(loaders, state, tag.substring(1));

					if (partialRenderer.getTemplate() == null) {
						throw new LoadException(loaders, "Invalid anonymous section partial");
					}

					final Template partial = new Template(partialRenderer.getTemplate().getSection().getName(), state.toLocation());

					state.getRenderLists().peek().add(new Renderer() {
						@Override
						public void render(final RenderContext context, final Writer writer) throws IOException {
							context.getSectionPartials().push(partial);
							partialRenderer.render(context, writer);
							context.getSectionPartials().pop();
						}
					});

					state.getSections().push(partial.getSection());
					state.getTemplateBindings().push(partial.getBindings());
					state.getRenderLists().push(partial.getSection().getRenderList());
					return partialRenderer;
				}

				// Start a new section, or repeat the previous section
				final TrimmedString expression = Utilities.trim(tag, 1);

				if (expression.string.isEmpty() && extensions.contains(Extension.REPEATED_SECTIONS)) { // Repeat the previous section
					state.getSections().push(Section.repeat(state.getSections().peek(), state.toLocation()));
				} else if (!expression.string.isEmpty() && expression.string.charAt(0) == '@' && extensions.contains(Extension.ANNOTATIONS)) { // Annotation section
					final Matcher annotation = ANNOTATION_PATTERN.matcher(tag).region(expression.start, tag.length());

					if (!annotation.matches()) {
						throw new LoadException(loaders, "Invalid annotation format");
					}

					final String sectionName = annotation.group("name");
					final String arguments = annotation.group("arguments");
					final Object location = state.toLocation();

					// Load the annotation arguments
					state.getSections().push(new Section(state.getSections().peek(), sectionName, location, arguments == null ? null : state.createExpression(location, state.createExpressionParser(new TrimmedString(annotation.start("arguments"), arguments.trim()), true), extensions), sectionName.substring(1), true));
				} else { // Start a new section
					final Object location = state.toLocation();

					state.getSections().push(new Section(state.getSections().peek(), location, state.createExpression(location, state.createExpressionParser(expression, false), extensions)));
				}

				// Add a new render section action
				state.getRenderLists().peek().add(new SectionRenderer(state.getSections().peek()));
				state.getRenderLists().push(state.getSections().peek().getRenderList());
				return null;
			}

			case '^': { // Start a new inverted section, or else block for the current section
				final TrimmedString expression = Utilities.trim(tag, 1);
				final boolean elseIf = expression.string.startsWith("#");
				final boolean elseDoc = expression.string.startsWith("^");

				if ((expression.string.isEmpty() || elseIf || elseDoc) && extensions.contains(Extension.ELSE_TAGS)) { // Else block for the current section
					if (state.getSections().peek().getExpression() == null && state.getSections().peek().getAnnotation() == null || state.getSections().peek().getRenderList() != state.getRenderLists().pop()) {
						throw new LoadException(loaders, "Section else tag outside section start tag");
					}

					final Section previousSection = state.getSections().peek();

					if (elseIf) { // "else if" tag
						final Object location = state.toLocation();

						state.getSections().pop(1).push(new Section(state.getSections().peek(), location, state.createExpression(location, state.createExpressionParser(Utilities.trim(tag, 2), false), extensions)));
						previousSection.getInvertedRenderList().add(new SectionRenderer(state.getSections().peek()));
						state.getRenderLists().push(state.getSections().peek().getRenderList());
						return null;
					} else if (elseDoc) { // documentative "else" tag
						final Section section = state.getSections().peek();
						final String expressionString = expression.string.substring(1).trim();

						if (!section.getName().contentEquals(expressionString)) {
							throw new LoadException(loaders, "Section else tag mismatch, expecting else tag for section " + section);
						}
					}

					state.getRenderLists().push(previousSection.getInvertedRenderList());
				} else { // Start a new inverted section
					final Object location = state.toLocation();

					state.getSections().push(new Section(state.getSections().peek(), location, state.createExpression(location, state.createExpressionParser(expression, false), extensions)));
					state.getRenderLists().peek().add(new SectionRenderer(state.getSections().peek()));
					state.getRenderLists().push(state.getSections().peek().getInvertedRenderList());
				}

				return null;
			}

			case '/': { // Close the current section
				final TrimmedString expression = Utilities.trim(tag, 1);
				final Section section = state.getSections().peek();

				if (state.getSections().size() <= 1) { // There should always be at least one section on the stack (the template root section)
					throw new LoadException(loaders, "Section close tag without matching section start tag");
				} else if ((!expression.string.isEmpty() || !extensions.contains(Extension.EMPTY_END_TAGS)) && !section.getName().contentEquals(expression.string)) {
					if (!expression.string.isEmpty() && extensions.contains(Extension.SMART_END_TAGS)) {
						break;
					}

					throw new LoadException(loaders, "Section close tag mismatch, expecting close tag for section " + section);
				}

				if (state.getSections().pop().getParent() == null) { // Null parent means top-level, which indicates an inline partial
					state.removeStandaloneTagHead();
					state.setStandaloneStatus(TemplateLoadState.TAG_CHECK_TAIL_STANDALONE)
							.getTemplateBindings().pop();
				}

				state.getRenderLists().pop();
				return null;
			}

			case '=': { // Set delimiter
				final Matcher matcher = SET_DELIMITER_PATTERN.matcher(tag);

				if (!matcher.matches()) {
					throw new LoadException(loaders, "Invalid set delimiter tag");
				}

				state.setDelimiter(new TemplateLoadState.Delimiter(matcher.group("start"), matcher.group("end")));
				return null;
			}

			case '{': // Unescaped content tag
			case '&':
				state.setStandaloneStatus(TemplateLoadState.TAG_CANT_BE_STANDALONE) // Content tags cannot be stand-alone tags
					.getRenderLists().peek().add(new DynamicContentRenderer(state.createExpression(state.toLocation(), state.createExpressionParser(Utilities.trim(tag, 1), false), extensions), false));
				return null;

			default:
				break;
		}

		// Load the expression
		final ExpressionParseState parseState = state.createExpressionParser(Utilities.trim(tag, 0), false);
		final Expression expression = state.createExpression(state.toLocation(), parseState, extensions);

		switch (parseState.getEvaluation()) {
			case EVALUATE:
				state.getRenderLists().peek().add(new ExpressionEvaluationRenderer(expression));
				break;
			case EVALUATE_AND_RENDER: // Parse as a dynamic content tag
				state.setStandaloneStatus(TemplateLoadState.TAG_CANT_BE_STANDALONE) // Content tags cannot be stand-alone tags
					.getRenderLists().peek().add(new DynamicContentRenderer(expression, true));
				break;
			default:
				break;
		}

		return null;
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
