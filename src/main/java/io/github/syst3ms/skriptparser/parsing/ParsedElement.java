package io.github.syst3ms.skriptparser.parsing;


/**
 * A syntax element that has been parsed but not yet loaded.
 */
public class ParsedElement {
	
	/**
	 * Input parsed elements.
	 */
	private final ParsedElement[] inputs;
	
	/**
	 * Context for parsing.
	 */
	private final ParseContext context;
	
	/**
	 * If this element is a literal.
	 */
	private final boolean isLiteral;
	
	/**
	 * If this element is a variable.
	 */
	private final boolean isVariable;

	public ParsedElement(ParsedElement[] inputs, ParseContext context, boolean isLiteral, boolean isVariable) {
		this.inputs = inputs;
		this.context = context;
		this.isLiteral = isLiteral;
		this.isVariable = isVariable;
	}
	
	public ParsedElement[] getInputs() {
		return inputs;
	}
	
	public ParseContext getContext() {
		return context;
	}
	
	public boolean isLiteral() {
		return isLiteral;
	}
	
	public boolean isVariable() {
		return isVariable;
	}
	
}
