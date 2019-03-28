package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.IntegerField;
import org.globsframework.model.Key;

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
