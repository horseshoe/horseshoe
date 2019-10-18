package horseshoe;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Partials {

	interface LazyTemplate {
		Template get(Context context) throws IOException, LoadException;
	}

	final Map<String, LazyTemplate> map = new LinkedHashMap<>();

	public Partials add(final String name, final String partial) {
		map.put(name, new LazyTemplate() {
			private Template t = null;
			@Override
			public synchronized Template get(final Context context) throws IOException, LoadException {
				if (t == null) {
					t = Template.RECURSIVE_TEMPLATE_DETECTED;
					t = new Template(name, partial, context);
				}
				return t;
			}
		});
		return this;
	}

	public Partials add(final String name, final Reader reader) {
		map.put(name, new LazyTemplate() {
			private Template t = null;
			@Override
			public synchronized Template get(final Context context) throws IOException, LoadException {
				if (t == null) {
					t = Template.RECURSIVE_TEMPLATE_DETECTED;
					t = new Template(name, reader, context);
				}
				return t;
			}
		});
		return this;
	}

	public Partials directory(final Path directory, final Charset charset, final String fileExtension) throws IOException {
		Files.walkFileTree(directory, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
				final Path f = file.getFileName();
				final String fileName;
				if (f != null && (fileName = f.toString()).endsWith(fileExtension)) {
					final String fileNameNoExtension = fileName.substring(0, fileName.lastIndexOf('.'));
					map.put(fileNameNoExtension, new LazyTemplate() {
						private Template t = null;
						@Override
						public synchronized Template get(final Context context) throws IOException, LoadException {
							if (t == null) {
								t = Template.RECURSIVE_TEMPLATE_DETECTED;
								t = new Template(fileNameNoExtension, file, charset, context);
							}
							return t;
						}
					});
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return this;
	}

	public Partials add(final String name, final Template template) {
		// TODO: seems bizarre to have to specify the name of a template when its an argument (either implicitly through the file name or explicitly)
		map.put(name, new LazyTemplate() {
			@Override
			public Template get(final Context context) {
				return template;
			}
		});
		return this;
	}

}
