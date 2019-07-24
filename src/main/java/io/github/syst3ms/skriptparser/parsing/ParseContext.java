package io.github.syst3ms.skriptparser.parsing;

import io.github.syst3ms.skriptparser.event.TriggerContext;
import io.github.syst3ms.skriptparser.pattern.PatternElement;

import java.util.List;
import java.util.regex.MatchResult;

import ch.njol.skript.lang.SkriptParser.ParseResult;

/**
 * Stores data about parsing operation of a syntax element.
 * Unlike {@link MatchContext}, this type is immutable.
 */
public class ParseContext {
	
	/**
	 * Current trigger contexts.
	 */
    private final Class<? extends TriggerContext>[] currentContexts;
    
    /**
     * The top-level pattern element that was successfully parsed.
     */
    private final PatternElement element;
    
    /**
     * Original expression string.
     */
    private final String unparsed;
    
    /**
     * Matched regular expressions.
     */
    private final List<MatchResult> matches;
    
    /**
     * Index of pattern that was matched.
     */
    private final int matchedPattern;
    
    /**
     * Parser mark.
     */
    private final int parseMark;

    public ParseContext(Class<? extends TriggerContext>[] currentContexts, PatternElement element,
    		List<MatchResult> matches, int matchedPattern, int parseMark, String unparsed) {
        this.currentContexts = currentContexts;
        this.element = element;
        this.unparsed = unparsed;
        this.matches = matches;
        this.matchedPattern = matchedPattern;
        this.parseMark = parseMark;
    }

	
	public Class<? extends TriggerContext>[] getCurrentContexts() {
		return currentContexts;
	}
	
	public PatternElement getElement() {
		return element;
	}

	
	public String getUnparsed() {
		return unparsed;
	}
	
	public List<MatchResult> getMatches() {
		return matches;
	}
	
	public int getMatchedPattern() {
		return matchedPattern;
	}
	
	public int getParseMark() {
		return parseMark;
	}

	public ParseResult toLegacyResult() {
		throw new UnsupportedOperationException("TODO");
	}
    
}
