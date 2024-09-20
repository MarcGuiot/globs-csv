package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.core.metamodel.annotations.InitUniqueKey;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Key;

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
