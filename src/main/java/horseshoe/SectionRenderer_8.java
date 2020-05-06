package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import horseshoe.internal.RenderContext;

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

	@Override
	protected void dispatchData(final RenderContext context, final Object data, final Writer writer) throws IOException {
		final Object unwrappedData;

		if (data instanceof Optional<?>) {
			unwrappedData = ((Optional<?>)data).orElse(null);
		} else {
			unwrappedData = data;
		}

		if (!(unwrappedData instanceof Stream<?>)) {
			super.dispatchData(context, unwrappedData, writer);
		} else if (section.cacheResult()) { // Only collect to a list if we are required to cache the results
			super.dispatchData(context, ((Stream<?>)unwrappedData).collect(Collectors.toList()), writer);
		} else {
			super.dispatchIteratorData(context, ((Stream<?>)unwrappedData).iterator(), writer);
		}
	}

}
