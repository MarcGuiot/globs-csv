package org.globsframework.export;

import org.globsframework.metamodel.GlobType;
import org.globsframework.model.Glob;

import java.util.List;

public interface Reformater {

    interface DataAccess {
        String get(String fieldName, Glob data);
    }

    interface CustomDataAccessFactory {
        CustomDataAccess create(String fieldName, GlobType lineType, String name, String params);
    }

    interface CustomDataAccess {
        String get(String fieldName, List<String> input, Glob data);
    }

    GlobType getResultType();

    Glob transform(Glob from);
}
