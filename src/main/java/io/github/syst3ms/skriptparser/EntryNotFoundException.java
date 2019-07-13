package io.github.syst3ms.skriptparser;


/**
 * Thrown when Skript registry entry does not exist.
 */
public class EntryNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public EntryNotFoundException(Object key) {
		super("entry not found:" + key);
	}
}
