package org.globsframework.csv.annotation;

import org.globsframework.core.metamodel.GlobModel;
import org.globsframework.core.metamodel.impl.DefaultGlobModel;

public class AllAnnotations {
    public static GlobModel MODEL =
            new DefaultGlobModel(ExportBooleanFormat.TYPE, ExportColumnSize.TYPE, ExportDateFormat.TYPE,
                    ImportEmptyStringHasEmptyStringFormat.TYPE, CsvSeparator.TYPE, NamedExport.TYPE, ReNamedExport.TYPE, CsvValueSeparator.TYPE);
}
