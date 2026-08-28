package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;

public class ExportDoubleFormat {
    public static final GlobType TYPE;

    public static final StringField FORMAT;

    public static final StringField DECIMAL_SEPARATOR;

    public static final Key KEY;

    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("ExportDoubleFormat");
        FORMAT = typeBuilder.declareStringField("format");
        DECIMAL_SEPARATOR = typeBuilder.declareStringField("decimalSeparator");
        TYPE = typeBuilder.build();
        KEY = KeyBuilder.newEmptyKey(TYPE);
    }
}
