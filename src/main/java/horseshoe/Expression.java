package horseshoe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.CharSequenceUtils;

final class Expression {

	private static final ExpressionSegment EMPTY_SEGMENTS[] = new ExpressionSegment[0];

	private static final Pattern GLOBAL_BACKREACH = Pattern.compile("\\s/");
	private static final Pattern SEGMENT_BACKREACH = Pattern.compile("\\s[.][.]\\s/");
	private static final Pattern ONLY_WHITESPACE = Pattern.compile("\\s*");

	// ".", "(", ",", ")", "[", "]", "{", "}", "-", "+", "*", "/", "%", "^", "!", "<", ">", "=", "-=", "+=", "*=", "/=", "%=", "^=", "!=", "<=", ">=", "=="
	private static final Pattern SEGMENT_LOADER_TOKEN = Pattern.compile("[.(,)\\[\\]{}]|[-+*/%^!<>=]=?");

	public static Expression load(final CharSequence value, final int length, final Matcher matcher, final int maxBackreach) throws Exception {
		int backreach = 0;

		if (matcher.lookingAt()) {
			matcher.region(matcher.end(), length);
			backreach = Integer.MAX_VALUE;
		} else {
			// Determine the backreach
			for (matcher.usePattern(SEGMENT_BACKREACH); matcher.lookingAt(); matcher.region(matcher.end(), length)) {
				backreach++;
			}

			if (backreach > maxBackreach) {
				throw new Exception("Maximum backreach was exceeded near \"" + value.subSequence(0, Math.min(length, 32)) + "\"");
			}
		}

		final Expression expression = new Expression(backreach);
		final List<ExpressionSegment> segments = new ArrayList<>();

		// Load all segments
		for (matcher.usePattern(SEGMENT_LOADER_TOKEN); matcher.find(); matcher.region(matcher.end(), length)) {
			switch (value.charAt(matcher.start())) {
			// ".", "(", ",", ")", "[", "]", "{", "}", "+", "-", "*", "/", "%", "^", "!", "<", ">", "=", "+=", "-=", "*=", "/=", "%=", "^=", "!=", "<=", ">=", "=="
			case '.': {
				final String identifier = CharSequenceUtils.trim(value, matcher.regionStart(), matcher.start()).toString();

				if (!identifier.isEmpty()) {
					segments.add(new ExpressionSegment(identifier, null));
				} else if (segments.isEmpty() && matcher.region(matcher.end(), length).usePattern(ONLY_WHITESPACE).matches()) {
					// A single dot resolves to the current object
					return expression;
				} else {
					; // TODO: throw exception
				}

				break;
			}

			case '(': {
				final Expression argExpression = load(value, length, matcher, maxBackreach);
			}

			case ')':
			case ',': {
				final String identifier = CharSequenceUtils.trim(value, matcher.regionStart(), matcher.start()).toString();


			}

			default: break;
			}
		}

		segments.add(new ExpressionSegment(CharSequenceUtils.trim(value, matcher.regionStart(), length).toString(), null));
		expression.segments = segments.toArray(EMPTY_SEGMENTS);
		return expression;
	}

	public static Expression load(final CharSequence value, final int maxBackreach) throws Exception {
		final Expression expression = load(value, value.length(), GLOBAL_BACKREACH.matcher(value), maxBackreach);

		expression.originalString = value.toString();
		return expression;
	}

	private final int backreach;
	private String originalString;
	private ExpressionSegment segments[] = EMPTY_SEGMENTS;

	/**
	 * Creates a new expression
	 *
	 * @param backreach the backreach for the expression
	 */
	private Expression(final int backreach) {
		this.backreach = backreach;
	}

	@Override
	public boolean equals(final Object object) {
		if (this == object) {
			return true;
		} else if (object instanceof Expression) {
			return backreach == ((Expression)object).backreach && segments.equals(((Expression)object).segments);
		}

		return false;
	}

	/**
	 * Evaluates the expression using the given context
	 *
	 * @param context the context to use for evaluating the expression
	 * @return the evaluated expression or null if the expression could not be evaluated
	 */
	public Object evaluate(final RenderContext context) {
		if (context.getSettings().getAllowAccessToFullContextStack()) {
			nextContext:
			for (int i = Integer.min(backreach, context.getSectionData().size()); i < context.getSectionData().size(); i++) {
				Object object = context.getSectionData().peek(i);

				if (segments.length > 0) {
					object = segments[0].evaluate(context, object);

					// Only continue with the next context if the first segment fails to load an object.
					if (object == null) {
						continue nextContext;
					} else {
						for (int j = 1; object != null && j < segments.length; j++) {
							object = segments[j].evaluate(context, object);
						}
					}
				}

				return object;
			}
		} else { // Only allow access to context at the specified scope
			final Object contextObject = context.getSectionData().peek(backreach);

			if (segments.length > 0) {
				Object object = segments[0].evaluate(context, contextObject);

				// Only search the global data if the context access fails on the first lookup
				if (object == null && backreach < context.getSectionData().size()) {
					object = segments[0].evaluate(context, context.getGlobalData());
				}

				for (int i = 1; object != null && i < segments.length; i++) {
					object = segments[i].evaluate(context, object);
				}

				return object;
			}

			return contextObject;
		}

		return null;
	}

	@Override
	public int hashCode() {
		int hash = backreach;

		for (final ExpressionSegment segment : segments) {
			hash = hash * 31 + segment.hashCode();
		}

		return hash;
	}

	/**
	 * Checks if the specified expression exactly matches the original expression
	 *
	 * @param expression the expression
	 * @return true if the specified expression matches, otherwise false
	 */
	public boolean exactlyMatches(final CharSequence expression) {
		return originalString.contentEquals(expression);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		for (int i = 0; i < backreach; i++) {
			sb.append("../");
		}

		// Append all the segments (or "." if none exist)
		if (segments.length > 0) {
			sb.append(segments[0].toString());

			for (int i = 1; i < segments.length; i++) {
				sb.append(".").append(segments[i].toString());
			}
		} else {
			sb.append(".");
		}

		return sb.toString();
	}

}
