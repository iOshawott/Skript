package io.github.syst3ms.skriptparser.ast;

import ch.njol.skript.lang.SyntaxElement;
import io.github.syst3ms.skriptparser.parsing.ParseContext;

/**
 * An AST node representing an expression. Note: in AST, statements and
 * conditions are treated as expressions.
 */
public class ExpressionNode extends AstNode {
	
	/**
	 * Type of the syntax element.
	 */
	private final Class<? extends SyntaxElement> type;
	
	/**
	 * Context under which this node was parsed.
	 */
	private final ParseContext context;
	
	/**
	 * Other nodes this node takes as a parameter.
	 */
	private final AstNode[] inputs;
	
	public ExpressionNode(String original, Class<?> returnType, boolean isSingle,
			Class<? extends SyntaxElement> type, ParseContext context, AstNode[] inputs) {
		super(original, returnType, isSingle);
		this.type = type;
		this.context = context;
		this.inputs = inputs;
	}
	
	public final Class<? extends SyntaxElement> getType() {
		return type;
	}
	
	public ParseContext getContext() {
		return context;
	}
	
	public AstNode[] getInputs() {
		return inputs;
	}
}
