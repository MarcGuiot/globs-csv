package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.Key;
import org.globsframework.core.model.KeyBuilder;

public class ExportDateFormat {
    public static final GlobType TYPE;

    public static final StringField FORMAT;

    public static final StringField ZONE_ID;

    public static final Key KEY;

    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("ExportDateFormat");
        FORMAT = typeBuilder.declareStringField("format");
        ZONE_ID = typeBuilder.declareStringField("zoneId");
        TYPE = typeBuilder.build();
        KEY = KeyBuilder.newEmptyKey(TYPE);
    }

    public static Glob create(String format) {
        return create(format, null);
    }

    public static Glob create(String format, String zoneId) {
        return TYPE.instantiate().set(FORMAT, format)
                .set(ZONE_ID, zoneId);
    }
}
