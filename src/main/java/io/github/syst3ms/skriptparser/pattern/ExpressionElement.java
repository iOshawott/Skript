package io.github.syst3ms.skriptparser.pattern;

import io.github.syst3ms.skriptparser.ast.AstNode;
import io.github.syst3ms.skriptparser.ast.LiteralNode;
import io.github.syst3ms.skriptparser.parsing.MatchContext;
import io.github.syst3ms.skriptparser.parsing.SyntaxParser;
import io.github.syst3ms.skriptparser.types.PatternType;
import io.github.syst3ms.skriptparser.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;

/**
 * A variable/expression, declared in syntax using {@literal %type%}
 * Has :
 * <ul>
 * <li>a {@link List} of {@link PatternType}</li>
 * <li>a field determining what type of values this expression accepts : literals, expressions or both ({@literal %*type%}, {@literal %~type%} and {@literal %type%} respectively)</li>
 * <li>whether the expression resorts to default expressions or not, defaulting to {@literal null} instead</li>
 * </ul>
 */
public class ExpressionElement implements PatternElement {
	private List<PatternType<?>> types;
	private Acceptance acceptance;
	private boolean nullable, acceptsConditional;
	
	public ExpressionElement(List<PatternType<?>> types, Acceptance acceptance, boolean nullable, boolean acceptsConditional) {
		this.types = types;
		this.acceptance = acceptance;
		this.nullable = nullable;
		this.acceptsConditional = acceptsConditional;
	}
	
	@Override
	public int match(String s, int index, MatchContext parser) {
		if (parser.getOriginalElement().equals(this))
			parser.advanceInPattern();
		PatternType<?>[] typeArray = types.toArray(new PatternType<?>[0]);
		assert typeArray != null;
		if (index >= s.length()) {
			return -1;
		}
		
		List<PatternElement> flattened = PatternElement.flatten(parser.getOriginalElement());
		// We look at what could possibly be after the expression in the current syntax
		List<PatternElement> possibleInputs = PatternElement.getPossibleInputs(flattened.subList(parser.getPatternIndex(), flattened.size()));
		for (PatternElement possibleInput : possibleInputs) {  // We iterate over those possibilities
			if (possibleInput instanceof TextElement) {
				String text = ((TextElement) possibleInput).getText();
				if (text.isEmpty())
					continue;
				if (text.equals("\0")) { // There is nothing after this; end of line reached
					if (index == 0)
						return -1;
					
					// Take rest of line and attempt to parse it
					String toParse = s.substring(index).trim();
					assert toParse != null;
					AstNode expression = parse(toParse, typeArray, parser.getSyntaxParser());
					if (expression != null) {
						parser.addInput(expression);
						return index + toParse.length();
					}
					
					return -1; // Parsing failed, and we have nothing more to parse
				}
				
				/*
				 * 1. Find next occurrence of text element.
				 * 2. Try to parse between current index and start of it.
				 * 3. If it failed, repeat.
				 */
				int i = StringUtils.indexOfIgnoreCase(s, text, index);
				while (i != -1) {
					String toParse = s.substring(index, i).trim();
					assert toParse != null;
					AstNode expression = parse(toParse, typeArray, parser.getSyntaxParser());
					if (expression != null) {
						parser.addInput(expression);
						return index + toParse.length();
					}
					i = StringUtils.indexOfIgnoreCase(s, text, i + 1);
				}
				// All occurrences checked, parsing failed; try next possible input
			} else if (possibleInput instanceof RegexGroup) {
				// Create matcher against rest of input string
				// TODO check possible index out of bounds here
				Matcher m = ((RegexGroup) possibleInput).getPattern().matcher(s).region(index, s.length());
				while (m.lookingAt()) { // Go through all matches
					int i = m.start();
					if (i == -1) {
						continue;
					}
					
					// Parse between current index and start of match
					String toParse = s.substring(index, i);
					if (toParse.length() == parser.getOriginalPattern().length())
						continue;
					AstNode expression = parse(toParse, typeArray, parser.getSyntaxParser());
					if (expression != null) {
						parser.addInput(expression);
						return index + toParse.length();
					}
				}
				// No more matches, parsing failed; try next possible input
			} else {
				assert possibleInput instanceof ExpressionElement;
				// Next element is expression, like this one it has many possible inputs
				List<PatternElement> nextPossibleInputs = PatternElement.getPossibleInputs(flattened.subList(parser.getPatternIndex() + 1, flattened.size()));
				if (nextPossibleInputs.stream().anyMatch(pe -> !(pe instanceof TextElement))) {
					// Expression element after this has non-text inputs
					// This means we can't determine end of this expression efficiently
					// TODO but inefficient parsing COULD be implemented
					continue; // Ambiguous, don't parse
				}
				
				// Go through possible text elements
				for (PatternElement nextPossibleInput : nextPossibleInputs) {
					String text = ((TextElement) nextPossibleInput).getText();
					
					// TODO why this parsing routine is different from normal TextElement one?
					if (text.equals("")) {
						String rest = s.substring(index);
						assert rest != null;
						List<String> splits = splitAtSpaces(rest);
						for (String split : splits) {
							assert split != null;
							int i = StringUtils.indexOfIgnoreCase(s, split, index);
							if (i != -1) {
								String toParse = s.substring(index, i);
								assert toParse != null;
								AstNode expression = parse(toParse, typeArray, parser.getSyntaxParser());
								if (expression != null) {
									parser.addInput(expression);
									return index + toParse.length();
								}
							}
						}
						return -1;
					} else {
						int bound = StringUtils.indexOfIgnoreCase(s, text, index);
						if (bound == -1) {
							continue;
						}
						String rest = s.substring(index, bound);
						List<String> splits = splitAtSpaces(rest);
						assert rest != null;
						for (String split : splits) {
							assert split != null;
							int i = StringUtils.indexOfIgnoreCase(s, split, index);
							if (i != -1) {
								String toParse = s.substring(index, i);
								assert toParse != null;
								AstNode expression = parse(toParse, typeArray, parser.getSyntaxParser());
								if (expression != null) {
									parser.addInput(expression);
									return index + toParse.length();
								}
							}
						}
					}
				}
			}
		}
		return -1;
	}
	
	private static List<String> splitAtSpaces(String s) {
		List<String> splitted = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		char[] charArray = s.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			char c = charArray[i];
			if (c == ' ') {
				if (sb.length() > 0) {
					splitted.add(sb.toString());
					sb.setLength(0);
				}
			} else if (c == '(') {
				String enclosed = StringUtils.getEnclosedText(s, '(', ')', i);
				if (enclosed == null) {
					sb.append('(');
					continue;
				}
				sb.append('(').append(enclosed).append(')');
				i += enclosed.length() + 1;
			} else {
				sb.append(c);
			}
		}
		if (sb.length() > 0) {
			splitted.add(sb.toString());
		}
		return splitted;
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	private <T> AstNode parse(String s, PatternType<?>[] types, SyntaxParser parser) {
		for (PatternType<?> type : types) {
			assert type != null;
			AstNode expression;
			// TODO Skript doesn't really have boolean expressions; sort this out and enable this code
//			if (type.equals(SyntaxParser.BOOLEAN_PATTERN_TYPE)) {
//				// NOTE : conditions call parseBooleanExpression straight away
//				expression = (Expression<? extends T>) SyntaxParser.parseBooleanExpression(
//						s,
//						acceptsConditional ? SyntaxParser.MAYBE_CONDITIONAL : SyntaxParser.NOT_CONDITIONAL
//				);
//			} else {
			expression = parser.parseExpression(s, (PatternType<T>) type);
//			}
			if (expression == null)
				continue;
			switch (acceptance) {
				case ALL:
					break;
				case EXPRESSIONS_ONLY:
					if (expression instanceof LiteralNode) {
						Skript.error("Only expressions are allowed, found literal " + s);
						return null;
					}
					break;
				case LITERALS_ONLY:
					if (!(expression instanceof LiteralNode)) {
						Skript.error("Only literals are allowed, found expression " + s);
						return null;
					}
					break;
				case VARIABLES_ONLY:
					throw new UnsupportedOperationException("VARIABLES_ONLY");
//					if (!expression.getType().equals(Variable.class)) {
//						Skript.error("Only variables are allowed, found " + s);
//						return null;
//					}
//					break;
			}
			return expression;
		}
		return null;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(acceptance, acceptsConditional, types);
	}
	
	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionElement other = (ExpressionElement) obj;
		return acceptance == other.acceptance && acceptsConditional == other.acceptsConditional && Objects.equals(types, other.types);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("%");
		if (nullable)
			sb.append('-');
		switch (acceptance) {
			case ALL:
				break;
			case EXPRESSIONS_ONLY:
				sb.append('~');
				break;
			case LITERALS_ONLY:
				sb.append('*');
				break;
			case VARIABLES_ONLY:
				sb.append('^');
				break;
		}
		if (acceptsConditional)
			sb.append('=');
		sb.append(
				String.join(
						"/",
						types.stream().map(PatternType::toString).toArray(CharSequence[]::new)
						)
				);
		return sb.append("%").toString();
	}
	
	/**
	 * What kinds of inputs an expression element accepts.
	 */
	public enum Acceptance {
		/**
		 * Everything goes.
		 */
		ALL,
		
		/**
		 * Only expressions are accepted.
		 */
		EXPRESSIONS_ONLY,
		
		/**
		 * Only literals are accepted.
		 */
		LITERALS_ONLY,
		
		/**
		 * Only variables are accepted.
		 */
		VARIABLES_ONLY
	}
}
