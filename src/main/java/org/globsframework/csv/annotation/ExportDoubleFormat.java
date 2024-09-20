package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.InitUniqueKey;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Key;

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
