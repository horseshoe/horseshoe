package horseshoe.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Loader implements AutoCloseable {

	private static final Pattern NEW_LINE = Pattern.compile("\\r\\n?|\\n");

	private final String name;
	private final Reader reader;
	private Buffer streamBuffer;
	private CharSequence buffer;
	private int bufferOffset = 0;
	private boolean isFullyLoaded = false;
	private final Matcher matcher;

	private final Matcher newLineMatcher;
	private int line = 1;
	private int column = 1;

	/**
	 * Creates a new loader from a character sequence.
	 *
	 * @param name the name of the loader
	 * @param value the character sequence to load
	 */
	public Loader(final String name, final CharSequence value) {
		this.name = name;
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
	 * @param stream the stream to load
	 * @param charset the character set to use while loading the stream
	 * @throws IOException if the stream cannot be read
	 */
	public Loader(final String name, final InputStream stream, final Charset charset) throws IOException {
		this.name = name;
		this.reader = new InputStreamReader(stream, charset);
		this.streamBuffer = new Buffer(4096);
		this.buffer = this.streamBuffer;

		streamBuffer.setLength(reader.read(streamBuffer.getBuffer()));
		this.matcher = NEW_LINE.matcher(streamBuffer);
		this.newLineMatcher = NEW_LINE.matcher(streamBuffer);
	}

	/**
	 * Creates a new loader from a file.
	 *
	 * @param name the name of the loader
	 * @param file the file to load
	 * @param charset the character set to use while loading the file
	 * @throws IOException if the file cannot be opened or read
	 */
	public Loader(final String name, final Path file, final Charset charset) throws IOException {
		this(name, new FileInputStream(file.toFile()), charset);
	}

	/**
	 * Advances the internal loader pointer the specified length. This should be called after every call to next() if maintaining the line and column count is needed. The length should be the length of the subsequence returned by next().
	 *
	 * @param length the length to advance the internal loader pointer
	 * @param lines a list that will be populated with the lines parsed from the string
	 */
	public void advanceInternalPointer(final int length, final List<ParsedLine> lines) {
		final int endOfBuffer = matcher.hitEnd() ? matcher.regionEnd() : matcher.start();
		int startOfBuffer = endOfBuffer - length;

		for (newLineMatcher.reset(buffer).region(startOfBuffer, bufferOffset); newLineMatcher.find(); line++, column = 1) {
			final int newLineStart = Math.min(newLineMatcher.start(), endOfBuffer);
			final int newLineEnd = Math.min(newLineMatcher.end(), endOfBuffer);

			lines.add(new ParsedLine(buffer.subSequence(startOfBuffer, newLineStart).toString(), buffer.subSequence(newLineStart, newLineEnd).toString()));
			startOfBuffer = newLineMatcher.end();
		}

		if (startOfBuffer <= endOfBuffer) {
			lines.add(new ParsedLine(buffer.subSequence(startOfBuffer, endOfBuffer).toString(), ""));
		}

		column += bufferOffset - startOfBuffer;
	}

	/**
	 * Advances the internal loader pointer the specified length. This should be called after every call to next() if maintaining the line and column count is needed. The length should be the length of the subsequence returned by next().
	 *
	 * @param length the length to advance the internal loader pointer
	 */
	public void advanceInternalPointer(final int length) {
		int startOfBuffer = (matcher.hitEnd() ? matcher.regionEnd() : matcher.start()) - length;

		for (newLineMatcher.reset(buffer).region(startOfBuffer, bufferOffset); newLineMatcher.find(); line++, column = 1) {
			startOfBuffer = newLineMatcher.end();
		}

		column += bufferOffset - startOfBuffer;
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
		return column;
	}

	/**
	 * Gets the current line being loaded. This is only up-to-date if advanceInternalPointer() has been called after every next() call
	 *
	 * @return the current line being loaded
	 */
	public int getLine() {
		return line;
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
	 * Checks if more input is available from the loader.
	 *
	 * @return true if more input is available from the loader, otherwise false
	 */
	public boolean hasNext() {
		return !isFullyLoaded || !matcher.hitEnd();
	}

	/**
	 * Loads more data from the buffer. This does not work for loaders initialized with a string value, as the buffer is already fully loaded.
	 *
	 * @return the number of characters read into the buffer
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	private int loadMoreData() throws IOException {
		if (streamBuffer.length() - bufferOffset >= streamBuffer.capacity() / 4) {
			// Resize buffer
			final Buffer oldBuffer = streamBuffer;
			streamBuffer = new Buffer(oldBuffer.capacity() * 2, oldBuffer.length() - bufferOffset);
			buffer = streamBuffer;

			for (int oldBufferLength = oldBuffer.length(), i = 0, j = bufferOffset; j < oldBufferLength; i++, j++) {
				streamBuffer.getBuffer()[i] = oldBuffer.getBuffer()[j];
			}

			bufferOffset = 0;
			matcher.reset(streamBuffer);
		} else if (bufferOffset >= streamBuffer.capacity() / 8) {
			// Move data to start of buffer
			for (int streamBufferLength = streamBuffer.length(), i = 0, j = bufferOffset; j < streamBufferLength; i++, j++) {
				streamBuffer.getBuffer()[i] = streamBuffer.getBuffer()[j];
			}

			streamBuffer.setLength(streamBuffer.length() - bufferOffset);
			bufferOffset = 0;
			matcher.reset(streamBuffer);
		}

		final int read = reader.read(streamBuffer.getBuffer(), streamBuffer.length(), streamBuffer.capacity() - streamBuffer.length());

		if (read < 0) {
			isFullyLoaded = true;
		} else {
			streamBuffer.setLength(streamBuffer.length() + read);
		}

		return read;
	}

	/**
	 * Gets the next character sequence up to the next matching delimiter or end-of-stream. On construction, the delimiter is initialized as a new line.
	 *
	 * @return the next character sequence up to the next matching delimiter or end-of-stream
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	public CharSequence next() throws IOException {
		while (!matcher.find()) {
			if (isFullyLoaded) { // Check if we've reached the end
				final int start = bufferOffset;
				bufferOffset = buffer.length();
				return buffer.subSequence(start, bufferOffset);
			} else { // If we haven't reached the end, then attempt to pull in more data
				loadMoreData();
			}
		}

		final int start = bufferOffset;
		bufferOffset = matcher.end();
		return buffer.subSequence(start, matcher.start());
	}

	/**
	 * Replaces the delimiter and gets the next character sequence up to the next match or end-of-stream. The delimiter will be used in subsequent calls to next().
	 *
	 * @param delimiter the replacement delimiter used for subsequent calls to next()
	 * @return the next character sequence up to the next matching delimiter or end-of-stream
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	public CharSequence next(final Pattern delimiter) throws IOException {
		matcher.usePattern(delimiter);
		return next();
	}

	/**
	 * Peeks into the buffer to see if the specified character sequence matches the expected value.
	 *
	 * @param expected the expected value
	 * @return true if the expected value matches the upcoming sequence in the buffer
	 * @throws IOException if an error was encountered while trying to read more data into the buffer
	 */
	public boolean peek(final CharSequence expected) throws IOException {
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
	public String toString() {
		return name;
	}

}
