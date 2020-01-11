package horseshoe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.util.Random;
import java.util.regex.Pattern;

import org.junit.Test;

import horseshoe.Loader;

public class LoaderTests {

	private static final Random rand = new Random();

	private static class StringPartReader extends Reader {

		public static final String DELIMITER = System.lineSeparator();
		public static final char TO_LOAD[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzrrrrssstttlllnnnaaaaeeeeeeeeoooiiuu    .?!".toCharArray();

		public boolean isClosed = false;
		public int toRead = DELIMITER.length();
		public String lastRead = null;

		@Override
		public void close() throws IOException {
			isClosed = true;
		}

		@Override
		public int read(final char[] cbuf, final int off, final int len) {
			if (isClosed) {
				return -1;
			}

			final StringBuilder sb = new StringBuilder();
			final int toReadLength = Math.min(toRead + DELIMITER.length(), len);

			for (int i = DELIMITER.length(); i < toReadLength; i++) {
				sb.append(TO_LOAD[rand.nextInt(TO_LOAD.length)]);
			}

			lastRead = sb.toString();
			sb.append(DELIMITER);
			sb.getChars(0, toReadLength, cbuf, off);

			return toReadLength;
		}

	}

	@Test
	public void readerTest() throws IOException {
		final StringPartReader reader = new StringPartReader();
		final Pattern newLine = Pattern.compile(Pattern.quote(StringPartReader.DELIMITER));

		try (final Loader loader = new Loader("ReaderTest", reader)) {
			reader.toRead = 8;

			for (int i = 0; i < 80_000; i++) {
				final String read = loader.next(newLine, null).toString();
				assertEquals(reader.lastRead, read);
			}

			for (int i = 0; i < 8_000; i++) {
				reader.toRead = (1 << rand.nextInt(16)) + rand.nextInt(8 * 1024);
				final String read = loader.next(newLine, null).toString();
				assertEquals(reader.lastRead, read);
			}
		}

		assertEquals(true, reader.isClosed);
	}

}
