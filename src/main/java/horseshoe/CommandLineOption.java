package horseshoe;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An option represents a named command line argument.
 */
final class CommandLineOption {

	public final char shortName;
	public final boolean hasArgument;
	public final String longName;
	public final boolean hasLongOptionalArgument;
	public final String argumentName;
	public final String description;

	/**
	 * An argument pair is a an option with an associated argument.
	 */
	static final class ArgumentPair {

		public final CommandLineOption option;
		public final String argument;

		/**
		 * Creates a new option / argument pair.
		 *
		 * @param option the option of the pair
		 * @param argument the argument of the pair
		 */
		public ArgumentPair(final CommandLineOption option, final String argument) {
			this.option = option;
			this.argument = argument;
		}

	}

	/**
	 * An option set is a group of options.
	 */
	static final class OptionSet implements Iterable<CommandLineOption> {

		private final CommandLineOption[] allOptions;
		private final Map<Character, CommandLineOption> shortOptions;
		private final Map<String, CommandLineOption> longOptions;

		/**
		 * Creates a set of options.
		 *
		 * @param options the set of options
		 */
		public OptionSet(final CommandLineOption... options) {
			final HashMap<Character, CommandLineOption> shortMap = new HashMap<>();
			final HashMap<String, CommandLineOption> longMap = new HashMap<>();

			for (final CommandLineOption option : options) {
				if (option.shortName != 0 && shortMap.put(option.shortName, option) != null) {
					throw new IllegalArgumentException("Duplicate option detected: -" + option.shortName);
				}

				if (option.longName != null && longMap.put(option.longName, option) != null) {
					throw new IllegalArgumentException("Duplicate option detected: --" + option.longName);
				}
			}

			this.allOptions = options;
			this.shortOptions = Collections.unmodifiableMap(shortMap);
			this.longOptions = Collections.unmodifiableMap(longMap);
		}

		@Override
		public Iterator<CommandLineOption> iterator() {
			return new Iterator<CommandLineOption>() {
				int index = 0;

				@Override
				public boolean hasNext() {
					return index < allOptions.length;
				}

				@Override
				public CommandLineOption next() {
					if (index >= allOptions.length) {
						throw new NoSuchElementException();
					}

					return allOptions[index++];
				}
			};
		}

		/**
		 * Parses the specified arguments and associates any relevant options with the arguments. The returned iterator iterates through the option and argument pairs.
		 *
		 * @param args the arguments to parse
		 * @return an iterator over the option and argument pairs
		 */
		public Iterator<ArgumentPair> parse(final String... args) {
			return new Iterator<ArgumentPair>() {
				private boolean doneParsingOptions = false;
				private int index = 0;
				private int shortOptionIndex = 0;

				@Override
				public boolean hasNext() {
					return index < args.length;
				}

				@Override
				public ArgumentPair next() {
					if (index >= args.length) {
						throw new NoSuchElementException();
					} else if (doneParsingOptions || "--".equals(args[index])) { // End of options ("--")
						doneParsingOptions = true;
						return new ArgumentPair(null, args[index++]);
					} else if (args[index].startsWith("--")) { // Long option
						return parseLongOption();
					} else if (shortOptionIndex > 0 || (args[index].startsWith("-") && args[index].length() > 1)) { // Short option
						return parseShortOption();
					}

					return new ArgumentPair(null, args[index++]);
				}

				/**
				 * Parses the next long option.
				 *
				 * @return the next option argument pair
				 */
				private ArgumentPair parseLongOption() {
					final String arg = args[index++];

					final int separator = arg.indexOf('=', 2);
					final String longName = arg.substring(2, separator >= 0 ? separator : arg.length());
					final CommandLineOption option = longOptions.get(longName);

					if (option == null) { // Option not found
						throw new IllegalArgumentException("Option --" + longName + " does not exist");
					} else if ((option.hasArgument || option.hasLongOptionalArgument) && separator >= 0) { // "--option=<argument>"
						return new ArgumentPair(option, arg.substring(separator + 1));
					} else if (!option.hasArgument) { // "--option"
						if (separator >= 0) {
							throw new IllegalArgumentException("Option " + option + " has unexpected argument: " + arg.substring(separator + 1));
						} else {
							return new ArgumentPair(option, null);
						}
					} else if (index < args.length) { // "--option argument"
						return new ArgumentPair(option, args[index++]);
					}

					throw new IllegalArgumentException("Option " + option + " expecting argument " + option.argumentName);
				}

				/**
				 * Parses the next short option.
				 *
				 * @return the next option argument pair
				 */
				private ArgumentPair parseShortOption() {
					final String arg = args[index];
					final char shortName = arg.charAt(++shortOptionIndex);
					final CommandLineOption option = shortOptions.get(shortName);

					if (shortOptionIndex + 1 >= arg.length()) {
						shortOptionIndex = 0;
						index++;
					}

					if (option == null) { // Option not found
						throw new IllegalArgumentException("Option -" + shortName + " does not exist");
					} else if (!option.hasArgument) { // "-o"
						return new ArgumentPair(option, null);
					} else if (shortOptionIndex > 0) { // "-o<argument>"
						final int i = shortOptionIndex + 1;

						shortOptionIndex = 0;
						index++;
						return new ArgumentPair(option, arg.substring(i));
					} else if (index < args.length) { // "-o <argument>"
						return new ArgumentPair(option, args[index++]);
					}

					throw new IllegalArgumentException("Option -" + shortName + " expecting argument " + option.argumentName);
				}
			};
		}

		/**
		 * Prints the set of options to the print stream. The options are formatted to match common application usage / help layouts.
		 *
		 * @param stream the stream used to print the options
		 */
		public void print(final PrintStream stream) {
			final String indent = new String(new char[2]).replace('\0', ' ');
			final String descriptionIndent = new String(new char[6]).replace('\0', ' ');
			final int maxDescriptionLength = 80 - descriptionIndent.length();
			final Pattern linePattern = Pattern.compile("\\s*(?<line>\\S.{1," + (maxDescriptionLength - 1) + "}(?=\\s|$)|\\S{" + maxDescriptionLength + "})\\s*", Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);

			for (final CommandLineOption option : allOptions) {
				stream.append(indent);
				option.toOptionString(stream);
				stream.println();

				// Print out the description in as many lines as needed
				String lineIndent = descriptionIndent;

				for (final Matcher lineMatcher = linePattern.matcher(option.description); lineMatcher.find(); ) {
					stream.append(lineIndent).println(lineMatcher.group("line"));
					lineIndent = descriptionIndent + indent;
				}

				stream.println();
			}
		}

	}

	/**
	 * Creates a new command line option.
	 *
	 * @param shortName the single character name of the option
	 * @param hasArgument true to indicate the option has a required argument
	 * @param longName the long name of the option
	 * @param hasLongOptionalArgument true to indicate the option has an optional argument (only available to long options without a required argument)
	 * @param argumentName the name of the argument
	 * @param description a description of the option
	 */
	private CommandLineOption(final char shortName, final boolean hasArgument, final String longName, final boolean hasLongOptionalArgument, final String argumentName, final String description) {
		this.shortName = shortName;
		this.hasArgument = hasArgument;
		this.longName = longName;
		this.hasLongOptionalArgument = hasLongOptionalArgument;
		this.argumentName = argumentName;
		this.description = description;

		assert Character.isAlphabetic(shortName) || Character.isDigit(shortName) || longName != null : "Option must have a name";
		assert description != null : "Option " + this + " must have a description";
		assert (hasArgument || hasLongOptionalArgument) == (argumentName != null) : "Option " + this + " has conflicting argument name " + argumentName;
		assert !hasArgument || !hasLongOptionalArgument : "Option " + this + " cannot have an argument that is required and optional";
	}

	/**
	 * Creates a new short option.
	 *
	 * @param shortName the single character name of the option
	 * @param description a description of the option
	 * @return the new option
	 */
	public static CommandLineOption ofName(final char shortName, final String description) {
		return ofName(shortName, null, description);
	}

	/**
	 * Creates a new short and long option.
	 *
	 * @param shortName the single character name of the option
	 * @param longName the long name of the option
	 * @param description a description of the option
	 * @return the new option
	 */
	public static CommandLineOption ofName(final char shortName, final String longName, final String description) {
		return new CommandLineOption(shortName, false, longName, false, null, description);
	}

	/**
	 * Creates a new long option.
	 *
	 * @param longName the long name of the option
	 * @param description a description of the option
	 * @return the new option
	 */
	public static CommandLineOption ofName(final String longName, final String description) {
		return ofName((char)0, longName, description);
	}

	/**
	 * Creates a new short option with a required argument.
	 *
	 * @param shortName the single character name of the option
	 * @param argumentName the name of the argument
	 * @param description a description of the option
	 * @return the new option
	 */
	public static CommandLineOption ofNameWithArgument(final char shortName, final String argumentName, final String description) {
		return ofNameWithArgument(shortName, null, argumentName, description);
	}

	/**
	 * Creates a new short and long option with a required argument.
	 *
	 * @param shortName the single character name of the option
	 * @param longName the long name of the option
	 * @param argumentName the name of the argument
	 * @param description a description of the option
	 * @return the new option
	 */
	public static CommandLineOption ofNameWithArgument(final char shortName, final String longName, final String argumentName, final String description) {
		return new CommandLineOption(shortName, true, longName, false, argumentName, description);
	}

	/**
	 * Creates a new long option with a required argument.
	 *
	 * @param longName the long name of the option
	 * @param argumentName the name of the argument
	 * @param description a description of the option
	 * @return the new option
	 */
	public static CommandLineOption ofNameWithArgument(final String longName, final String argumentName, final String description) {
		return ofNameWithArgument((char)0, longName, argumentName, description);
	}

	/**
	 * Creates a new long option with an optional argument.
	 *
	 * @param longName the long name of the option
	 * @param argumentName the name of the argument
	 * @param description a description of the option
	 * @return the new option
	 */
	public static CommandLineOption ofNameWithOptionalArgument(final String longName, final String argumentName, final String description) {
		return new CommandLineOption((char)0, false, longName, true, argumentName, description);
	}

	/**
	 * Prints the option to the print stream. The option is printed using the following format: "-o, --option=&lt;argument&gt;".
	 *
	 * @param stream the stream used to print the option
	 * @return the original stream parameter
	 */
	public PrintStream toOptionString(final PrintStream stream) {
		if (shortName != 0) {
			stream.append('-').append(shortName);

			if (longName != null) {
				stream.append(", --").append(longName);

				if (hasArgument) {
					stream.append("=<").append(argumentName).append('>');
				}
			} else if (hasArgument) {
				stream.append(" <").append(argumentName).append('>');
			}
		} else if (longName != null) {
			stream.append("--").append(longName);

			if (hasArgument) {
				stream.append("=<").append(argumentName).append('>');
			} else if (hasLongOptionalArgument) {
				stream.append("[=<").append(argumentName).append(">]");
			}

		}

		return stream;
	}

	@Override
	public String toString() {
		return longName == null ? "-" + shortName : "--" + longName;
	}

}
