package horseshoe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

import horseshoe.CommandLineOption.ArgumentPair;
import horseshoe.CommandLineOption.OptionSet;

import org.junit.jupiter.api.Test;

class CommandLineOptionTests {

	@Test
	void testAllOptionTypes() throws UnsupportedEncodingException {
		try (final PrintStream stream = new PrintStream(new OutputStream() {
				@Override
				public void write(final int b) throws IOException {
				}
			}, false, StandardCharsets.UTF_16.name())) {
			final OptionSet options = new OptionSet(
					CommandLineOption.ofName('a', "Test a"),
					CommandLineOption.ofName("b-long", "Test b"),
					CommandLineOption.ofName('c', "c-long", "Test c"),
					CommandLineOption.ofNameWithArgument('d', "d-arg", "Test d"),
					CommandLineOption.ofNameWithArgument("e-long", "e-arg", "Test e"),
					CommandLineOption.ofNameWithArgument('f', "f-long", "f-arg", "Test f"),
					CommandLineOption.ofNameWithOptionalArgument("g-long", "g-arg", "")
			);

			assertDoesNotThrow(() -> options.print(stream));

			for (final CommandLineOption option : options) {
				option.toOptionString(stream);
				stream.println(option.toString());
			}
		}
	}

	@Test
	void testBadLongOption() {
		for (final Iterator<ArgumentPair> it = new OptionSet(CommandLineOption.ofNameWithArgument("a-long", "a-arg", "Test a 2")).parse("--a-long"); it.hasNext(); ) {
			assertThrows(RuntimeException.class, () -> it.next());
		}
	}

	@Test
	void testBadOptions() {
		final CommandLineOption option1 = CommandLineOption.ofName('a', "Test a");
		final CommandLineOption option2 = CommandLineOption.ofName('a', "a-long", "Test a 2");

		assertThrows(RuntimeException.class, () -> new OptionSet(option1, option2));
	}

	@Test
	void testBadOptions2() {
		final CommandLineOption option1 = CommandLineOption.ofName("a-long", "Test a");
		final CommandLineOption option2 = CommandLineOption.ofName('a', "a-long", "Test a 2");

		assertThrows(RuntimeException.class, () -> new OptionSet(option1, option2));
	}

	@Test
	void testBadSet() {
		final Iterator<?> it = new OptionSet().iterator();
		assertThrows(NoSuchElementException.class, () -> it.next());
	}

	@Test
	void testBadSet2() {
		final Iterator<?> it = new OptionSet().iterator();
		assertThrows(RuntimeException.class, () -> it.remove());
	}

	@Test
	void testBadSet3() {
		final Iterator<?> it = new OptionSet().parse();
		assertThrows(NoSuchElementException.class, () -> it.next());
	}

	@Test
	void testBadSet4() {
		final Iterator<?> it = new OptionSet().parse();
		assertThrows(RuntimeException.class, () -> it.remove());
	}

	@Test
	void testBadShortOption() {
		for (final Iterator<ArgumentPair> it = new OptionSet(CommandLineOption.ofNameWithArgument('a', "a-arg", "Test a 2")).parse("-a"); it.hasNext(); ) {
			assertThrows(RuntimeException.class, () -> it.next());
		}
	}

	@Test
	void testLongOption() {
		for (final Iterator<ArgumentPair> it = new OptionSet(CommandLineOption.ofNameWithArgument("a-long", "a-arg", "Test a 2"), CommandLineOption.ofName("b-long", "")).parse("--a-long=56", "--a-long", "56", "--b-long"); it.hasNext(); ) {
			final ArgumentPair pair = it.next();

			if ("a-long".equals(pair.option.longName)) {
				assertEquals("56", pair.argument);
			} else if ("b-long".equals(pair.option.longName)) {
				assertNull(pair.argument);
			} else {
				fail("Invalid option: " + pair.option);
			}
		}
	}

	@Test
	void testLongOptionalArgument() {
		for (final Iterator<ArgumentPair> it = new OptionSet(CommandLineOption.ofNameWithOptionalArgument("a-long", "a-arg", "Test a 2")).parse("--a-long=56"); it.hasNext(); ) {
			final ArgumentPair pair = it.next();

			if ("a-long".equals(pair.option.longName)) {
				assertEquals("56", pair.argument);
			} else {
				fail("Invalid option: " + pair.option);
			}
		}
	}

	@Test
	void testLongOptionalArgument2() {
		for (final Iterator<ArgumentPair> it = new OptionSet(CommandLineOption.ofNameWithOptionalArgument("a-long", "a-arg", "Test a 2")).parse("--a-long", "56"); it.hasNext(); ) {
			final ArgumentPair pair = it.next();

			if (pair.option == null) {
				assertEquals("56", pair.argument);
			} else {
				assertEquals("a-long", pair.option.longName);
				assertNull(pair.argument);
			}
		}
	}

	@Test
	void testShortOption() {
		for (final Iterator<ArgumentPair> it = new OptionSet(CommandLineOption.ofNameWithArgument('a', "a-arg", "Test a 2"), CommandLineOption.ofName('b', "")).parse("-a56", "-a", "56", "-bb", "-ba56"); it.hasNext(); ) {
			final ArgumentPair pair = it.next();

			if (pair.option.shortName == 'a') {
				assertEquals("56", pair.argument);
			} else if (pair.option.shortName == 'b') {
				assertNull(pair.argument);
			} else {
				fail("Invalid option: " + pair.option);
			}
		}
	}

}
