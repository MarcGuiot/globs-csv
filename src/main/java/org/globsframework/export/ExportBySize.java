package org.globsframework.export;

import org.globsframework.export.annotation.*;
import org.globsframework.metamodel.fields.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.annotations.IsDate;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.Glob;
import org.globsframework.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ExportBySize {
    private static Logger LOGGER = LoggerFactory.getLogger(ExportBySize.class);
    private PaddingType withPadding = null;
    private boolean withSeparator = false;
    private char separator;
    private char arraySeparator = ',';
    private String defaultDateFormat;
    private String defaultDoubleFormat;
    private String trueValue;
    private String falseValue;
    private Character escape = '"';
    private ExportGlob exportGlob;
    private Set<Field> fieldsToExclude = new HashSet<>();
    private Set<String> filter = new HashSet<>();
    private String name;

    public ExportBySize() {
    }

    public ExportBySize withArraySeparator(char arraySeparator) {
        this.arraySeparator = arraySeparator;
        return this;
    }

    public ExportBySize withSeparator(char separator) {
        withSeparator = true;
        this.separator = separator;
        exportGlob = null;
        return this;
    }

    public ExportBySize withRightPadding() {
        withPadding = PaddingType.right;
        exportGlob = null;
        return this;
    }

    public ExportBySize withLeftPadding() {
        withPadding = PaddingType.left;
        exportGlob = null;
        return this;
    }

    public ExportBySize filterBy(String... name) {
        this.filter.addAll(List.of(name));
        return this;
    }

    public void exportMulti(GlobType rootType, Stream<Glob> globStream, Writer writer) {
        if (exportGlob == null) {
            exportGlob = new ExportGlob(this, withPadding != null ? new RealPaddingFactory(withPadding) : field -> Padding.NOPADDING);
        }
        Field[] fields = rootType.getFields();
        LineWriter lineWriter = new LineWriterToWriter(writer);
        List<Consumer<Glob>> consumers = new ArrayList<>();
        for (Field field : fields) {
            if (field.hasAnnotation(CsvHeader.KEY)) {
                String header = field.getAnnotation(CsvHeader.KEY).get(CsvHeader.name);
                if (field instanceof GlobArrayField) {
                    consumers.add(glob -> {
                        Glob[] orEmpty = glob.getOrEmpty((GlobArrayField) field);
                        for (Glob glob1 : orEmpty) {
                            exportGlob.add(lineWriter, header, false);
                            exportGlob.accept(glob1, lineWriter);
                        }
                    });
                } else if (field instanceof GlobField) {
                    consumers.add(glob -> {
                        exportGlob.add(lineWriter, header, false);
                        exportGlob.accept(glob.get((GlobField) field), lineWriter);
                    });
                }
            }
        }
        globStream.forEach(glob -> {
            for (Consumer<Glob> consumer : consumers) {
                consumer.accept(glob);
            }
        });
    }

    public void export(Stream<Glob> globStream, Writer writer) {
        export(globStream, new LineWriterToWriter(writer));
    }

    public void export(Stream<Glob> globStream, LineWriter writer) {
        if (exportGlob == null) {
            exportGlob = new ExportGlob(this, withPadding != null ? new RealPaddingFactory(withPadding) : field -> Padding.NOPADDING);
        }
        globStream.forEach(glob -> {
            exportGlob.accept(glob, writer);
        });
    }

    public Consumer<Glob> export(Writer writer) {
        return export(new LineWriterToWriter(writer));
    }

    public Consumer<Glob> export(LineWriter writer) {
        if (exportGlob == null) {
            exportGlob = new ExportGlob(this, withPadding != null ? new RealPaddingFactory(withPadding) : field -> Padding.NOPADDING);
        }
        return glob -> exportGlob.accept(glob, writer);
    }

    public ExportBySize excludeField(Field field) {
        this.fieldsToExclude.add(field);
        return this;
    }

    public void exportHeader(GlobType headerType, Writer writer) {
        exportHeader(headerType, new LineWriterToWriter(writer));
    }

    public void exportHeader(GlobType headerType, LineWriter writer) {
        if (exportGlob == null) {
            exportGlob = new ExportGlob(this, withPadding != null ? new RealPaddingFactory(withPadding) : field -> Padding.NOPADDING);
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

    public ExportBySize named(String name) {
        this.name = name;
        return this;
    }

    enum PaddingType {
        left, right
    }

    interface FieldWrite {
        void write(Glob glob, LineWriter writer);

        void writeHeader(LineWriter writer, String name);
    }

    interface AddSeperator {
        AddSeperator NULL = (lineWriter, isLast) -> {
        };

        void seperate(LineWriter writer, boolean isLast);
    }

    interface Padding {
        Padding NOPADDING = s -> s == null ? "" : s;

        String pad(String string);
    }

    interface PaddingFactory {
        Padding create(Field field);
    }

    public interface LineWriter {
        void append(String str);

        void append(char str);

        void newLine();
    }

    static class RealAddSeparator implements AddSeperator {
        final char sep;

        RealAddSeparator(char sep) {
            this.sep = sep;
        }

        public void seperate(LineWriter writer, boolean isLast) {
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
        private PaddingType paddingType;

        public RealPadding(Field field, int size, PaddingType paddingType) {
            this.field = field;
            blank = BLANK.substring(0, size);
            this.size = size;
            this.paddingType = paddingType;
        }

        public String pad(String strValue) {
            if (strValue == null) {
                return blank;
            } else {
                if (strValue.length() > size) {
                    throw new RuntimeException("Invalid size '" + strValue + "' took more than " + size + " character for " + field.getFullName());
                }
                if (paddingType == PaddingType.left) {
                    String s = blank + strValue;
                    return s.substring(s.length() - size, s.length());
                } else if (paddingType == PaddingType.right) {
                    String s = strValue + blank;
                    return s.substring(0, size);
                } else {
                    throw new RuntimeException("Unexpected padding " + paddingType);
                }
            }
        }
    }

    static class RealPaddingFactory implements PaddingFactory {
        private PaddingType withPadding;

        public RealPaddingFactory(PaddingType withPadding) {
            this.withPadding = withPadding;
        }

        public Padding create(Field field) {
            Glob annotation = field.getAnnotation(ExportColumnSize.KEY);
            if (annotation == null) {
                LOGGER.warn("No column size defined for " + field.getFullName());
                return null;
            }
            Integer size = annotation.get(ExportColumnSize.SIZE);
            return new RealPadding(field, size, withPadding);
        }
    }

    static class WriteObject {
        private final ExportBySize exportBySize;
        private final List<FieldWrite> fieldWrites = new ArrayList<>();
        private final AddSeperator separator;
        private final String name;

        public WriteObject(ExportBySize exportBySize, GlobType type, AddSeperator separator, PaddingFactory paddingFactory,
                           Set<Field> fieldsToExclude, Set<String> names, String name) {
            this.exportBySize = exportBySize;
            this.separator = separator;
            this.name = name;
            for (Field field : type.getFields()) {
                if (names.isEmpty() || field.findOptAnnotation(NamedExport.KEY)
                        .map(NamedExport.names).stream().flatMap(Stream::of).anyMatch(names::contains)) {
                    if (!fieldsToExclude.contains(field)) {
                        Padding padding = paddingFactory.create(field);
                        if (padding == null) {
                            LOGGER.warn("Field Ignored " + field.getFullName());
                        } else {
                            fieldWrites.add(field.safeAccept(new FieldWriterVisitor(exportBySize, padding, name)).fieldWrite);
                        }
                    }
                }
            }
        }

        public void writeHeader(LineWriter writer) {
            for (Iterator<FieldWrite> iterator = fieldWrites.iterator(); iterator.hasNext(); ) {
                FieldWrite fieldWrite = iterator.next();
                fieldWrite.writeHeader(writer, name);
                separator.seperate(writer, !iterator.hasNext());
            }
        }

        public void write(Glob glob, LineWriter writer) throws IOException {
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

        public FieldWriterVisitor(ExportBySize exportBySize, Padding padding, String name) {
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
            fieldWrite = new StringFieldWrite(field, exportBySize, padding);
        }

        public void visitLong(LongField field) throws Exception {
            // date or dateTime as long
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

        public void visitStringArray(StringArrayField field) throws Exception {
            fieldWrite = new StringArrayFieldWrite(field, exportBySize, padding);
        }
    }

    static abstract class HeaderFieldWrite implements FieldWrite {
        final Field field;

        protected HeaderFieldWrite(Field field) {
            this.field = field;
        }

        public void writeHeader(LineWriter writer, String name) {
            writer.append(ReNamedExport.getHeaderName(name, field));
        }
    }

    static class StringFieldWrite extends HeaderFieldWrite {
        private final Padding padding;
        private final StringField field;
        private ExportBySize exportBySize;

        public StringFieldWrite(StringField field, ExportBySize exportBySize, Padding padding) {
            super(field);
            this.field = field;
            this.exportBySize = exportBySize;
            this.padding = padding;
        }

        public void write(Glob glob, LineWriter writer) {
            String value = glob.get(field);
            if (value == null) {
                writer.append(padding.pad(null));
                return;
            }
            if (value.indexOf(exportBySize.separator) != -1) {
                if (value.indexOf(exportBySize.escape) != -1) {
                    value = value.replaceAll("" + exportBySize.escape, "" + exportBySize.escape + "" + exportBySize.escape);
                }
                value = exportBySize.escape + value + exportBySize.escape;
            }
            else {
                if (value.indexOf(exportBySize.escape) != -1) {
                    value = value.replaceAll("" + exportBySize.escape, "" + exportBySize.escape + "" + exportBySize.escape);
                    value = exportBySize.escape + value + exportBySize.escape;
                }
            }
            writer.append(padding.pad(value.replace("\n", "\\n")));
        }
    }

    static class StringArrayFieldWrite extends HeaderFieldWrite {
        private final Padding padding;
        private final StringArrayField field;
        private ExportBySize exportBySize;

        public StringArrayFieldWrite(StringArrayField field, ExportBySize exportBySize, Padding padding) {
            super(field);
            this.field = field;
            this.exportBySize = exportBySize;
            this.padding = padding;
        }

        public void write(Glob glob, LineWriter writer) {
            String[] value = glob.get(field);
            if (value == null || value.length == 0) {
                writer.append(padding.pad(null));
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : value) {
                if (s.indexOf(exportBySize.separator) != -1) {
                    if (s.indexOf(exportBySize.escape) != -1) {
                        s = s.replaceAll("" + exportBySize.escape, "" + exportBySize.escape + "" + exportBySize.escape);
                    }
                    s = exportBySize.escape + s + exportBySize.escape;
                }
                stringBuilder.append(s)
                        .append(exportBySize.arraySeparator);
            }
            if (stringBuilder.length() != 0) {
                // remove last arraySeparator.
                stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
            }
            String outputValue = stringBuilder.toString();
            writer.append(padding.pad(outputValue.replace("\n", "\\n")));
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

        public void write(Glob glob, LineWriter writer) {
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

        public void write(Glob glob, LineWriter writer) {
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

        public void write(Glob glob, LineWriter writer) {
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

        public void write(Glob glob, LineWriter writer) {
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
                    LOGGER.warn("No date format for " + field.getName() + " , export to yyyy/MM/dd");
                    format = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                }
            } else {
                format = DateTimeFormatter.ofPattern(dateFormat.get(ExportDateFormat.FORMAT));
            }
        }

        public void write(Glob glob, LineWriter writer) {
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
                    LOGGER.warn("No date format, export to yyyy/MM/dd HH:mm:ss");
                    format = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                }
            } else {
                format = DateTimeFormatter.ofPattern(dateFormat.get(ExportDateFormat.FORMAT));
            }
        }

        public void write(Glob glob, LineWriter writer) {
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

        public void write(Glob glob, LineWriter writer) {
            Integer value = glob.get(field);
            writer.append(padding.pad(value == null ? null : format.format(LocalDate.ofEpochDay(value))));
        }
    }

    public static class LineWriterToWriter implements LineWriter {
        private final Writer writer;

        public LineWriterToWriter(Writer writer) {
            this.writer = writer;
        }

        public void append(String str) {
            try {
                if (str != null) {
                    writer.append(str);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void append(char ch) {
            try {
                writer.append(ch);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void newLine() {
            try {
                writer.append("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class ExportGlob {
        private final Map<GlobType, WriteObject> writeObjectMap = new HashMap<>();
        private ExportBySize exportBySize;
        private PaddingFactory paddingFactory;
        private AddSeperator separator;

        public ExportGlob(ExportBySize exportBySize, PaddingFactory paddingFactory) {
            this.exportBySize = exportBySize;
            this.paddingFactory = paddingFactory;
            separator = exportBySize.withSeparator ? new RealAddSeparator(exportBySize.separator) : AddSeperator.NULL;
        }

        public void accept(Glob glob, LineWriter writer) {
            GlobType type = glob.getType();
            WriteObject writeObject = writeObjectMap.computeIfAbsent(type, this::apply);
            try {
                writeObject.write(glob, writer);
                writer.newLine();
            } catch (IOException e) {
                throw new RuntimeException("Error in export", e);
            }
        }

        private void add(LineWriter writer, String value, boolean isLast) {
            writer.append(value);
            separator.seperate(writer, isLast);
        }

        private WriteObject apply(GlobType globType) {
            return new WriteObject(exportBySize, globType, separator, paddingFactory, exportBySize.fieldsToExclude,
                    exportBySize.filter, exportBySize.name);
        }

        public void exportHeader(GlobType headerType, Writer writer) {
            exportHeader(headerType, new LineWriterToWriter(writer));
        }

        public void exportHeader(GlobType headerType, LineWriter writer) {
            WriteObject writeObject = writeObjectMap.computeIfAbsent(headerType, this::apply);
            writeObject.writeHeader(writer);
            writer.newLine();
        }

    }

}

