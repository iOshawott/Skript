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
	
	private final boolean isVariable;
	
	public LiteralNode(Class<?> returnType, boolean isSingle,
			Class<?> type, String text, boolean isVariable) {
		super(returnType, isSingle);
		this.type = type;
		this.text = text;
		this.isVariable = isVariable;
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public String getText() {
		return text;
	}
	
	public boolean isVariable() {
		return isVariable;
	}
}
