package io.github.syst3ms.skriptparser.script;

import java.util.List;

import ch.njol.skript.lang.Statement;
import io.github.syst3ms.skriptparser.ast.AstNode;

/**
 * A parsed, but not loaded script.
 */
public class ParsedScript {
	
	public static abstract class Statement {
		
	}
	
	public static class SimpleStatement extends Statement {
		
		final AstNode node;
		
		public SimpleStatement(AstNode node) {
			this.node = node;
		}
	}
	
	public static class ScopeStatement extends Statement {
		
		final AstNode scope;
		
		final Statement[] statements;

		public ScopeStatement(AstNode scope, Statement[] statements) {
			this.scope = scope;
			this.statements = statements;
		}
		
	}
	
	public static class Trigger {
		
		final AstNode event;
		
		final Statement[] statements;

		public Trigger(AstNode event, Statement[] statements) {
			this.event = event;
			this.statements = statements;
		}
		
	}
	
	private final List<Trigger> triggers;
	
	public ParsedScript(List<Trigger> triggers) {
		this.triggers = triggers;
	}
}
