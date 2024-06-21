package org.globsframework.csv.model;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.Target;
import org.globsframework.metamodel.fields.GlobArrayField;
import org.globsframework.metamodel.fields.StringField;

public class CsvImportType {
    public static GlobType TYPE;

    public static StringField separator;

    @Target(FieldMappingType.class)
    public static GlobArrayField fieldMapping;

    static {
        GlobTypeLoaderFactory.create(CsvImportType.class).load();
    }
}
