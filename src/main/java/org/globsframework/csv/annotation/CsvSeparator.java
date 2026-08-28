package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;

public class CsvSeparator {
    public static final GlobType TYPE;

    public static final StringField SEPARATOR;

    public static final Key KEY;


    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("CsvSeparator");
        SEPARATOR = typeBuilder.declareStringField("separator");
        TYPE = typeBuilder.build();
        KEY = KeyBuilder.newEmptyKey(TYPE);
    }

}
