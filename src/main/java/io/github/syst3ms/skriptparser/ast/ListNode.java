package io.github.syst3ms.skriptparser.ast;


/**
 * Represents a list literal, i.e. a comma-separated list in a script.
 */
public class ListNode extends AstNode {
	
	/**
	 * If all members are literals.
	 */
	private final boolean isLiteralList;
	
	/**
	 * If this list is AND, not OR list.
	 */
	private final boolean isAndList;

	/**
	 * Members of the list.
	 */
	private final AstNode[] members;
	
	public ListNode(String original, Class<?> returnType, boolean isLiteralList, boolean isAndList, AstNode[] members) {
		super(original, returnType, false);
		this.isLiteralList = isLiteralList;
		this.isAndList = isAndList;
		this.members = members;
	}
	
	public boolean isLiteralList() {
		return isLiteralList;
	}
	
	public boolean isAndList() {
		return isAndList;
	}

	public AstNode[] getMembers() {
		return members;
	}
}
