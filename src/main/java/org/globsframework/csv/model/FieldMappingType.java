package org.globsframework.csv.model;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.fields.*;
import org.globsframework.core.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.core.model.Glob;
import org.globsframework.json.annottations.IsJsonContent;

import java.util.function.Supplier;

public class FieldMappingType {
    public static final GlobType TYPE;

    public static final GlobUnionField from;

    public static final StringField to;

    public static final StringField targetType; //default string

    static {
        GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("FieldMapping");
        from = typeBuilder.declareGlobUnionField("from",
                new Supplier[]{() -> FromType.TYPE, () -> TemplateType.TYPE, () -> SumData.TYPE,
                        () -> OverrideData.TYPE, () -> MappingData.TYPE, () -> JoinType.TYPE});
        to = typeBuilder.declareStringField("to");
        targetType = typeBuilder.declareStringField("targetType");
        TYPE = typeBuilder.build();
    }

//  TODO   {a1,a2}{b}{c1,c2} => a1.b.c1, a1.b.c2, a1.b.c1, a1.b.c2

    public static class SumData {
        public static final GlobType TYPE;

        public static final GlobArrayField<FromType> from;

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("SumData");
            from = typeBuilder.declareGlobArrayField("from", () -> FromType.TYPE);
            TYPE = typeBuilder.build();
        }
    }

    public static class OverrideData {
        public static final GlobType TYPE;

        public static final StringField name;

        public static final GlobArrayField<FromType> inputField;

        public static final StringField additionalParams;

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("OverrideData");
            name = typeBuilder.declareStringField("name");
            inputField = typeBuilder.declareGlobArrayField("inputField", () -> FromType.TYPE);
            additionalParams = typeBuilder.declareStringField("additionalParams", IsJsonContent.UNIQUE_GLOB);
            TYPE = typeBuilder.build();
        }
    }

    public static class FromType {
        public static final GlobType TYPE;

        public static final StringField from;

        public static final StringField defaultValueIfEmpty;

        public static final StringField toStringFormater;

        public static final GlobArrayField<FormatType> formater;

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("FromType");
            from = typeBuilder.declareStringField("from");
            defaultValueIfEmpty = typeBuilder.declareStringField("defaultValueIfEmpty");
            toStringFormater = typeBuilder.declareStringField("toStringFormater");
            formater = typeBuilder.declareGlobArrayField("formater", () -> FormatType.TYPE);
            TYPE = typeBuilder.build();
        }
    }

    public static class RenamedType {

        public static final GlobType TYPE;

        public static final GlobField<FromType> from;

        public static final StringField renameTo; // par defaut identique a from

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("RenamedType");
            from = typeBuilder.declareGlobField("from", () -> FromType.TYPE);
            renameTo = typeBuilder.declareStringField("renameTo");
            TYPE = typeBuilder.build();
        }
    }

    public static class JoinType {
        public static final GlobType TYPE;

        public static final GlobArrayField<FromType> from;

        public static final StringField separator;

        public static final StringField first;

        public static final BooleanField addFirstIfEmpty;

        public static final StringField last;

        public static final BooleanField addLastIfEmpty;

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("Join");
            from = typeBuilder.declareGlobArrayField("from", () -> FromType.TYPE);
            separator = typeBuilder.declareStringField("separator");
            first = typeBuilder.declareStringField("first");
            addFirstIfEmpty = typeBuilder.declareBooleanField("addFirstIfEmpty");
            last = typeBuilder.declareStringField("last");
            addLastIfEmpty = typeBuilder.declareBooleanField("addLastIfEmpty");
            TYPE = typeBuilder.build();
        }
    }

    public static class TemplateType {
        public static final GlobType TYPE;

        public static final GlobArrayField<RenamedType> from;

        public static final StringField template;

        public static final BooleanField noValueIfOnIsMissing;

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("Template");
            from = typeBuilder.declareGlobArrayField("from", () -> RenamedType.TYPE);
            template = typeBuilder.declareStringField("template");
            noValueIfOnIsMissing = typeBuilder.declareBooleanField("noValueIfOnIsMissing");
            TYPE = typeBuilder.build();
        }
    }

    public static class FormatType {
        public static final GlobType TYPE;

        public static final StringField matcher;

        public static final StringField result;

        // TODO
//        @EnumAnnotation("")
//        public static StringField valueIfNoMatch;

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("FormatType");
            matcher = typeBuilder.declareStringField("matcher");
            result = typeBuilder.declareStringField("result");
            TYPE = typeBuilder.build();
        }
    }

    public static class MappingData {
        public static final GlobType TYPE;

        public static final StringField mappingName;

        public static final GlobField<FromType> from;

        public static final BooleanField copyValueIfNoMapping;

        public static final StringField defaultValueNoMapping;

        public static final StringField defaultEmptyValue;

        public static final GlobArrayField<KeyValue> mapping;

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("MappingData");
            mappingName = typeBuilder.declareStringField("mappingName");
            from = typeBuilder.declareGlobField("from", () -> FromType.TYPE);
            copyValueIfNoMapping = typeBuilder.declareBooleanField("copyValueIfNoMapping");
            defaultValueNoMapping = typeBuilder.declareStringField("defaultValueNoMapping");
            defaultEmptyValue = typeBuilder.declareStringField("defaultEmptyValue");
            mapping = typeBuilder.declareGlobArrayField("mapping", () -> KeyValue.TYPE);
            TYPE = typeBuilder.build();
        }
    }

    public static class KeyValue {
        public static final GlobType TYPE;

        public static final StringField key;

        public static final StringField value;

        public static Glob create(String key, String value) {
            return TYPE.instantiate().set(KeyValue.key, key).set(KeyValue.value, value);
        }

        static {
            GlobTypeBuilder typeBuilder = new DefaultGlobTypeBuilder("KeyValue");
            key = typeBuilder.declareStringField("key");
            value = typeBuilder.declareStringField("value");
            TYPE = typeBuilder.build();
        }
    }
}
