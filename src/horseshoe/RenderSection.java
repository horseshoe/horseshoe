package horseshoe;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import horseshoe.internal.Properties;

class RenderSection implements Action {

	static interface Factory {
		Action create(final Expression resolver, final Section section);
	}

	static final Factory FACTORY;

	static {
		Factory factory = new Factory() {
			@Override
			public Action create(final Expression resolver, final Section section) {
				return new RenderSection(resolver, section);
			}
		};

		try { // Try to load the Java 8+ version
			if (Properties.JAVA_VERSION >= 8.0) {
				factory = (Factory)Factory.class.getClassLoader().loadClass(Factory.class.getName().replace("RenderSectionAction", "RenderSectionAction_8")).newInstance();
			}
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
		}

		FACTORY = factory;
	}

	protected static void executeActionsWith(final RenderContext context, final Object data, final PrintStream stream, final List<Action> actions) {
		context.getSectionData().push(data);

		for (final Action action : actions) {
			action.perform(context, stream);
		}

		context.getSectionData().pop();
	}

	protected final Expression resolver;
	protected final Section section;

	RenderSection(final Expression resolver, final Section section) {
		this.resolver = resolver;
		this.section = section;
	}

	protected void dispatchData(final RenderContext context, final Object data, final PrintStream stream) {
		if (data instanceof Iterable<?>) {
			final Iterator<?> it = ((Iterable<?>)data).iterator();

			if (it.hasNext()) {
				do {
					executeActionsWith(context, it.next(), stream, section.getActions());
				} while (it.hasNext());
			} else {
				executeActionsWith(context, context.getSectionData().peek(), stream, section.getInvertedActions());
			}
		} else if (data instanceof Boolean) {
			if ((Boolean)data) {
				executeActionsWith(context, data, stream, section.getActions());
			} else {
				executeActionsWith(context, context.getSectionData().peek(), stream, section.getInvertedActions());
			}
		} else if (data != null) {
			executeActionsWith(context, data, stream, section.getActions());
		} else {
			executeActionsWith(context, context.getSectionData().peek(), stream, section.getInvertedActions());
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
		dispatchData(context, resolver.evaluate(context), stream);
	}

}
