package io.github.syst3ms.skriptparser.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.njol.skript.config.Config;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.function.Namespace;
import io.github.syst3ms.skriptparser.ast.AstNode;
import io.github.syst3ms.skriptparser.parsing.SyntaxParser;

/**
 * Parses scripts.
 */
public class ScriptParser {
	
	private final SyntaxParser syntaxParser;
	
	public ScriptParser(SyntaxParser syntaxParser) {
		this.syntaxParser = syntaxParser;
	}
	
	/**
	 * Contains symbols from script mapped to unparsed scopes.
	 */
	public static class SymbolTable {
		
		private static class Trigger {
			private final AstNode event;
			private final SectionNode code;
			
			public Trigger(AstNode event, SectionNode code) {
				this.event = event;
				this.code = code;
			}
		}

		/**
		 * Defined options. They are replaced with their values in other
		 * parts of script code.
		 */
		Map<String, String> options;
		
		/**
		 * Function namespace. Signatures are stored here.
		 */
		Namespace functions;
		
		/**
		 * Triggers found in script.
		 */
		List<Trigger> triggers;
		
		SymbolTable() {
			this.options = new HashMap<>();
			this.functions = new Namespace();
			this.triggers = new ArrayList<>();
		}
		
		String replaceOptions(String input) {
			throw new UnsupportedOperationException("TODO");
		}
		
		// TODO commands
	}
	
	public SymbolTable parseSymbols(SectionNode root) {
		SymbolTable symbols = new SymbolTable();
		
		// Parse options first, because they may be used everywhere
		Node options = root.get("options");
		if (options instanceof SectionNode) {
			for (Node node : ((SectionNode) options)) {
				if (!(node instanceof EntryNode)) {
					// TODO show error
					continue;
				}
				symbols.options.put(node.getKey(), ((EntryNode) node).getValue());
			}
		}
		
		// Go through other nodes under root (events, functions, commands, ...)
		for (Node node : root) {
			if (!(node instanceof SectionNode)) {
				// TODO show error
			}
			SectionNode section = (SectionNode) node;
			String event = section.getKey();
			if (event == null)
				continue;
			event = symbols.replaceOptions(event);
			
			if (event.equals("options")) {
				continue; // Handled earlier
			} else if (event.startsWith("function")) {
				// TODO parse signatures without loading them
			} else { // Normal event
				AstNode parsedEvent = syntaxParser.parseEvent(event);
				if (parsedEvent != null) {
					symbols.triggers.add(new SymbolTable.Trigger(parsedEvent, section));
				}
			}
			
		}
		
		return symbols;
	}
	
	public ParsedScript parseCode(SymbolTable symbols) {
		List<ParsedScript.Trigger> triggers = new ArrayList<>();
		for (SymbolTable.Trigger t : symbols.triggers) {
			
		}
		// TODO
	}
}
