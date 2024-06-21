package org.globsframework.csv.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Key;

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
