package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.StringArrayField;
import org.globsframework.model.Key;

public class NamedExport {
    public static GlobType TYPE;

    public static StringArrayField names;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(NamedExport.class)
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(names, ((NamedExport_) annotation).value())
                )
                .load();
    }
}
