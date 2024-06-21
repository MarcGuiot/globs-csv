package org.globsframework.csv.annotation;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Key;

public class ExportDateFormat {
    public static GlobType TYPE;

    public static StringField FORMAT;

    public static StringField ZONE_ID;

    @InitUniqueKey
    public static Key KEY;

    static {
        GlobTypeLoaderFactory.create(ExportDateFormat.class, "ExportDateFormat")
                .register(GlobCreateFromAnnotation.class, annotation -> {
                    String zoneId = ((ExportDateFormat_) annotation).zoneId();
                            return TYPE.instantiate()
                                    .set(FORMAT, ((ExportDateFormat_) annotation).value())
                                    .set(ZONE_ID, zoneId.isEmpty() ? null : zoneId);
                        }
                )
                .load();
    }
}
