package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;

public class ExportBooleanFormat {
    public static final GlobType TYPE;

    public static final StringField TRUE_;

    public static final StringField FALSE_;

    public static final Key KEY;

    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("ExportBooleanFormat");
        TRUE_ = typeBuilder.declareStringField("true");
        FALSE_ = typeBuilder.declareStringField("false");
        TYPE = typeBuilder.build();

        KEY = KeyBuilder.newEmptyKey(TYPE);
    }
}
