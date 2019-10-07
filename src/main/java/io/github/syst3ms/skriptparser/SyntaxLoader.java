package io.github.syst3ms.skriptparser;

import java.util.Arrays;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralList;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import io.github.syst3ms.skriptparser.ast.AstNode;
import io.github.syst3ms.skriptparser.ast.ExpressionNode;
import io.github.syst3ms.skriptparser.ast.ListNode;
import io.github.syst3ms.skriptparser.ast.LiteralNode;
import io.github.syst3ms.skriptparser.parsing.MatchContext;
import io.github.syst3ms.skriptparser.parsing.ParseContext;
import io.github.syst3ms.skriptparser.util.ClassUtils;
import io.github.syst3ms.skriptparser.util.StringUtils;

/**
 * Loads AST nodes.
 */
public class SyntaxLoader {
	
	private final SkriptRegistry registry;
	
	public SyntaxLoader(SkriptRegistry registry) {
		this.registry = registry;
	}
	
	/**
	 * Loads an AST node. This might produce errors or warnings if the nodes
	 * contain errors that could not be detected parse-time. Additionally,
	 * errors may be produced in rare cases where state of Skript has changed
	 * significantly after successfully parsing to AST.
	 * @param node AST node to load.
	 * @return An expression, or null if something went wrong when loading it.
	 * Errors have been printed to logger if null is returned.
	 */
	@Nullable
	public Expression<?> loadNode(AstNode node) {
		if (node instanceof LiteralNode) {
			return loadLiteral((LiteralNode) node);
		} else if (node instanceof ExpressionNode) {
			return loadExpression((ExpressionNode) node);
		} else {
			assert node instanceof ListNode : "AST: unknown node type; can't load";
			return loadList((ListNode) node);
		}
	}
	
	private Expression<?> loadLiteral(LiteralNode node) {
		if (node.isVariable()) {
			Variable<?> var = Variable.newInstance(node.getText(), new Class[] {node.getType()});
			assert var != null : "invalid AST";
			return var;
		} else {
			Class<?> expectedClass = node.getReturnType();

			ClassInfo<?> info = registry.getTypes().get(node.getType());
			Parser<?> literalParser = info.getParser();
			assert literalParser != null : "AST: type doesn't have literals";
			
			// TODO is this correct parse mode?
			Object literal = literalParser.parse(node.getText(), ch.njol.skript.lang.ParseContext.DEFAULT);
			if (literal != null && expectedClass.isAssignableFrom(info.getC())) {
				return new SimpleLiteral<>(literal, false);
			} else if (literal != null) {
				Expression<?> converted = new SimpleLiteral<>(literal, false).getConvertedExpression(expectedClass);
				assert converted != null : "AST: converter exists, but can't create converted expression";
				return converted;
			}
		}
		throw new AssertionError("AST: loading literal failed");
	}
	
	@Nullable
	private Expression<?> loadExpression(ExpressionNode node) {
		// Recursively load our inputs
		Expression<?>[] inputs = new Expression[node.getInputs().length];
		for (int i = 0; i < inputs.length; i++) {
			inputs[i] = loadNode(node.getInputs()[i]);
		}
		
		Class<?> expectedType = node.getReturnType();
		ParseContext context = node.getContext();
		ExpressionInfo<?, ?> info = registry.getExpressions().get(node.getType());
		try {
			// Initialize expression; allows individual syntaxes to check for errors
			Expression<?> expression = info.c.newInstance();
			if (!expression.init(inputs, context.getMatchedPattern(),
					Kleenean.UNKNOWN, context.toLegacyResult())) {
				return null; // Hopefully init() printed errors
			}
			
			// Wrap expression with a converter if needed
			Class<?> exprReturnType = expression.getReturnType();
			if (!expectedType.isAssignableFrom(exprReturnType)) {
				Expression<?> converted = expression.getConvertedExpression(expectedType);
				if (converted != null) { // It worked, we got converted type
					return converted;
				} else {
					ClassInfo<?> type = registry.getTypes().get(exprReturnType);
					assert type != null;
					Skript.error(StringUtils.withIndefiniteArticle(expectedType.toString(), false) +
							" was expected, but " +
									StringUtils.withIndefiniteArticle(type.toString(), false) +
							" was found");
					return null;
				}
			}
			if (!expression.isSingle() && node.isSingle()) {
				// TODO original string in AST nodes
				Skript.error("A single value was expected, but " + node.getOriginal() + " represents multiple values.");
				return null; // Note: unlike skript-parser does, treat this as fatal parse error
			}
			return expression;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new Error(e);
		}
	}
	
	public ExpressionList<?> loadList(ListNode node) {
		// TODO fix generic type safety
		if (node.isLiteralList()) {
			Literal<?>[] members = new Literal[node.getMembers().length];
			for (int i = 0; i < members.length; i++) {
				members[i] = (Literal<?>) loadNode(node.getMembers()[i]);
			}
			assert members != null;
			return new LiteralList(members, node.getReturnType(), node.isAndList());
		} else {
			Expression<?>[] members = new Expression[node.getMembers().length];
			for (int i = 0; i < members.length; i++) {
				members[i] = loadNode(node.getMembers()[i]);
			}
			assert members != null;
			return new ExpressionList(members, node.getReturnType(), node.isAndList());
		}
	}
}
