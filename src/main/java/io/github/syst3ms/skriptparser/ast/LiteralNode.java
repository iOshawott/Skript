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
	
	private final boolean isVariable;
	
	public LiteralNode(Class<?> returnType, boolean isSingle,
			Class<?> type, String text, boolean isVariable) {
		super(text, returnType, isSingle);
		this.type = type;
		this.isVariable = isVariable;
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public String getText() {
		return getOriginal();
	}
	
	public boolean isVariable() {
		return isVariable;
	}
}
