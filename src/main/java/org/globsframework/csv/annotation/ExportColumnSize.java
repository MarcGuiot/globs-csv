package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.fields.IntegerField;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;

public class ExportColumnSize {
    public static final GlobType TYPE;

    public static final IntegerField SIZE;

    public static final Key KEY;


    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("ExportColumnSize");
        SIZE = typeBuilder.declareIntegerField("size");
        TYPE = typeBuilder.build();
        KEY = KeyBuilder.newEmptyKey(TYPE);
    }

    public static Glob create(int size) {
        return TYPE.instantiate().set(SIZE, size);
    }
}
