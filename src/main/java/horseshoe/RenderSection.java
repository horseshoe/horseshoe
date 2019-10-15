package horseshoe;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import horseshoe.internal.Properties;

class RenderSection implements Action {

	public static class Factory {

		/**
		 * Creates a new render section action using the specified resolver and section.
		 *
		 * @param expression the expression used in the section
		 * @param section the section to be rendered
		 * @return the created render section action
		 */
		RenderSection create(final Expression expression, final Section section){
			return new RenderSection(expression, section);
		}

	}

	static final Factory FACTORY;

	static {
		Factory factory = new Factory();

		try { // Try to load the Java 8+ version
			if (Properties.JAVA_VERSION >= 8.0) {
				factory = (Factory)Factory.class.getClassLoader().loadClass(Factory.class.getName().replace("RenderSectionAction", "RenderSectionAction_8")).newInstance();
			}
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
		}

		FACTORY = factory;
	}

	/**
	 * Renders the actions using the specified context, data, and stream.
	 *
	 * @param context the render context
	 * @param data the data to render
	 * @param stream the stream used for rendering
	 * @param actions the actions to render
	 */
	protected static void renderActions(final RenderContext context, final Object data, final PrintStream stream, final List<Action> actions) {
		context.getSectionData().push(data);

		for (final Action action : actions) {
			action.perform(context, stream);
		}

		context.getSectionData().pop();
	}

	protected final Expression expression;
	protected final Section section;

	/**
	 * Creates a new render section action using the specified resolver and section.
	 *
	 * @param expression the expression used in the section
	 * @param section the section to be rendered
	 */
	protected RenderSection(final Expression expression, final Section section) {
		this.expression = expression;
		this.section = section;
	}

	/**
	 * Dispatches the data for rendering using the appropriate transformation for the specified data. Lists are iterated, booleans are evaluated, etc.
	 *
	 * @param context the render context
	 * @param data the data to render
	 * @param stream the stream used for rendering
	 */
	protected void dispatchData(final RenderContext context, final Object data, final PrintStream stream) {
		if (data instanceof Iterable<?>) {
			final Iterator<?> it = ((Iterable<?>)data).iterator();

			if (it.hasNext()) {
				do {
					renderActions(context, it.next(), stream, section.getActions());
				} while (it.hasNext());
			} else {
				renderActions(context, context.getSectionData().peek(), stream, section.getInvertedActions());
			}
		} else if (data instanceof Boolean) {
			if ((Boolean)data) {
				renderActions(context, context.getSectionData().peek(), stream, section.getActions());
			} else {
				renderActions(context, context.getSectionData().peek(), stream, section.getInvertedActions());
			}
		} else if (data != null) {
			renderActions(context, data, stream, section.getActions());
		} else {
			renderActions(context, context.getSectionData().peek(), stream, section.getInvertedActions());
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
	public final void perform(final RenderContext context, final PrintStream stream) {
		dispatchData(context, expression.evaluate(context), stream);
	}

}
