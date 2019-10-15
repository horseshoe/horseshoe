package horseshoe;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class RenderSection_8 extends RenderSection {

	public static class Factory extends RenderSection.Factory {
		@Override
		public RenderSection_8 create(final Expression expression, final Section section) {
			return new RenderSection_8(expression, section);
		}
	}

	/**
	 * Creates a new render section action using the specified resolver and section.
	 *
	 * @param expression the expression used in the section
	 * @param section the section to be rendered
	 */
	protected RenderSection_8(final Expression expression, final Section section) {
		super(expression, section);
	}

	@Override
	protected void dispatchData(final RenderContext context, final Object data, final PrintStream stream) {
		if (data instanceof Stream<?>) {
			final List<Object> list = ((Stream<?>)data).collect(Collectors.toList()); // TODO: Stream.forEach().orElse()?

			if (!list.isEmpty()) {
				for (final Object obj : list) {
					renderActions(context, obj, stream, section.getActions());
				}
			} else {
				renderActions(context, context.getSectionData().peek(), stream, section.getInvertedActions());
			}
		} else if (data instanceof Optional<?>) {
			final Optional<?> optional = (Optional<?>)data;

			if (optional.isPresent()) {
				renderActions(context, optional.get(), stream, section.getActions());
			} else {
				renderActions(context, context.getSectionData().peek(), stream, section.getInvertedActions());
			}
		} else {
			super.dispatchData(context, data, stream);
		}
	}

}
