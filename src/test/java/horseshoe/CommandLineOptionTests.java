package horseshoe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import horseshoe.CommandLineOption.ArgumentPair;
import horseshoe.CommandLineOption.OptionSet;

import org.junit.Test;

public class CommandLineOptionTests {

	@Test (expected = Test.None.class) // No exception expected
	public void testAllOptionTypes() throws UnsupportedEncodingException {
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

			options.print(stream);

			for (final CommandLineOption option : options) {
				option.toOptionString(stream);
				stream.println(option.toString());
			}
		}
	}

	@Test (expected = RuntimeException.class)
	public void testBadLongOption() {
		for (final Iterator<ArgumentPair> it = new OptionSet(CommandLineOption.ofNameWithArgument("a-long", "a-arg", "Test a 2")).parse("--a-long"); it.hasNext(); ) {
			it.next();
		}
	}

	@Test (expected = RuntimeException.class)
	public void testBadOptions() {
		new OptionSet(
				CommandLineOption.ofName('a', "Test a"),
				CommandLineOption.ofName('a', "a-long", "Test a 2")
		);
	}

	@Test (expected = RuntimeException.class)
	public void testBadOptions2() {
		new OptionSet(
				CommandLineOption.ofName("a-long", "Test a"),
				CommandLineOption.ofName('a', "a-long", "Test a 2")
		);
	}

	@Test (expected = RuntimeException.class)
	public void testBadSet() {
		new OptionSet().iterator().next();
	}

	@Test (expected = RuntimeException.class)
	public void testBadSet2() {
		new OptionSet().iterator().remove();
	}

	@Test (expected = RuntimeException.class)
	public void testBadSet3() {
		new OptionSet().parse().next();
	}

	@Test (expected = RuntimeException.class)
	public void testBadSet4() {
		new OptionSet().parse().remove();
	}

	@Test (expected = RuntimeException.class)
	public void testBadShortOption() {
		for (final Iterator<ArgumentPair> it = new OptionSet(CommandLineOption.ofNameWithArgument('a', "a-arg", "Test a 2")).parse("-a"); it.hasNext(); ) {
			it.next();
		}
	}

	@Test
	public void testLongOption() {
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
	public void testLongOptionalArgument() {
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
	public void testLongOptionalArgument2() {
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
	public void testShortOption() {
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
