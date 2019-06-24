package io.github.syst3ms.skriptparser.types;

import ch.njol.skript.classes.ClassInfo;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Manages the registration and usage of {@link ClassInfo}
 */
@SuppressWarnings("unchecked")
public class TypeManager {
    public static final String NULL_REPRESENTATION = "<none>";
    public static final String EMPTY_REPRESENTATION = "<empty>";
    private static Map<String, ClassInfo<?>> nameToType = new HashMap<>();
    private static Map<Class<?>, ClassInfo<?>> classToType = new LinkedHashMap<>(); // Ordering is important for stuff like number types

    public static Map<Class<?>, ClassInfo<?>> getClassToTypeMap() {
        return classToType;
    }

    /**
     * Gets a {@link ClassInfo} by its exact name (the baseName parameter used in {@link ClassInfo#ClassInfo(Class, String, String)})
     * @param name the name to get the ClassInfo from
     * @return the corresponding ClassInfo, or {@literal null} if nothing matched
     */
    @Nullable
    public static ClassInfo<?> getByExactName(String name) {
        return nameToType.get(name);
    }

    /**
     * Gets a {@link ClassInfo} using its plural forms, which means this matches any alternate and/or plural form.
     * @param name the name to get a ClassInfo from
     * @return the matching ClassInfo, or {@literal null} if nothing matched
     */
    @Nullable
    public static ClassInfo<?> getByName(String name) {
        for (ClassInfo<?> t : nameToType.values()) {
        	// TODO port to Skript's plural handling or change ClassInfo
            String[] forms = t.getPluralForms();
            if (name.equalsIgnoreCase(forms[0]) || name.equalsIgnoreCase(forms[1])) {
                return t;
            }
        }
        return null;
    }

    /**
     * Gets a {@link ClassInfo} from its associated {@link Class}.
     * @param c the Class to get the ClassInfo from
     * @param <T> the underlying type of the Class and the returned ClassInfo
     * @return the associated ClassInfo, or {@literal null}
     */
    @Nullable
    public static <T> ClassInfo<T> getByClassExact(Class<T> c) {
        if (c.isArray())
            c = (Class<T>) c.getComponentType();
        return (ClassInfo<T>) classToType.get(c);
    }

    @Nullable
    public static <T> ClassInfo<? super T> getByClass(Class<T> c) {
        ClassInfo<? super T> type = getByClassExact(c);
        Class<? super T> superclass = c;
        while (superclass != null && type == null) {
            type = getByClassExact(superclass);
            superclass = superclass.getSuperclass();
        }
        return type;
    }

    public static String toString(Object... objects) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            if (i > 0) {
                sb.append(i == objects.length - 1 ? " and " : ", ");
            }
            Object o = objects[i];
            if (o == null) {
                sb.append(NULL_REPRESENTATION);
                continue;
            }
            ClassInfo<?> type = getByClass(o.getClass());
            if (type == null) {
                sb.append(Objects.toString(o));
            } else {
                sb.append(type.getToStringFunction().apply(o));
            }
        }
        return sb.length() == 0 ? EMPTY_REPRESENTATION : sb.toString();
    }

    /**
     * Gets a {@link PatternType} from a name. This determines the number (single/plural) from the input.
     * If the input happens to be the base name of a type, then a single PatternType (as in "not plural") of the corresponding type is returned.
     * @param name the name input
     * @return a corresponding PatternType, or {@literal null} if nothing matched
     */
    @Nullable
    public static PatternType<?> getPatternType(String name) {
        for (ClassInfo<?> t : nameToType.values()) {
            String[] forms = t.getPluralForms();
            if (name.equalsIgnoreCase(forms[0])) {
                return new PatternType<>(t, true);
            } else if (name.equalsIgnoreCase(forms[1])) {
                return new PatternType<>(t, false);
            }
        }
        return null;
    }

    public static void register(SkriptRegistration reg) {
        for (ClassInfo<?> type : reg.getTypes()) {
            nameToType.put(type.getBaseName(), type);
            classToType.put(type.getTypeClass(), type);
        }
    }
}