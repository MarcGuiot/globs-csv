package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.core.metamodel.annotations.InitUniqueKey;
import org.globsframework.core.metamodel.fields.IntegerField;
import org.globsframework.core.model.Key;

public class ExportColumnSize {
    public static GlobType TYPE;

    public static IntegerField SIZE;

    @InitUniqueKey
    public static Key KEY;


    static {
        GlobTypeLoaderFactory.create(ExportColumnSize.class, "ExportColumnSize")
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(SIZE, ((ExportColumnSize_) annotation).value()))
                .load();
    }
}
