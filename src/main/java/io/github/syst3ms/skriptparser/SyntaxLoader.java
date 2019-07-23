package io.github.syst3ms.skriptparser;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleLiteral;
import io.github.syst3ms.skriptparser.ast.AstNode;
import io.github.syst3ms.skriptparser.ast.ExpressionNode;
import io.github.syst3ms.skriptparser.ast.LiteralNode;

/**
 * Loads AST nodes.
 */
public class SyntaxLoader {
	
	private final SkriptRegistry registry;
	
	public Expression<?> loadExpression(AstNode node) {
		if (node instanceof LiteralNode) {
			return loadLiteral((LiteralNode) node);
		} else {
			
		}
	}
	
	private Expression<?> loadLiteral(LiteralNode node) {
		if (node.getType().equals(Variable.class)) {
			Variable<?> var = Variable.newInstance(node.getText(), node.getReturnTypes());
			assert var != null : "invalid AST";
			return var;
		} else {
			ClassInfo<?> info = registry.getTypes().get(node.getType());
			Parser<?> literalParser = info.getParser();
			Class<?> expectedClass = node.getReturnTypes()[0]; // TODO multiple types?
			
			if (literalParser != null) {
				// TODO is this correct parse mode?
				Object literal = literalParser.parse(node.getText(), ch.njol.skript.lang.ParseContext.DEFAULT);
				if (literal != null && expectedClass.isAssignableFrom(info.getC())) {
					return new SimpleLiteral<>(literal, false);
				} else if (literal != null) {
					Expression<?> converted = new SimpleLiteral<>(literal, false).getConvertedExpression(expectedClass);
					assert converted != null : "AST: converter exists, but can't create converted expression";
					return converted;
				}
			} else if (expectedClass == String.class || info.getC() == String.class) {
				// TODO ensure quotes are actually required; skript-parser string handling is different
				VariableString str = VariableString.newInstance(node.getText());
				assert str != null : "AST: found invalid string";
				return str;
			}
		}
		throw new AssertionError("AST: loading literal failed");
	}
	
	private Expression<?> loadPlainExpression(ExpressionNode node) {
		ExpressionInfo<?, ?> info = registry.getExpressions().get(node.getType());
		// TODO
	}
}
