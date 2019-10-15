package horseshoe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.CharSequenceUtils;

class Expression {

	private static final ExpressionSegment EMPTY_SEGMENTS[] = new ExpressionSegment[0];

	private static final Pattern SEGMENT_BACKREACH = Pattern.compile("\\s[.][.]\\s/");
	private static final Pattern SEGMENT_LOADER_TOKEN = Pattern.compile("[.(,)]");
	private static final Pattern ONLY_WHITESPACE = Pattern.compile("\\s*");

	public static Expression load(final LoadContext context, final CharSequence value, final Matcher matcher, final int maxBackreach) {
		final int length = value.length();
		final int parentheses = 0;
		int backreach = 0;

		// Determine the backreach
		for (; matcher.lookingAt(); matcher.region(matcher.end(), length)) {
			backreach++;
		}

		final Expression resolver = new Expression(backreach);
		final List<ExpressionSegment> segments = new ArrayList<>();
		// final List<ExpressionResolver> args = new ArrayList<>();

		// Load all segments
		for (matcher.usePattern(SEGMENT_LOADER_TOKEN); matcher.find(); matcher.region(matcher.end(), length)) {
			switch (value.charAt(matcher.start())) {
			case '.': {
				final String identifier = CharSequenceUtils.trim(value, matcher.regionStart(), matcher.start()).toString();

				if (!identifier.isEmpty()) {
					segments.add(new ExpressionSegment(identifier, null));
				} else if (segments.isEmpty() && matcher.region(matcher.end(), length).usePattern(ONLY_WHITESPACE).matches()) {
					// A single dot resolves to the current object
					resolver.segments = EMPTY_SEGMENTS;
					return resolver;
				} else {
					; // TODO: throw exception
				}

				break;
			}

			default: break;
			}
		}

		segments.add(new ExpressionSegment(CharSequenceUtils.trim(value, matcher.regionStart(), length).toString(), null));
		resolver.segments = segments.toArray(EMPTY_SEGMENTS);
		return resolver;
	}

	public static Expression load(final LoadContext context, final CharSequence value, final int maxBackreach) {
		return load(context, value, SEGMENT_BACKREACH.matcher(value), maxBackreach);
	}

	private final int backreach;
	private ExpressionSegment segments[];

	private Expression(final int backreach) {
		this.backreach = backreach;
	}

	@Override
	public boolean equals(final Object object) {
		if (this == object) {
			return true;
		} else if (object instanceof Expression) {
			return backreach == ((Expression)object).backreach && segments.equals(((Expression)object).segments);
		} else if (object instanceof CharSequence) {
			return true; // TODO
		}

		return false;
	}

	public Object evaluate(final RenderContext context) {
		if (context.getAllowAccessToFullContextStack()) {
			nextContext:
			for (final Object contextObject : context.getSectionData()) {
				if (segments.length > 0) {
					Object object = segments[0].evaluate(context, contextObject);

					// Only continue with the next context if the first segment fails to load an object.
					if (object == null) {
						continue nextContext;
					}

					for (int i = 1; i < segments.length; i++) {
						object = segments[i].evaluate(context, object);

						if (object == null) {
							return null;
						}
					}

					return object;
				}

				return contextObject;
			}
		} else { // Only allow access to context at the specified scope
			Object object = context.getSectionData().peek(backreach);

			for (int i = 0; object != null && i < segments.length; i++) {
				object = segments[i].evaluate(context, object);
			}

			return object;
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
