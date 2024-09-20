package org.globsframework.csv.model;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.Target;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.StringField;

public class CsvImportType {
    public static GlobType TYPE;

    public static StringField separator;

    @Target(FieldMappingType.class)
    public static GlobArrayField fieldMapping;

    static {
        GlobTypeLoaderFactory.create(CsvImportType.class).load();
    }
}
