package io.github.syst3ms.skriptparser.parsing;

import io.github.syst3ms.skriptparser.SkriptRegistry;
import io.github.syst3ms.skriptparser.ast.AstNode;
import io.github.syst3ms.skriptparser.ast.ExpressionNode;
import io.github.syst3ms.skriptparser.ast.LiteralNode;
import io.github.syst3ms.skriptparser.event.TriggerContext;
import io.github.syst3ms.skriptparser.pattern.PatternElement;
import io.github.syst3ms.skriptparser.types.PatternType;
import io.github.syst3ms.skriptparser.util.ClassUtils;
import io.github.syst3ms.skriptparser.util.StringUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralList;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.Statement;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Converters;
import ch.njol.util.Kleenean;

/**
 * Parses syntax elements from registry that it was created with.
 */
public class SyntaxParser {
	/**
	 * Tells {@link #parseBooleanExpression(String, int, SkriptLogger)} to only return expressions that are not conditional
	 * @see #parseBooleanExpression(String, int, SkriptLogger)
	 */
	public static final int NOT_CONDITIONAL = 0;
	/**
	 * Tells {@link #parseBooleanExpression(String, int, SkriptLogger)} to return any expressions, conditional or not
	 * @see #parseBooleanExpression(String, int, SkriptLogger)
	 */
	public static final int MAYBE_CONDITIONAL = 1;
	/**
	 * Tells {@link #parseBooleanExpression(String, int, SkriptLogger)} to only return conditional expressions
	 * @see #parseBooleanExpression(String, int, SkriptLogger)
	 */
	public static final int CONDITIONAL = 2;
	
	@SuppressWarnings("null")
	public static final Pattern LIST_SPLIT_PATTERN = Pattern.compile("\\s*(,)\\s*|\\s+(and|or)\\s+", Pattern.CASE_INSENSITIVE);
	
	private final SkriptRegistry registry;
	
	/**
	 * The pattern type representing {@link Boolean}
	 */
	@SuppressWarnings("null")
	public final PatternType<Boolean> booleanPatternType;
	
	/**
	 * The pattern type representing {@link Object}
	 */
	@SuppressWarnings("null")
	public final PatternType<Object> objectPatternType;
	
	@SuppressWarnings("unchecked")
	private Class<? extends TriggerContext>[] currentContexts = new Class[]{};
	
	@SuppressWarnings("unchecked")
	public SyntaxParser(SkriptRegistry registry) {
		this.registry = registry;
		this.booleanPatternType = new PatternType<>((ClassInfo<Boolean>) registry.getTypes().get(Boolean.class), true);
		this.objectPatternType = new PatternType<>((ClassInfo<Object>) registry.getTypes().get(Object.class), true);
	}
	
	/**
	 * Parses an {@link Expression} from the given {@linkplain String} and {@link PatternType expected return type}
	 * @param <T> the type of the expression
	 * @param s the string to be parsed as an expression
	 * @param expectedType the expected return type
	 * @return an expression that was successfully parsed, or {@literal null} if the string is empty,
	 * no match was found
	 * or for another reason detailed in an error message.
	 */
	@Nullable
	public <T> AstNode parseExpression(String s, PatternType<T> expectedType) {
		if (s.isEmpty())
			return null;
		
		// Remove brackets if they exist
		if (s.startsWith("(") && s.endsWith(")") && StringUtils.findClosingIndex(s, '(', ')', 0) == s.length() - 1) {
			s = s.substring(1, s.length() - 1);
		}
		
		// Check if this is literal, i.e. known compile-time
		LiteralNode literal = parseLiteral(s, expectedType);
		if (literal != null) {
			return literal;
		}
		
		// Might be a variable here
		Variable<? extends T> variable = Variable.newInstance(s, new Class[] {expectedType.getType().getC()});
		if (variable != null) { // TODO check for variable syntax without making a variable
			return new LiteralNode(Variable.class, s);
			
			// TODO move to loader
			if (!variable.isSingle() && expectedType.isSingle()) {
				Skript.error("A single value was expected, but " + s + " represents multiple values.");
				return null;
			}
			return variable;
		}
		
		// A list literal is could be here; try to parse it
		if (!expectedType.isSingle()) {
			AstNode listLiteral = parseListLiteral(s, expectedType);
			if (listLiteral != null) {
				return listLiteral;
			}
		}
		
		
		// Test all expressions that might return this type
		Iterator<ExpressionInfo<?, ?>> it = registry.getExpressions(expectedType.getType().getC());
		while (it.hasNext()) {
			ExpressionInfo<?, ?> info = it.next();
			assert info != null;
			AstNode expr = matchExpressionInfo(s, info, expectedType);
			if (expr != null) {
				return expr;
			}
		}
		Skript.error("No expression matching ''" + s + "' was found");
		return null;
	}
	
	/**
	 * Parses a line of code as an {@link Condition}
	 * @param s the line to be parsed
	 * @return an condition that was successfully parsed, or {@literal null} if the string is empty,
	 * no match was found
	 * or for another reason detailed in an error message
	 */
	@Nullable
	public AstNode parseCondition(String s) {
		if (s.isEmpty())
			return null;
		
		for (SyntaxElementInfo<?> info : registry.getConditions()) {
			assert info != null;
			AstNode cond = matchStatementInfo(s, info);
			if (cond != null) {
				return cond;
			}
		}
		return null;
	}
	
	@Nullable
	private <T> AstNode matchExpressionInfo(String s, ExpressionInfo<?, ?> info, PatternType<T> expectedType) {
		PatternElement[] patterns = info.getCompiledPatterns();
		Class<?> infoTypeClass = info.returnType;
		Class<T> expectedTypeClass = expectedType.getType().getC();
		if (!expectedTypeClass.isAssignableFrom(infoTypeClass) && !Converters.converterExists(infoTypeClass, expectedTypeClass))
			return null; // Would need to convert, but we definitely can't do that
		for (int i = 0; i < patterns.length; i++) {
			PatternElement element = patterns[i];
			MatchContext parser = new MatchContext(this, element, currentContexts, i);
			if (element.match(s, 0, parser) != -1) {
				List<AstNode> inputs = parser.getInputs();
				return new ExpressionNode(info.c, parser.toParseResult(), inputs.toArray(new AstNode[inputs.size()]));
				
				// TODO move most of below to loader
				try {
					Expression<? extends T> expression = (Expression<? extends T>) info.c.newInstance();
					if (!expression.init(parser.getParsedExpressions().toArray(new Expression[0]), i,
							Kleenean.UNKNOWN, parser.toParseResult())) {
						continue; // Didn't match this pattern, but try next one...
					}
					
					// Wrap expression with a converter if needed
					Class<?> expressionReturnType = expression.getReturnType();
					if (!expectedTypeClass.isAssignableFrom(expressionReturnType)) {
						Expression<?> converted = expression.getConvertedExpression(expectedTypeClass);
						if (converted != null) { // It worked, we got converted type
							return (Expression<? extends T>) converted;
						} else {
							ClassInfo<?> type = registry.getTypes().get(expressionReturnType);
							assert type != null;
							Skript.error(StringUtils.withIndefiniteArticle(expectedType.toString(), false) +
									" was expected, but " +
											StringUtils.withIndefiniteArticle(type.toString(), false) +
									" was found");
							return null;
						}
					}
					if (!expression.isSingle() &&
							expectedType.isSingle()) {
						Skript.error("A single value was expected, but " + s + " represents multiple values.");
						continue;
					}
					return expression;
				} catch (InstantiationException | IllegalAccessException e) {
					Skript.exception("Couldn't instantiate class " + info.c.getName());
				}
			}
		}
		return null;
	}
	
	/**
	 * Parses a list literal expression (of the form {@code ..., ... and ...}) from the given {@linkplain String}  and {@link PatternType expected return type}
	 * @param <T> the type of the list literal
	 * @param s the string to be parsed as a list literal
	 * @param expectedType the expected return type (must be plural)
	 * @return a list literal that was successfully parsed, or {@literal null} if the string is empty,
	 * no match was found
	 * or for another reason detailed in an error message.
	 */
	@Nullable
	public <T> AstNode parseListLiteral(String s, PatternType<T> expectedType) {
		assert !expectedType.isSingle();
		if (!s.contains(",") && !s.contains("and") && !s.contains("nor") && !s.contains("or"))
			return null;
		List<String> parts = new ArrayList<>();
		Matcher m = LIST_SPLIT_PATTERN.matcher(s);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i = StringUtils.nextSimpleCharacterIndex(s, i + 1)) {
			char c = s.charAt(i);
			if (c == ' ' || c == ',') {
				m.region(i, s.length());
				if (m.lookingAt()) {
					if (sb.length() == 0)
						return null;
					parts.add(sb.toString());
					parts.add(m.group());
					sb.setLength(0);
					i = m.end() - 1;
					continue;
				}
			} else if (c == '(') {
				String closing = StringUtils.getEnclosedText(s, '(', ')', i);
				if (closing != null) {
					int endIndex = i + closing.length() + 1;
					sb.append("(").append(s, i + 1, endIndex).append(")");
					i = endIndex;
					continue;
				}
			}
			sb.append(c);
		}
		if (sb.length() > 0)
			parts.add(sb.toString());
		if (parts.size() == 1)
			return null;
		Boolean isAndList = null; // Hello nullable booleans, it had been a pleasure NOT using you
		for (int i = 0; i < parts.size(); i++) {
			if ((i & 1) == 1) { // Odd index == separator
				String separator = parts.get(i).trim();
				if (separator.equalsIgnoreCase("and") || separator.equalsIgnoreCase("nor")) {
					isAndList = true;
				} else if (separator.equalsIgnoreCase("or")) {
					isAndList = isAndList != null && isAndList;
				}
			}
		}
		isAndList = isAndList == null || isAndList; // Defaults to true
		List<AstNode> expressions = new ArrayList<>();
		boolean isLiteralList = true;
		for (int i = 0; i < parts.size(); i++) {
			if ((i & 1) == 0) { // Even index == element
				String part = parts.get(i);
				AstNode expression = parseExpression(part, expectedType);
				if (expression == null) {
					return null;
				}
				isLiteralList &= expression instanceof Literal;
				expressions.add(expression);
			}
		}
		if (expressions.size() == 1)
			return expressions.get(0);
		
		// TODO implement literal list AST node
		if (isLiteralList) {
			Literal<T>[] literals = expressions.toArray(new Literal[0]);
			assert literals != null;
			@SuppressWarnings("unchecked")
			Class<T> returnType = (Class<T>) ClassUtils.getCommonSuperclass(Arrays.stream(literals).map(Literal::getReturnType).toArray(Class[]::new));
			return new LiteralList<>(literals, returnType, isAndList);
		} else {
			Expression<T>[] exprs = expressions.toArray(new Expression[0]);
			assert exprs != null;
			@SuppressWarnings("unchecked")
			Class<T> returnType = (Class<T>) ClassUtils.getCommonSuperclass(Arrays.stream(exprs).map(Expression::getReturnType).toArray(Class[]::new));
			return new ExpressionList<>(exprs, returnType, isAndList);
		}
	}
	
	/**
	 * Parses a literal of a given {@link PatternType type} from the given {@linkplain String}
	 * @param <T> the type of the literal
	 * @param s the string to be parsed as a literal
	 * @param expectedType the expected return type
	 * @return a literal that was successfully parsed, or {@literal null} if the string is empty,
	 * no match was found
	 * or for another reason detailed in an error message.
	 */
	@Nullable
	public <T> LiteralNode parseLiteral(String s, PatternType<T> expectedType) {
		for (ClassInfo<?> info : registry.getTypes()) {
			Class<?> c = info.getC();
			
			Class<? extends T> expectedClass = expectedType.getType().getC();
			if (expectedClass.isAssignableFrom(c) || Converters.converterExists(c, expectedClass)) {
				return new LiteralNode(c, s);
				
				// TODO move to loader
				Parser<?> literalParser = info.getParser();
				if (literalParser != null) {
					// TODO is this correct parse mode?
					T literal = (T) literalParser.parse(s, ch.njol.skript.lang.ParseContext.DEFAULT);
					if (literal != null && expectedClass.isAssignableFrom(c)) {
						return new SimpleLiteral<>(literal, false);
					} else if (literal != null) {
						return new SimpleLiteral<>(literal, false).getConvertedExpression(expectedType.getType().getC());
					}
				} else if (expectedClass == String.class || c == String.class) {
					VariableString vs = VariableString.newInstance(s);
					if (vs != null) {
						return (Expression<? extends T>) vs;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Parses a line of code as an {@link Effect}
	 * @param s the line to be parsed
	 * @return an effect that was successfully parsed, or {@literal null} if the string is empty,
	 * no match was found
	 * or for another reason detailed in an error message
	 */
	@Nullable
	public AstNode parseEffect(String s) {
		if (s.isEmpty())
			return null;

		// Go through all effects (they have no return types, obviously)
		for (SyntaxElementInfo<?> effect : registry.getEffects()) {
			assert effect != null;
			AstNode eff = matchStatementInfo(s, effect);
			if (eff != null) {
				return eff;
			}
		}
		Skript.error("No effect matching '" + s + "' was found"); // TODO can't print here, inline condition parsing breaks
		return null;
	}
	
	/**
	 * Parses a line of code as a {@link Statement}, either an {@link Effect} or an {@link InlineCondition}
	 * @param s the line to be parsed
	 * @return a statement that was successfully parsed, or {@literal null} if the string is empty,
	 * no match was found
	 * or for another reason detailed in an error message
	 */
	@Nullable
	public AstNode parseStatement(String s) {
		if (s.isEmpty())
			return null;
		AstNode eff = parseEffect(s);
		if (eff == null) { // Might be an inline condition, then
			return parseCondition(s);
		}
		return eff;
	}
	
	/**
	 * Parses the given string as an event.
	 * @param s String to be parsed.
	 * @return a trigger that was successfully parsed, or {@literal null} if
	 * the section is empty, no match was found or for another reason
	 * detailed in an error message
	 */
	@Nullable
	public AstNode parseEvent(String s) {
		if (s.isEmpty())
			return null;
		
		// Test against all events
		for (SkriptEventInfo<?> info : registry.getEvents()) {
			assert info != null;
			AstNode event = matchStatementInfo(s, info);
			if (event != null) {
				return event;
			}
		}
		Skript.error("No event matching '" + s + "' was found");
		return null;
	}
	
	@Nullable
	private AstNode matchStatementInfo(String s, SyntaxElementInfo<?> info) {
		PatternElement[] patterns = info.getCompiledPatterns();
		for (int i = 0; i < patterns.length; i++) {
			PatternElement element = patterns[i];
			MatchContext parser = new MatchContext(this, element, currentContexts, i);
			if (element.match(s, 0, parser) != -1) {
				List<AstNode> inputs = parser.getInputs();
				return new ExpressionNode(info.c, parser.toParseResult(), inputs.toArray(new AstNode[inputs.size()]));
				
				// TODO move to loader
				try {
					SyntaxElement syntax = info.c.newInstance();
					if (!syntax.init(parser.getParsedExpressions().toArray(new Expression[0]), i,
							Kleenean.UNKNOWN, parser.toParseResult())) {
						continue;
					}
					return syntax;
				} catch (InstantiationException | IllegalAccessException e) {
					Skript.exception(e, "Couldn't instantiate class " + info.c);
				}
			}
		}
		return null;
	}
}
