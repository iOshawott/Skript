package io.github.syst3ms.skriptparser.parsing;

import io.github.syst3ms.skriptparser.event.TriggerContext;
import io.github.syst3ms.skriptparser.pattern.PatternElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;

import ch.njol.skript.lang.Expression;

/**
 * A parser instance used for matching a pattern to a syntax
 */
public class MatchContext {

	/**
	 * Syntax parser that is currently in use.
	 */
	private final SyntaxParser parser;
	
	/**
	 * Original pattern found in script.
	 */
    private final String originalPattern;
    
    public final PatternElement originalElement;
    
    private final int matchedPattern;
    
    // Provided to the syntax's class
    private final Class<? extends TriggerContext>[] currentContext;
    
    /**
     * Inputs given to expression with this match context.
     */
    private List<ParsedElement> inputs = new ArrayList<>();
    
    private List<MatchResult> regexMatches = new ArrayList<>();
    private int patternIndex = 0;
    private int parseMark = 0;

    public MatchContext(SyntaxParser parser, PatternElement e, Class<? extends TriggerContext>[] currentContext,
    		int matchedPattern) {
    	this.parser = parser;
        this.originalPattern = e.toString();
        this.originalElement = e;
        this.currentContext = currentContext;
        this.matchedPattern = matchedPattern;
    }
    
    public SyntaxParser getSyntaxParser() {
    	return parser;
    }

    public PatternElement getOriginalElement() {
        return originalElement;
    }

    public String getOriginalPattern() {
        return originalPattern;
    }

    public int getPatternIndex() {
        return patternIndex;
    }

    public void advanceInPattern() {
        patternIndex++;
    }
    
    public List<ParsedElement> getInputs() {
        return inputs;
    }

    public void addInput(ParsedElement input) {
        inputs.add(input);
    }

    public void addRegexMatch(MatchResult match) {
        regexMatches.add(match);
    }

    public int getParseMark() {
        return parseMark;
    }

    public void addMark(int mark) {
        parseMark ^= mark;
    }

    /**
     * Turns this {@link MatchContext} into a {@link ParseContext} used in {@linkplain io.github.syst3ms.skriptparser.lang.SyntaxElement}s
     * @return a {@link ParseContext} based on this {@link MatchContext}
     */
    public ParseContext toParseResult() {
        return new ParseContext(currentContext, originalElement, regexMatches, matchedPattern, parseMark, originalPattern);
    }
}
