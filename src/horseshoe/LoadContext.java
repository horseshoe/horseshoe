package horseshoe;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import horseshoe.internal.Loader;
import horseshoe.internal.PersistentStack;

public class LoadContext {

	public static final String PARTIAL_EXTENSION = ".U";

	private static final Template EMPTY_TEMPLATE = new Template();
	private static final Template RECURSIVE_TEMPLATE_DETECTED = new Template();

	/**
	 * Creates a new mustache-compatible load context using the specified string partials.
	 *
	 * @param stringPartials the string partials to use when a partial is included in a template
	 * @return a new mustache-compatible load context
	 */
	public static LoadContext newMustacheLoadContext(final Map<String, String> stringPartials) {
		return new LoadContext(stringPartials).setThrowOnPartialNotFound(false);
	}

	/**
	 * Creates a new mustache-compatible load context.
	 *
	 * @return a new mustache-compatible load context
	 */
	public static LoadContext newMustacheLoadContext() {
		return newMustacheLoadContext(Collections.emptyMap());
	}

	private final PersistentStack<Loader> loaders = new PersistentStack<>();
	private Charset charset = StandardCharsets.UTF_8;

	private final Map<String, Template> partials = new LinkedHashMap<>();
	private final Map<String, String> stringPartials = new LinkedHashMap<>();
	private final List<Path> includeDirectories = new ArrayList<>();
	private boolean throwOnPartialNotFound = true;

	/**
	 * Creates a load context using the specified string partials and include directories.
	 *
	 * @param stringPartials the string partials to use when a partial is included in a template
	 * @param includeDirectories the list of directories used to locate partial files included in a template
	 */
	public LoadContext(final Map<String, String> stringPartials, final Iterable<? extends Path> partialDirectories) {
		this.stringPartials.putAll(stringPartials);

		for (final Path path : partialDirectories) {
			this.includeDirectories.add(path);
		}
	}

	/**
	 * Creates a load context using the specified string partials. The default list of include directories contains only the current directory.
	 *
	 * @param stringPartials the string partials to use when a partial is included in a template
	 */
	public LoadContext(final Map<String, String> stringPartials) {
		this(stringPartials, Collections.singletonList(Paths.get(".")));
	}

	/**
	 * Creates a load context using the specified include directories.
	 *
	 * @param includeDirectories the list of directories used to locate partial files included in a template
	 */
	public LoadContext(final Iterable<? extends Path> includeDirectories) {
		this(Collections.emptyMap(), includeDirectories);
	}

	/**
	 * Creates a default load context. The default list of include directories contains only the current directory.
	 */
	public LoadContext() {
		this(Collections.emptyMap());
	}

	/**
	 * Gets the character set used for loading templates.
	 *
	 * @return the character set used for loading templates
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
	 * Gets the map of string partials. This map is used to locate partials included in a template. It is searched before files in the include directories.
	 *
	 * @return the map of string partials
	 */
	public Map<String, String> getStringPartials() {
		return stringPartials;
	}

	/**
	 * Gets whether or not an exception will be thrown when a partial is not found.
	 *
	 * @return true if an exception will be thrown when a partial is not found, otherwise false
	 */
	public boolean getThrowOnPartialNotFound() {
		return throwOnPartialNotFound;
	}

	/**
	 * Resets the context, so it can be reused.
	 *
	 * @return the list of active loaders before resetting the context
	 */
	List<Loader> reset() {
		final List<Loader> loaders = new ArrayList<>(this.loaders.size());

		for (final Loader loader : this.loaders) {
			loaders.add(loader);
		}

		this.loaders.clear();
		return loaders;
	}

	/**
	 * Loads a partial by name.
	 *
	 * @param name the name of the partial
	 * @return The loaded partial
	 * @throws LoadException if an error is encountered while loading the partial
	 */
	Template loadPartial(final String name) throws LoadException {
		// First, try to load the partial from an existing template
		Template found = partials.get(name);

		if (found == null) {
			// Next, try to load the partial from an internal string
			final String templateText = stringPartials.get(name);

			if (templateText != null) {
				partials.put(name, RECURSIVE_TEMPLATE_DETECTED);
				found = new Template(name, templateText, this);
				partials.put(name, found);
			} else {
				// Lastly, try to load the partial from file
				for (final String ext : new String[] { "", PARTIAL_EXTENSION }) {
					final Path currentDirectoryFile = Paths.get(name + ext);

					// Try to load the partial from the current directory
					if (currentDirectoryFile.toFile().isFile()) {
						partials.put(name, RECURSIVE_TEMPLATE_DETECTED);
						found = new Template(currentDirectoryFile, charset, this);
						partials.put(name, found);
						return found;
					}

					// Try to load the partial from the list of include directories
					for (final Path directory : includeDirectories) {
						final Path file = directory.resolve(name + ext);

						if (file.toFile().isFile()) {
							partials.put(name, RECURSIVE_TEMPLATE_DETECTED);
							found = new Template(file, charset, this);
							partials.put(name, found);
							return found;
						}
					}
				}

				if (found == null) {
					if (throwOnPartialNotFound) {
						throw new LoadException(reset(), "Partial not found: " + name);
					} else {
						return EMPTY_TEMPLATE;
					}
				}
			}
		} else if (found == RECURSIVE_TEMPLATE_DETECTED) {
			for (final Entry<String, Template> entry : partials.entrySet()) {
				if (entry.getValue() == RECURSIVE_TEMPLATE_DETECTED) {
					entry.setValue(null);
				}
			}

			throw new LoadException(reset(), "Recursive template detected: " + name);
		}

		return found;
	}

	/**
	 * Pops a loader off the loaders stack
	 *
	 * @return the loader popped off the loaders stack
	 */
	Loader popLoader() {
		return loaders.pop();
	}

	/**
	 * Pushes a new loader onto the loaders stack
	 *
	 * @param loader the loader to push onto the stack
	 * @return this load context
	 */
	LoadContext pushLoader(final Loader loader) {
		loaders.push(loader);
		return this;
	}

	/**
	 * Sets the character set used for loading templates
	 *
	 * @param charset the character set used for loading templates
	 * @return this load context
	 */
	public LoadContext setCharset(final Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * Sets whether or not an exception will be thrown when a partial is not found.
	 *
	 * @param throwOnPartialNotFound true to throw an exception when a partial is not found, otherwise false
	 * @return this load context
	 */
	public LoadContext setThrowOnPartialNotFound(final boolean throwOnPartialNotFound) {
		this.throwOnPartialNotFound = throwOnPartialNotFound;
		return this;
	}

}
