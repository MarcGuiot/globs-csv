package org.globsframework.export;

import org.globsframework.export.model.FieldMappingType;
import org.globsframework.json.GSonUtils;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;
import org.globsframework.utils.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RealReformater implements Reformater {
    private final List<Mapper> fieldMerger = new ArrayList<>();
    private final GlobType resultType;
    private final Map<String, DataAccess> externalVariables = new HashMap<>();
    private final CustomDataAccessFactory dataAccessFactory;

    public RealReformater(GlobType fromType, List<Glob> fieldMapping) {
        this(fromType, fieldMapping, false, Map.of(), DefaultDataAccessFactory.DEFAULT);
    }

    public RealReformater(GlobType fromType, List<Glob> fieldMapping, boolean addFromType) {
        this(fromType, fieldMapping, addFromType, Map.of(), DefaultDataAccessFactory.DEFAULT);
    }

    public RealReformater(GlobType fromType, List<Glob> fieldMapping, boolean addFromType,
                          Map<String, DataAccess> externalVariables, CustomDataAccessFactory dataAccessFactory) {
        this.dataAccessFactory = dataAccessFactory;
        DefaultGlobTypeBuilder outTypeBuilder = new DefaultGlobTypeBuilder("adapted");
        if (externalVariables != null) {
            this.externalVariables.putAll(externalVariables);
        }
        if (addFromType) {
            for (Field field : fromType.getFields()) {
                Field newField = outTypeBuilder.declare(field.getName(), field.getDataType(), field.streamAnnotations().collect(Collectors.toList()));
                fieldMerger.add(new Mapper() {
                    @Override
                    public void apply(Glob from, MutableGlob to) {
                        if (from.isSet(field)) {
                            to.setValue(newField, from.getValue(field));
                        }
                    }
                });
            }
        }

        for (Glob mapping : fieldMapping) {
            String fieldName = mapping.get(FieldMappingType.to);
            Glob from = mapping.get(FieldMappingType.from);
            StringField str = outTypeBuilder.declareStringField(fieldName);
            if (from.getType() == FieldMappingType.FromType.TYPE) {
                ExtractField extractField =
                        new ExtractField(fromType.getField(from.get(FieldMappingType.FromType.from)).asStringField(),
                                from.get(FieldMappingType.FromType.defaultValueIfEmpty),
                                buildFormater(from.getOrEmpty(FieldMappingType.FromType.formater)));
                Merger merger = new FormatMerger(extractField);
                fieldMerger.add((input, to) -> {
                            String res = merger.merge(input);
                            if (res != null) {
                                to.set(str, res);
                            }
                        }
                );
            } else if (from.getType() == FieldMappingType.TemplateType.TYPE) {
                Map<String, ExtractField> extractFields = new HashMap<>();
                for (Glob extr : from.getOrEmpty(FieldMappingType.TemplateType.from)) {
                    Glob f = extr.get(FieldMappingType.RenamedType.from);
                    String renamed = extr.get(FieldMappingType.RenamedType.renameTo,
                            f.get(FieldMappingType.FromType.from));
                    extractFields.put(renamed,
                            new ExtractField(
                                    fromType.getField(f.get(FieldMappingType.FromType.from)).asStringField(),
                                    f.get(FieldMappingType.FromType.defaultValueIfEmpty),
                                    buildFormater(f.getOrEmpty(FieldMappingType.FromType.formater)))
                    );
                }
                Merger merger =
                        new MergerTemplate(fromType, from.get(FieldMappingType.TemplateType.template), extractFields, this.externalVariables);
                fieldMerger.add((input, to) -> {
                            String res = merger.merge(input);
                            if (res != null) {
                                to.set(str, res);
                            }
                        }
                );
            } else if (from.getType() == FieldMappingType.OverrideData.TYPE) {
                List<ExtractField> extractFields = new ArrayList<>();
                for (Glob f : from.getOrEmpty(FieldMappingType.OverrideData.inputField)) {
                    extractFields.add(
                            new ExtractField(
                                    fromType.getField(f.get(FieldMappingType.FromType.from)).asStringField(),
                                    f.get(FieldMappingType.FromType.defaultValueIfEmpty),
                                    buildFormater(f.getOrEmpty(FieldMappingType.FromType.formater)))
                    );
                }

                CustomDataAccess dataAccess = this.dataAccessFactory.create(fieldName, fromType,
                        from.get(FieldMappingType.OverrideData.name),
                        from.get(FieldMappingType.OverrideData.additionalParams));
                fieldMerger.add((input, to) -> {
                    List<String> data = new ArrayList<>(extractFields.size());
                    for (ExtractField extractField : extractFields) {
                        data.add(extractField.tr(input));
                    }
                    String res = dataAccess.get(fieldName, data, input);
                    if (res != null) {
                        to.set(str, res);
                    }
                });
            } else if (from.getType() == FieldMappingType.SumData.TYPE) {
                List<ExtractField> extractFields = new ArrayList<>();
                for (Glob f : from.getOrEmpty(FieldMappingType.SumData.from)) {
                    extractFields.add(
                            new ExtractField(
                                    fromType.getField(f.get(FieldMappingType.FromType.from)).asStringField(),
                                    f.get(FieldMappingType.FromType.defaultValueIfEmpty),
                                    buildFormater(f.getOrEmpty(FieldMappingType.FromType.formater)))
                    );
                }
                Merger merger = new SumDataOp(fromType, extractFields);
                fieldMerger.add((input, to) -> {
                            String res = merger.merge(input);
                            if (res != null) {
                                to.set(str, res);
                            }
                        }
                );
            }
        }
        resultType = outTypeBuilder.get();
    }

    @Override
    public GlobType getResultType() {
        return resultType;
    }

    @Override
    public Glob transform(Glob from) {
        MutableGlob mutableGlob = resultType.instantiate();
        for (Mapper mapper : fieldMerger) {
            mapper.apply(from, mutableGlob);
        }
        return mutableGlob;
    }

    Formatter buildFormater(Glob[] formats) {
        if (formats.length == 0) {
            return new NoFormatter();
        }
        if (formats.length == 1) {
            return new PaternFormatter(formats[0].get(FieldMappingType.FormatType.matcher),
                    formats[0].get(FieldMappingType.FormatType.result));
        }
        Formatter[] f = new Formatter[formats.length];
        for (int i = 0, formatsLength = formats.length; i < formatsLength; i++) {
            f[i] = new PaternFormatter(formats[i].get(FieldMappingType.FormatType.matcher),
                    formats[i].get(FieldMappingType.FormatType.result));
        }
        return new Formatter() {
            public String format(String value) {
                for (Formatter formatter : f) {
                    value = formatter.format(value);
                }
                return value;
            }
        };
    }

    interface Mapper {
        void apply(Glob from, MutableGlob to);
    }

    interface Merger {
        String merge(Glob from);
    }

    interface Formatter {
        String format(String value);
    }

    static class FormatMerger implements Merger {
        private final ExtractField extractField;

        public FormatMerger(ExtractField extractField) {
            this.extractField = extractField;
        }

        public String merge(Glob from) {
            return extractField.tr(from);
        }
    }

    //template format is {a}-{b} and {c}
    static class MergerTemplate implements Merger {
        private final List<Token> tokens = new ArrayList<>();

        MergerTemplate(GlobType fromType, String template, Map<String, ExtractField> extractFields, Map<String, DataAccess> externalVariables) {
            Pattern pattern = Pattern.compile("\\{[^\\{\\}]*\\}");
            Matcher matcher = pattern.matcher(template);
            if (matcher.find()) {
                String[] split = pattern.split(template);
                int i = 0;
                do {
                    String group = matcher.group();
                    String name = group.substring(1, group.length() - 1);
                    if (i < split.length && !split[i].isEmpty()) {
                        tokens.add(new StrToken(split[i]));
                    }
                    ExtractField extractField = extractFields.get(name);
                    if (extractField == null) {
                        Field orgField = fromType.findField(name);
                        if (orgField == null) {
                            DataAccess dataAccess = externalVariables.get(name);
                            if (dataAccess != null) {
                                tokens.add(from -> dataAccess.get(name, from));
                            } else {
                                throw new RuntimeException("field " + name + " not found in " + extractFields.keySet() +
                                        " for template " + template + " and not in org type");
                            }
                        } else {
                            tokens.add(new FieldToken(orgField));
                        }
                    } else {
                        tokens.add(new ExtractFieldToken(extractField));
                    }
                    i++;
                }
                while (matcher.find());
            } else {
                tokens.add(new StrToken(template));
            }
        }

        public String merge(Glob from) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Token token : tokens) {
                stringBuilder.append(token.getToken(from));
            }
            return stringBuilder.toString();
        }

        interface Token {
            String getToken(Glob from);
        }

        static class FieldToken implements Token {
            private final Field field;

            FieldToken(Field field) {
                this.field = field;
            }

            public String getToken(Glob from) {
                Object value = from.getValue(field);
                return value == null ? "" : String.valueOf(value);
            }
        }

        static class ExtractFieldToken implements Token {
            private final ExtractField extractField;

            public ExtractFieldToken(ExtractField extractField) {
                this.extractField = extractField;
            }

            public String getToken(Glob from) {
                return extractField.tr(from);
            }
        }

        static class StrToken implements Token {
            final private String str;

            public StrToken(String str) {
                this.str = str;
            }

            public String getToken(Glob from) {
                return str;
            }
        }
    }

    static class NoFormatter implements Formatter {
        public String format(String value) {
            return value;
        }
    }

    static class PaternFormatter implements Formatter {
        private final Pattern pattern;
        private final String result;

        PaternFormatter(String matcher, String result) {
            pattern = Pattern.compile(matcher);
            this.result = result;
        }

        public String format(String value) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
                return matcher.replaceAll(result);
            }
            throw new RuntimeException(value + " do not match.");
        }
    }

    static class ExtractField {
        private final StringField fromField;
        private final String defaultValue;
        private final Formatter formatter;

        public ExtractField(StringField fromField, String defaultValue, Formatter formatter) {
            this.formatter = formatter;
            this.fromField = fromField;
            this.defaultValue = defaultValue;
        }

        public String tr(Glob from) {
            String str = from.get(fromField);
            if (Strings.isNullOrEmpty(str)) {
                return defaultValue;
            }
            str = formatter.format(str);
            return str;
        }
    }

    public static class DefaultDataAccessFactory implements CustomDataAccessFactory {
        public static CustomDataAccessFactory DEFAULT = new DefaultDataAccessFactory();
        public CustomDataAccess create(String fieldName, GlobType lineType, String s, String from) {
            return new CustomDataAccess() {
                public String get(String fieldName, List<String> input, Glob data) {
                    throw new RuntimeException("No factory for " + fieldName + " " + GSonUtils.encode(data, false));
                }
            };
        }
    }

    private class SumDataOp implements Merger {
        private final List<ExtractField> extractFields;

        public SumDataOp(GlobType fromType, List<ExtractField> extractFields) {
            this.extractFields = extractFields;
        }

        public String merge(Glob from) {
            double total = 0;
            for (ExtractField s : extractFields) {
                String tr = s.tr(from);
                if (Strings.isNotEmpty(tr)) {
                    total += Double.parseDouble(tr);
                }
            }
            return Double.toString(total);
        }
    }
}
