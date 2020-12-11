package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.ZoneId;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ElementType.FIELD})

public @interface ExportDateFormat_ {

    String value();

    String zoneId() default "";

    GlobType TYPE = ExportDateFormat.TYPE;
}
