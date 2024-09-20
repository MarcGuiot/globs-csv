package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ElementType.FIELD})
public @interface ReNamedMappingExport_ {
    String name();

    String to();

    GlobType TYPE = NamedExport.TYPE;
}
