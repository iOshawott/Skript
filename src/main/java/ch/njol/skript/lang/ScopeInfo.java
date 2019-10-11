package ch.njol.skript.lang;

import org.eclipse.jdt.annotation.Nullable;

import io.github.syst3ms.skriptparser.PatternParser;
import io.github.syst3ms.skriptparser.SkriptRegistry;
import io.github.syst3ms.skriptparser.parsing.SyntaxParser;

/**
 * Statement is a block of code under a trigger. Common statements include
 * loops and conditional blocks.
 */
public class ScopeInfo<E extends SyntaxElement> extends SyntaxElementInfo<E> {

	/**
	 * If present, used for parsing everything inside this scope.
	 */
	@Nullable
	public final SyntaxParser parserOverride;
	
	/**
	 * Creates a new scope info.
	 * @param parser Pattern parser to use for parsing patterns.
	 * @param patterns Patterns to use.
	 * @param c Scope class.
	 * @param originClassPath Class who registered this scope.
	 * @param parserOverride Optional parser override for everything inside
	 * this scope.
	 */
	public ScopeInfo(PatternParser parser, String[] patterns, Class<E> c, String originClassPath,
			@Nullable SyntaxParser parserOverride) {
		super(parser, patterns, c, originClassPath);
		this.parserOverride = parserOverride;
	}
	
}
