package org.globsframework.export.annotation;

import org.globsframework.metamodel.GlobModel;
import org.globsframework.metamodel.impl.DefaultGlobModel;

public class ExportMetamodel {
    public static GlobModel globModel =
            new DefaultGlobModel(ExportBooleanFormat.TYPE,ExportColumnSize.TYPE, ExportDateFormat.TYPE);
}
