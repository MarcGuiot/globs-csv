package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.fields.Field;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;
import org.globsframework.core.utils.Strings;

import java.util.Optional;

public class ReNamedExport {
    public static final GlobType TYPE;

    public static final GlobArrayField<Mapping> names;

    public static final StringField defaultValue;

    public static final Key KEY;

    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("ReNamedExport");
        names = typeBuilder.declareGlobArrayField("names", () -> Mapping.TYPE);
        defaultValue = typeBuilder.declareStringField("defaultValue");
        TYPE = typeBuilder.build();
        KEY = KeyBuilder.newEmptyKey(TYPE);
    }

    public static String getHeaderName(String name, Field field) {
        Optional<Glob> annotation = field.findOptAnnotation(KEY);
        if (annotation.isPresent()) {
            if (Strings.isNotEmpty(name)) {
                Glob[] renamed = annotation.get().getOrEmpty(names);
                for (Glob glob : renamed) {
                    if (glob.get(Mapping.name).equals(name)) {
                        return glob.get(Mapping.renamed);
                    }
                }
            }
            String def = annotation.get().get(defaultValue);
            if (Strings.isNotEmpty(def)) {
                return def;
            }
        }
        return field.getName();
    }


    public static Glob create(String defaultValue) {
        return create(defaultValue, null);
    }

    public static Glob create(Glob... glob) {
        return create("", glob);
    }

    public static Glob create(String defaultValue, Glob... mapping) {
        return TYPE.instantiate()
                .set(ReNamedExport.defaultValue, defaultValue)
                .set(names, mapping);
    }

    public static class Mapping {
        public static final GlobType TYPE;

        public static final StringField name;

        public static final StringField renamed;

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("Mapping");
            name = typeBuilder.declareStringField("name");
            renamed = typeBuilder.declareStringField("renamed");
            TYPE = typeBuilder.build();
        }

        public static Glob create(String name, String renamed) {
            return TYPE.instantiate()
                    .set(Mapping.name, name)
                    .set(Mapping.renamed, renamed)
                    ;
        }
    }
}
