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
	
	/**
	 * Original representation of this node, exactly as it was written in
	 * the script.
	 */
	private final String original;
	
	/**
	 * Expected return type of this AST node.
	 */
	private final Class<?> returnType;
	
	/**
	 * Whether one or multiple return values are wanted.
	 */
	private final boolean isSingle;
	
	public AstNode(String original, Class<?> returnType, boolean isSingle) {
		this.original = original;
		this.returnType = returnType;
		this.isSingle = isSingle;
	}
	
	public String getOriginal() {
		return original;
	}
	
	public Class<?> getReturnType() {
		return returnType;
	}
	
	public boolean isSingle() {
		return isSingle;
	}
}
