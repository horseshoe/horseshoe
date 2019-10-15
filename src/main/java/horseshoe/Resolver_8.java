package horseshoe;

import java.util.Optional;
import java.util.stream.Stream;

import horseshoe.Resolver.MapResolver;

abstract class Resolver_8 {

	public static class Factory extends Resolver.Factory {
		@Override
		public Resolver create(final ResolverContext context, final String identifier) {
			if (Stream.class.isAssignableFrom(context.getObjectClass())) {
				return new MapResolver(identifier);
			} else if (Optional.class.isAssignableFrom(context.getObjectClass())) {
				return null;
			}

			return super.create(context, identifier);
		}
	}

}
