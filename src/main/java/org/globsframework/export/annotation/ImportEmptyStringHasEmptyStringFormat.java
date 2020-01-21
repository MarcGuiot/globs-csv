package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.BooleanField;
import org.globsframework.model.Key;

public class ImportEmptyStringHasEmptyStringFormat {
    public static GlobType TYPE;

    public static BooleanField EMPTY_STRING;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(ImportEmptyStringHasEmptyStringFormat.class, "ImportEmptyStringHasEmptyStringFormat")
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(EMPTY_STRING, ((ImportEmptyStringHasEmptyStringFormat_) annotation).value())
                )
                .load();
    }
}
