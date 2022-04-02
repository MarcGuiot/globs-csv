package org.globsframework.export;

import org.globsframework.export.model.FieldMappingType;
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

public class RealReformater implements Reformater{
    private final List<Mapper> fieldMerger = new ArrayList<>();
    private final GlobType resultType;

    public RealReformater(GlobType fromType, List<Glob> fieldMapping) {
        DefaultGlobTypeBuilder outTypeBuilder = new DefaultGlobTypeBuilder("adapted");
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
                fieldMerger.add(new Mapper() {
                                    public void apply(Glob from, MutableGlob to) {
                                        String res = merger.merge(from);
                                        if (res != null) {
                                            to.set(str, res);
                                        }
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
                Merger merger = new MergerTemplate(from.get(FieldMappingType.TemplateType.template),
                        extractFields);
                fieldMerger.add(new Mapper() {
                                    public void apply(Glob from, MutableGlob to) {
                                        String res = merger.merge(from);
                                        if (res != null) {
                                            to.set(str, res);
                                        }
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

        MergerTemplate(String template, Map<String, ExtractField> extractFields) {
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
                    tokens.add(new ExtractFieldToken(extractFields.get(name)));
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

        public ExtractField(StringField fromField, String defaultValue,
                            Formatter formatter) {
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
}
