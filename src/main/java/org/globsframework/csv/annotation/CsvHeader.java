package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.core.metamodel.annotations.InitUniqueKey;
import org.globsframework.core.metamodel.fields.BooleanField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Key;

public class CsvHeader {
    public static GlobType TYPE;

    public static StringField name;

    public static BooleanField firstLineIsHeader;

//    public static BooleanField noHeader;

//    public static StringArrayField header;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(CsvHeader.class, "CsvHeader")
                .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                                .set(name, ((CsvHeader_) annotation).value())
                                .set(firstLineIsHeader, ((CsvHeader_) annotation).firstLineIsHeader())
//                        .set(noHeader, ((CsvHeader_) annotation).noHeader())
//                        .set(header, ((CsvHeader_) annotation).header())
                )
                .load();
    }
}
