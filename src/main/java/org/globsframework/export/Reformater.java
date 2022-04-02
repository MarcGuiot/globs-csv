package org.globsframework.export;

import org.globsframework.metamodel.GlobType;
import org.globsframework.model.Glob;

public interface Reformater {
    GlobType getResultType();

    Glob transform(Glob from);
}
