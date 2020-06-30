package horseshoe;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import horseshoe.internal.Buffer;
import horseshoe.internal.ParsedLine;
import horseshoe.internal.StringUtils;
import horseshoe.internal.StringUtils.Range;

/**
 * Loaders are used to parse {@link Template}s. It keeps track of the current state of the internal reader used to load the template text.
 */
public final class Loader implements AutoCloseable {

	private final String name;
	private final Path file;
	private final Reader reader;
	private Buffer streamBuffer;
	private final String stringToLoad;
	private int bufferOffset = 0;
	private boolean isFullyLoaded = false;
	private boolean hasNext = true;

	private final Location location = new Location();
	private final Location nextLocation = new Location();

	private static class Location {
		public static final int FIRST_COLUMN = 1;

		private int line;
		private int column;

		public Location(final int line, final int column) {
			this.line = line;
			this.column = column;
		}

		public Location() {
			this(1, FIRST_COLUMN);
		}

		public void set(final Location other) {
			line = other.line;
			column = other.column;
		}
	}

	private static class LineLocation {
		private final Object location;
		private final int line;

		private LineLocation(final Object location, final int line) {
			this.location = location;
			this.line = line;
		}

		@Override
		public String toString() {
			return location + ":" + line;
		}
	}

	/**
	 * Creates a new loader from a string.
	 *
	 * @param name the name of the loader
	 * @param value the string to load
	 */
	Loader(final String name, final String value) {
		this.name = name;
		this.file = null;
		this.reader = null;
		this.stringToLoad = value;
		this.isFullyLoaded = true;
	}

	/**
	 * Creates a new loader from an input stream.
	 *
	 * @param name the name of the loader
	 * @param file the file being loaded
	 * @param reader the reader to load
	 */
	private Loader(final String name, final Path file, final Reader reader) {
		this.name = name;
		this.file = file;
		this.reader = reader;
		this.streamBuffer = new Buffer(4096);
		this.stringToLoad = null;
	}

	/**
	 * Creates a new loader from an input stream.
	 *
	 * @param name the name of the loader
	 * @param reader the reader to load
	 */
	Loader(final String name, final Reader reader) {
		this(name, null, reader);
	}

	/**
	 * Creates a new loader from a file.
	 *
	 * @param file the file to load
	 * @param charset the character set to use while loading the file
	 * @throws FileNotFoundException if the file does not exist
	 */
	Loader(final Path file, final Charset charset) throws FileNotFoundException {
		this(file.toString(), file, new InputStreamReader(new FileInputStream(file.toFile()), charset));
	}

	/**
	 * Checks if the expected string matches the upcoming sequence in the buffer. Any previous results are invalidated through the use of this function.
	 *
	 * @param expected the expected string
	 * @return true if the expected string matches the upcoming sequence in the buffer
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	boolean checkNext(final String expected) throws IOException {
		if (stringToLoad != null) {
			return stringToLoad.regionMatches(bufferOffset, expected, 0, expected.length());
		}

		while (expected.length() > streamBuffer.length() - bufferOffset) {
			if (isFullyLoaded) { // Check if we've reached the end
				return false;
			} else { // If we haven't reached the end, then attempt to pull in more data
				loadMoreData();
			}
		}

		return expected.equals(new String(streamBuffer.getData(), bufferOffset, expected.length()));
	}

	@Override
	public void close() {
		if (reader != null) {
			try {
				reader.close();
			} catch (final IOException e) {
				Template.LOGGER.log(Level.WARNING, "Failed to close reader for template \"" + name + "\"", e);
			}
		}
	}

	/**
	 * Gets the column within the current line being loaded.
	 *
	 * @return the column within the current line being loaded
	 */
	public int getColumn() {
		return location.column;
	}

	/**
	 * Gets the current line being loaded.
	 *
	 * @return the current line being loaded
	 */
	public int getLine() {
		return location.line;
	}

	/**
	 * Gets the name of the load item.
	 *
	 * @return the name of the load item
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the file being loaded.
	 *
	 * @return the file being loaded
	 */
	public Path getFile() {
		return file;
	}

	/**
	 * Checks if more input is available from the loader.
	 *
	 * @return true if more input is available from the loader, otherwise false
	 */
	boolean hasNext() {
		return hasNext;
	}

	/**
	 * Loads more data from the buffer. This does not work for loaders initialized with a string value, as the buffer is already fully loaded.
	 *
	 * @return the number of characters read into the buffer
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	private int loadMoreData() throws IOException {
		final int length = streamBuffer.length() - bufferOffset;

		if (length > 0) {
			final Buffer oldBuffer = streamBuffer;

			// If the current data is using more than 50% of the buffer, then double the size of the buffer
			if (length >= oldBuffer.capacity() / 2) {
				streamBuffer = new Buffer(oldBuffer.capacity() * 2);
			}

			// Copy the data to the beginning of the buffer and try to read more data
			System.arraycopy(oldBuffer.getData(), bufferOffset, streamBuffer.getData(), 0, length);
		}

		final int read = reader.read(streamBuffer.getData(), length, streamBuffer.capacity() - length);

		if (read < 0) {
			streamBuffer.setLength(length);
			isFullyLoaded = true;
		} else {
			streamBuffer.setLength(length + read);
		}

		bufferOffset = 0;
		return read;
	}

	/**
	 * Gets the next string up to the next matching delimiter or end-of-stream.
	 *
	 * @param delimiter the delimiter used to end the next string
	 * @return the next string up to the next matching delimiter or end-of-stream
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	String next(final String delimiter) throws IOException {
		final Range range = nextMatch(delimiter);
		final String value;

		location.set(nextLocation);
		nextLocation.column += bufferOffset - range.start;

		if (stringToLoad != null) {
			for (Range eolRange = StringUtils.findNewLine(stringToLoad, range.start, bufferOffset);
					eolRange != null;
					nextLocation.line++, nextLocation.column = Location.FIRST_COLUMN + bufferOffset - eolRange.end, eolRange = StringUtils.findNewLine(stringToLoad, eolRange.end, bufferOffset));

			value = stringToLoad.substring(range.start, range.end);
		} else {
			for (Range eolRange = StringUtils.findNewLine(streamBuffer.getData(), range.start, bufferOffset);
					eolRange != null;
					nextLocation.line++, nextLocation.column = Location.FIRST_COLUMN + bufferOffset - eolRange.end, eolRange = StringUtils.findNewLine(streamBuffer.getData(), eolRange.end, bufferOffset));

			value = streamBuffer.substring(range.start, range.end);
		}

		return value;
	}

	/**
	 * Gets the next list of lines up to the next matching delimiter or end-of-stream. At least one line will always be present.
	 *
	 * @param delimiter the delimiter used to end the next string
	 * @return the list of lines up to the next matching delimiter or end-of-stream
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	List<ParsedLine> nextLines(final String delimiter) throws IOException {
		final List<ParsedLine> lines = new ArrayList<>();
		final StringUtils.Range range = nextMatch(delimiter);
		int startOfLine = range.start;

		location.set(nextLocation);
		nextLocation.column += bufferOffset - range.start;

		if (stringToLoad != null) {
			for (Range eolRange = StringUtils.findNewLine(stringToLoad, range.start, bufferOffset);
					eolRange != null;
					startOfLine = eolRange.end, nextLocation.line++, nextLocation.column = Location.FIRST_COLUMN + bufferOffset - eolRange.end, eolRange = StringUtils.findNewLine(stringToLoad, eolRange.end, bufferOffset)) {
				if (startOfLine <= range.end) {
					final int newLineStart = Math.min(eolRange.start, range.end);
					final int newLineEnd = Math.min(eolRange.end, range.end);

					lines.add(new ParsedLine(stringToLoad.substring(startOfLine, newLineStart), stringToLoad.substring(newLineStart, newLineEnd)));
				}
			}

			if (startOfLine <= range.end) {
				lines.add(new ParsedLine(stringToLoad.substring(startOfLine, range.end), ""));
			}

			return lines;
		}

		for (Range eolRange = StringUtils.findNewLine(streamBuffer.getData(), range.start, bufferOffset);
				eolRange != null;
				startOfLine = eolRange.end, nextLocation.line++, nextLocation.column = Location.FIRST_COLUMN + bufferOffset - eolRange.end, eolRange = StringUtils.findNewLine(streamBuffer.getData(), eolRange.end, bufferOffset)) {
			if (startOfLine <= range.end) {
				final int newLineStart = Math.min(eolRange.start, range.end);
				final int newLineEnd = Math.min(eolRange.end, range.end);

				lines.add(new ParsedLine(streamBuffer.substring(startOfLine, newLineStart), streamBuffer.substring(newLineStart, newLineEnd)));
			}
		}

		if (startOfLine <= range.end) {
			lines.add(new ParsedLine(streamBuffer.substring(startOfLine, range.end), ""));
		}

		return lines;
	}

	/**
	 * Gets the next range up to the next matching delimiter or end-of-stream. The bufferOffset is updated as part of this search.
	 *
	 * @param delimiter the delimiter used to end the next string
	 * @return the range up to the next matching delimiter or end-of-stream
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	private StringUtils.Range nextMatch(final String delimiter) throws IOException {
		// Find the next match and increment the buffer offset
		if (stringToLoad != null) {
			final int start = bufferOffset;
			final int foundMatch = stringToLoad.indexOf(delimiter, bufferOffset);

			if (foundMatch >= 0) {
				bufferOffset = foundMatch + delimiter.length();
				return new StringUtils.Range(start, foundMatch);
			}

			bufferOffset = stringToLoad.length();
			hasNext = false;
			return new StringUtils.Range(start, bufferOffset);
		}

		int foundMatch;

		for (foundMatch = streamBuffer.indexOf(delimiter, bufferOffset); foundMatch < 0 && !isFullyLoaded; foundMatch = streamBuffer.indexOf(delimiter, foundMatch + bufferOffset)) {
			foundMatch = Math.max(0, streamBuffer.length() - (delimiter.length() - 1) - bufferOffset);
			loadMoreData();
		}

		final int start = bufferOffset;

		if (foundMatch >= 0) {
			bufferOffset = foundMatch + delimiter.length();
			return new StringUtils.Range(start, foundMatch);
		}

		bufferOffset = streamBuffer.length();
		hasNext = false;
		return new StringUtils.Range(start, bufferOffset);
	}

	/**
	 * Gets the current location object being parsed by the loader.
	 *
	 * @return the location
	 */
	public Object toLocation() {
		return new LineLocation(file == null ? name : file, location.line);
	}

	@Override
	public String toString() {
		return name;
	}

}
