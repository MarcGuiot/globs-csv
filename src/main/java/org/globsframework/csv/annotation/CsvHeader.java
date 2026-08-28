package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.fields.BooleanField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;

public class CsvHeader {
    public static final GlobType TYPE;

    public static final StringField name;

    public static final BooleanField firstLineIsHeader;

//    public static BooleanField noHeader;

//    public static StringArrayField header;

    public static final Key KEY;

    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("CsvHeader");
        name = typeBuilder.declareStringField("name");
        firstLineIsHeader = typeBuilder.declareBooleanField("firstLineIsHeader");
        TYPE = typeBuilder.build();
        KEY = KeyBuilder.newEmptyKey(TYPE);
    }

    public static Glob create(String name) {
        return TYPE.instantiate()
                .set(CsvHeader.name, name)
                .set(CsvHeader.firstLineIsHeader, false);
    }

    public static Glob create(String name, boolean firstLineIsHeader) {
        return TYPE.instantiate()
                .set(CsvHeader.name, name)
                .set(CsvHeader.firstLineIsHeader, firstLineIsHeader);
    }
}
