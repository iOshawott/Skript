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

/**
 * Contains registered types, conditions, expressions, effects and events.
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
		
		public List<T> getEntries() {
			return entries;
		}
		
		public T get(String name) {
			T entry = nameToEntry.get(name);
			if (entry == null) {
				throw new EntryNotFoundException(name);
			}
			return entry;
		}
		
		public T get(Class<T> type) {
			T entry = classToEntry.get(type);
			if (entry == null) {
				throw new EntryNotFoundException(type);
			}
			return entry;
		}

		@Override
		public Iterator<T> iterator() {
			return entries.iterator();
		}
	}
	
	private final Bundle<ClassInfo<?>> types;
	private final Bundle<ExpressionInfo<?, ?>> expressions;
	private final Bundle<SyntaxElementInfo<?>> conditions;
	private final Bundle<SyntaxElementInfo<?>> effects;
	private final Bundle<SkriptEventInfo<?>> events;
	
	public SkriptRegistry() {
		this.types = new Bundle<>();
		this.expressions = new Bundle<>();
		this.conditions = new Bundle<>();
		this.effects = new Bundle<>();
		this.events = new Bundle<>();
	}
	
	public Bundle<ClassInfo<?>> getTypes() {
		return types;
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
	
	public Bundle<SkriptEventInfo<?>> getEvents() {
		return events;
	}
}
