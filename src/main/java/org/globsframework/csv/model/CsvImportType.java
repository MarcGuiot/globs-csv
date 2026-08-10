package org.globsframework.csv.model;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.annotations.Target;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;

public class CsvImportType {
    public static final GlobType TYPE;

    public static final StringField separator;

    @Target(FieldMappingType.class)
    public static final GlobArrayField<FieldMappingType> fieldMapping;

    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("CsvImport");
        separator = typeBuilder.declareStringField("separator");
        fieldMapping = typeBuilder.declareGlobArrayField("fieldMapping", () -> FieldMappingType.TYPE);
        TYPE = typeBuilder.build();
    }
}
