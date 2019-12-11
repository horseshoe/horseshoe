package horseshoe;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import horseshoe.internal.Properties;

class SectionRenderer implements Action {

	public static class Factory {

		/**
		 * Creates a new section renderer using the specified section.
		 *
		 * @param section the section to be rendered
		 * @return the created section renderer
		 */
		SectionRenderer create(final Section section){
			return new SectionRenderer(section);
		}

	}

	static final Factory FACTORY;

	static {
		Factory factory = new Factory();

		try { // Try to load the Java 8+ version
			if (Properties.JAVA_VERSION >= 8.0) {
				factory = (Factory)Factory.class.getClassLoader().loadClass(Factory.class.getName().replace(SectionRenderer.class.getSimpleName(), SectionRenderer.class.getSimpleName() + "_8")).getConstructor().newInstance();
			}
		} catch (final ReflectiveOperationException e) {
		}

		FACTORY = factory;
	}

	/**
	 * Renders the actions using the specified context, data, and writer.
	 *
	 * @param context the render context
	 * @param data the data to render
	 * @param writer the writer used for rendering
	 * @param actions the actions to render
	 * @throws IOException if an error occurs while writing to the writer
	 */
	protected static void renderActions(final RenderContext context, final Object data, final Writer writer, final List<Action> actions) throws IOException {
		context.getSectionData().push(data);

		for (final Action action : actions) {
			action.perform(context, writer);
		}

		context.getSectionData().pop();
	}

	protected final Section section;

	/**
	 * Creates a new section renderer using the specified resolver and section.
	 *
	 * @param section the section to be rendered
	 */
	protected SectionRenderer(final Section section) {
		this.section = section;
	}

	/**
	 * Dispatches the data for rendering using the appropriate transformation for the specified data. Lists are iterated, booleans are evaluated, etc.
	 *
	 * @param context the render context
	 * @param data the data to render
	 * @param writer the writer used for rendering
	 * @throws IOException if an error occurs while writing to the writer
	 */
	protected void dispatchData(final RenderContext context, final Object data, final Writer writer) throws IOException {
		if (data instanceof Iterable<?>) {
			final Iterator<?> it = ((Iterable<?>)data).iterator();

			if (it.hasNext()) {
				while (true) {
					final Object object = it.next();

					if (it.hasNext()) {
						renderActions(context, object, writer, section.getActions());
					} else {
						renderActions(context, object, writer, section.getActions());
						break;
					}
				}
			} else {
				renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
			}
		} else if (data instanceof Boolean) {
			if ((Boolean)data) {
				renderActions(context, context.getSectionData().peek(), writer, section.getActions());
			} else {
				renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
			}
		} else if (data != null) {
			renderActions(context, data, writer, section.getActions());
		} else {
			renderActions(context, context.getSectionData().peek(), writer, section.getInvertedActions());
		}
	}

	/**
	 * Gets the section that is rendered by this action
	 *
	 * @return the section that is rendered by this action
	 */
	Section getSection() {
		return section;
	}

	@Override
	public final void perform(final RenderContext context, Writer writer) throws IOException {
		final Object value;

		if (section.getWriterName() != null) {
			final Writer newWriter = context.getWriterMap().getWriter(section.getWriterName());

			if (newWriter != null) {
				writer = newWriter;
			}

			value = context.getSectionData().peek();
		} else {
			value = section.getExpression().evaluate(context.getSectionData(), context.getSettings().getContextAccess());
		}

		dispatchData(context, value, writer);
	}

}
