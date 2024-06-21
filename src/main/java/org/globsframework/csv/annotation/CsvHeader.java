package org.globsframework.csv.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.BooleanField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Key;

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
