package horseshoe;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;

public class ParseException extends Exception {

	private final Collection<FileFrame> frames;
	private final String message;

	public ParseException(Collection<FileFrame> frames, String error) {
		final StringBuilder sb = new StringBuilder(error);
		final Iterator<FileFrame> it = frames.iterator();

		if (it.hasNext()) {
			FileFrame frame = it.next();
			sb.append(" at ").append(frame.toString());

			while (it.hasNext()) {
				frame = it.next();
				sb.append(System.lineSeparator()).append(" included from ").append(frame.toString());
			}
		}

		this.frames = frames;
		this.message = sb.toString();
	}

	public Collection<FileFrame> getFrames() {
		return frames;
	}

	@Override
	public String getMessage() {
		return message;
	}

}
