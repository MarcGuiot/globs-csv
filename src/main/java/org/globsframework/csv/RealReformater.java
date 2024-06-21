package org.globsframework.csv;

import org.globsframework.csv.model.FieldMappingType;
import org.globsframework.json.GSonUtils;
import org.globsframework.metamodel.fields.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.*;
import org.globsframework.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;
import org.globsframework.utils.Strings;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
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
                onFrom(fromType, from, str);
            } else if (from.getType() == FieldMappingType.TemplateType.TYPE) {
                onTemplate(fromType, from, str);
            } else if (from.getType() == FieldMappingType.OverrideData.TYPE) {
                onOverride(fromType, fieldName, from, str);
            } else if (from.getType() == FieldMappingType.MappingData.TYPE) {
                onMapping(fromType, from, str);
            } else if (from.getType() == FieldMappingType.JoinType.TYPE) {
                onJoin(fromType, from, str);
            } else if (from.getType() == FieldMappingType.SumData.TYPE) {
                onSum(fromType, from, str);
            }
        }
        resultType = outTypeBuilder.get();
    }

    private void onFrom(GlobType fromType, Glob from, StringField str) {
        final Field fromTypeField = fromType.getField(from.get(FieldMappingType.FromType.from));
        ExtractField extractField =
                ExtractField.create(fromTypeField,
                        from.get(FieldMappingType.FromType.defaultValueIfEmpty),
                        from.get(FieldMappingType.FromType.toStringFormater),
                        buildFormater(from.getOrEmpty(FieldMappingType.FromType.formater)));
        Merger merger = new FormatMerger(extractField);
        fieldMerger.add((input, to) -> {
                    String res = merger.merge(input);
                    if (res != null) {
                        to.set(str, res);
                    }
                }
        );
    }

    private void onMapping(GlobType fromType, Glob from, StringField str) {
        final Glob f = from.get(FieldMappingType.MappingData.from);
        final Field field = fromType.getField(f.get(FieldMappingType.FromType.from));
        ExtractField extractField =
                ExtractField.create(field,
                        f.get(FieldMappingType.FromType.defaultValueIfEmpty),
                        f.get(FieldMappingType.FromType.toStringFormater),
                        buildFormater(f.getOrEmpty(FieldMappingType.FromType.formater)));
        final Glob[] data = from.getOrEmpty(FieldMappingType.MappingData.mapping);
        Map<String, String> keyToValues = Arrays.stream(data).collect(Collectors.toMap(FieldMappingType.KeyValue.key, FieldMappingType.KeyValue.value));
        if (from.isTrue(FieldMappingType.MappingData.copyValueIfNoMapping)) {
            fieldMerger.add((input, to) -> {
                final String tr = extractField.tr(input);
                if (tr != null) {
                    final String newValue = keyToValues.get(tr);
                    to.set(str, newValue != null ? newValue : tr);
                }
            });
        } else {
            final String def = from.get(FieldMappingType.MappingData.defaultValueNoMapping);
            fieldMerger.add((input, to) -> {
                final String tr = extractField.tr(input);
                if (tr != null) {
                    final String newValue = keyToValues.get(tr);
                    if (newValue != null) {
                        to.set(str, newValue);
                    } else if (Strings.isNotEmpty(def)) {
                        to.set(str, def);
                    }
                }
            });
        }
    }

    private void onOverride(GlobType fromType, String fieldName, Glob from, StringField str) {
        List<ExtractField> extractFields = new ArrayList<>();
        for (Glob f : from.getOrEmpty(FieldMappingType.OverrideData.inputField)) {
            extractFields.add(
                    ExtractField.create(
                            fromType.getField(f.get(FieldMappingType.FromType.from)),
                            f.get(FieldMappingType.FromType.defaultValueIfEmpty),
                            f.get(FieldMappingType.FromType.toStringFormater),
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
    }

    private void onTemplate(GlobType fromType, Glob from, StringField str) {
        Merger merger = getMerger(fromType, from);
        fieldMerger.add((input, to) -> {
                    String res = merger.merge(input);
                    if (res != null) {
                        to.set(str, res);
                    }
                }
        );
    }

    private Merger getMerger(GlobType fromType, Glob template) {
        Map<String, ExtractField> extractFields = new HashMap<>();
        for (Glob extr : template.getOrEmpty(FieldMappingType.TemplateType.from)) {
            Glob f = extr.get(FieldMappingType.RenamedType.from);
            String renamed = extr.get(FieldMappingType.RenamedType.renameTo,
                    f.get(FieldMappingType.FromType.from));
            extractFields.put(renamed,
                    ExtractField.create(
                            fromType.getField(f.get(FieldMappingType.FromType.from)),
                            f.get(FieldMappingType.FromType.defaultValueIfEmpty),
                            f.get(FieldMappingType.FromType.toStringFormater),
                            buildFormater(f.getOrEmpty(FieldMappingType.FromType.formater)))
            );
        }
        Merger merger =
                new MergerTemplate(fromType, template.get(FieldMappingType.TemplateType.template), extractFields,
                        this.externalVariables, template.isTrue(FieldMappingType.TemplateType.noValueIfOnIsMissing));
        return merger;
    }

    private void onSum(GlobType fromType, Glob from, StringField str) {
        List<ExtractField> extractFields = new ArrayList<>();
        for (Glob f : from.getOrEmpty(FieldMappingType.SumData.from)) {
            extractFields.add(
                    ExtractField.create(
                            fromType.getField(f.get(FieldMappingType.FromType.from)),
                            f.get(FieldMappingType.FromType.defaultValueIfEmpty),
                            f.get(FieldMappingType.FromType.toStringFormater),
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

    private void onJoin(GlobType fromType, Glob from, StringField str) {
        final Glob[] on = from.getOrEmpty(FieldMappingType.JoinType.from);
        final ExtractField[] extractFields = Arrays.stream(on).map(f ->
                        ExtractField.create(fromType.getField(f.get(FieldMappingType.FromType.from)),
                                f.get(FieldMappingType.FromType.defaultValueIfEmpty),
                                f.get(FieldMappingType.FromType.toStringFormater),
                                buildFormater(f.getOrEmpty(FieldMappingType.FromType.formater))))
                .toArray(ExtractField[]::new);

        final String separator = from.get(FieldMappingType.JoinType.separator, "");
        final String first = from.get(FieldMappingType.JoinType.first, "");
        final boolean addFirst = from.isTrue(FieldMappingType.JoinType.addFirstIfEmpty);
        final String last = from.get(FieldMappingType.JoinType.last, "");
        final boolean addLast = from.isTrue(FieldMappingType.JoinType.addLastIfEmpty);

        fieldMerger.add((input, to) -> {
            StringBuilder data = new StringBuilder();
            for (ExtractField extractField : extractFields) {
                final String tr = extractField.tr(input);
                if (Strings.isNotEmpty(tr)) { // should be manage using defaultValueIfEmpty but front do not differ null and empty.
                    if (data.isEmpty()) {
                        if (!first.isEmpty()) {
                            data.append(first);
                        }
                    } else {
                        data.append(separator);
                    }
                    data.append(tr);
                }
            }

            if (data.isEmpty()) {
                if (!first.isEmpty() && addFirst) {
                    data.insert(0, first);
                }
                if (!last.isEmpty() && addLast) {
                    data.append(last);
                }
            }
            else {
                data.append(last);
            }
            if (!data.isEmpty()) {
                to.set(str, data.toString());
            }
        });
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

    interface AnyFormater<T> {
        String format(T value);
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
        private final boolean noValueIfOneIsUnset;
        private final List<Token> tokens = new ArrayList<>();

        MergerTemplate(GlobType fromType, String template, Map<String, ExtractField> extractFields,
                       Map<String, DataAccess> externalVariables, boolean noValueIfOneIsUnset) {
            this.noValueIfOneIsUnset = noValueIfOneIsUnset;
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
                if (i < split.length && !split[i].isEmpty()) {
                    tokens.add(new StrToken(split[i]));
                }
            } else {
                tokens.add(new StrToken(template));
            }
        }

        public String merge(Glob from) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Token token : tokens) {
                String token1 = token.getToken(from);
                if (Strings.isNullOrEmpty(token1) && noValueIfOneIsUnset) {
                    return null;
                }
                stringBuilder.append(token1);
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
//            if (matcher.matches()) {
            return matcher.replaceAll(result);
//            }
//            return value;
        }
    }


    interface ExtractField {
        String tr(Glob from);

        static ExtractField create(Field fromField, String defaultValue, String typeFormatter, Formatter formatter) {
            if (fromField instanceof StringField) {
                return new StringExtractField((StringField) fromField, defaultValue, formatter);
            }
            if (fromField instanceof DoubleField) {
                AnyFormater<Object> decimalFormat;
                if (Strings.isNotEmpty(typeFormatter)) {
                    decimalFormat = new DecimalFormat(typeFormatter)::format;
                }
                else {
                    decimalFormat = value -> Double.toString((Double) value);
                }
                return new DecimalExtractField((DoubleField) fromField, defaultValue, formatter, decimalFormat);
            }
            if (fromField instanceof LongField) {
                AnyFormater<Object> decimalFormat;
                if (Strings.isNotEmpty(typeFormatter)) {
                    decimalFormat = new DecimalFormat(typeFormatter)::format;
                }
                else {
                    decimalFormat = value -> Long.toString((Long) value);
                }
                return new LongExtractField((LongField) fromField, defaultValue, formatter, decimalFormat);
            }
            if (fromField instanceof IntegerField) {
                AnyFormater<Object> decimalFormat;
                if (Strings.isNotEmpty(typeFormatter)) {
                    decimalFormat = new DecimalFormat(typeFormatter)::format;
                }
                else {
                    decimalFormat = value -> Integer.toString((Integer) value);
                }
                return new IntegerExtractField((IntegerField) fromField, defaultValue, formatter, decimalFormat);
            }
            if (fromField instanceof DateTimeField) {
                DateTimeFormatter dateTimeFormatter;
                if (Strings.isNotEmpty(typeFormatter)) {
                    dateTimeFormatter = DateTimeFormatter.ofPattern(typeFormatter);
                }
                else {
                    dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                }
                return new DateTimeExtractField((DateTimeField) fromField, defaultValue, formatter, dateTimeFormatter::format);
            }
            if (fromField instanceof DateField) {
                DateTimeFormatter dateTimeFormatter;
                if (Strings.isNotEmpty(typeFormatter)) {
                    dateTimeFormatter = DateTimeFormatter.ofPattern(typeFormatter);
                }
                else {
                    dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                }
                return new DateExtractField((DateField) fromField, defaultValue, formatter, dateTimeFormatter::format);
            }
            return new ExtractField() {
                @Override
                public String tr(Glob from) {
                    Object data = from.getValue(fromField);
                    if (data == null) {
                        return defaultValue;
                    }
                    return formatter.format(data.toString());
                }
            };
        }


        class DecimalExtractField implements ExtractField {
            private final DoubleField fromField;
            private final String defaultValue;
            private final Formatter formatter;
            private final AnyFormater<Object> decimalFormat;

            public DecimalExtractField(DoubleField fromField, String defaultValue, Formatter formatter, AnyFormater<Object> decimalFormat) {
                this.fromField = fromField;
                this.defaultValue = defaultValue;
                this.formatter = formatter;
                this.decimalFormat = decimalFormat;
            }

            @Override
            public String tr(Glob from) {
                Double data = from.get(fromField);
                if (data == null) {
                    return defaultValue;
                }
                return formatter.format(decimalFormat.format(data));
            }
        }

        class LongExtractField implements ExtractField {
            private final LongField fromField;
            private final String defaultValue;
            private final Formatter formatter;
            private final AnyFormater<Object> decimalFormat;

            public LongExtractField(LongField fromField, String defaultValue, Formatter formatter, AnyFormater<Object> decimalFormat) {
                this.fromField = fromField;
                this.defaultValue = defaultValue;
                this.formatter = formatter;
                this.decimalFormat = decimalFormat;
            }

            @Override
            public String tr(Glob from) {
                Long data = from.get(fromField);
                if (data == null) {
                    return defaultValue;
                }
                return formatter.format(decimalFormat.format(data));
            }
        }

        class IntegerExtractField implements ExtractField {
            private final IntegerField fromField;
            private final String defaultValue;
            private final Formatter formatter;
            private final AnyFormater<Object> decimalFormat;

            public IntegerExtractField(IntegerField fromField, String defaultValue, Formatter formatter, AnyFormater<Object> decimalFormat) {
                this.fromField = fromField;
                this.defaultValue = defaultValue;
                this.formatter = formatter;
                this.decimalFormat = decimalFormat;
            }

            @Override
            public String tr(Glob from) {
                Integer data = from.get(fromField);
                if (data == null) {
                    return defaultValue;
                }
                return formatter.format(decimalFormat.format(data));
            }
        }

        class DateTimeExtractField implements ExtractField {
            private final DateTimeField fromField;
            private final String defaultValue;
            private final Formatter formatter;
            private final AnyFormater<TemporalAccessor> dateTimeFormatter;

            public DateTimeExtractField(DateTimeField fromField, String defaultValue, Formatter formatter, AnyFormater<TemporalAccessor> dateTimeFormatter) {
                this.fromField = fromField;
                this.defaultValue = defaultValue;
                this.formatter = formatter;
                this.dateTimeFormatter = dateTimeFormatter;
            }

            @Override
            public String tr(Glob from) {
                ZonedDateTime data = from.get(fromField);
                if (data == null) {
                    return defaultValue;
                }
                return formatter.format(dateTimeFormatter.format(data));
            }
        }

        class DateExtractField implements ExtractField {
            private final DateField fromField;
            private final String defaultValue;
            private final Formatter formatter;
            private final AnyFormater<TemporalAccessor> dateTimeFormatter;

            public DateExtractField(DateField fromField, String defaultValue, Formatter formatter, AnyFormater<TemporalAccessor> dateTimeFormatter) {
                this.fromField = fromField;
                this.defaultValue = defaultValue;
                this.formatter = formatter;
                this.dateTimeFormatter = dateTimeFormatter;
            }

            @Override
            public String tr(Glob from) {
                LocalDate data = from.get(fromField);
                if (data == null) {
                    return defaultValue;
                }
                return formatter.format(dateTimeFormatter.format(data));
            }
        }
    }

    static class StringExtractField  implements ExtractField {
        private final StringField fromField;
        private final String defaultValue;
        private final Formatter formatter;

        public StringExtractField(StringField fromField, String defaultValue, Formatter formatter) {
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
