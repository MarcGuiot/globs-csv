package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ElementType.FIELD})

public @interface CsvHeader_ {

    String value();

    boolean firstLineIsHeader() default false;

//    boolean noHeader() default true;

//    String[] header() default {};

    GlobType TYPE = CsvHeader.TYPE;
}
