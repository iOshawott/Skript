package io.github.syst3ms.skriptparser.ast;

import ch.njol.skript.lang.SyntaxElement;
import io.github.syst3ms.skriptparser.parsing.ParseContext;

/**
 * Represents a literal that cannot be parsed into a serializable AST.
 * For example, references to aliases are literals.
 */
public class LiteralNode extends AstNode {

	/**
	 * Type of this literal.
	 */
	private final Class<?> type;
	
	/**
	 * Unparsed literal text.
	 */
	private final String text;
	
	/**
	 * Return types wanted.
	 */
	private final Class<?>[] returnTypes;
	
	public LiteralNode(Class<?> type, String text, Class<?>[] returnTypes) {
		this.type = type;
		this.text = text;
		this.returnTypes = returnTypes;
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public String getText() {
		return text;
	}
	
	public Class<?>[] getReturnTypes() {
		return returnTypes;
	}
}
