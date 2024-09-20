package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.core.metamodel.annotations.InitUniqueKey;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Key;

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
