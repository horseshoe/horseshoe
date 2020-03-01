package horseshoe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.CharSequenceUtils;
import horseshoe.internal.Expression;
import horseshoe.internal.Identifier;
import horseshoe.internal.ParsedLine;
import horseshoe.internal.PersistentStack;

/**
 * The TemplateLoader class is used to load any number of {@link Template}s before rendering. Various properties can be configured to load templates with different settings.
 */
public class TemplateLoader {

	private static final StaticContentRenderer EMPTY_STATIC_CONTENT_RENDERER = new StaticContentRenderer(Arrays.asList(new ParsedLine("", null)), true);
	private static final Pattern WHITESPACE_ONLY_PATTERN = Pattern.compile("\\s*");
	private static final Pattern SET_DELIMITER_PATTERN = Pattern.compile("=\\s*(?<start>[^\\s]+)\\s+(?<end>[^\\s]+)\\s*=");
	private static final Pattern ANNOTATION_PATTERN = Pattern.compile("(?<name>@" + Identifier.PATTERN + ")\\s*(?:\\(\\s*(?<parameters>.*)\\s*\\)\\s*)?", Pattern.DOTALL);
	private static final Pattern NAMED_EXPRESSION_PATTERN = Pattern.compile("(?<name>" + Identifier.PATTERN + ")\\s*(?:\\(\\s*\\)\\s*)?[-=]>\\s*");

	private final Map<String, Template> templates = new HashMap<>();
	private final Map<String, Loader> templateLoaders = new HashMap<>();
	private final List<Path> includeDirectories = new ArrayList<>();
	private Charset charset = StandardCharsets.UTF_8;
	private boolean preventPartialPathTraversal = true;
	private EnumSet<Extension> extensions = EnumSet.allOf(Extension.class);

	private static final class Delimiter {

		private static final Pattern DEFAULT_START_PATTERN = Pattern.compile(Pattern.quote("{{"));
		private static final Pattern DEFAULT_END_PATTERN = Pattern.compile(Pattern.quote("}}"));
		private static final Pattern DEFAULT_END_UNESCAPED_PATTERN = Pattern.compile(Pattern.quote("}}}"));

		private final Pattern start;
		private final Pattern end;
		private final Pattern unescapedEnd;

		public Delimiter() {
			this.start = DEFAULT_START_PATTERN;
			this.end = DEFAULT_END_PATTERN;
			this.unescapedEnd = DEFAULT_END_UNESCAPED_PATTERN;
		}

		public Delimiter(final String start, final String end) {
			this.start = Pattern.compile(Pattern.quote(start));
			this.end = Pattern.compile(Pattern.quote(end));
			this.unescapedEnd = Pattern.compile(Pattern.quote("}" + end));
		}

	}

	/**
	 * Creates a new template loader that is mustache-compatible
	 *
	 * @return the new mustache loader
	 */
	public static TemplateLoader newMustacheLoader() {
		return new TemplateLoader().setExtensions(EnumSet.noneOf(Extension.class));
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
	 * Creates a default template loader. The default list of include directories contains only the current directory.
	 */
	public TemplateLoader() {
		this(Collections.singletonList(Paths.get(".")));
	}

	/**
	 * Adds templates from strings. The templates will not be loaded until they are referenced by another template being loaded or the {@link #load(String)} method is called with the specified template name. Any templates already loaded are ignored.
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
	 * Adds a template from a string. The template will not be loaded until it is referenced by another template being loaded or the {@link #load(String)} method is called with the specified template name. If the template is already loaded, this has no effect.
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
	 * Adds a template to the loader. The template will not be loaded until it is referenced by another template being loaded or the {@link #load(String)} method is called with the specified template name. If the template is already loaded, this has no effect.
	 *
	 * @param template the template to add to the loader
	 * @return this loader
	 */
	public TemplateLoader add(final Template template) {
		if (!templates.containsKey(template.getName())) {
			templates.put(template.getName(), template);
		}

		return this;
	}

	/**
	 * Adds a template from a file. The template will not be loaded until it is referenced by another template being loaded or the {@link #load(String)} method is called with the specified template name. If the template is already loaded, this has no effect.
	 *
	 * @param name the name of the template
	 * @param file the file to load as a template
	 * @param charset the character set to use when loading the file
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
	 * Adds a template from a file. The template will not be loaded until it is referenced by another template being loaded or the {@link #load(String)} method is called with the specified template name. If the template is already loaded, this has no effect.
	 *
	 * @param name the name of the template
	 * @param file the file to load as a template
	 * @return this loader
	 * @throws FileNotFoundException if the file does not exist
	 */
	public TemplateLoader add(final String name, final Path file) throws FileNotFoundException {
		return add(name, file, charset);
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
	 * Closes any open readers. If no open readers exist, this has no effect. This can be used to cleanup after a load exception occurs.
	 */
	public void close() {
		for (final Loader loader : templateLoaders.values()) {
			loader.close();
		}

		templateLoaders.clear();
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
	public Template load(final String name, final CharSequence value) throws LoadException {
		return add(name, value).load(name);
	}

	/**
	 * Loads a template from a file. If the template is already loaded, this has no effect.
	 *
	 * @param name the name of the template
	 * @param file the file to load as a template
	 * @param charset the character encoding to use when loading the template from the file
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws FileNotFoundException if the file does not exist
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	public Template load(final String name, final Path file, final Charset charset) throws FileNotFoundException, LoadException {
		return add(name, file, charset).load(name);
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
	 * Loads the specified template using the specified settings. If the template is already loaded the settings are ignored.
	 *
	 * @param name the name of the template to load
	 * @param loaders the stack of items being loaded
	 * @return the loaded template, or null if the template could not be loaded and the settings specify not to throw on a load failure
	 * @throws LoadException if a Horseshoe error is encountered while loading the template
	 */
	private Template load(final String name, final PersistentStack<Loader> loaders) throws LoadException {
		Template template = templates.get(name);

		if (template == null) {
			template = new Template(name);
			templates.put(name, template);

			Loader loader = templateLoaders.remove(name);

			try {
				if (loader == null) {
					// Try to load the template from the current template directory
					final Path baseDirectory = loaders.isEmpty() || loaders.peek().getFile() == null ? Paths.get(".") : loaders.peek().getFile().getParent();
					final Path toLoadFromBase = baseDirectory == null ? null : baseDirectory.resolve(name).normalize();

					if (toLoadFromBase != null && toLoadFromBase.toFile().isFile()) {
						if (!getPreventPartialPathTraversal() || toLoadFromBase.startsWith(baseDirectory)) {
							loader = new Loader(name, toLoadFromBase, charset);
						} else {
							for (final Path directory : includeDirectories) {
								if (toLoadFromBase.startsWith(directory.normalize())) {
									loader = new Loader(name, toLoadFromBase, charset);
									break;
								}
							}
						}
					}

					if (loader == null) { // Try to load the template from the list of include directories
						for (final Path directory : includeDirectories) {
							final Path toLoad = directory.resolve(name).normalize();

							if ((!getPreventPartialPathTraversal() || toLoad.startsWith(directory.normalize())) && toLoad.toFile().isFile()) {
								loader = new Loader(name, toLoad, charset);
								break;
							}
						}
					}

					if (loader == null) {
						if (extensions.contains(Extension.THROW_ON_PARTIAL_NOT_FOUND)) {
							throw new LoadException(loaders, "Template \"" + name + "\" could not be found");
						} else {
							return template; // Return empty template, per mustache spec
						}
					}
				}

				loadTemplate(template, loaders, loader);
			} catch (final IOException | RuntimeException e) {
				throw new LoadException(loaders, "Template \"" + name + "\" could not be loaded: " + e.getMessage(), e);
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
	 * @param template the template to load
	 * @param loaders the stack of items being loaded
	 * @param loader the item being loaded
	 * @throws LoadException if an error is encountered while loading the template
	 * @throws IOException if an error is encountered while reading from a file or stream
	 */
	private void loadTemplate(final Template template, final PersistentStack<Loader> loaders, final Loader loader) throws LoadException, IOException {
		final PersistentStack<Section> sections = new PersistentStack<>();
		final PersistentStack<List<Action>> actionStack = new PersistentStack<>();

		Delimiter delimiter = new Delimiter();
		StaticContentRenderer textBeforeStandaloneTag = null;

		loaders.push(loader);
		sections.push(template.getSection());
		actionStack.push(template.getActions());

		// Parse all tags
		for (int tags = 0; true; tags++) {
			// Get text before this tag
			final List<ParsedLine> lines = new ArrayList<>();
			final CharSequence freeText = loader.next(delimiter.start, lines);

			if (freeText.length() > 0) {
				final StaticContentRenderer currentText = new StaticContentRenderer(lines, actionStack.peek().isEmpty());
				actionStack.peek().add(currentText);

				// Check for stand-alone tags
				if (textBeforeStandaloneTag != null && currentText.isMultiline() &&
						WHITESPACE_ONLY_PATTERN.matcher(textBeforeStandaloneTag.getLastLine()).matches() &&
						WHITESPACE_ONLY_PATTERN.matcher(currentText.getFirstLine()).matches()) {
					textBeforeStandaloneTag.ignoreLastLine();
					currentText.ignoreFirstLine();
				}

				// Use the current text as the basis for future stand-alone tag detection if the currentText is multiline or it is the first line in the template
				if (currentText.isMultiline() || tags == 0) {
					textBeforeStandaloneTag = currentText;
				} else {
					textBeforeStandaloneTag = null;
				}
			} else if (tags > 0) { // length == 0
				// The text can only be following a stand-alone tag if it is at the very end of the template
				if (!loader.hasNext() && textBeforeStandaloneTag != null && WHITESPACE_ONLY_PATTERN.matcher(textBeforeStandaloneTag.getLastLine()).matches()) {
					textBeforeStandaloneTag.ignoreLastLine();
				}

				// Empty text can never be just before a stand-alone tag, unless it is the first content in the template
				textBeforeStandaloneTag = null;
			} else {
				textBeforeStandaloneTag = EMPTY_STATIC_CONTENT_RENDERER;
			}

			if (!loader.hasNext()) {
				break;
			}

			// Parse the expression
			final CharSequence tag = loader.next(loader.checkNext("{") ? delimiter.unescapedEnd : delimiter.end, null);

			if (!loader.hasNext()) {
				throw new LoadException(loaders, "Unclosed tag, unexpected end of stream");
			}

			processTag:
			while (tag.length() != 0) {
				try {
					switch (tag.charAt(0)) {
					case '!': // Comments are completely ignored
						break processTag;

					case '<': // Local partial
						if (extensions.contains(Extension.INLINE_PARTIALS)) {
							final String name = CharSequenceUtils.trim(tag, 1, tag.length()).toString();
							final Template partial = new Template(name);

							sections.peek().getLocalPartials().put(name, partial);
							sections.push(partial.getSection());
							actionStack.push(partial.getActions());
							break processTag;
						}

						break;

					case '>': { // Load partial
						final String name = CharSequenceUtils.trim(tag, 1, tag.length()).toString();
						final Template localPartial = sections.peek().getLocalPartials().get(name);
						final Template partial = (localPartial != null ? localPartial : load(name, loaders));

						sections.peek().getNamedExpressions().putAll(partial.getSection().getNamedExpressions());
						actionStack.peek().add(new TemplateRenderer(partial, textBeforeStandaloneTag != null ? textBeforeStandaloneTag : EMPTY_STATIC_CONTENT_RENDERER));
						break processTag;
					}

					case '#': { // Start a new section, or repeat the previous section
						final CharSequence expression = CharSequenceUtils.trim(tag, 1, tag.length());

						if (expression.length() == 0 && extensions.contains(Extension.REPEATED_SECTIONS)) { // Repeat the previous section
							sections.push(Section.repeat(sections.peek()));
						} else if (expression.length() != 0 && expression.charAt(0) == '@' && extensions.contains(Extension.ANNOTATIONS)) { // Annotation section
							final Matcher annotation = ANNOTATION_PATTERN.matcher(expression);

							if (!annotation.matches()) {
								throw new LoadException(loaders, "Invalid annotation format");
							}

							final String sectionName = annotation.group("name");
							final String parameters = annotation.group("parameters");

							// Load the annotation arguments
							sections.push(new Section(sections.peek(), sectionName, parameters == null ? null : new Expression(loader.toLocationString(), null, parameters, sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS)), sectionName.substring(1), true));
						} else { // Start a new section
							sections.push(new Section(sections.peek(), new Expression(loader.toLocationString(), null, expression, sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS))));
						}

						// Add a new render section action
						actionStack.peek().add(SectionRenderer.FACTORY.create(sections.peek()));
						actionStack.push(sections.peek().getActions());
						break processTag;
					}

					case '^': { // Start a new inverted section, or else block for the current section
						final CharSequence expression = CharSequenceUtils.trim(tag, 1, tag.length());

						if (expression.length() == 0 && extensions.contains(Extension.ELSE_TAGS)) { // Else block for the current section
							if (sections.isEmpty() || (sections.peek().getExpression() == null && sections.peek().getAnnotation() == null)) {
								throw new LoadException(loaders, "Section else tag outside section start tag");
							}

							actionStack.pop();
						} else { // Start a new inverted section
							sections.push(new Section(sections.peek(), new Expression(loader.toLocationString(), null, expression, sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS))));
							actionStack.peek().add(SectionRenderer.FACTORY.create(sections.peek()));
						}

						// Grab the inverted action list from the section
						final List<Action> actions = actionStack.peek(); // TODO: use a more robust method than peeking into the action stack and assuming it is a SectionRenderer

						actionStack.push(((SectionRenderer)actions.get(actions.size() - 1)).getSection().getInvertedActions());
						break processTag;
					}

					case '/': { // Close the current section
						final CharSequence expression = CharSequenceUtils.trim(tag, 1, tag.length());
						final Section section = sections.peek();

						if (sections.size() <= 1) { // There should always be at least one section on the stack (the template root section)
							throw new LoadException(loaders, "Section close tag without matching section start tag");
						} else if ((expression.length() > 0 || !extensions.contains(Extension.EMPTY_END_TAGS)) && !section.getName().contentEquals(expression)) {
							if (expression.length() > 0 && extensions.contains(Extension.SMART_END_TAGS)) {
								break;
							}

							throw new LoadException(loaders, "Unmatched section start tag, expecting \"" + section.getName() + "\"");
						}

						sections.pop();
						actionStack.pop();
						break processTag;
					}

					case '=': { // Set delimiter
						final Matcher matcher = SET_DELIMITER_PATTERN.matcher(tag);

						if (!matcher.matches()) {
							throw new LoadException(loaders, "Invalid set delimiter tag");
						}

						delimiter = new Delimiter(matcher.group("start"), matcher.group("end"));
						break processTag;
					}

					case '{': // Unescaped content tag
					case '&':
						textBeforeStandaloneTag = null; // Content tags cannot be stand-alone tags
						actionStack.peek().add(new DynamicContentRenderer(new Expression(loader.toLocationString(), null, CharSequenceUtils.trim(tag, 1, tag.length()), sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS)), false));
						break processTag;

					default:
						// Check for a named expression
						if (extensions.containsAll(EnumSet.of(Extension.EXPRESSIONS, Extension.NAMED_EXPRESSIONS))) {
							final CharSequence expression = CharSequenceUtils.trim(tag, 0, tag.length());
							final Matcher namedExpression = NAMED_EXPRESSION_PATTERN.matcher(expression);

							if (namedExpression.lookingAt()) {
								new Expression(loader.toLocationString(), namedExpression.group("name"), expression.subSequence(namedExpression.end(), expression.length()), sections.peek().getNamedExpressions(), true);
								break processTag;
							}
						}

						break;
					}

					// Default to parsing as a dynamic content tag
					textBeforeStandaloneTag = null; // Content tags cannot be stand-alone tags
					actionStack.peek().add(new DynamicContentRenderer(new Expression(loader.toLocationString(), null, CharSequenceUtils.trim(tag, 0, tag.length()), sections.peek().getNamedExpressions(), extensions.contains(Extension.EXPRESSIONS)), true));
					break;
				} catch (final LoadException e) {
					throw e;
				} catch (final Exception e) {
					throw new LoadException(loaders, "Invalid tag \"" + tag + "\"", e);
				}
			}
		}

		// Pop off the top section (should be the template root section) and verify that the section stack is empty
		final Section topSection = sections.pop();

		if (!sections.isEmpty()) {
			throw new LoadException(loaders, "Unmatched section tag \"" + topSection.toString() + "\", unexpected end of stream");
		}

		// Check for empty last line of template, so that indentation is not applied
		if (textBeforeStandaloneTag != null && textBeforeStandaloneTag.getLastLine().isEmpty()) {
			textBeforeStandaloneTag.ignoreLastLine();
		}

		loaders.pop();
	}

	/**
	 * Sets the character set used for loading additional templates
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
