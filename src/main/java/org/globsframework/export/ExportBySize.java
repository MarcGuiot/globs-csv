package org.globsframework.export;

import org.globsframework.export.annotation.ExportBooleanFormat;
import org.globsframework.export.annotation.ExportColumnSize;
import org.globsframework.export.annotation.ExportDateFormat;
import org.globsframework.export.annotation.ExportDoubleFormat;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.Glob;
import org.globsframework.sqlstreams.annotations.IsDate;
import org.globsframework.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class ExportBySize {
    private static Logger LOGGER = LoggerFactory.getLogger(ExportBySize.class);
    private boolean withPadding = false;
    private boolean withSeparator = false;
    private char separator;
    private String defaultDateFormat;
    private String defaultDoubleFormat;
    private String trueValue;
    private String falseValue;
    private ExportGlob exportGlob;

    public ExportBySize() {
    }

    public ExportBySize withSeparator(char separator) {
        withSeparator = true;
        this.separator = separator;
        exportGlob = null;
        return this;
    }

    public ExportBySize withPadding() {
        withPadding = true;
        exportGlob = null;
        return this;
    }

    public void export(Stream<Glob> globStream, StringWriter writer) {
        if (exportGlob == null) {
            exportGlob = new ExportGlob(this, withPadding ? new RealPaddingFactory() : field -> Padding.NOPADDING);
        }
        globStream.forEach(glob -> {
            exportGlob.accept(glob, writer);
        });
    }

    public void exportHeader(GlobType headerType, StringWriter writer) {
        if (exportGlob == null) {
            exportGlob = new ExportGlob(this, withPadding ? new RealPaddingFactory() : field -> Padding.NOPADDING);
        }
        exportGlob.exportHeader(headerType, writer);
    }

    public String getDefaultDateFormat() {
        return defaultDateFormat;
    }

    public void setDefaultDateFormat(String defaultDateFormat) {
        this.defaultDateFormat = defaultDateFormat;
        exportGlob = null;
    }

    public void setDefaultDoubleFormat(String defaultDoubleFormat) {
        this.defaultDoubleFormat = defaultDoubleFormat;
        exportGlob = null;
    }

    public void setBooleanValue(String trueValue, String falseValue) {
        this.trueValue = trueValue;
        this.falseValue = falseValue;
        exportGlob = null;
    }

    interface FieldWrite {
        void write(Glob glob, Writer writer) throws IOException;

        void writeHeader(Writer writer) throws IOException;
    }

    interface AddSeperator {
        AddSeperator NULL = (writer, isLast) -> {
        };

        void seperate(Writer writer, boolean isLast) throws IOException;
    }

    interface Padding {
        Padding NOPADDING = s -> s == null ? "" : s;

        String pad(String string);
    }

    interface PaddingFactory {
        Padding create(Field field);
    }

    static class RealAddSeparator implements AddSeperator {
        final char sep;

        RealAddSeparator(char sep) {
            this.sep = sep;
        }

        public void seperate(Writer writer, boolean isLast) throws IOException {
            if (!isLast) {
                writer.append(sep);
            }
        }
    }

    static class RealPadding implements Padding {
        static String BLANK = "                                                                                                            ";
        protected String blank;
        private Field field;
        private int size;

        public RealPadding(Field field, int size) {
            this.field = field;
            blank = BLANK.substring(0, size);
            this.size = size;
        }

        public String pad(String strValue) {
            if (strValue == null) {
                return blank;
            } else {
                if (strValue.length() > size) {
                    throw new RuntimeException("Invalid size '" + strValue + "' took more than " + size + " character for " + field.getFullName());
                }
                String s = blank + strValue;
                return s.substring(s.length() - size, s.length());
            }
        }
    }

    static class RealPaddingFactory implements PaddingFactory {

        public Padding create(Field field) {
            Glob annotation = field.getAnnotation(ExportColumnSize.KEY);
            if (annotation == null) {
                LOGGER.warn("No column size defined for " + field.getFullName());
                return null;
            }
            Integer size = annotation.get(ExportColumnSize.SIZE);
            return new RealPadding(field, size);
        }
    }


    static class WriteObject {
        private final ExportBySize exportBySize;
        private final List<FieldWrite> fieldWrites = new ArrayList<>();
        private final AddSeperator separator;

        public WriteObject(ExportBySize exportBySize, GlobType type, AddSeperator separator, PaddingFactory paddingFactory) {
            this.exportBySize = exportBySize;
            this.separator = separator;
            for (Field field : type.getFields()) {
                Padding padding = paddingFactory.create(field);
                if (padding == null) {
                    LOGGER.warn("Field Ignored " + field.getFullName());
                } else {
                    fieldWrites.add(field.safeVisit(new FieldWriterVisitor(exportBySize, padding)).fieldWrite);
                }
            }
        }

        public void writeHeader(Writer writer) throws IOException {
            for (Iterator<FieldWrite> iterator = fieldWrites.iterator(); iterator.hasNext(); ) {
                FieldWrite fieldWrite = iterator.next();
                fieldWrite.writeHeader(writer);
                separator.seperate(writer, !iterator.hasNext());
            }
        }

        public void write(Glob glob, Writer writer) throws IOException {
            for (Iterator<FieldWrite> iterator = fieldWrites.iterator(); iterator.hasNext(); ) {
                FieldWrite fieldWrite = iterator.next();
                fieldWrite.write(glob, writer);
                separator.seperate(writer, !iterator.hasNext());
            }
        }
    }


    private static class FieldWriterVisitor extends FieldVisitor.AbstractWithErrorVisitor {
        FieldWrite fieldWrite;
        private ExportBySize exportBySize;
        private Padding padding;

        public FieldWriterVisitor(ExportBySize exportBySize, Padding padding) {
            this.exportBySize = exportBySize;
            this.padding = padding;
        }

        public void visitInteger(IntegerField field) throws Exception {
            if (field.hasAnnotation(IsDate.KEY)) {
                fieldWrite = new DateAsIntFieldWrite(exportBySize, field, padding);
            } else {
                fieldWrite = new IntegerFieldWrite(field, padding);
            }
        }

        public void visitDouble(DoubleField field) throws Exception {
            fieldWrite = new DoubleFieldWrite(exportBySize, field, padding);
        }

        public void visitString(StringField field) throws Exception {
            fieldWrite = new StringFieldWrite(field, padding);
        }

        public void visitLong(LongField field) throws Exception {
            fieldWrite = new LongFieldWrite(field, padding);
        }

        public void visitBoolean(BooleanField field) throws Exception {
            fieldWrite = new BooleanFieldWrite(exportBySize, field, padding);
        }

        public void visitDate(DateField field) throws Exception {
            fieldWrite = new DateFieldWrite(exportBySize, field, padding);
        }

        public void visitDateTime(DateTimeField field) throws Exception {
            fieldWrite = new DatetimeFieldWrite(exportBySize, field, padding);
        }
    }

    static abstract class HeaderFieldWrite implements FieldWrite {
        final Field field;

        protected HeaderFieldWrite(Field field) {
            this.field = field;
        }

        public void writeHeader(Writer writer) throws IOException {
            writer.append(field.getName());
        }
    }


    static class StringFieldWrite extends HeaderFieldWrite {
        private final Padding padding;
        private final StringField field;

        public StringFieldWrite(StringField field, Padding padding) {
            super(field);
            this.field = field;
            this.padding = padding;
        }

        public void write(Glob glob, Writer writer) throws IOException {
            String value = glob.get(field);
            writer.append(padding.pad(value == null ? null : "" + value));
        }
    }

    static class IntegerFieldWrite extends HeaderFieldWrite {
        private IntegerField field;
        private Padding padding;

        public IntegerFieldWrite(IntegerField field, Padding padding) {
            super(field);
            this.field = field;
            this.padding = padding;
        }

        public void write(Glob glob, Writer writer) throws IOException {
            Integer value = glob.get(field);
            writer.append(padding.pad(value == null ? null : "" + value));
        }
    }


    static class BooleanFieldWrite extends HeaderFieldWrite {
        private BooleanField field;
        private Padding padding;
        private String TRUE;
        private String FALSE;

        public BooleanFieldWrite(ExportBySize exportBySize, BooleanField field, Padding padding) {
            super(field);
            this.field = field;
            this.padding = padding;
            Glob booleanFormat = field.findAnnotation(ExportBooleanFormat.KEY);
            if (booleanFormat == null) {
                TRUE = exportBySize.trueValue != null ? exportBySize.trueValue : "1";
                FALSE = exportBySize.falseValue != null ? exportBySize.falseValue : "0";
            } else {
                // use field specific value or file default value or java default falue
                TRUE = booleanFormat.get(ExportBooleanFormat.TRUE_, Strings.isNotEmpty(exportBySize.trueValue) ? exportBySize.trueValue : Boolean.TRUE.toString());
                FALSE = booleanFormat.get(ExportBooleanFormat.FALSE_, Strings.isNotEmpty(exportBySize.falseValue) ? exportBySize.falseValue : Boolean.FALSE.toString());
            }
        }

        public void write(Glob glob, Writer writer) throws IOException {
            Boolean value = glob.get(field);
            writer.append(padding.pad(value == null ? null : value ? TRUE : FALSE));
        }
    }

    static class LongFieldWrite extends HeaderFieldWrite {
        private LongField field;
        private Padding padding;
        private Format format;

        public LongFieldWrite(LongField field, Padding padding) {
            super(field);
            this.field = field;
            this.padding = padding;
        }

        public void write(Glob glob, Writer writer) throws IOException {
            Long value = glob.get(field);
            String strValue = "" + value;
            writer.append(padding.pad(value == null ? null : "" + strValue));
        }
    }

    static class DoubleFieldWrite extends HeaderFieldWrite {
        private DoubleField field;
        private Padding padding;
        private DecimalFormat format;

        public DoubleFieldWrite(ExportBySize exportBySize, DoubleField field, Padding padding) {
            super(field);
            this.field = field;
            this.padding = padding;
            Glob exportDoubleFormat = field.findAnnotation(ExportDoubleFormat.KEY);
            String defaultFormat = Strings.isNotEmpty(exportBySize.defaultDoubleFormat) ? exportBySize.defaultDoubleFormat : "###.#########";
            if (exportDoubleFormat != null) {
                DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
                decimalFormatSymbols.setDecimalSeparator(exportDoubleFormat.get(ExportDoubleFormat.DECIMAL_SEPARATOR, ".").charAt(0));
                format = new DecimalFormat(exportDoubleFormat.get(ExportDoubleFormat.FORMAT, defaultFormat));
                format.setDecimalFormatSymbols(decimalFormatSymbols);
            } else {
                DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
                decimalFormatSymbols.setDecimalSeparator('.');
                format = new DecimalFormat(defaultFormat);
                format.setDecimalFormatSymbols(decimalFormatSymbols);
            }
        }

        public void write(Glob glob, Writer writer) throws IOException {
            Double value = glob.get(field);
            writer.append(padding.pad(value == null ? null : format.format(value)));
        }
    }

    static class DateFieldWrite extends HeaderFieldWrite {
        private DateField field;
        private Padding padding;
        private DateTimeFormatter format;

        public DateFieldWrite(ExportBySize exportBySize, DateField field, Padding padding) {
            super(field);
            this.field = field;
            this.padding = padding;
            Glob dateFormat = field.findAnnotation(ExportDateFormat.KEY);
            if (dateFormat == null || dateFormat.get(ExportDateFormat.FORMAT) == null) {
                if (Strings.isNotEmpty(exportBySize.defaultDateFormat)) {
                    format = DateTimeFormatter.ofPattern(exportBySize.defaultDateFormat);
                } else {
                    LOGGER.warn("No date format, export to yyyy/MM/dd");
                    format = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                }
            } else {
                format = DateTimeFormatter.ofPattern(dateFormat.get(ExportDateFormat.FORMAT));
            }
        }

        public void write(Glob glob, Writer writer) throws IOException {
            LocalDate value = glob.get(field);
            writer.append(padding.pad(value == null ? null : format.format(value)));
        }
    }

    static class DatetimeFieldWrite extends HeaderFieldWrite {
        private DateTimeField field;
        private Padding padding;
        private DateTimeFormatter format;

        public DatetimeFieldWrite(ExportBySize exportBySize, DateTimeField field, Padding padding) {
            super(field);
            this.field = field;
            this.padding = padding;
            Glob dateFormat = field.findAnnotation(ExportDateFormat.KEY);
            if (dateFormat == null || dateFormat.get(ExportDateFormat.FORMAT) == null) {
                if (Strings.isNotEmpty(exportBySize.defaultDateFormat)) {
                    format = DateTimeFormatter.ofPattern(exportBySize.defaultDateFormat);
                } else {
                    LOGGER.warn("No date format, export to yyyy/MM/dd HH/mm/SS");
                    format = DateTimeFormatter.ofPattern("yyyy/MM/dd HH/mm/SS");
                }
            } else {
                format = DateTimeFormatter.ofPattern(dateFormat.get(ExportDateFormat.FORMAT));
            }
        }

        public void write(Glob glob, Writer writer) throws IOException {
            ZonedDateTime value = glob.get(field);
            writer.append(padding.pad(value == null ? null : format.format(value)));
        }
    }

    static class DateAsIntFieldWrite extends HeaderFieldWrite {
        private IntegerField field;
        private Padding padding;
        private DateTimeFormatter format;

        public DateAsIntFieldWrite(ExportBySize exportBySize, IntegerField field, Padding padding) {
            super(field);
            this.field = field;
            this.padding = padding;
            Glob dateFormat = field.getAnnotation(ExportDateFormat.KEY);
            if (dateFormat == null) {
                if (Strings.isNotEmpty(exportBySize.defaultDateFormat)) {
                    format = DateTimeFormatter.ofPattern(exportBySize.defaultDateFormat);
                } else {
                    LOGGER.warn("No date format, export to yyyy/mm/dd");
                    format = DateTimeFormatter.ofPattern("yyyy/mm/dd");
                }
            } else {
                format = DateTimeFormatter.ofPattern(dateFormat.get(ExportDateFormat.FORMAT));
            }
        }

        public void write(Glob glob, Writer writer) throws IOException {
            Integer value = glob.get(field);
            writer.append(padding.pad(value == null ? null : format.format(LocalDate.ofEpochDay(value))));
        }
    }

    private class ExportGlob {
        private final Map<GlobType, WriteObject> writeObjectMap = new HashMap<>();
        private ExportBySize exportBySize;
        private PaddingFactory paddingFactory;

        public ExportGlob(ExportBySize exportBySize, PaddingFactory paddingFactory) {
            this.exportBySize = exportBySize;
            this.paddingFactory = paddingFactory;
        }

        public void accept(Glob glob, Writer writer) {
            GlobType type = glob.getType();
            WriteObject writeObject = writeObjectMap.computeIfAbsent(type, this::apply);
            try {
                writeObject.write(glob, writer);
            } catch (IOException e) {
                throw new RuntimeException("Error in export", e);
            }
        }

        private WriteObject apply(GlobType globType) {
            return new WriteObject(exportBySize, globType,
                    withSeparator ? new RealAddSeparator(separator) : AddSeperator.NULL,
                    paddingFactory);
        }

        public void exportHeader(GlobType headerType, StringWriter writer) {
            try {
                WriteObject writeObject = writeObjectMap.computeIfAbsent(headerType, this::apply);
                writeObject.writeHeader(writer);
            } catch (IOException e) {
                throw new RuntimeException("Error in export", e);
            }
        }
    }
}

