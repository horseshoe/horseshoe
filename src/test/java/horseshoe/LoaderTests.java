package horseshoe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import horseshoe.internal.ParsedLine;

import org.junit.Test;

public class LoaderTests {

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
	public void readerTest() throws IOException {
		final StringPartReader reader = new StringPartReader();

		try (final Loader loader = new Loader("ReaderTest", reader)) {
			loader.setDelimiter(Pattern.compile(Pattern.quote(StringPartReader.DELIMITER)));
			reader.toRead = 8;

			for (int i = 0; i < 8_000; i++) {
				final String read = loader.next().toString();
				assertEquals(reader.lastRead, read);
			}

			for (int i = 0; i < 800; i++) {
				reader.toRead = (1 << RAND.nextInt(16)) + RAND.nextInt(8 * 1024);
				final String read = loader.next().toString();
				assertEquals(reader.lastRead, read);
			}

			// Test checkNext()
			reader.toRead = 1;
			assertFalse(loader.checkNext("This is a code coverage test line!"));
			loader.next();
			reader.close();
			assertFalse(loader.checkNext("This is a code coverage test line!"));

			assertNotNull(loader.toString());
		}

		assertEquals(true, reader.isClosed);
	}

	@Test
	public void testIgnoreCloseException() throws IOException {
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
			assertEquals(0, loader.next().length());
		}
	}

	@Test
	public void testOverlappedLines() throws IOException {
		try (final Loader loader = new Loader("OverlappedLinesTest", "a" + LS + "a" + LS)) {
			final List<ParsedLine> lines = new ArrayList<>();

			assertEquals("a", loader.setDelimiter(Pattern.compile(Pattern.quote(LS + "a" + LS))).next(lines));
			assertEquals(1, lines.size());
		}
	}

}
