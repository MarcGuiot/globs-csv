package org.globsframework.export;

import org.globsframework.export.annotation.ExportDateFormat;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeBuilder;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;
import org.globsframework.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComplexImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexImporter.class);
    private final GlobType csvType;
    private final GlobType target;

    public ComplexImporter(GlobType sourceType, GlobType target) {
//        GlobTypeBuilder builder = new DefaultGlobTypeBuilder("csv");
//        flat(builder, target);
        csvType = sourceType;
        this.target = target;
    }

    public static void flat(GlobTypeBuilder builder, GlobType type) {
        for (Field field : type.getFields()) {
            if (field.getDataType().isPrimive()) {
                builder.declare(field.getName(), field.getDataType(), field.streamAnnotations().collect(Collectors.toList()));
            } else if (field instanceof GlobArrayField) {
                flat(builder, ((GlobArrayField) field).getTargetType());
            } else if (field instanceof GlobField) {
                throw new RuntimeException("Not implemented");
                //flat(builder, ((GlobField) field).getTargetType());
            }
        }
    }

    public static ConvertFromStr convert(Field field, boolean trim, boolean emptyIsNotNull) {
        return field.safeVisit(new FieldVisitor.AbstractFieldVisitor() {
            ConvertFromStr convert;

            @Override
            public void visitInteger(IntegerField field) throws Exception {
                convert = new IntegerFieldReader(field);
            }

            @Override
            public void visitDouble(DoubleField field) throws Exception {
                convert = new DoubleFieldReader(field);
            }

            @Override
            public void visitBigDecimal(BigDecimalField field) throws Exception {
                convert = new BigDecimalFieldReader(field);
            }

            @Override
            public void visitString(StringField field) throws Exception {
                convert = new StringFieldReader(emptyIsNotNull, field, trim);
            }

            @Override
            public void visitBoolean(BooleanField field) throws Exception {
                convert = new BooleanFieldReader(field);
            }

            @Override
            public void visitLong(LongField field) throws Exception {
                convert = new LongFieldReader(field);
            }

            @Override
            public void visitDate(DateField field) throws Exception {
                convert = new DateFieldReader(field);
            }

            @Override
            public void visitDateTime(DateTimeField field) throws Exception {
                convert = new DateTimeFieldReader(field);
            }
        }).convert;
    }

    ConsumerWithCurrent create(Consumer<Glob> consumer) {
        State build = CompositeState.build(target, csvType);
        return new ConsumerWithCurrent(consumer, build);
    }

    interface State {
        Glob onNewLine(Glob line);

        void reset();
    }

    interface ConvertFromStr {
        Object convert(String value);
    }

    static class ConsumerWithCurrent implements Consumer<Glob> {
        final Consumer<Glob> consumer;
        private final State build;
        private Glob current = null;

        ConsumerWithCurrent(Consumer<Glob> consumer, State build) {
            this.consumer = consumer;
            this.build = build;
        }

        public void accept(Glob glob) {
            Glob newGlob = build.onNewLine(glob);
            if (newGlob != null) {
                if (current != null) {
                    consumer.accept(current);
                }
                current = newGlob;
            }
        }

        void end() {
            if (current != null) {
                consumer.accept(current);
                current = null;
            }
        }
    }

    record Attr(Field array, State state) {
    }

    record LineToTargetField(Field from, Field to, ConvertFromStr convert) {
    }

    static class FieldMapper {
        List<LineToTargetField> fields = new ArrayList<>();

        void add(Field fromField, Field toField, ConvertFromStr convert) {
            fields.add(new LineToTargetField(fromField, toField, convert));
        }

        boolean isSame(Glob current, Glob line) {
            for (LineToTargetField field : fields) {
                if (!field.to.valueEqual(current.getValue(field.to), field.convert.convert((String) line.getValue(field.from)))) {
                    return false;
                }
            }
            return true;
        }

        boolean copy(MutableGlob to, Glob from) {
            boolean hasChange = false;
            for (LineToTargetField field : fields) {
                Object value = from.getValue(field.from);
                if (value != null) {
                    hasChange = true;
                    to.setValue(field.to, field.convert.convert((String) value));
                }
            }
            return hasChange;
        }
    }

    static class CompositeState implements State {
        FieldMapper fieldMapper;
        GlobType type;
        List<Attr> attrs;
        MutableGlob current;
        private boolean wasReturn;

        public CompositeState(FieldMapper fieldMapper, GlobType type, List<Attr> attrs) {
            this.fieldMapper = fieldMapper;
            this.type = type;
            this.attrs = attrs;
        }

        public static State build(GlobType to, GlobType from) {
            List<Attr> attrs = new ArrayList<>();
            FieldMapper fieldMapper = new FieldMapper();
            for (Field toField : to.getFields()) {
                if (toField.getDataType().isPrimive()) {
                    Field fromField = from.findField(toField.getName());
                    if (fromField == null) {
                        LOGGER.info("field " + toField.getName() + " not found in " + to.getName());
                    } else {
                        ConvertFromStr convert = convert(toField, true, true);
                        fieldMapper.add(fromField, toField, convert);
                    }
                } else if (toField instanceof GlobArrayField) {
                    attrs.add(new Attr(toField, build(((GlobArrayField) toField).getTargetType(), from)));
                } else if (toField instanceof GlobField) {
                    attrs.add(new Attr(toField, build(((GlobField) toField).getTargetType(), from)));
                } else {
                    throw new RuntimeException("Not managed");
                }
            }
            return new CompositeState(fieldMapper, to, attrs);
        }

        public Glob onNewLine(Glob line) {
            boolean hasChange = false;
            if (current == null || !fieldMapper.isSame(current, line)) {
                current = type.instantiate();
                wasReturn = false;
                for (Attr attr : attrs) {
                    attr.state.reset();
                }
                hasChange = fieldMapper.copy(current, line);
            }
            for (Attr attr : attrs) {
                Glob glob = attr.state.onNewLine(line);
                if (glob != null) {
                    if (attr.array instanceof GlobArrayField arrayField) {
                        Glob[] d = current.getOrEmpty(arrayField);
                        d = Arrays.copyOf(d, d.length + 1);
                        d[d.length - 1] = glob;
                        current.set(arrayField, d);
                        hasChange = true;
                    } else {
                        GlobField field = (GlobField) attr.array;
                        current.set(field, glob);
                        hasChange = true;
                    }
                }
            }

            if (hasChange && !wasReturn) {
                wasReturn = true;
                return current;
            } else {
                return null;
            }
        }

        public void reset() {
            for (Attr attr : attrs) {
                attr.state.reset();
            }
            current = null;
        }
    }

    static class IntegerFieldReader implements ConvertFromStr {
        final IntegerField field;
        private final Pattern removeZero;

        IntegerFieldReader(IntegerField field) {
            this.field = field;
            removeZero = Pattern.compile("\\.0*$");
        }

        public Object convert(String s) {
            if (Strings.isNotEmpty(s)) {
                s = removeZero.matcher(s.trim()).replaceAll("");
                return Integer.parseInt(s);
            }
            return null;
        }
    }

    static class BooleanFieldReader implements ConvertFromStr {
        final BooleanField field;

        public BooleanFieldReader(BooleanField field) {
            this.field = field;
        }

        public Object convert(String s) {
            if (Strings.isNotEmpty(s)) {
                return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("1");
            }
            return null;
        }
    }

    static class LongFieldReader implements ConvertFromStr {
        final LongField field;
        private final Pattern removeZero;

        LongFieldReader(LongField field) {
            this.field = field;
            removeZero = Pattern.compile("\\.0*$");
        }

        public Object convert(String s) {
            if (Strings.isNotEmpty(s)) {
                s = removeZero.matcher(s.trim()).replaceAll("");
                return Long.parseLong(s);
            } else {
                return null;
            }
        }
    }

    static class DateFieldReader implements ConvertFromStr {
        final DateField field;
        private DateTimeFormatter dateTimeFormatter;

        DateFieldReader(DateField field) {
            this.field = field;
            Glob dataFormat = field.findAnnotation(ExportDateFormat.KEY);
            if (dataFormat != null) {
                String s = dataFormat.get(ExportDateFormat.FORMAT);
                dateTimeFormatter = DateTimeFormatter.ofPattern(s);
            } else {
                dateTimeFormatter = DateTimeFormatter.ISO_DATE;
            }
        }

        public Object convert(String s) {
            if (Strings.isNotEmpty(s)) {
                return LocalDate.from(dateTimeFormatter.parse(s.trim()));
            } else {
                return null;
            }
        }
    }

    static class DateTimeFieldReader implements ConvertFromStr {
        final DateTimeField field;
        private final ZoneId zoneId;
        private DateTimeFormatter dateTimeFormatter;

        DateTimeFieldReader(DateTimeField field) {
            this.field = field;
            Glob dataFormat = field.findAnnotation(ExportDateFormat.KEY);
            if (dataFormat != null) {
                String s = dataFormat.get(ExportDateFormat.FORMAT);
                zoneId = ZoneId.of(dataFormat.get(ExportDateFormat.ZONE_ID, ZoneId.systemDefault().getId()));
                dateTimeFormatter = DateTimeFormatter.ofPattern(s).withZone(zoneId);
            } else {
                zoneId = ZoneId.systemDefault();
                dateTimeFormatter = DateTimeFormatter.ISO_DATE.withZone(zoneId);
            }
        }

        public Object convert(String s) {
            if (Strings.isNotEmpty(s)) {
                TemporalAccessor temporalAccessor = dateTimeFormatter.parseBest(s.trim(), ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
                if (temporalAccessor instanceof ZonedDateTime) {
                    return (ZonedDateTime) temporalAccessor;
                } else if (temporalAccessor instanceof LocalDateTime) {
                    return ((LocalDateTime) temporalAccessor).atZone(zoneId);
                } else if (temporalAccessor instanceof LocalDate) {
                    return ZonedDateTime.of((LocalDate) temporalAccessor, LocalTime.MIDNIGHT, zoneId);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    static class DoubleFieldReader implements ConvertFromStr {
        final DoubleField field;

        DoubleFieldReader(DoubleField field) {
            this.field = field;
        }

        public Object convert(String s) {
            if (Strings.isNotEmpty(s)) {
                return Double.parseDouble(s.trim());
            }
            else {
                return null;
            }
        }
    }

    static class StringFieldReader implements ConvertFromStr {
        final boolean emptyIsNotNull;
        final StringField field;
        private boolean trim;

        StringFieldReader(boolean emptyIsNotNull, StringField field, boolean trim) {
            this.emptyIsNotNull = emptyIsNotNull;
            this.field = field;
            this.trim = trim;
        }

        public Object convert(String s) {
            if (emptyIsNotNull || Strings.isNotEmpty(s)) {
                return s;
            }
            else {
                return null;
            }
        }
    }

    static class BigDecimalFieldReader implements ConvertFromStr {
        final BigDecimalField field;

        BigDecimalFieldReader(BigDecimalField field) {
            this.field = field;
        }

        public Object convert(String s) {
            if (Strings.isNotEmpty(s)) {
                return new BigDecimal(s);
            }
            else {
                return null;
            }
        }
    }
}
