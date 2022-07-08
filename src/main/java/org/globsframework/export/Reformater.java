package org.globsframework.export;

import org.globsframework.metamodel.GlobType;
import org.globsframework.model.Glob;

public interface Reformater {

    interface DataAccess {
        String get(String fieldName, Glob data);
    }

    GlobType getResultType();

    Glob transform(Glob from);
}
