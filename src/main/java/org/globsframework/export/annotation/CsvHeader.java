package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Key;

public class CsvHeader {
    public static GlobType TYPE;

    public static StringField name;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(CsvHeader.class, "CsvHeader")
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(name, ((CsvHeader_) annotation).value()))
                .load();
    }
}
