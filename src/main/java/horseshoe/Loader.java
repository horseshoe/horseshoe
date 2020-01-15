package horseshoe;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.Buffer;
import horseshoe.internal.CharSequenceUtils;
import horseshoe.internal.ParsedLine;

/**
 * Loaders are used to parse {@link Template}s. It keeps track of the current state of the internal reader used to load the template text.
 */
public final class Loader implements AutoCloseable {

	private static final Pattern NEW_LINE = Pattern.compile("\\r\\n?|\\n");

	private static class Location {
		public static final int FIRST_COLUMN = 1;

		public final int line;
		public final int column;

		public Location(final int line, final int column) {
			this.line = line;
			this.column = column;
		}

		public Location() {
			this(1, FIRST_COLUMN);
		}
	}

	private final String name;
	private final Path file;
	private final Reader reader;
	private Buffer streamBuffer;
	private CharSequence buffer;
	private int bufferOffset = 0;
	private boolean isFullyLoaded = false;
	private final Matcher matcher;

	private final Matcher newLineMatcher;
	private Location location = new Location();
	private Location nextLocation = location;

	/**
	 * Creates a new loader from a character sequence.
	 *
	 * @param name the name of the loader
	 * @param value the character sequence to load
	 */
	Loader(final String name, final CharSequence value) {
		this.name = name;
		this.file = null;
		this.reader = null;
		this.buffer = value;
		this.isFullyLoaded = true;
		this.matcher = NEW_LINE.matcher(value);
		this.newLineMatcher = NEW_LINE.matcher(value);
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
		this.buffer = this.streamBuffer;
		this.matcher = NEW_LINE.matcher(streamBuffer);
		this.newLineMatcher = NEW_LINE.matcher(streamBuffer);
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
	 * @param name the name of the loader
	 * @param file the file to load
	 * @param charset the character set to use while loading the file
	 * @throws FileNotFoundException if the file does not exist
	 */
	Loader(final String name, final Path file, final Charset charset) throws FileNotFoundException {
		this(name, file, new InputStreamReader(new FileInputStream(file.toFile()), charset));
	}

	/**
	 * Checks if the expected character sequence matches the upcoming sequence in the buffer. Any previous results are invalidated through the use of this function.
	 *
	 * @param expected the expected character sequence
	 * @return true if the expected character sequence matches the upcoming sequence in the buffer
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	boolean checkNext(final CharSequence expected) throws IOException {
		while (expected.length() > buffer.length() - bufferOffset) {
			if (isFullyLoaded) { // Check if we've reached the end
				return false;
			} else { // If we haven't reached the end, then attempt to pull in more data
				loadMoreData();
			}
		}

		return CharSequenceUtils.matches(buffer, bufferOffset, expected);
	}

	@Override
	public void close() {
		if (reader != null) {
			try {
				reader.close();
			} catch (final IOException e) {
				// Assuming use of the reader is finished at this point, so disregard the exception
			}
		}
	}

	/**
	 * Gets the column within the current line being loaded. This is only up-to-date if advanceInternalPointer() has been called after every next() call
	 *
	 * @return the column within the current line being loaded
	 */
	public int getColumn() {
		return location.column;
	}

	/**
	 * Gets the current line being loaded. This is only up-to-date if advanceInternalPointer() has been called after every next() call
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
		return !isFullyLoaded || !matcher.hitEnd();
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
				buffer = streamBuffer = new Buffer(oldBuffer.capacity() * 2);
			}

			// Copy the data to the beginning of the buffer and try to read more data
			System.arraycopy(oldBuffer.getBuffer(), bufferOffset, streamBuffer.getBuffer(), 0, length);
		}

		final int read = reader.read(streamBuffer.getBuffer(), length, streamBuffer.capacity() - length);

		if (read < 0) {
			streamBuffer.setLength(length);
			isFullyLoaded = true;
		} else {
			streamBuffer.setLength(length + read);
		}

		bufferOffset = 0;
		matcher.reset(streamBuffer);
		return read;
	}

	/**
	 * Gets the next character sequence up to the next matching delimiter or end-of-stream. On construction, the delimiter is initialized as a new line. Any previous results are invalidated through the use of this function.
	 *
	 * @param lines the list of lines to be updated as part of the call (may be null)
	 * @return the next character sequence up to the next matching delimiter or end-of-stream
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	CharSequence next(final List<ParsedLine> lines) throws IOException {
		int start = bufferOffset;
		int end;

		while (true) {
			if (matcher.find()) {
				end = matcher.start();
				bufferOffset = matcher.end();
				break;
			} else if (isFullyLoaded) { // Check if we've reached the end
				end = bufferOffset = buffer.length();
				break;
			} else { // If we haven't reached the end, then attempt to pull in more data
				loadMoreData();
				start = bufferOffset;
			}
		}

		// Match the new lines
		int startOfLine = start;
		int line = nextLocation.line;
		int column = nextLocation.column;
		location = nextLocation;
		newLineMatcher.reset(buffer).region(start, bufferOffset);

		if (lines != null) {
			for (; newLineMatcher.find(); line++, column = Location.FIRST_COLUMN) {
				final int newLineStart = Math.min(newLineMatcher.start(), end);
				final int newLineEnd = Math.min(newLineMatcher.end(), end);

				if (startOfLine <= end) {
					lines.add(new ParsedLine(buffer.subSequence(startOfLine, newLineStart).toString(), buffer.subSequence(newLineStart, newLineEnd).toString()));
				}

				startOfLine = newLineMatcher.end();
			}

			if (startOfLine <= end) {
				lines.add(new ParsedLine(buffer.subSequence(startOfLine, end).toString(), ""));
			}
		} else {
			for (; newLineMatcher.find(); line++, column = Location.FIRST_COLUMN) {
				startOfLine = newLineMatcher.end();
			}
		}

		nextLocation = new Location(line, column + bufferOffset - startOfLine);
		return buffer.subSequence(start, end);
	}

	/**
	 * Replaces the delimiter and gets the next character sequence up to the next match or end-of-stream. The delimiter will be used in subsequent calls to next(). Any previous results are invalidated through the use of this function.
	 *
	 * @param delimiter the replacement delimiter used for subsequent calls to next()
	 * @param lines the list of lines to be updated as part of the call (may be null)
	 * @return the next character sequence up to the next matching delimiter or end-of-stream
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	CharSequence next(final Pattern delimiter, final List<ParsedLine> lines) throws IOException {
		matcher.usePattern(delimiter);
		return next(lines);
	}

	/**
	 * Gets the appropriate location string of the form file:line.
	 *
	 * @return the location string
	 */
	public String toLocationString() {
		return (file == null ? name : file.toString()) + ":" + location.line;
	}

	@Override
	public String toString() {
		return name;
	}

}
