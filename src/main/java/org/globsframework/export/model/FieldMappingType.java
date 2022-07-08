package org.globsframework.export.model;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.Target;
import org.globsframework.metamodel.annotations.Targets;
import org.globsframework.metamodel.fields.GlobArrayField;
import org.globsframework.metamodel.fields.GlobField;
import org.globsframework.metamodel.fields.GlobUnionField;
import org.globsframework.metamodel.fields.StringField;

public class FieldMappingType {
    public static GlobType TYPE;

    @Targets({FromType.class, TemplateType.class, SumData.class})
    public static GlobUnionField from;

    public static StringField to;

    public static StringField targetType; //default string

    static {
        GlobTypeLoaderFactory.create(FieldMappingType.class).load();
    }

    public static class SumData {
        public static GlobType TYPE;

        @Target(FromType.class)
        public static GlobArrayField from;

        static {
            GlobTypeLoaderFactory.create(SumData.class).load();
        }
    }

    public static class FromType {
        public static GlobType TYPE;

        public static StringField from;

        public static StringField defaultValueIfEmpty;

        @Target(FormatType.class)
        public static GlobArrayField formater;

        static {
            GlobTypeLoaderFactory.create(FromType.class).load();
        }
    }

    public static class RenamedType {

        public static GlobType TYPE;

        @Target(FromType.class)
        public static GlobField from;

        public static StringField renameTo; // par defaut identique a from

        static {
            GlobTypeLoaderFactory.create(RenamedType.class).load();
        }
    }

    public static class TemplateType {
        public static GlobType TYPE;

        @Target(RenamedType.class)
        public static GlobArrayField from;

        public static StringField template;

        static {
            GlobTypeLoaderFactory.create(TemplateType.class).load();
        }
    }

    public static class FormatType {
        public static GlobType TYPE;

        public static StringField matcher;

        public static StringField result;

        // TODO
//        @EnumAnnotation("")
//        public static StringField valueIfNoMatch;

        static {
            GlobTypeLoaderFactory.create(FormatType.class).load();
        }
    }
}
