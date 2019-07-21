package io.github.syst3ms.skriptparser.ast;

import ch.njol.skript.lang.SyntaxElement;
import io.github.syst3ms.skriptparser.parsing.ParseContext;
import io.github.syst3ms.skriptparser.parsing.SyntaxParser;

/**
 * An abstract syntax tree node representing a syntax element.
 * 
 * <p>Skript AST is initially produced by {@link SyntaxParser}. After that, it
 * should be loaded using TBD which also causes error checking to be done. Only
 * after successful load the AST can be serialized for caching etc.
 */
public abstract class AstNode {
	
}
