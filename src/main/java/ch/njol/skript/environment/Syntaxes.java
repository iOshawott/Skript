package ch.njol.skript.environment;

import org.bukkit.event.Event;

import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptEventInfo;
import io.github.syst3ms.skriptparser.SkriptRegistry;

/**
 * Allows registering new syntaxes to Skript.
 */
public interface Syntaxes {
	
	/**
	 * Registers a condition.
	 * @param <E> Type of class implementing the condition.
	 * @param condition Class that implements the condition.
	 * @param patterns Patterns that match the condition.
	 */
	<E extends Condition> void registerCondition(Class<E> condition, String... patterns);
	
	/**
	 * Registers an effect.
	 * @param <E> Type of class implementing the effect.
	 * @param effect Class that implements the effect.
	 * @param patterns Patterns that match the effect.
	 */
	<E extends Effect> void registerEffect(Class<E> effect, String... patterns);
	
	/**
	 * Registers an expression.
	 * @param <E> Type of class implementing the expression.
	 * @param <T> Return type of the expression.
	 * @param c Class that implements the expression.
	 * @param returnType Supertype of all possible return types of the
	 * expression.
	 * @param type Expression type, TODO is this still used?
	 * @param patterns Patterns that match this expression.
	 */
	<E extends Expression<T>, T> void registerExpression(Class<E> c, Class<T> returnType, ExpressionType type, String... patterns);
	
	<E extends SkriptEvent> SkriptEventInfo<E> registerEvent(String name, Class<E> c, Class<? extends Event> event, String... patterns);
	
	<E extends SkriptEvent> SkriptEventInfo<E> registerEvent(String name, Class<E> c, Class<? extends Event>[] events, String... patterns);
	
	// TODO scopes
}
