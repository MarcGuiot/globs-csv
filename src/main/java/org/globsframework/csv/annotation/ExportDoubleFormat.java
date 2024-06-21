package org.globsframework.csv.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Key;

public class ExportDoubleFormat {
    public static GlobType TYPE;

    public static StringField FORMAT;

    public static StringField DECIMAL_SEPARATOR;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(ExportDoubleFormat.class, "ExportDoubleFormat")
                .load();
    }
}
