package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.core.metamodel.annotations.InitUniqueKey;
import org.globsframework.core.metamodel.fields.BooleanField;
import org.globsframework.core.model.Key;

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
