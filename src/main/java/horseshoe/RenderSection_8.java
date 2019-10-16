package horseshoe;

import java.io.IOException;
import java.io.Writer;
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
	protected void dispatchData(final RenderContext context, final Object data, final Writer writer) throws IOException {
		if (data instanceof Stream<?>) {
			final List<Object> list = ((Stream<?>)data).collect(Collectors.toList()); // TODO: Stream.forEach().orElse()?

			if (!list.isEmpty()) {
				for (final Object obj : list) {
					renderActions(context, obj, writer, section.getActions());
				}
			} else {
				renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
			}
		} else if (data instanceof Optional<?>) {
			final Optional<?> optional = (Optional<?>)data;

			if (optional.isPresent()) {
				renderActions(context, optional.get(), writer, section.getActions());
			} else {
				renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
			}
		} else {
			super.dispatchData(context, data, writer);
		}
	}

}
