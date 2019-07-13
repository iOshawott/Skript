/**
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
 * Copyright 2011-2017 Peter Güttinger and contributors
 */
package ch.njol.skript.lang;

import java.lang.reflect.InvocationTargetException;

import io.github.syst3ms.skriptparser.PatternParser;
import io.github.syst3ms.skriptparser.pattern.PatternElement;

/**
 * Stores information about a {@link SyntaxElement} needed by Skript
 * parser.
 * @param <E> The syntax element this info is for.
 */
public class SyntaxElementInfo<E extends SyntaxElement> {
	
	/**
	 * The class that implements this syntax element.
	 */
	public final Class<E> c;
	
	/**
	 * Original patterns for parsing this syntax element.
	 */
	public final String[] patterns;
	
	/**
	 * Who registered this syntax element.
	 */
	public final String originClassPath;
	
	/**
	 * Precompiled patterns.
	 */
	private final PatternElement[] compiledPatterns;
	
	public SyntaxElementInfo(PatternParser parser, String[] patterns, Class<E> c, String originClassPath) throws IllegalArgumentException {
		this.patterns = patterns;
		this.c = c;
		this.originClassPath = originClassPath;
		
		// Parse the patterns now for improved runtime performance
		this.compiledPatterns = new PatternElement[patterns.length];
		for (int i = 0; i < patterns.length; i++) {
			String pattern = patterns[i];
			assert pattern != null;
			compiledPatterns[i] = parser.parsePattern(pattern);
		}
		
		try {
			c.getConstructor().newInstance();
		} catch (NoSuchMethodException e) {
			// throwing an Exception throws an (empty) ExceptionInInitializerError instead, thus an Error is used
			throw new Error(c + " does not have a public nullary constructor", e);
		} catch (SecurityException e) {
			throw new IllegalStateException("Skript cannot run properly because a security manager is blocking it!", e);
		} catch (InstantiationException e) {
			throw new Error(c + " has a nullary constructor, but Skript can't access it", e);
		} catch (IllegalAccessException e) {
			throw new Error(c + " has a nullary constructor, but Skript can't access it", e);
		} catch (InvocationTargetException e) {
			throw new Error(c + " has a nullary constructor, but it threw an exception", e.getCause());
		}
	}
	
	/**
	 * Gets the patterns that match this syntax element in compiled form.
	 * @return Compiled patterns.
	 */
	public PatternElement[] getCompiledPatterns() {
		return compiledPatterns;
	}
	
}
