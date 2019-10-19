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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Partials {

	static abstract class LazyTemplate {
		final Map<Context, Template> values = new HashMap<>();
		final Template get(Context context) throws IOException, LoadException {
				Template t = values.get(context);
				if (t == null) {
					values.put(context, Template.RECURSIVE_TEMPLATE_DETECTED);
					t = createNewTemplate(context);
					values.put(context, t);
				}
				return t;
		}
		abstract Template createNewTemplate(Context context) throws IOException, LoadException;
	}

	final Map<String, LazyTemplate> map = new LinkedHashMap<>();

	public Partials add(final String name, final String partial) {
		map.put(name, new LazyTemplate() {
			@Override
			public synchronized Template createNewTemplate(final Context context) throws IOException, LoadException {
				return new Template(name, partial, context);
			}
		});
		return this;
	}

	public Partials add(final String name, final Reader reader) {
		map.put(name, new LazyTemplate() {
			@Override
			public synchronized Template createNewTemplate(final Context context) throws IOException, LoadException {
				return new Template(name, reader, context);
			}
		});
		return this;
	}

	public Partials add(final String name, final Path file, final Charset charset) {
		map.put(name, new LazyTemplate() {
			@Override
			public synchronized Template createNewTemplate(final Context context) throws IOException, LoadException {
				return new Template(name, file, charset, context);
			}
		});
		return this;
	}

	public Partials addDirectory(final Path directory, final Charset charset, final String fileExtension) throws IOException {
		Files.walkFileTree(directory, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
				final Path f = file.getFileName();
				final String fileName;
				if (f != null && (fileName = f.toString()).endsWith(fileExtension)) {
					final String fileNameNoExtension = fileName.substring(0, fileName.lastIndexOf('.'));
					map.put(fileNameNoExtension, new LazyTemplate() {
						@Override
						public synchronized Template createNewTemplate(final Context context) throws IOException, LoadException {
							return new Template(fileNameNoExtension, file, charset, context);
						}
					});
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return this;
	}

}