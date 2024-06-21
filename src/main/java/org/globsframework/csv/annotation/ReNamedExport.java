package org.globsframework.csv.annotation;

import org.globsframework.metamodel.fields.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.annotations.Target;
import org.globsframework.metamodel.fields.GlobArrayField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Glob;
import org.globsframework.model.Key;
import org.globsframework.utils.Strings;

import java.util.Arrays;
import java.util.Optional;

public class ReNamedExport {
    public static GlobType TYPE;

    @Target(Mapping.class)
    public static GlobArrayField names;

    public static StringField defaultValue;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(ReNamedExport.class)
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(names, Arrays.stream(((ReNamedExport_) annotation).multi())
                                .map(Mapping::create)
                                .toArray(Glob[]::new))
                        .set(defaultValue, ((ReNamedExport_) annotation).value())
                )
                .load();
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

    public static class Mapping {
        public static GlobType TYPE;

        public static StringField name;

        public static StringField renamed;

        static {
            GlobTypeLoaderFactory.create(Mapping.class).load();
        }

        public static Glob create(ReNamedMappingExport_ mapping) {
            return TYPE.instantiate()
                    .set(name, mapping.name())
                    .set(renamed, mapping.to())
                    ;
        }
    }
}
