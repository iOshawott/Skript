package io.github.syst3ms.skriptparser;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import io.github.syst3ms.skriptparser.ast.AstNode;
import io.github.syst3ms.skriptparser.ast.ExpressionNode;
import io.github.syst3ms.skriptparser.ast.LiteralNode;
import io.github.syst3ms.skriptparser.parsing.MatchContext;
import io.github.syst3ms.skriptparser.parsing.ParseContext;
import io.github.syst3ms.skriptparser.util.StringUtils;

/**
 * Loads AST nodes.
 */
public class SyntaxLoader {
	
	private final SkriptRegistry registry;
	
	public Expression<?> loadExpression(AstNode node) {
		if (node instanceof LiteralNode) {
			return loadLiteral((LiteralNode) node);
		} else {
			return loadOnlyExpression((ExpressionNode) node);
		}
	}
	
	private Expression<?> loadLiteral(LiteralNode node) {
		if (node.isVariable()) {
			Variable<?> var = Variable.newInstance(node.getText(), node.getTypes());
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
			} // else: literal == null, will try next one
		}
		throw new AssertionError("AST: loading literal failed");
	}
	
	@Nullable
	private Expression<?> loadOnlyExpression(ExpressionNode node) {
		// Recursively load our inputs
		Expression<?>[] inputs = new Expression[node.getInputs().length];
		for (int i = 0; i < inputs.length; i++) {
			inputs[i] = loadExpression(node.getInputs()[i]);
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
				Skript.error("A single value was expected, but " + s + " represents multiple values.");
				continue;
			}
			return expression;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new Error(e);
		}
	}
}
