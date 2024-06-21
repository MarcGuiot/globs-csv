package org.globsframework.csv.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Key;

public class CsvSeparator {
    public static GlobType TYPE;

    public static StringField SEPARATOR;

    @InitUniqueKey
    public static Key KEY;


    static {
        GlobTypeLoaderFactory.create(CsvSeparator.class, "CsvSeparator")
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(SEPARATOR, new String(new char[]{((CsvSeparator_) annotation).value()})))
                .load();
    }
}
