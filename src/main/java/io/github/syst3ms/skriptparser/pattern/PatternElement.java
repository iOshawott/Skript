package io.github.syst3ms.skriptparser.pattern;

import io.github.syst3ms.skriptparser.parsing.MatchContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The superclass of all elements of a pattern.
 */
public interface PatternElement {
	
	/**
	 * Attempts to match the {@link PatternElement} to a string at a specified index.
	 * About the index, make sure to never increment the index by some arbitrary value when returning
	 *
	 * @param s the string to match this PatternElement against
	 * @param index the index of the string at which this PatternElement should be matched
	 * @return the index at which the matching should continue afterwards if successful. Otherwise, {@literal -1}
	 */
	int match(String s, int index, MatchContext parser);
	
	/**
	 * Gets pattern elements the given element consists of.
	 * @param element Pattern element to flatten.
	 * @return The elements that form given element.
	 */
	static List<PatternElement> flatten(PatternElement element) {
		if (element instanceof CompoundElement) {
			return ((CompoundElement) element).getElements();
		} else {
			return Collections.singletonList(element);
		}
	}
	
	static List<PatternElement> getPossibleInputs(List<PatternElement> elements) {
		List<PatternElement> possibilities = new ArrayList<>();
		for (PatternElement element : elements) {
			if (element instanceof TextElement || element instanceof RegexGroup) {
				if (element instanceof TextElement) {
					String text = ((TextElement) element).getText();
					
					// If empty or blank text element is only element, everything matches
					if (text.isEmpty() || text.matches("\\s*") && elements.size() == 1) {
						return possibilities;
					} else if (text.matches("\\s*")) {
						continue; // Ignore
					}
				}
				
				// One regex or text input is possible here
				possibilities.add(element);
				return possibilities;
			} else if (element instanceof ChoiceGroup) {
				// Record all possible inputs of all choices
				for (ChoiceElement choice : ((ChoiceGroup) element).getChoices()) {
					List<PatternElement> possibleInputs = getPossibleInputs(flatten(choice.getElement()));
					possibilities.addAll(possibleInputs);
				}
				return possibilities;
			} else if (element instanceof ExpressionElement) {
				possibilities.add(element); // Input is an expression
				return possibilities;
			} else if (element instanceof OptionalGroup) {
				// Everything that is in optional group is POSSIBLE input
				possibilities.addAll(getPossibleInputs(flatten(((OptionalGroup) element).getElement())));
			}
		}
		
		// Reached end-of-line, signal that with NULL text element
		possibilities.add(new TextElement("\0"));
		return possibilities;
	}
}
