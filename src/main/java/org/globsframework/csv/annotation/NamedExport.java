package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.core.metamodel.annotations.InitUniqueKey;
import org.globsframework.core.metamodel.fields.StringArrayField;
import org.globsframework.core.model.Key;

public class NamedExport {
    public static GlobType TYPE;

    public static StringArrayField names;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(NamedExport.class, "NamedExport")
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(names, ((NamedExport_) annotation).value())
                )
                .load();
    }
}
