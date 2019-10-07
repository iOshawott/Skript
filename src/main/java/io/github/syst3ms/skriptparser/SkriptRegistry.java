package io.github.syst3ms.skriptparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.registrations.Converters;
import ch.njol.util.NullableChecker;
import ch.njol.util.coll.iterator.CheckedIterator;
import io.github.syst3ms.skriptparser.types.PatternType;

/**
 * Contains registered types, expressions, conditions, effects, scopes
 * and events.
 * 
 * <ul>
 * <li>Types are same as in Java
 * <li>Expressions are close to Java's getter/setter pairs
 * <li>Conditions are boolean expressions that can be used in place of effects
 * <li>Effects are basically methods that return nothing
 * <li>Scopes are used to implement control flow and other blocks
 * </ul>
 */
public class SkriptRegistry {
	
	public static class Bundle<T> implements Iterable<T> {
		
		/**
		 * Entries in undefined order.
		 */
		private final List<T> entries;
		
		/**
		 * Code names to entries.
		 */
		private final Map<String,T> nameToEntry;
		
		/**
		 * Classes of entries to entries.
		 */
		private final Map<Class<?>,T> classToEntry;
		
		Bundle() {
			this.entries = new ArrayList<>();
			this.nameToEntry = new HashMap<>();
			this.classToEntry = new HashMap<>();
		}
		
		public T get(String name) {
			T entry = nameToEntry.get(name);
			if (entry == null) {
				throw new EntryNotFoundException(name);
			}
			return entry;
		}
		
		public T get(Class<?> type) {
			T entry = classToEntry.get(type);
			if (entry == null) {
				throw new EntryNotFoundException(type);
			}
			return entry;
		}

		@SuppressWarnings("null")
		@Override
		public Iterator<T> iterator() {
			return entries.iterator();
		}
		
		public int size() {
			return entries.size();
		}
	}
	
	private final Bundle<ClassInfo<?>> types;
	private final Bundle<ExpressionInfo<?, ?>> expressions;
	private final Bundle<SyntaxElementInfo<?>> conditions;
	private final Bundle<SyntaxElementInfo<?>> effects;
	private final Bundle<SyntaxElementInfo<?>> scopes;
	private final Bundle<SkriptEventInfo<?>> events;
	
	public SkriptRegistry() {
		this.types = new Bundle<>();
		this.expressions = new Bundle<>();
		this.conditions = new Bundle<>();
		this.effects = new Bundle<>();
		this.scopes = new Bundle<>();
		this.events = new Bundle<>();
	}
	
	public Bundle<ClassInfo<?>> getTypes() {
		return types;
	}
	
    /**
     * Gets a {@link PatternType} from a name. This determines the number
     * (single/plural) from the input. If the input happens to be the base
     * name of a type, then a single PatternType (as in "not plural") of
     * the corresponding type is returned.
     * @param name Input name.
     * @return The corresponding PatternType, or {@literal null} if nothing matched.
     */
    @Nullable
    public PatternType<?> getPatternType(String name) {
        for (ClassInfo<?> t : getTypes()) {
            if (name.equalsIgnoreCase(t.getName().getSingular())) {
                return new PatternType<>(t, true);
            } else if (name.equalsIgnoreCase(t.getName().getPlural())) {
                return new PatternType<>(t, false);
            }
        }
        return null;
    }
	
	public Bundle<ExpressionInfo<?, ?>> getExpressions() {
		return expressions;
	}
	
	public Iterator<ExpressionInfo<?, ?>> getExpressions(Class<?>... returnTypes) {
		return new CheckedIterator<>(getExpressions().iterator(), new NullableChecker<ExpressionInfo<?, ?>>() {
			@Override
			public boolean check(final @Nullable ExpressionInfo<?, ?> i) {
				if (i == null || i.returnType == Object.class)
					return true;
				for (final Class<?> returnType : returnTypes) {
					assert returnType != null;
					if (Converters.converterExists(i.returnType, returnType))
						return true;
				}
				return false;
			}
		});
	}
	
	public Bundle<SyntaxElementInfo<?>> getConditions() {
		return conditions;
	}
	
	public Bundle<SyntaxElementInfo<?>> getEffects() {
		return effects;
	}
	
	public Bundle<SyntaxElementInfo<?>> getScopes() {
		return scopes;
	}
	
	public Bundle<SkriptEventInfo<?>> getEvents() {
		return events;
	}
}
