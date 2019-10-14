package horseshoe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import horseshoe.internal.CharSequenceUtils;

class Expression {

	private static final Pattern SEGMENT_BACKREACH = Pattern.compile("\\s[.][.]\\s/");
	private static final Pattern SEGMENT_LOADER_TOKEN = Pattern.compile("[.(,)]");

	static Expression load(final LoadContext context, final CharSequence value, final Matcher matcher, final int maxBackreach) {
		final int length = value.length();
		final int parentheses = 0;
		int backreach = 0;

		// Determine the backreach
		for (; matcher.lookingAt(); matcher.region(matcher.end(), length)) {
			backreach++;
		}

		final Expression resolver = new Expression(backreach);
		// final List<ExpressionResolver> args = new ArrayList<>();

		// Load all segments
		for (matcher.usePattern(SEGMENT_LOADER_TOKEN); matcher.find(); matcher.region(matcher.end(), length)) {
			switch (value.charAt(matcher.start())) {
			case '.':
				resolver.segments.add(new ExpressionSegment(CharSequenceUtils.trim(value, matcher.regionStart(), matcher.start()).toString(), null));
				break;

			default: break;
			}
		}

		resolver.segments.add(new ExpressionSegment(CharSequenceUtils.trim(value, matcher.regionStart(), length).toString(), null));

		return resolver;
	}

	static Expression load(final LoadContext context, final CharSequence value, final int maxBackreach) {
		return load(context, value, SEGMENT_BACKREACH.matcher(value), maxBackreach);
	}

	private final int backreach;
	private final List<ExpressionSegment> segments = new ArrayList<>();

	private Expression(final int backreach) {
		this.backreach = backreach;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof Expression) {
			return backreach == ((Expression)obj).backreach && segments.equals(((Expression)obj).segments);
		} else if (obj instanceof CharSequence) {
			return true; // TODO
		}

		return false;
	}

	Object evaluate(final RenderContext context) {
		Object obj = context.getSectionData().peek(backreach);

		for (final ExpressionSegment segment : segments) {
			if (obj == null) {
				return null;
			}

			obj = segment.evaluate(context, obj);
		}

		return obj;
	}

	@Override
	public int hashCode() { // TODO
		final int prime = 31;
		int result = 1;
		result = prime * result + backreach;
		result = prime * result + ((segments == null) ? 0 : segments.hashCode());
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		String separator = "";

		for (int i = 0; i < backreach; i++) {
			sb.append("../");
		}

		for (final ExpressionSegment segment : segments) {
			sb.append(separator).append(segment.toString());
			separator = ".";
		}

		return sb.toString();
	}

}
