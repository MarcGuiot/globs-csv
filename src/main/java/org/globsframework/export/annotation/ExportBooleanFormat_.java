package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ElementType.FIELD})

public @interface ExportBooleanFormat_ {

    String true_();

    String false_();

    GlobType TYPE = ExportBooleanFormat.TYPE;
}
