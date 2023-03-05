package horseshoe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

class LoaderTests {

	private static final String LS = System.lineSeparator();
	private static final Random RAND = new Random(0);

	private static class StringPartReader extends Reader {

		public static final String DELIMITER = LS;
		public static final char[] TO_LOAD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzrrrrssstttlllnnnaaaaeeeeeeeeoooiiuu    .?!".toCharArray();

		public boolean isClosed = false;
		public int toRead = DELIMITER.length();
		public String lastRead = null;
		private int remainingChars = Integer.MIN_VALUE;

		@Override
		public void close() throws IOException {
			isClosed = true;
		}

		@Override
		public int read(final char[] cbuf, final int off, final int len) {
			if (isClosed) {
				return -1;
			}

			if (remainingChars <= -DELIMITER.length()) { // Reset, based on toRead
				remainingChars = toRead;
				lastRead = "";
			}

			// Fill the buffer with data
			final StringBuilder sb = new StringBuilder();
			final int toReadLength = Math.min(remainingChars, len);

			for (int i = 0; i < toReadLength; i++) {
				sb.append(TO_LOAD[RAND.nextInt(TO_LOAD.length)]);
			}

			// Update the remaining number of characters
			lastRead += sb.toString();
			remainingChars -= toReadLength;

			if (remainingChars <= 0) {
				final String partialDelimiter = DELIMITER.substring(-remainingChars, Math.min(len - toReadLength, DELIMITER.length()));

				sb.append(partialDelimiter);
				remainingChars -= partialDelimiter.length();
			}

			sb.getChars(0, sb.length(), cbuf, off);
			return sb.length();
		}

	}

	@Test
	void readerTest() throws IOException {
		final StringPartReader reader = new StringPartReader();

		try (final Loader loader = new Loader("ReaderTest", reader)) {
			reader.toRead = 8;

			for (int i = 0; i < 64; i++) {
				final String read = loader.next(StringPartReader.DELIMITER);
				assertEquals(reader.lastRead, read);
			}

			for (int i = 0; i < 16; i++) {
				reader.toRead = (1 << RAND.nextInt(16)) + RAND.nextInt(8 * 1024);
				final String read = loader.next(StringPartReader.DELIMITER);
				assertEquals(reader.lastRead, read);
			}

			// Test checkNext()
			reader.toRead = 1;
			assertFalse(loader.checkNext("This is a code coverage test line!"));
			loader.next(StringPartReader.DELIMITER);
			reader.close();
			assertFalse(loader.checkNext("This is a code coverage test line!"));

			assertNotNull(loader.toString());
		}

		assertEquals(true, reader.isClosed);
	}

	@Test
	void testBufferToString() throws IOException {
		assertEquals("", new Buffer(10).toString());
	}

	@Test
	void testIgnoreCloseException() throws IOException {
		try (final Loader loader = new Loader("IgnoreCloseExceptionTest", new Reader() {
			@Override
			public int read(char[] cbuf, int off, int len) throws IOException {
				return -1;
			}

			@Override
			public void close() throws IOException {
				throw new IOException("Simulated IOException");
			}
		})) {
			assertEquals(0, loader.next("").length());
			assertEquals(0, loader.next("?").length());
		}
	}

	@Test
	void testNewLineDelimiters() throws IOException {
		try (final Loader loader = new Loader("NewLineDelimitersTest", "a\r\n\r\n\ra")) {
			final ArrayList<ParsedLine> lines = loader.nextLines("\n\r\n\r");

			assertEquals(1, lines.size());
			assertEquals("a", lines.get(0).getLine());
			assertEquals("\r", lines.get(0).getEnding());

			assertEquals("", loader.next("a"));
			assertTrue(loader.hasNext());
			assertEquals(4, loader.getLine());
			assertEquals(0, loader.getIndex());

			assertEquals("", loader.next("a"));
			assertFalse(loader.hasNext());
			assertEquals(4, loader.getLine());
			assertEquals(1, loader.getIndex());
		}

		try (final Loader loader = new Loader("NewLineDelimitersTest", new StringReader("a\r\n\r\n\r\na"))) {
			final ArrayList<ParsedLine> lines = loader.nextLines("\r\n\r\n\r"); // It can be problematic if a delimiter ends with '\r' (incorrect line & column numbers), so we just assume it will never happen, but test it here just for fun.

			assertEquals(1, lines.size());
			assertEquals("a", lines.get(0).getLine());
			assertEquals("", lines.get(0).getEnding());

			assertEquals("\n", loader.next("a"));
			assertTrue(loader.hasNext());
			assertEquals(4, loader.getLine());
			assertEquals(0, loader.getIndex());

			assertEquals("", loader.next("a"));
			assertFalse(loader.hasNext());
			assertEquals(5, loader.getLine()); // Should be line 4, but the delimiter messes with the lines
			assertEquals(1, loader.getIndex());
		}
	}

	@Test
	void testOverlappedLines() throws IOException {
		try (final Loader loader = new Loader("OverlappedLinesTest", "a" + LS + "a" + LS)) {
			assertEquals("a", loader.next(LS + "a" + LS));
		}
	}

}
