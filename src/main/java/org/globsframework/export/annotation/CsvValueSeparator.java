package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Key;

public class CsvValueSeparator {
    public static GlobType TYPE;

    public static StringField SEPARATOR;

    @InitUniqueKey
    public static Key KEY;


    static {
        GlobTypeLoaderFactory.create(CsvValueSeparator.class, "ValueSeparator")
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(SEPARATOR, String.valueOf(((CsvValueSeparator_) annotation).value())))
                .load();
    }
}
