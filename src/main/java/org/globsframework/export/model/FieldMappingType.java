package org.globsframework.export.model;

import org.globsframework.json.annottations.IsJsonContentAnnotation;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.Target;
import org.globsframework.metamodel.annotations.Targets;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.Glob;

public class FieldMappingType {
    public static GlobType TYPE;

    @Targets({FromType.class, TemplateType.class, SumData.class, OverrideData.class, MappingData.class, JoinType.class})
    public static GlobUnionField from;

    public static StringField to;

    public static StringField targetType; //default string

    static {
        GlobTypeLoaderFactory.create(FieldMappingType.class).load();
    }

//  TODO   {a1,a2}{b}{c1,c2} => a1.b.c1, a1.b.c2, a1.b.c1, a1.b.c2

    public static class SumData {
        public static GlobType TYPE;

        @Target(FromType.class)
        public static GlobArrayField from;

        static {
            GlobTypeLoaderFactory.create(SumData.class).load();
        }
    }

    public static class OverrideData {
        public static GlobType TYPE;

        public static StringField name;

        @Target(FromType.class)
        public static GlobArrayField inputField;

        @IsJsonContentAnnotation
        public static StringField additionalParams;

        static {
            GlobTypeLoaderFactory.create(OverrideData.class).load();
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

    public static class JoinType {
        public static GlobType TYPE;

        @Target(FromType.class)
        public static GlobArrayField from;

        public static StringField separator;

        public static StringField first;

        public static BooleanField addFirstIfEmpty;

        public static StringField last;

        public static BooleanField addLastIfEmpty;

        static {
            GlobTypeLoaderFactory.create(JoinType.class).load();
        }
    }

    public static class TemplateType {
        public static GlobType TYPE;

        @Target(RenamedType.class)
        public static GlobArrayField from;

        public static StringField template;

        public static BooleanField noValueIfOnIsMissing;

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

    public static class MappingData {
        public static GlobType TYPE;

        public static StringField mappingName;

        @Target(FromType.class)
        public static GlobField from;

        public static BooleanField copyValueIfNoMapping;

        @Target(KeyValue.class)
        public static GlobArrayField mapping;

        static {
            GlobTypeLoaderFactory.create(MappingData.class).load();
        }
    }

    public static class KeyValue {
        public static GlobType TYPE;

        public static StringField key;

        public static StringField value;

        public static Glob create(String key, String value) {
            return TYPE.instantiate().set(KeyValue.key, key).set(KeyValue.value, value);
        }

        static {
            GlobTypeLoaderFactory.create(KeyValue.class).load();
        }
    }
}
