/*
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * Copyright 2011-2013 Peter Güttinger
 * 
 */

package ch.njol.skript.variables;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.FileUtils;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Pair;

/**
 * @author Peter Güttinger
 */
public abstract class VariablesStorage implements Closeable {
	
	private final static int QUEUE_SIZE = 1000, FIRST_WARNING = 300;
	
	final LinkedBlockingQueue<Pair<String, Pair<String, byte[]>>> changesQueue = new LinkedBlockingQueue<Pair<String, Pair<String, byte[]>>>(QUEUE_SIZE);
	
	protected volatile boolean closed = false;
	
	protected final String name;
	
	protected File file;
	
	/**
	 * null for '.*' or '.+'
	 */
	private Pattern variablePattern;
	
	private final Thread writeThread;
	
	protected VariablesStorage(final SectionNode n) {
		name = n.getKey();
		writeThread = Skript.newThread(new Runnable() {
			@Override
			public void run() {
				while (!closed) {
					try {
						final Pair<String, Pair<String, byte[]>> var = changesQueue.take();
						if (var.second != null)
							save(var.first, var.second.first, var.second.second);
						else
							save(var.first, null, null);
					} catch (final InterruptedException e) {}
				}
			}
		}, "Skript variable save thread for database '" + name + "'");
	}
	
	protected String getValue(final SectionNode n, final String key) {
		return getValue(n, key, String.class);
	}
	
	protected <T> T getValue(final SectionNode n, final String key, final Class<T> type) {
		final String v = n.getValue(key);
		if (v == null) {
			Skript.error("The config is missing the entry for '" + key + "' in the database '" + name + "'");
			return null;
		}
		final ParseLogHandler log = SkriptLogger.startParseLogHandler();
		try {
			final T r = Classes.parse(v, type, ParseContext.CONFIG);
			if (r == null)
				log.printError("The entry for '" + key + "' in the database '" + name + "' must be a " + Classes.getSuperClassInfo(type).getName());
			else
				log.printLog();
			return r;
		} finally {
			log.stop();
		}
	}
	
	public final boolean load(final SectionNode n) {
		final String pattern = getValue(n, "pattern");
		if (pattern == null)
			return false;
		try {
			variablePattern = pattern.equals(".*") || pattern.equals(".+") ? null : Pattern.compile(pattern);
		} catch (final PatternSyntaxException e) {
			Skript.error("Invalid pattern '" + pattern + "': " + e.getLocalizedMessage());
			return false;
		}
		
		if (requiresFile()) {
			final String f = getValue(n, "file");
			if (f == null)
				return false;
			file = getFile(f).getAbsoluteFile();
			if (file.exists() && !file.isFile()) {
				Skript.error("The database file '" + file.getName() + "' must be an actual file, not a directory.");
				return false;
			} else {
				try {
					file.createNewFile();
				} catch (final IOException e) {
					Skript.error("Cannot create the database file '" + file.getName() + "': " + e.getLocalizedMessage());
					return false;
				}
			}
			if (!file.canWrite()) {
				Skript.error("Cannot write to the database file '" + file.getName() + "'!");
				return false;
			}
			if (!file.canRead()) {
				Skript.error("Cannot read from the database file '" + file.getName() + "'!");
//				Skript.error("This means that no variables will be available and can also prevent new variables from being saved!");
//				try {
//					final File backup = FileUtils.backup(file);
//					Skript.error("A backup of your variables.csv was created as " + backup.getName());
//				} catch (final IOException e) {
//					Skript.error("Failed to create a backup of your variables.csv: " + e.getLocalizedMessage());
//					loadError = true;
//				}
				return false;
			}
			
			if (!"0".equals(getValue(n, "backup interval"))) {
				final Timespan backupInterval = getValue(n, "backup interval", Timespan.class);
				if (backupInterval != null)
					startBackupTask(backupInterval);
			}
		}
		
		if (!load_i(n))
			return false;
		
		Skript.closeOnDisable(this);
		writeThread.start();
		return true;
	}
	
	/**
	 * Loads variables stored here.
	 * 
	 * @return Whether
	 */
	protected abstract boolean load_i(SectionNode n);
	
	protected abstract boolean requiresFile();
	
	protected abstract File getFile(String file);
	
	protected final Object connectionLock = new Object();
	
	/**
	 * Called after a backup of the file has been made
	 */
	protected abstract void connect();
	
	/**
	 * Called before a backup of the file is being made
	 */
	protected abstract void disconnect();
	
	protected Task backupTask = null;
	
	public void startBackupTask(final Timespan t) {
		assert file != null;
		if (t.getTicks() == 0)
			return;
		backupTask = new Task(Skript.getInstance(), t.getTicks(), t.getTicks(), true) {
			@Override
			public void run() {
				synchronized (connectionLock) {
					try {
						Variables.getReadLock().lock();
						disconnect();
						try {
							FileUtils.backup(file);
						} catch (final IOException e) {
							Skript.error("Automatic variables backup failed: " + e.getLocalizedMessage());
						} finally {
							connect();
							connectionLock.notifyAll();
						}
					} finally {
						Variables.getReadLock().unlock();
					}
				}
			}
		};
	}
	
	final boolean accept(final String var) {
		return variablePattern == null ? true : variablePattern.matcher(var).matches();
	}
	
	private long lastWarning = Long.MIN_VALUE;
	private final static int WARNING_INTERVAL = 10;
	private long lastError = Long.MIN_VALUE;
	private final static int ERROR_INTERVAL = 10;
	
	/**
	 * May be called from a different thread than Bukkit's main thread.
	 */
	final void save(final Pair<String, Pair<String, byte[]>> var) {
		if (changesQueue.size() > FIRST_WARNING && lastWarning < System.currentTimeMillis() - WARNING_INTERVAL * 1000) {
			Skript.warning("Cannot write variables to the database '" + name + "' at sufficient speed; server performance may suffer and many variables will be lost if the server crashes. (this warning will be repeated at most once every " + ERROR_INTERVAL + " seconds)");
			lastWarning = System.currentTimeMillis();
		}
		if (!changesQueue.offer(var)) {
			if (lastError < System.currentTimeMillis() - ERROR_INTERVAL * 1000) {
				Skript.error("Skript cannot save any variables to the database '" + name + "'. The server will hang and may crash if no more variables can be saved.");
				lastError = System.currentTimeMillis();
			}
			while (true) {
				try {
					// REMIND add repetitive error and/or stop saving variables altogether?
					changesQueue.put(var);
					break;
				} catch (final InterruptedException e) {}
			}
		}
	}
	
	@Override
	public void close() {
		while (changesQueue.size() > 0) {
			try {
				Thread.sleep(10);
			} catch (final InterruptedException e) {}
		}
		closed = true;
		writeThread.interrupt();
	}
	
	/**
	 * Clears the queue of unsaved variables. Only used if all variables are saved immediately after calling this method.
	 */
	protected void clearChangesQueue() {
		changesQueue.clear();
	}
	
	/**
	 * Saves a variable.
	 * 
	 * @param name
	 * @param type
	 * @param value
	 */
	protected abstract void save(String name, String type, byte[] value);
	
}
