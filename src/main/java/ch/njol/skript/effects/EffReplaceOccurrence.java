/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Copyright 2011-2017 Peter Güttinger and contributors
 */
package ch.njol.skript.effects;

import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.Changer.ChangerUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;

@Name("Replace Occurrences")
@Description({"Replaces the first or last occurrence of a given text with another text.",
		"Please note that you can only change variables and a few expressions, e.g. a <a href='../expressions.html#ExprMessage'>message</a> or a line of a sign."})
@Examples({"on chat:",
		"\treplace first \"[item]\" in player's message with \"%player's tool%\" # Prevents Spam"})
@Since("INSERT VERSION")
public class EffReplaceOccurrence extends Effect {
	
	static {
		Skript.registerEffect(EffReplaceOccurrence.class,
				"replace first [(instance|occurrence) of] %strings% in %strings% with %string%",
				"replace first [(instance|occurrence) of] %strings% with %string% in %strings%");
	}
	
	@SuppressWarnings("null")
	private Expression<String> target, source, replacement;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		target = (Expression<String>) exprs[0];
		if (matchedPattern % 2 == 0) { // Format is target, source, replacement 
			source = (Expression<String>) exprs[1];
			replacement = (Expression<String>) exprs[2];
		} else { // Format is target, replacement, source 
			replacement = (Expression<String>) exprs[1];
			source = (Expression<String>) exprs[2];
		}
		if (!ChangerUtils.acceptsChange(source, ChangeMode.SET, String.class)) {
			Skript.error(source + " cannot be changed and can thus not have parts replaced.");
			return false;
		}
		return true;
	}

	@Override
	protected void execute(Event e) {
		String[] target = this.target.getAll(e);
		String[] source = this.source.getAll(e);
		String replacement = this.replacement.getSingle(e);
		for (int i = 0; i < source.length; i++)
			if (source[i] != null)
				for (String s2 : target)
					if (s2 != null)
						source[i] = source[i].replaceFirst(s2, replacement);
		this.source.change(e, source, ChangeMode.SET);
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "replace first " + target.toString(e, debug) + " with " + replacement.toString(e, debug) + " in " + source.toString(e, debug);
	}	
	
}
