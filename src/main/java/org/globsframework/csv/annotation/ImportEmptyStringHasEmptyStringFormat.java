package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.fields.BooleanField;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;

public class ImportEmptyStringHasEmptyStringFormat {
    public static final GlobType TYPE;

    public static final BooleanField EMPTY_STRING;

    public static final Key KEY;

    public static Glob TRUE;
    public static Glob FALSE;

    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("ImportEmptyStringHasEmptyStringFormat");
        EMPTY_STRING = typeBuilder.declareBooleanField("emptyString");
        TYPE = typeBuilder.build();
        TRUE = TYPE.instantiate().set(EMPTY_STRING, true);
        FALSE = TYPE.instantiate().set(EMPTY_STRING, false);
        KEY = KeyBuilder.newEmptyKey(TYPE);
    }
}
