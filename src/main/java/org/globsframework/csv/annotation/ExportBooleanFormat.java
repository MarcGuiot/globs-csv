package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.core.metamodel.annotations.InitUniqueKey;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Key;

public class ExportBooleanFormat {
    public static GlobType TYPE;

    public static StringField TRUE_;

    public static StringField FALSE_;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(ExportBooleanFormat.class, "ExportBooleanFormat")
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                        .set(TRUE_, ((ExportBooleanFormat_) annotation).true_())
                        .set(FALSE_, ((ExportBooleanFormat_) annotation).false_())
                )
                .load();
    }
}
