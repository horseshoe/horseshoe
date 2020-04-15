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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import horseshoe.CommandLineOption.ArgumentPair;
import horseshoe.CommandLineOption.OptionSet;
import horseshoe.Settings.ContextAccess;

/**
 * The runner enables rendering a Horseshoe template via the command line. While not as full-featured as the software library, several options can be changed using command line arguments.
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
			CommandLineOption.ofNameWithArgument('D', "data", "entry", "Adds <entry> to the global data map in the form key=value (value defaults to true when not specified). The value is parsed as follows: Boolean if \"true\" or \"false\", Long or Integer if integral, Double if floating-point, null if \"null\", or String. This option may be specified multiple times."),
			CommandLineOption.ofNameWithArgument('I', "include", "directory", "Adds <directory> to the list of directories to be included when searching for partials. This option may be specified multiple times."),
			CommandLineOption.ofNameWithArgument("input-charset", "charset", "Sets the charset of the input to <charset>. Defaults to UTF-8 for files. Valid values are " + join(", ", getCharsetNames()) + "."),
			CommandLineOption.ofNameWithArgument('o', "output", "output-filename", "Writes output to <output-filename>."),
			CommandLineOption.ofNameWithArgument('c', "output-charset", "charset", "Sets the charset of the output to <charset>. Defaults to UTF-8 for files. Valid values are " + join(", ", getCharsetNames()) + ".")
	);

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
	 * Parses a command line argument not part of any option.
	 *
	 * @param argument the argument to parse
	 * @param expectingArgument true if an argument is expected, otherwise false
	 * @return null if the argument should not be used, otherwise the parsed argument
	 */
	private static String parseArgument(final String argument, final boolean expectingArgument) {
		if ("--".equals(argument)) {
			return null;
		} else if (!expectingArgument) {
			throw new IllegalArgumentException("Unexpected argument detected: " + argument);
		}

		return argument;
	}

	/**
	 * Parses and adds a data define of the form "key[=value]" to the specified map. Supported values are Boolean "true" and "false", "null", a Long or Integer, a Double, or a String otherwise.
	 *
	 * @param dataMap the map used to add the data define
	 * @param value the data define in the form "key[=value]"
	 */
	private static void parseDataDefine(final Map<String, Object> dataMap, final String value) {
		final String[] definition = value.split("=", 2);

		// Check for boolean (don't ignore case, so that toString() always returns original value)
		if (definition.length == 1 || "true".equals(definition[1])) {
			dataMap.put(definition[0], Boolean.TRUE);
		} else if ("false".equals(definition[1])) {
			dataMap.put(definition[0], Boolean.FALSE);
		} else if ("null".equals(definition[1])) {
			dataMap.put(definition[0], null);
		} else {
			try { // Try long
				final long longValue = Long.parseLong(definition[1]);

				if ((int)longValue == longValue) {
					dataMap.put(definition[0], Integer.valueOf((int)longValue));
				} else {
					dataMap.put(definition[0], Long.valueOf(longValue));
				}
			} catch (final NumberFormatException longE) {
				try { // Try double
					dataMap.put(definition[0], Double.parseDouble(definition[1]));
				} catch (final NumberFormatException doubleE) { // Fall back to string
					dataMap.put(definition[0], definition[1]);
				}
			}
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
		String inputFile = null;
		String outputFile = null;
		Charset stdInCharset = Charset.defaultCharset();
		Charset stdOutCharset = Charset.defaultCharset();
		Charset outputCharset = loader.getCharset();

		// Parse the arguments
		for (final Iterator<ArgumentPair> it = OPTIONS.parse(args); it.hasNext(); ) {
			final ArgumentPair pair = it.next();

			if (pair.option != null) {
				assert pair.option.longName != null: "Option " + pair.option + " does not have a long name.";

				switch (pair.option.longName) {
				case "help": showHelp(System.out); return;
				case "version": System.out.println("Horseshoe (java) version " + Template.VERSION); return;
				case "log-level": Template.LOGGER.setLevel(Level.parse(pair.argument)); break;
				case "disable-extensions": loader.setExtensions(EnumSet.noneOf(Extension.class)); break;
				case "html": settings.setEscapeFunction(Settings.HTML_ESCAPE_FUNCTION); break;
				case "add-class": settings.getLoadableClasses().add(pair.argument); break;
				case "access": settings.setContextAccess(ContextAccess.valueOf(pair.argument)); break;
				case "data": parseDataDefine(globalData, pair.argument); break;
				case "include": loader.getIncludeDirectories().add(Paths.get(pair.argument)); break;
				case "input-charset": stdInCharset = loader.setCharset(Charset.forName(pair.argument)).getCharset(); break;
				case "output": outputFile = pair.argument; break;
				case "output-charset": stdOutCharset = outputCharset = Charset.forName(pair.argument); break;

				default: throw new IllegalArgumentException("Unrecognized option detected: " + pair.option);
				}
			} else {
				inputFile = parseArgument(pair.argument, inputFile == null);
			}
		}

		final Template template;

		// Load template (readers are automatically closed by Horseshoe, so no need to use try-with-resources)
		if (inputFile == null || "-".equals(inputFile)) {
			template = loader.load("<stdin>", new BufferedReader(new InputStreamReader(System.in, stdInCharset)));
		} else {
			template = loader.load(inputFile, Paths.get(inputFile), loader.getCharset());
		}

		// Render the template
		try (final Writer writer = new BufferedWriter(outputFile == null ? new OutputStreamWriter(System.out, stdOutCharset) : new OutputStreamWriter(new FileOutputStream(outputFile), outputCharset))) {
			template.render(settings, globalData, writer);
		}
	}

	/**
	 * Prints the help information to a print stream.
	 *
	 * @param stream the stream used to print the help information
	 */
	private static void showHelp(final PrintStream stream) {
		stream.println("Usage: " + Runner.class.getName() + " [option]... [file]");
		stream.println("Renders a Horseshoe template to <stdout> or an output file.");
		stream.println();
		OPTIONS.print(stream);
	}

}
