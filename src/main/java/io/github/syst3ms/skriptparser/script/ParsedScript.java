package io.github.syst3ms.skriptparser.script;

import ch.njol.skript.lang.Statement;
import io.github.syst3ms.skriptparser.ast.AstNode;

/**
 * A parsed, but not loaded script.
 */
public class ParsedScript {
	
	public static abstract class Statement {
		
	}
	
	public static class SimpleStatement extends Statement {
		
		AstNode node;
	}
	
	public static class ScopeStatement extends Statement {
		
		AstNode scope;
		
		Statement[] statements;
	}
	
	public static class Trigger {
		
		AstNode event;
		
		Statement[] statements;
	}
	
	private final Trigger[] triggers;
	
	public ParsedScript(Trigger[] triggers) {
		this.triggers = triggers;
	}
}
