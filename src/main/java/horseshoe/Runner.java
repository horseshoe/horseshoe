package horseshoe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.CommandLineOption.ArgumentPair;
import horseshoe.CommandLineOption.OptionSet;
import horseshoe.Settings.ContextAccess;

/**
 * The runner enables rendering Horseshoe templates via the command line. While not as full-featured as the software library, several options can be changed using command line arguments.
 */
public class Runner {

	static final int ERROR_EXIT_CODE = 1;

	public static final OptionSet OPTIONS = new OptionSet(
			CommandLineOption.ofName('h', "help", "Displays usage and lists all options."),
			CommandLineOption.ofName('v', "version", "Displays the Horseshoe version and exits."),
			CommandLineOption.ofNameWithArgument('l', "log-level", "level", "Sets the logging <level> for the Horseshoe logger. Valid values are " + join(", ", Level.class.getFields()) + "."),
			CommandLineOption.ofName("disable-extensions", "Disables all Horseshoe extensions."),
			CommandLineOption.ofName("html", "Enables HTML escaping of rendered content."),
			CommandLineOption.ofNameWithArgument("add-class", "class", "Adds <class> as a loadable class in the template using the ~@'java.lang.System'.currentTimeMillis() syntax."),
			CommandLineOption.ofNameWithArgument("access", "level", "Changes the access to <level>. Valid values are " + join(", ", Settings.ContextAccess.class.getEnumConstants()) + ". The default value is " + new Settings().getContextAccess() + "."),
			CommandLineOption.ofNameWithArgument('D', "define", "entry", "Adds <entry> to the global data map in the form key=value (value defaults to true when not specified). The value is parsed as JSON, with some YAML features allowed: anchors, aliases, and plain single-line scalars. This option may be specified multiple times."),
			CommandLineOption.ofNameWithArgument('d', "data-file", "file", "Adds the contents of <file> to the global data map. The file is parsed as a UTF-8 JSON object, with some YAML-like features allowed: anchors, aliases, and plain single-line scalars. All members of the JSON object are added to the map. This option may be specified multiple times."),
			CommandLineOption.ofNameWithArgument('I', "include", "directory", "Adds <directory> to the list of directories to be included when searching for partials. This option may be specified multiple times."),
			CommandLineOption.ofNameWithArgument("input-charset", "charset", "Sets the charset of the input to <charset>. Defaults to UTF-8 for files. Valid values are " + join(", ", getCharsetNames()) + "."),
			CommandLineOption.ofNameWithArgument('o', "output", "output-filename", "Writes output to <output-filename> instead of <stdout>."),
			CommandLineOption.ofNameWithArgument('c', "output-charset", "charset", "Sets the charset of the output to <charset>. Defaults to UTF-8 for files. Valid values are " + join(", ", getCharsetNames()) + ".")
	);

	/**
	 * The data parser is a JSON-compliant data parser with extensions to allow YAML-like syntax, including anchors, aliases, and simplified single-line plain scalar values.
	 */
	static class DataParser {

		private static final String SPACE_CHARACTER = "[ \t\r\n]";
		private static final String NON_SPACE_CHARACTER = "[!-\\x{10FFFF}&&[^\"&*:,\\[\\]{}]]";

		private static final Pattern TOKEN_PATTERN = Pattern.compile("\"(?<string>(?:[ -\\x{10FFFF}&&[^\"\\\\]]|\\\\[\"\\\\/bfnrt]|\\\\u[0-9A-Fa-f]{4})*)\"|(?<raw>" + NON_SPACE_CHARACTER + "+)");

		private static final Pattern ANCHOR_PATTERN = Pattern.compile("&" + SPACE_CHARACTER + "*(?<anchor>" + NON_SPACE_CHARACTER + "+)");
		private static final Pattern ALIAS_PATTERN = Pattern.compile("[*]" + SPACE_CHARACTER + "*(?<alias>" + NON_SPACE_CHARACTER + "+)");

		private static final Pattern KEY_PATTERN = Pattern.compile("(?:" + TOKEN_PATTERN.pattern() + ")" + SPACE_CHARACTER + "*:");

		private static final Pattern NUMBER_PATTERN = Pattern.compile("(?<value>-?(?:0|[1-9][0-9]*)(?<double>(?:[.][0-9]+)?[eE][-+]?[0-9]+|[.][0-9]+)?)");
		private static final Pattern STRING_PATTERN = TOKEN_PATTERN;

		private final String data;
		private int index;
		private final boolean allowRawStrings;
		private final Matcher matcher;
		private final Map<String, Object> anchors = new LinkedHashMap<>();
		private final Map<String, Alias> unresolvedAliases = new LinkedHashMap<>();

		/**
		 * An alias updater is used to update the value of a previously unresolved alias in the data.
		 */
		private interface AliasUpdater {
			/**
			 * Updates the value of the alias.
			 *
			 * @param value the new value
			 */
			void update(final Object value);
		}

		/**
		 * An alias represents an unresolved alias in the data that will be updated when the next anchor is found.
		 */
		private static final class Alias {
			public final List<AliasUpdater> updaters = new ArrayList<>();
		}

		/**
		 * Unescapes a string that matches a JSON quoted string.
		 *
		 * @param value the JSON string
		 * @return the unescaped string
		 */
		private static String parseString(final String value) {
			int backslash = value.indexOf('\\');

			if (backslash < 0) {
				return value;
			}

			// Find escape sequences and replace them with the proper character
			final StringBuilder sb = new StringBuilder(value.length());
			int start = 0;

			do {
				sb.append(value, start, backslash);
				start = backslash + 2;

				switch (value.charAt(backslash + 1)) {
					case 'b':
						sb.append('\b');
						break;
					case 'f':
						sb.append('\f');
						break;
					case 'n':
						sb.append('\n');
						break;
					case 'r':
						sb.append('\r');
						break;
					case 't':
						sb.append('\t');
						break;
					case 'u':
						sb.appendCodePoint(Integer.parseInt(value.substring(backslash + 2, backslash + 6), 16));
						start = backslash + 6;
						break;
					default:
						sb.append(value.charAt(backslash + 1));
						break;
				}
			} while ((backslash = value.indexOf('\\', start)) >= 0);

			return sb.append(value, start, value.length()).toString();
		}

		/**
		 * Creates a new data parser from the given string.
		 *
		 * @param data the string data representation
		 */
		DataParser(final String data) {
			this(data, 0, true);
		}

		/**
		 * Creates a new data parser from the given string.
		 *
		 * @param data the string data representation
		 * @param startingIndex the index at which parsing will start
		 * @param allowRawStrings true to allow simplified single-line plain scalar values
		 */
		DataParser(final String data, final int startingIndex, final boolean allowRawStrings) {
			this.data = data;
			this.index = skipWhitespace(startingIndex);
			this.allowRawStrings = allowRawStrings;
			this.matcher = TOKEN_PATTERN.matcher(data);
		}

		/**
		 * Parses a list of values that the matcher is currently looking at.
		 *
		 * @return the parsed list
		 */
		private List<Object> parseList() {
			if (index < data.length() && data.charAt(index) == ']') {
				index = skipWhitespace(index + 1);
				return new ArrayList<>();
			}

			// Populate the list
			final List<Object> list = new ArrayList<>();

			do {
				final Object value = parseNode();

				if (value instanceof Alias) {
					final int indexToUpdate = list.size();

					list.add(null);
					((Alias)value).updaters.add(new AliasUpdater() {
						@Override
						public void update(final Object value) {
							list.set(indexToUpdate, value);
						}
					});
				} else {
					list.add(value);
				}
			} while (requireNext(',', ']'));

			return list;
		}

		/**
		 * Parses a map at the current index. A check is made to ensure the end of the data has been reached after parsing.
		 *
		 * @return the parsed map
		 */
		public Map<String, Object> parseAsMap() {
			if (index >= data.length() || data.charAt(index) != '{') {
				throw new IllegalStateException("Failed to find starting object character \"{\" at offset " + index);
			}

			index = skipWhitespace(index + 1);
			final Map<String, Object> map = parseMap();

			requireEnd();
			return map;
		}

		/**
		 * Parses a value at the current index. A check is made to ensure the end of the data has been reached after parsing.
		 *
		 * @return the parsed value
		 */
		public Object parseAsValue() {
			final Object value = parseNode();

			requireEnd();
			return value;
		}

		/**
		 * Parses a map at the current index.
		 *
		 * @return the parsed map
		 */
		private Map<String, Object> parseMap() {
			if (index < data.length() && data.charAt(index) == '}') {
				index = skipWhitespace(index + 1);
				return Collections.emptyMap();
			}

			// Populate the map
			final Map<String, Object> map = new LinkedHashMap<>();

			do {
				// Parse the key
				final String raw;

				if (!matcher.usePattern(KEY_PATTERN).region(index, data.length()).lookingAt() || (!allowRawStrings && matcher.group("raw") != null)) {
					throw new IllegalStateException("Failed to find key at offset " + index);
				} else if (allowRawStrings) {
					raw = matcher.group("raw");
				} else {
					raw = null;
				}

				final String key = (raw == null ? parseString(matcher.group("string")) : raw);

				// Parse the value
				index = skipWhitespace(matcher.end());
				final Object value = parseNode();

				if (value instanceof Alias) {
					((Alias)value).updaters.add(new AliasUpdater() {
						@Override
						public void update(final Object value) {
							map.put(key, value);
						}
					});
				} else {
					map.put(key, value);
				}
			} while (requireNext(',', '}'));

			return map;
		}

		/**
		 * Parses a node at the current index.
		 *
		 * @return the parsed node
		 */
		private Object parseNode() {
			final List<String> anchorsToSet = new ArrayList<>();

			// Find all anchors
			for (matcher.usePattern(ANCHOR_PATTERN); matcher.region(index, data.length()).lookingAt(); index = skipWhitespace(matcher.end())) {
				anchorsToSet.add(matcher.group("anchor"));
			}

			// Check for an alias
			if (matcher.usePattern(ALIAS_PATTERN).lookingAt()) {
				final String aliasName = matcher.group("alias");
				final Object aliasValue = anchors.get(aliasName);

				index = skipWhitespace(matcher.end());

				if (aliasValue != null || anchors.containsKey(aliasName)) {
					return aliasValue;
				}

				Alias alias = unresolvedAliases.get(aliasName);

				if (alias == null) {
					alias = new Alias();
					unresolvedAliases.put(aliasName, alias);
				}

				return alias;
			}

			final Object value = parseValue();

			// Update all the anchor values
			for (final String anchor : anchorsToSet) {
				final Alias toUpdate = unresolvedAliases.remove(anchor);

				if (toUpdate != null) {
					for (final AliasUpdater updater : toUpdate.updaters) {
						updater.update(value);
					}
				}

				anchors.put(anchor, value);
			}

			return value;
		}

		/**
		 * Parses a value at the current index.
		 *
		 * @return the parsed value
		 */
		private Object parseValue() {
			if (index >= data.length()) {
				throw new IllegalStateException("Failed to match value, unexpected end of data");
			}

			if (data.charAt(index) == '{') {
				index = skipWhitespace(index + 1);
				return parseMap();
			} else if (data.charAt(index) == '[') {
				index = skipWhitespace(index + 1);
				return parseList();
			} else if (matcher.usePattern(NUMBER_PATTERN).lookingAt()) {
				index = skipWhitespace(matcher.end());
				final String number = matcher.group("value");

				if (matcher.group("double") != null) {
					return Double.parseDouble(number);
				}

				final long longValue = Long.parseLong(number);

				if ((int)longValue == longValue) {
					return (int)longValue;
				}

				return longValue;
			} else if (matcher.usePattern(STRING_PATTERN).lookingAt()) {
				index = skipWhitespace(matcher.end());
				final String raw = matcher.group("raw");

				if (raw == null) {
					return parseString(matcher.group("string"));
				} else if ("true".equals(raw)) {
					return Boolean.TRUE;
				} else if ("false".equals(raw)) {
					return Boolean.FALSE;
				} else if ("null".equals(raw)) {
					return null;
				} else if (allowRawStrings) {
					return raw;
				}
			}

			throw new IllegalStateException("Failed to match value at offset " + index);
		}

		/**
		 * Requires that the data has been fully consumed. If not, an exception is thrown.
		 */
		private void requireEnd() {
			if (index < data.length()) {
				throw new IllegalStateException("Unexpected extra data at offset " + index);
			}
		}

		/**
		 * Requires either a continuation character or a stop character at the current index. If neither is found, an exception is thrown. Returns true if the character is the continuation character.
		 *
		 * @param continuationCharacter the character that results in the current operation continuing
		 * @param stopCharacter the character that results in the current operation stopping
		 * @return true if the character at the current index is the continuation character, otherwise false
		 */
		private boolean requireNext(final char continuationCharacter, final char stopCharacter) {
			if (index >= data.length() || (data.charAt(index) != continuationCharacter && data.charAt(index) != stopCharacter)) {
				throw new IllegalStateException("Failed to find \"" + continuationCharacter + "\" or \"" + stopCharacter + "\" character at offset " + index);
			}

			final boolean hasNext = data.charAt(index) == continuationCharacter;

			index = skipWhitespace(index + 1);
			return hasNext;
		}

		/**
		 * Skips all whitespace characters from the starting index within the data.
		 *
		 * @param start the starting index within the data
		 * @return the first non-whitespace index, or the length of the data if all remaining characters are whitespace
		 */
		private int skipWhitespace(final int start) {
			int i = start;

			while (i < data.length() && (data.charAt(i) == ' ' || data.charAt(i) == '\t' || data.charAt(i) == '\n' || data.charAt(i) == '\r')) {
				i++;
			}

			return i;
		}

	}

	/**
	 * Gets the list of character set names.
	 *
	 * @return the list of character set names
	 */
	private static Object[] getCharsetNames() {
		final List<String> names = new ArrayList<>();

		for (final Field field : StandardCharsets.class.getFields()) { // Use standard charsets, too many in Charset.availableCharsets()
			if (Charset.class.equals(field.getType())) {
				try {
					names.add(((Charset)field.get(null)).name());
				} catch (final ReflectiveOperationException e) { // Ignore failures
				}
			}
		}

		return names.toArray();
	}

	/**
	 * Joins an array of objects using a delimiter.
	 *
	 * @param delimiter the delimiter that separates each element
	 * @param values the values to join together
	 * @return the joined string
	 */
	private static String join(final String delimiter, final Object[] values) {
		final StringBuilder sb = new StringBuilder();
		String separator = "";

		for (int i = 0; i < values.length; i++) {
			sb.append(separator).append(values[i] instanceof Member ? ((Member)values[i]).getName() : values[i]);
			separator = delimiter;
		}

		return sb.toString();
	}

	/**
	 * Renders a template using Horseshoe.
	 *
	 * @param args the arguments used when rendering the template
	 */
	public static void main(final String[] args) {
		try {
			renderTemplate(args);
		} catch (final Throwable t) {
			Template.LOGGER.log(Level.SEVERE, "Failed to render template: " + t.getMessage(), t);
			System.exit(ERROR_EXIT_CODE);
		}
	}

	/**
	 * Loads a template from stdin.
	 *
	 * @param loader the loader used to load the template
	 * @param charset the character set used to read stdin
	 * @return the loaded template
	 * @throws LoadException if an error occurs while loading the template
	 */
	private static Template loadTemplateFromStdIn(final TemplateLoader loader, final Charset charset) throws LoadException {
		return loader.load("<stdin>", new BufferedReader(new InputStreamReader(System.in, charset)));
	}

	/**
	 * Parses and adds a data define of the form "key[=value]" to the specified map. The value is parsed as JSON, with some YAML features allowed: anchors, aliases, and plain single-line scalars. If no value is specified, it is defaulted to true.
	 *
	 * @param dataMap the map used to add the data define
	 * @param value the data define in the form "key[=value]"
	 */
	private static void parseDataDefine(final Map<String, Object> dataMap, final String value) {
		final int split = value.indexOf('=');

		if (split < 0) {
			dataMap.put(value, Boolean.TRUE);
		} else {
			dataMap.put(value.substring(0, split), new DataParser(value, split + 1, true).parseAsValue());
		}
	}

	/**
	 * Renders a template using Horseshoe.
	 *
	 * @param args the arguments used when rendering the template
	 * @throws LoadException if an exception is thrown while loading the template
	 * @throws IOException if an exception is thrown while rendering to the writer
	 */
	private static void renderTemplate(final String[] args) throws LoadException, IOException {
		final TemplateLoader loader = new TemplateLoader();
		final Map<String, Object> globalData = new LinkedHashMap<>();
		final Settings settings = new Settings();
		final List<Template> templates = new ArrayList<>();
		String outputFile = null;
		Charset stdInCharset = Charset.defaultCharset();
		Charset stdOutCharset = Charset.defaultCharset();
		Charset outputCharset = loader.getCharset();

		// Parse the arguments
		for (final Iterator<ArgumentPair> it = OPTIONS.parse(args); it.hasNext(); ) {
			final ArgumentPair pair = it.next();

			if (pair.option != null) {
				assert pair.option.longName != null : "Option " + pair.option + " does not have a long name.";

				switch (pair.option.longName) {
					case "help":
						showHelp(System.out);
						return;
					case "version":
						System.out.println("Horseshoe (java) version " + Template.VERSION);
						return;
					case "log-level":
						Template.LOGGER.setLevel(Level.parse(pair.argument));
						break;
					case "disable-extensions":
						loader.setExtensions(EnumSet.noneOf(Extension.class));
						break;
					case "html":
						settings.setEscapeFunction(Settings.HTML_ESCAPE_FUNCTION);
						break;
					case "add-class":
						settings.getLoadableClasses().add(pair.argument);
						break;
					case "access":
						settings.setContextAccess(ContextAccess.valueOf(pair.argument));
						break;
					case "define":
						parseDataDefine(globalData, pair.argument);
						break;
					case "data-file":
						globalData.putAll(new DataParser(new String(Files.readAllBytes(Paths.get(pair.argument)), StandardCharsets.UTF_8)).parseAsMap());
						break;
					case "include":
						loader.getIncludeDirectories().add(Paths.get(pair.argument));
						break;
					case "input-charset":
						stdInCharset = loader.setCharset(Charset.forName(pair.argument)).getCharset();
						break;
					case "output":
						outputFile = pair.argument;
						break;
					case "output-charset":
						stdOutCharset = outputCharset = Charset.forName(pair.argument);
						break;
					default:
						throw new IllegalArgumentException("Unrecognized option detected: " + pair.option);
				}
			} else if ("-".equals(pair.argument)) {
				templates.add(loadTemplateFromStdIn(loader, stdInCharset));
			} else if (!"--".equals(pair.argument)) {
				templates.add(loader.load(Paths.get(pair.argument), loader.getCharset()));
			}
		}

		if (templates.isEmpty()) {
			templates.add(loadTemplateFromStdIn(loader, stdInCharset));
		}

		// Render the templates
		try (final Writer writer = new BufferedWriter(outputFile == null ? new OutputStreamWriter(System.out, stdOutCharset) : new OutputStreamWriter(new FileOutputStream(outputFile), outputCharset))) {
			for (final Template template : templates) {
				template.render(settings, globalData, writer);
			}
		}
	}

	/**
	 * Prints the help information to a print stream.
	 *
	 * @param stream the stream used to print the help information
	 */
	private static void showHelp(final PrintStream stream) {
		stream.println("Usage: " + Runner.class.getName() + " [[options] <input-file>]...");
		stream.println("  Renders Horseshoe templates from input files or <stdin>.");
		stream.println();
		stream.println("Options:");
		OPTIONS.print(stream);
	}

}
