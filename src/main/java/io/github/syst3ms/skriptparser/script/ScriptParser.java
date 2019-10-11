package io.github.syst3ms.skriptparser.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.config.Config;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.ScopeInfo;
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
			final AstNode event;
			final SectionNode code;
			
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
		// Parse trigger contents
		List<ParsedScript.Trigger> triggers = new ArrayList<>(symbols.triggers.size());
		for (SymbolTable.Trigger t : symbols.triggers) {
			ParsedScript.Statement[] statements = parseScope(t.code, syntaxParser);
			if (statements == null) {
				continue; // Error while parsing trigger
			}
			triggers.add(new ParsedScript.Trigger(t.event, statements));
		}
		return new ParsedScript(triggers);
	}
	
	@Nullable
	private ParsedScript.Statement[] parseScope(SectionNode root, SyntaxParser parser) {
		ParsedScript.Statement[] statements = new ParsedScript.Statement[root.size()];
		int i = 0;
		for (Node line : root) {
			String code = line.getKey();
			if (code == null) {
				continue;
			}
			
			if (line instanceof SectionNode) { // Looks like scope
				AstNode node = syntaxParser.parseScope(code);
				if (node == null) {
					return null; // Statement has errors
				}
				
				ScopeInfo<?> source = (ScopeInfo<?>) node.getSource();
				assert source != null : "AST: scope source unavailable";
				SyntaxParser contentParser = source.parserOverride != null ? source.parserOverride : parser;
				assert contentParser != null;
				ParsedScript.Statement[] contents = parseScope((SectionNode) line, contentParser);
				if (contents == null) {
					return null; // Inner scope has errors
				}
				statements[i++] = new ParsedScript.ScopeStatement(node, contents);
			} else { // Just normal statement
				AstNode node = syntaxParser.parseStatement(code);
				if (node == null) {
					return null; // Statement has errors
				}
				statements[i++] = new ParsedScript.SimpleStatement(node);
			}
		}
		return statements;
	}
}
