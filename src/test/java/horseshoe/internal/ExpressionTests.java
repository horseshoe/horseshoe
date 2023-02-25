package horseshoe.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import horseshoe.Extension;
import horseshoe.HaltRenderingException;
import horseshoe.Helper;
import horseshoe.RenderContext;
import horseshoe.Settings;
import horseshoe.Settings.ContextAccess;
import horseshoe.Stack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExpressionTests {

	private static final Map<String, Expression> EMPTY_EXPRESSIONS_MAP = Collections.<String, Expression>emptyMap();

	/**
	 * Creates a new expression.
	 *
	 * @param expression the trimmed, advanced expression string
	 * @param namedExpressions the map used to lookup named expressions
	 * @param extensions the extensions used to create the expression
	 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading the expression
	 */
	public static Expression createExpression(final String expression, final Map<String, Expression> namedExpressions, final StackTraceElement[] stackTrace, final int topOfStack, final EnumSet<Extension> extensions) throws ReflectiveOperationException {
		final String location = stackTrace.length >= topOfStack ? stackTrace[topOfStack].getFileName() + ":" + stackTrace[topOfStack].getLineNumber() : "[Unknown]";
		final ExpressionParseState parseState = new ExpressionParseState(0, expression, extensions, false, namedExpressions, new HashMap<>(), new Stack<>());

		return Expression.create(location, parseState);
	}

	/**
	 * Creates a new expression.
	 *
	 * @param expression the trimmed, advanced expression string
	 * @param namedExpressions the map used to lookup named expressions
	 * @throws ReflectiveOperationException if an error occurs while dynamically creating and loading the expression
	 */
	public static Expression createExpression(final String expression, final Map<String, Expression> namedExpressions) throws ReflectiveOperationException {
		return createExpression(expression, namedExpressions, new Throwable().getStackTrace(), 1, EnumSet.allOf(Extension.class));
	}

	/**
	 * Evaluates an {@code Expression}, throwing an exception if one is thrown by the evaluated {@code Expression}.
	 *
	 * @param expression the {@code Expression}
	 * @param context the {@Code Map} global data
	 * @return the result of the {@code Expression}, if no exception is thrown
	 * @throws Throwable an exception thrown by the evaluated {@code Expression}
	 */
	public static Object evaluateExpression(final Expression expression, final Map<String, Object> context) throws Throwable {
		class ThrowableLogger extends horseshoe.Logger {
			private Throwable lastError;

			@Override
			public void log(final Level level, final Throwable error, final String message, final Object... params) {
				if (error != null) {
					lastError = error;
				}
			}
		}

		final ThrowableLogger logger = new ThrowableLogger();
		final Settings settings = new Settings().setContextAccess(ContextAccess.CURRENT).setLogger(logger);
		final Object result = expression.evaluate(new RenderContext(settings, context));

		if (result == null) {
			throw logger.lastError;
		}

		return result;
	}

	@Test
	void testArrayLookup() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("a", new int[] { 5 }, "b", new short[] { 4 }, "c", new char[] { '\n' }, "d", new byte[] { 2 }, "e", new long[] { 1 }, "f", new float[] { 0 }, "g", new double[] { -1 }, "h", new boolean[] { true }, "i", new Object[] { "Blue" });
		assertEquals("5, 4, \n, 2, 1, 0.0, -1.0, true, Blue", createExpression("./a[0] + \", \" + b[0] + \", \" + c[0] + \", \" + d[0] + \", \" + e[0] + \", \" + f[0] + \", \" + g[0] + \", \" + h[0] + \", \" + i[0]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	void testArrayLookupRange() throws ReflectiveOperationException {
		assertArrayEquals(new int[] { 5, 4 }, (int[])createExpression("./a[0:2]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("a", new int[] { 5, 4 }))));
		assertArrayEquals(new short[] { 4 }, (short[])createExpression("b[0:1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("b", new short[] { 4, 3 }))));
		assertArrayEquals(new char[] { '\n' }, (char[])createExpression("c[0:1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("c", new char[] { '\n', 'o' }))));
		assertArrayEquals(new byte[] { 2 }, (byte[])createExpression("d[0:1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("d", new byte[] { 2, 1 }))));
		assertArrayEquals(new long[] { 1 }, (long[])createExpression("e[0:1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("e", new long[] { 1, 0 }))));
		assertArrayEquals(new float[] { 0.0f }, (float[])createExpression("f[0:1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("f", new float[] { 0, -1 }))), 0.0001f);
		assertArrayEquals(new double[] { -1.0 }, (double[])createExpression("g[0:1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("g", new double[] { -1, -2 }))), 0.0001);
		assertArrayEquals(new boolean[] { true }, (boolean[])createExpression("h[0:1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("h", new boolean[] { true, false }))));
		assertArrayEquals(new Object[] { "Blue" }, (Object[])createExpression("i[0:1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Helper.loadMap("i", new Object[] { "Blue", "Red" }))));
	}

	@Test
	void testBadRegularExpression() throws ReflectiveOperationException {
		assertThrows(PatternSyntaxException.class, () -> createExpression("~/^abc(.\\u0065/.matcher('abcde').matches()", EMPTY_EXPRESSIONS_MAP));
	}

	@ParameterizedTest
	@ValueSource(strings = { "[5)", "[5", "a, b[5", "[,,]", ",,", "5[5]", "a[]", "a[,]", "a[b,,]", "a =", "a ; = 2", "a../", "../", "", "()", "(,)", "(a,,)", "#", "a #", "a b", "a 1", "1 b", "\"blah\" a", "\"blah\" 3.5", "blah.3.5", "..\\3.5", "a += 5", "./a = 5", "./a++", "a--", "a = 0; a-- = 1", "a(,)", "a +", "call(/..)", "call(/.a)", "\"bad\\\"", "\"\\q\"", "\"\\u000\"", "\"\\U0000000\"", "true ? false" })
	void testBadSyntax(final String expression) throws ReflectiveOperationException {
		assertThrows(IllegalStateException.class, () -> createExpression(expression, EMPTY_EXPRESSIONS_MAP));
	}

	@Test
	void testCaching() throws ReflectiveOperationException {
		final Expression expression = createExpression("// Ignore this comment\n(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + /* Ignore multi-line comment with code\n '; ' + */ (5 + 8.3 == 5.31 + 8) /* Trailing double comment */ // Second comment", EMPTY_EXPRESSIONS_MAP);
		final Expression sameExpressionWithoutComments = createExpression("(\"a\"+\"bc\"==\"ab\"+\"c\")+\", \"+(5+8.3==5.31+8)", EMPTY_EXPRESSIONS_MAP);
		assertEquals(expression.getEvaluable(), sameExpressionWithoutComments.getEvaluable());
	}

	@Test
	void testCallParsing() throws ReflectiveOperationException {
		assertEquals("@File", createExpression("@File('Test')", EMPTY_EXPRESSIONS_MAP).getCallName());
		assertThrows(IllegalStateException.class, () -> createExpression("@File('Test')", EMPTY_EXPRESSIONS_MAP, new Throwable().getStackTrace(), 0, EnumSet.complementOf(EnumSet.of(Extension.ANNOTATIONS))));
	}

	@Test
	void testComments() throws ReflectiveOperationException {
		assertEquals("true, false", createExpression("// Ignore this comment\n(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + /* Ignore multi-line comment with code\n '; ' + */ (5 + 8.3 == 5.31 + 8) /* Trailing double comment */ // Second comment", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testCompareOperators() throws ReflectiveOperationException {
		assertEquals("true, false, true, false, true, false, true", createExpression("(\"a\" + \"bc\" == \"ab\" + \"c\") + \", \" + (5 + 8.3 == 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 == 0xFFFF) + \", \" + (\"A\" == \"B\") + \", \" + (null == null) + \", \" + (null == 5) + ', ' + (~@'java.time.DayOfWeek'.SUNDAY == ~@'java.time.DayOfWeek'.SUNDAY.getDeclaringClass().SUNDAY)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT).addLoadableClasses(java.time.DayOfWeek.class), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false, true, false, true, false, true, false", createExpression("(\"a\" + \"bc\" != \"ab\" + \"c\") + \", \" + (5 + 8.3 != 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 != 0xFFFF) + \", \" + (\"A\" != \"B\") + \", \" + (null != null) + \", \" + (null != 5) + ', ' + (~@'java.time.DayOfWeek'.SUNDAY != ~@'java.time.DayOfWeek'.SUNDAY)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT).addLoadableClasses(java.time.DayOfWeek.class), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true, true, true, true", createExpression("(\"a\" + \"bc\" <= \"ab\" + \"c\") + \", \" + (5 + 8.3 <= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <= 0xFFFF) + \", \" + (\"A\" <= \"B\")", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true, false, true, false", createExpression("(\"a\" + \"bc\" >= \"ab\" + \"c\") + \", \" + (5 + 8.3 >= 5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >= 0xFFFF) + \", \" + (\"A\" >= \"B\")", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false, true, false, true", createExpression("(\"a\" + \"bc\" <  \"ab\" + \"c\") + \", \" + (5 + 8.3 <  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 <  0xFFFF) + \", \" + (\"A\" <  \"B\")", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false, false, false, false", createExpression("(\"a\" + \"bc\" >  \"ab\" + \"c\") + \", \" + (5 + 8.3 >  5.31 + 8) + \", \" + (0xFFFFFFFFFFFF - 0xFFFFFFFF0000 >  0xFFFF) + \", \" + (\"A\" >  \"B\")", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", createExpression("(null?.toString() == true?.toString())", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", createExpression("\"a\" + \"b\" == \"ab\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals(0, ((Number)createExpression("\"a\" + \"b\" <=> \"ab\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext())).intValue());
		assertTrue(0 > (Integer)createExpression("null <=> \"ab\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext()));
		assertTrue(0 < (Integer)createExpression("\"a\" + \"b\" <=> null", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext()));
	}

	@Test
	void testDie() throws ReflectiveOperationException {
		final Expression expression = createExpression("☠ \"Should die with error\"; \"Did not die\"", EMPTY_EXPRESSIONS_MAP);
		final RenderContext context = new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap());
		assertThrows(HaltRenderingException.class, () -> expression.evaluate(context));
	}

	@Test
	void testDie2() throws ReflectiveOperationException {
		final Expression expression = createExpression("~:< 'Should die with error'; 'Did not die'", EMPTY_EXPRESSIONS_MAP);
		final RenderContext context = new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap());
		assertThrows(HaltRenderingException.class, () -> expression.evaluate(context));
	}

	@Test
	void testEquals() throws ReflectiveOperationException {
		assertNotEquals("", createExpression("a", EMPTY_EXPRESSIONS_MAP));
		assertEquals(createExpression("a", EMPTY_EXPRESSIONS_MAP), createExpression("a", EMPTY_EXPRESSIONS_MAP));
		assertNotEquals(createExpression("a", EMPTY_EXPRESSIONS_MAP), createExpression("b", EMPTY_EXPRESSIONS_MAP));

		// These are different because they occur on different lines
		final Expression first = createExpression("a", EMPTY_EXPRESSIONS_MAP);
		assertNotEquals(first, createExpression("a", EMPTY_EXPRESSIONS_MAP));
	}

	@Test
	void testIdentifier() {
		assertNotEquals(new Identifier("blah", 1), new Object());
		assertNotEquals(new Identifier("blah", 2), new Identifier("blah", 1));
	}

	@Test
	void testIn() throws ReflectiveOperationException {
		assertTrue((Boolean)createExpression("'Test' in ['Test', 'Retest']", EMPTY_EXPRESSIONS_MAP).evaluate());
		assertFalse((Boolean)createExpression("'test' in ['Test', 'Retest']", EMPTY_EXPRESSIONS_MAP).evaluate());
		assertTrue((Boolean)createExpression("'Test' in (['Test', 'Retest'] #? x -> x != null)", EMPTY_EXPRESSIONS_MAP).evaluate());
	}

	@Test
	void testIntegralOperators() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("r", 10, "i2", 2, "bigNum", 9999999999L);
		assertEquals((((10 | 2) ^ 2) >> 2) << 10, ((Number)createExpression("(((+r | i2) ^ 2) >> 2) << r", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals((9999999999L >>> (9999999999L & 10)) + -10 + (9999999999L >>> 10), ((Number)createExpression("(bigNum >>> (bigNum & r)) + -r + (bigNum >> r)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals(~(0 - +2) - -3, ((Number)createExpression("~(0 - +2) - -3", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals("name", createExpression("a = 'prefix:name'; a.substring(a.indexOf(':') + 1)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));
	}

	@Test
	void testLists() throws ReflectiveOperationException {
		assertFalse((Boolean)createExpression("{\"1\", \"2\"}.getClass().isArray()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertNull(createExpression("{\"1\", \"2\"}[1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("2", createExpression("{\"1\", \"2\"}['2']", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("2", createExpression("[\"1\", \"2\",][1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals(Arrays.asList(3, 4), createExpression("[\"1\", 2, 3, 4,][2:]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(Arrays.asList(3, 2, "1"), createExpression("[\"1\", 2, 3, 4,][2:<]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertFalse((Boolean)createExpression("[\"1\", \"2\"].getClass().isArray()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("2", createExpression("[\"1\", \"2\"][1]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("2", createExpression("null?[1] ?? \"2\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("4", createExpression("(1..5)[3]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("7", createExpression("(5..<10)[2]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertArrayEquals(new int[] { 10, 9, 8, 7, 6, 5 }, ((int[])createExpression("10..5", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))));
		assertArrayEquals(new int[] { 10, 9, 8, 7, 6 }, ((int[])createExpression("10..<5", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))));
		assertEquals(1, ((List<?>)createExpression("[10..5]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertNull(createExpression("[10..5][5,]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));

		assertEquals("blah", createExpression("[5, 6.7, \"string-1\", \"blah\"][1 + 2]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("string-1", ((List<?>)createExpression("5, 6.7, \"string-1\", \"blah\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).get(2).toString());
		assertTrue(((List<?>)createExpression("[]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).isEmpty());
		assertEquals(1, ((List<?>)createExpression("5,", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
	}

	@Test
	void testLiterals() throws ReflectiveOperationException {
		assertEquals(-255, createExpression("-0xFF", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(255, createExpression("0xFF", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(-255L, createExpression("-0xFFL", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(255L, createExpression("0xffl", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, createExpression("2d", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, createExpression("2e0", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, createExpression("2f", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, createExpression("2D", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, createExpression("2F", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, createExpression("2.0", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2.0, createExpression("2.e-0", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(0.5, createExpression("0.5", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(-0.5, createExpression("-0.5", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(1.5, createExpression("0x3p-1", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(-9.0, createExpression("-0x1.2p3", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(2_600.452_864, createExpression("2'600.452'864f", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(Double.NEGATIVE_INFINITY, createExpression("-Infinity", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(Double.POSITIVE_INFINITY, createExpression("+Infinity", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(new BigInteger("12345678909876543211234567889"), createExpression("~@BigInteger.new('12345678909876543211234567890').subtract(~@BigInteger.ONE)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext()));
	}

	@Test
	void testLocals() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L);
		assertTrue(createExpression("π = 3.14159265358979311599796346854; r /* radius */ = 4; \"r = \" + r + \", d = \" + (r * 2) + \", a = \" + (π * r * r)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString().startsWith("r = 4, d = 8, a = 50.26"));
		assertEquals(418.87902047863909846168578443727, ((Number)createExpression("r = 4; 4 / 3.0 * π * ./r *./r", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0.00001);
		assertNull(createExpression("b == 0 ? (a = 1) : 4; a", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));

		{
			final int length = 250;
			final ArrayList<Object> result = new ArrayList<>(length);
			final StringBuilder sb = new StringBuilder(length * 7);
			final StringBuilder resultSB = new StringBuilder(length * 5).append("[");

			for (int i = 0; i < length; i++) {
				result.add(i, i);
				sb.append('a').append(i).append('=').append(i).append(';');
				resultSB.append('a').append(i).append(',');
			}

			assertEquals(result, createExpression(sb.append(resultSB).append(']').toString(), EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));
		}
	}

	@Test
	void testLogicalOperators() throws ReflectiveOperationException {
		assertEquals("true", createExpression("(true && 1)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", createExpression("(true && !1)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", createExpression("(null && true)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", createExpression("(!null && (5 > 4))", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", createExpression("(null || (5 > 4))", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", createExpression("((\"four\".length() == 4) || false)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", createExpression("(null || !!null)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", createExpression("(true || 1)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", createExpression("(null?.toString() || true?.toString())", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testMathOperators() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("r", 10, "i2", 2, "π", 3.14159265358979311599796346854, "bigNum", 9999999999L);
		assertEquals(2 * 3.14159265358979311599796346854 * 10 - 2, ((Number)createExpression("(2 * π * r - i2)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0.00001);
		assertEquals(9999999999L + 9999999999L / 2 + 9999999999L % 2 - 3.14159265358979311599796346854, ((Number)createExpression("bigNum + bigNum / 2 + bigNum % 2 - π", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0.00001);
		assertEquals((int)(0x10000000L * 5280), ((Number)createExpression("0x10000000 * 5280", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).intValue());
		assertEquals(0x10000000L * 5280, ((Number)createExpression("0x10000000L * 5280", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).longValue());
		assertEquals(-Math.pow(2, 6), ((Number)createExpression("-i2 ** 6", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context))).doubleValue(), 0);
	}

	@Test
	void testMaps() throws ReflectiveOperationException {
		assertEquals("1", createExpression("{7: \"1\", \"2\"}[7]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("2", createExpression("{\"1\", \"blah\": \"2\"}[\"blah\"]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("1", createExpression("[7: \"1\", \"2\",][7]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("2", createExpression("[\"1\", \"blah\": \"2\"][\"blah\"]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());

		assertEquals(4, ((Map<?, ?>)createExpression("5, 6.7, \"string-1\": 7, \"blah\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertEquals(4, ((Map<?, ?>)createExpression("5, 6.7, \"string-1\": 7, \"blah\",", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertEquals(4, ((Map<?, ?>)createExpression("(5, 6.7, \"string-1\": 7, \"blah\",)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertEquals(1, ((Map<?, ?>)createExpression("7:5,", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertEquals(1, ((Map<?, ?>)createExpression("7:5", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).size());
		assertTrue(((Map<?, ?>)createExpression("[:]", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap()))).isEmpty());
	}

	@Test
	void testNull() throws ReflectiveOperationException {
		assertEquals(null, createExpression("null.toString()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
	}

	@Test
	void testObjectMethods() throws ReflectiveOperationException {
		assertTrue((Boolean) createExpression("'a'.equals('a')", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertFalse((Boolean) createExpression("'a'.equals('b')", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertFalse((Boolean) createExpression("~@Object.new().equals(~@Object.new())", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(Integer.class, createExpression("''.hashCode().getClass()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
	}

	@Test
	void testOperand() throws ReflectiveOperationException {
		assertEquals("", new Operand(Object.class, null).toString());
		assertNotEquals("", new Operand(Object.class, new MethodBuilder().pushConstant(0)).toString());
	}

	@Test
	void testPatternOperators() throws ReflectiveOperationException {
		assertTrue((Boolean)createExpression("'0xdeadBEEFd134' ==~ ~/0x[0-9a-fA-F]+/", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext()));
		assertFalse((Boolean)createExpression("'0x' + 'dead' + 'BEEF' + 'd134' ==~ '[0-9a-fA-F]' + '{2}'", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext()));
		assertFalse((Boolean)createExpression("'deadBEEFd134' =~ ~/0x[0-9a-fA-F]+/", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext()));
		assertTrue((Boolean)createExpression("'dead' + 'BEEF' + 'd134' =~ '[0-9a-fA-F]' + '{2}'", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext()));
	}

	@Test
	void testPlusOperator() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("a", "a", "b", 5, "c", "c", "d", 6.5, "e", Pattern.compile("test_[0-9]+"));
		assertEquals("a11.5", createExpression("a + (b + d)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("10.5c", createExpression("1 + 5.6; +4 + d + c", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("ctest_[0-9]+56.5", createExpression("c + e + b + d", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	void testPlusOperatorIterables() throws ReflectiveOperationException {
		final Settings settings = new Settings().setContextAccess(ContextAccess.CURRENT);
		final Map<String, Object> context = new HashMap<>();
		context.put("arr", new int[] { 1, 2, 3, 4 });
		assertEquals(Arrays.asList(1, 2, 3, 4), createExpression("(arr #? a -> a < 2) + (arr #? a -> a >= 2)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(settings, context)));
		context.put("col", Arrays.asList(2, 3));
		context.put("arr", new int[] { 5, 6 });
		assertEquals(Arrays.asList(2, 3, 5, 6), createExpression("col + (arr #? a != -1)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(settings, context)));
		assertEquals(Arrays.asList(5, 6, 2, 3), createExpression("(arr #? a != -1) + col", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(settings, context)));
		context.put("set", new LinkedHashSet<>(Arrays.asList(8, 2)));
		context.put("arr", new int[] { 1, 6, 7, 8 });
		assertEquals(new LinkedHashSet<>(Arrays.asList(8, 2, 1, 6, 7)), createExpression("set + (arr #? a != -1)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(settings, context)));
		assertEquals(new LinkedHashSet<>(Arrays.asList(1, 6, 7, 8, 2)), createExpression("(arr #? a != -1) + set", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(settings, context)));
	}

	@Test
	void testPlusOperatorMap() throws ReflectiveOperationException {
		final Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(5, 6);
		map.put(7, 8);
		final Map<String, Object> context = new HashMap<>();
		context.put("map", map);
		context.put("arr", new int[] { 1, 2, 3 });
		final Map<Integer, Integer> expected = new LinkedHashMap<>();
		expected.put(1, 1);
		expected.put(2, 2);
		expected.put(3, 3);
		expected.put(5, 6);
		expected.put(7, 8);
		assertEquals(expected, createExpression("(m = [:]; arr #< a -> m = m + [a: a]) + map", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)));
	}

	@Test
	void testQuotedIdentifiers() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("a\\", "a", "`b\\`", 5);
		assertEquals("a", createExpression("`a\\`", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("5", createExpression("```b\\```", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertEquals("a5", createExpression("`a\\` + ```b\\```", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	void testRegularExpression() throws ReflectiveOperationException {
		assertEquals("true, false", createExpression("~/^abc.\\u0065/.matcher('abcde').matches() + ', ' + ~/^abc.\\u0065/.matcher('abcdef').matches()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true, true, true", createExpression("~//.matcher('').matches() + ', ' + ~/\\//.matcher('/').matches() + ', ' + ~/\\\\/.matcher('\\').matches()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("true", createExpression("~/\\d/.matcher(\"\\u0660\").matches()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("false", createExpression("~/(?-U)\\d/.matcher(\"\\u0660\").matches()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testReturn() throws ReflectiveOperationException {
		assertEquals("BlueGreen", createExpression("#^'Blue' + 'Green'; 'Yellow'", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testRootIdentifiers() throws ReflectiveOperationException {
		assertEquals(String.class, createExpression("/getClass()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), "")));
		assertEquals("Test String", createExpression("/. ", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), "Test String")));
	}

	@Test
	void testSafeOperators() throws ReflectiveOperationException {
		assertEquals("good", createExpression("(null?.toString()) ?? \"good\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("good", createExpression("(null.?toString()) ?? \"good\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("good", createExpression("(7.?badMethod()) ?? \"good\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("7", createExpression("(7?.toString()) ?? \"bad\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testSeparatorOperator() throws ReflectiveOperationException {
		assertEquals("blah", createExpression("5; 6.7; \"string-1\"; \"blah\";", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testStreamingOperators() throws ReflectiveOperationException {
		assertEquals("[ a, b, d, e ]", createExpression("['a', 'b', ['c': ['d', 'e']]] #| s -> ~@String.isInstance(s) ? [s] : s['c'] #. s -> s.charAt(0)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testStringConcatenation() throws ReflectiveOperationException {
		final Map<String, Object> context = Helper.loadMap("cb", "bc");
		assertEquals("abcd \\\"\'\b\t\n\f\rƪāĂ\t", createExpression("\"\" + \"a\" + cb + \"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
		assertThrows(NullPointerException.class, () -> evaluateExpression(createExpression("\"a\" + ab", EMPTY_EXPRESSIONS_MAP), context));
		assertThrows(NullPointerException.class, () -> evaluateExpression(createExpression("cb + ab", EMPTY_EXPRESSIONS_MAP), context));
		assertThrows(NullPointerException.class, () -> evaluateExpression(createExpression("ab + cb", EMPTY_EXPRESSIONS_MAP), context));
		assertEquals("54", createExpression("5.toString() + 4.toString()", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), context)).toString());
	}

	@Test
	void testStringInterpolation() throws ReflectiveOperationException {
		assertEquals("$ 1.00, $2.50", createExpression("\"$ 1.00, $2.50\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("ab $cd$ e $", createExpression("blah = 'cd'; \"ab \\$$blah\\$ e $\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertThrows(IllegalStateException.class, () -> createExpression("\"${blah\\}\"", EMPTY_EXPRESSIONS_MAP));
		assertEquals("ab $cd$ e 11", createExpression("blah = 'cd'; \"ab \\$${ blah }\\$ e ${ 5 + 6 }\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("ab $cd$ e 11", createExpression("blah = 'cd'; \"ab \\$${ blah }\\$ e ${ \\\"${5 + 6\\}\\\" }\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("String interpolation can be done using $[identifier] (null) or ${ [expression] } (7), but is ignored for the literal \"$4.50\".", createExpression("\"String interpolation can be done using $[identifier] ($identifier) or \\${ [expression] } (${ sum = 0; \\{ 5, 2 \\} #< t -> sum = sum + t }), but is ignored for the literal \\\"$4.50\\\".\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
	}

	@Test
	void testStringLiterals() throws ReflectiveOperationException {
		assertEquals("d \\\"\'\b\t\n\f\r\0\0πƪāĂ\t", createExpression("\"d \\\\\\\"\\\'\\b\\t\\n\\f\\r\\0\\x0π\\x1Aa\\u0101\\U00000102\\x9\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("d \\\\\\\"\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9", createExpression("'d \\\\\\\"\\b\\t\\n\\f\\r\\x1Aa\\u0101\\U00000102\\x9'", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("a 'single' quoted string", createExpression("'a ''single'' quoted string'", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
	}

	@Test
	void testTernaryOperators() throws ReflectiveOperationException {
		assertEquals("a", createExpression("true ? \"a\" : 5", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("4", createExpression("false ? \"b\" : (1.0; 4)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("isZero", createExpression("0L * 5.6 ? \"notZero\" : \"isZero\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals("12345678901234", createExpression("0 ? 0 : (912345678901235; 12345678901234)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())).toString());
		assertEquals(1, createExpression("pain > 2 ? 'moderate' : #^pain", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 1))));
		assertEquals(Collections.singletonMap(1, 2), createExpression("test == 0 ? null ?: 'zero' : test : 2", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("test", 1))));
		assertEquals(Collections.singletonMap(1, 2), createExpression("1: test == 0 ? 'bad' : (test == 1 ? 2 : test)", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("test", 1))));
		assertEquals("extreme", createExpression("pain > 5 ? pain > 8 ? 'extreme' : 'bad' : pain < 2 ? 'good' : 'moderate'", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 10))).toString());
		assertEquals("bad", createExpression("pain > 5 ? pain > 8 ? 'extreme' : 'bad' : pain < 2 ? 'good' : 'moderate'", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 7))).toString());
		assertEquals("moderate", createExpression("pain > 5 ? pain > 8 ? 'extreme' : 'bad' : pain < 2 ? 'good' : 'moderate'", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 4))).toString());
		assertEquals("low", createExpression("pain > 5 ? pain > 8 ? 'extreme' : 'bad' : pain < 2 ? 'low' : 'moderate'", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.singletonMap("pain", 0))).toString());
	}

	@Test
	void testTernaryRelatedOperators() throws ReflectiveOperationException {
		assertEquals("good", createExpression("null ?? \"good\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(Boolean.FALSE, createExpression("false ?? \"bad\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("good", createExpression("\"good\" ?? \"bad\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("good", createExpression("null ?: \"good\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("good", createExpression("\"good\" ?: \"bad\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("good", createExpression("false ?: \"good\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));

		assertEquals(null, createExpression("null !? \"bad\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("good", createExpression("false !? \"good\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("good", createExpression("\"bad\" !? \"good\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(null, createExpression("null !: \"bad\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals("good", createExpression("\"bad\" !: \"good\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
		assertEquals(null, createExpression("false !: \"bad\"", EMPTY_EXPRESSIONS_MAP).evaluate(new RenderContext(new Settings().setContextAccess(ContextAccess.CURRENT), Collections.<String, Object>emptyMap())));
	}

}
