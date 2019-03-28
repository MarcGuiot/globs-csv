package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Key;

public class ExportDateFormat {
    public static GlobType TYPE;

    public static StringField FORMAT;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(ExportDateFormat.class, "ExportDateFormat")
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(FORMAT, ((ExportDateFormat_) annotation).value()))
                .load();
    }
}
