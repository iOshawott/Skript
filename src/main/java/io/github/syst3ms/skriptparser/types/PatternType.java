package io.github.syst3ms.skriptparser.types;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.classes.ClassInfo;

/**
 * A type used in a pattern.
 * Groups a {@link ClassInfo} and a number (single or plural) together (in contrast to {@link ClassInfo})
 */
public class PatternType<T> {
	
    private ClassInfo<T> type;
    private boolean single;

    public PatternType(ClassInfo<T> type, boolean single) {
        this.type = type;
        this.single = single;
    }

    public ClassInfo<T> getType() {
        return type;
    }

    public boolean isSingle() {
        return single;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof PatternType)) {
            return false;
        } else {
            PatternType<?> o = (PatternType<?>) obj;
            return type.equals(o.type) && single == o.single;
        }
    }

    @Override
    public String toString() {
    	// TODO handle Skript's plural forms
        String[] forms = type.getPluralForms();
        return forms[single ? 0 : 1];
    }
}
