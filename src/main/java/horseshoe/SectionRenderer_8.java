package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SectionRenderer_8 extends SectionRenderer {

	public static class Factory extends SectionRenderer.Factory {
		@Override
		public SectionRenderer_8 create(final Section section) {
			return new SectionRenderer_8(section);
		}
	}

	/**
	 * Creates a new section renderer using the specified section.
	 *
	 * @param section the section to be rendered
	 */
	protected SectionRenderer_8(final Section section) {
		super(section);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void dispatchData(final RenderContext context, final Object data, final Writer writer) throws IOException {
		if (data instanceof Supplier<?>) {
			dispatchData(context, ((Supplier<Object>)data).get(), writer);
			return;
		} else if (data instanceof Function<?, ?>) {
			final Function<String, Object> function = (Function<String, Object>)data;

			/*
			if (!list.isEmpty()) {
				for (final Object obj : list) {
					renderActions(context, obj, writer, section.getActions());
				}
			} else {
				renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
			}//*/
		} else if (data instanceof Stream<?>) {
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
