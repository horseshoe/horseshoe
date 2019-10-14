package horseshoe;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class RenderSection_8 extends RenderSection {

	static class Factory implements RenderSection.Factory {
		@Override
		public Action create(final Expression resolver, final Section section) {
			return new RenderSection_8(resolver, section);
		}
	}

	RenderSection_8(final Expression resolver, final Section section) {
		super(resolver, section);
	}

	@Override
	protected void dispatchData(final RenderContext context, final Object data, final PrintStream stream) {
		if (data instanceof Stream<?>) {
			final List<Object> list = ((Stream<?>)data).collect(Collectors.toList()); // TODO: Stream.forEach().orElse()?

			if (!list.isEmpty()) {
				for (final Object obj : list) {
					executeActionsWith(context, obj, stream, section.getActions());
				}
			} else {
				executeActionsWith(context, context.getSectionData().peek(), stream, section.getInvertedActions());
			}
		} else if (data instanceof Optional<?>) {
			final Optional<?> optional = (Optional<?>)data;

			if (optional.isPresent()) {
				executeActionsWith(context, optional.get(), stream, section.getActions());
			} else {
				executeActionsWith(context, context.getSectionData().peek(), stream, section.getInvertedActions());
			}
		} else {
			super.dispatchData(context, data, stream);
		}
	}

}
