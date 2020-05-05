package org.globsframework.export;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.globsframework.export.annotation.ExportDateFormat;
import org.globsframework.export.annotation.ImportEmptyStringHasEmptyStringFormat;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeBuilder;
import org.globsframework.metamodel.fields.*;
import org.globsframework.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.metamodel.utils.GlobTypeUtils;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;
import org.globsframework.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class ImportFile {
    private static Logger LOGGER = LoggerFactory.getLogger(ImportFile.class);
    private boolean withSeparator;
    private char separator;
    private Charset charSet = StandardCharsets.UTF_8;
    private Map<GlobType, ImportReader> importers = new HashMap<>();
    private ExportBySize.PaddingType paddingType;
    private boolean trim;

    public ImportFile withSeparator(char separator) {
        withSeparator = true;
        this.separator = separator;
        importers = null;
        return this;
    }

    public ImportFile trim() {
        trim = true;
        return this;
    }

    public ImportFile withCharSet(String charSet) {
        this.charSet = Charset.forName(charSet);
        return this;
    }

    public ImportFile withRightPadding() {
        paddingType = ExportBySize.PaddingType.right;
        importers = null;
        return this;
    }

    public ImportFile withLeftPadding() {
        paddingType = ExportBySize.PaddingType.left;
        importers = null;
        return this;
    }

    public Importer create(Reader reader) throws IOException {
        return create(reader, null);
    }

    public Importer create(Reader reader, GlobType globType) throws IOException {
        if (withSeparator) {
            CSVParser parse = load(reader);
            DefaultDataRead dataRead = new DefaultDataRead(parse, trim);
            if (globType == null) {
                globType = dataRead.createDefault();
            }
            return new DefaultImporter(globType, dataRead);
        } else {
            throw new RuntimeException("Not implemented");
        }
    }

    public void importContent(InputStream inputStream, Consumer<Glob> consumer, GlobType globType) throws IOException {
        importContent(new InputStreamReader(new BOMInputStream(inputStream), charSet), consumer, globType);
    }

    public void importContent(Reader reader, Consumer<Glob> consumer, GlobType globType) throws IOException {
        create(reader, globType).consume(consumer);
    }

    public DataRead getDataReader(InputStream inputStream) throws IOException {
        return new DefaultDataRead(load(new InputStreamReader(new BOMInputStream(inputStream), "UTF-8")), trim);
    }

    private CSVParser load(Reader reader) throws IOException {
        CSVFormat csvFormat =
                CSVFormat.DEFAULT
                        .withDelimiter(separator)
                        .withEscape('\\')
                        .withFirstRecordAsHeader();
        return csvFormat.parse(reader);
    }

    static public GlobType extractHeader(InputStream inputStream, Character separator) throws IOException {
        GlobTypeBuilder globTypeBuilder = new DefaultGlobTypeBuilder("DEFAULT");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8));
        String s = reader.readLine();
        if (s == null) {
            LOGGER.warn("Empty file");
            return null;
        }
        if (separator == null) {
            int expectedField = 0;
            int countDotComma = 0;
            int countComma = 0;
            int countTab = 0;
            for (char c : s.toCharArray()) {
                switch (c) {
                    case ';':
                        countDotComma++;
                        break;
                    case ',':
                        countComma++;
                        break;
                    case '\t':
                        countTab++;
                        break;
                }
            }
            if (countComma > countDotComma && countComma > countTab) {
                separator = ',';
                expectedField = countComma;
            }
            if (countDotComma > countComma && countDotComma > countTab) {
                separator = ';';
                expectedField = countDotComma;
            }
            if (countTab > countComma && countTab > countDotComma) {
                separator = '\t';
                expectedField = countTab;
            }
            if (separator != null) {
                LOGGER.info("Found separator '" + separator + "'");
                try {
                    String nexLine = reader.readLine();
                    if (nexLine != null) {
                        int count = 0;
                        for (char c : nexLine.toCharArray()) {
                            if (c == separator) {
                                count++;
                            }
                        }
                        if (count < expectedField) {
                            String message = "Fail to validate separator: '" + separator + "' is present " + count + " times instead of " + expectedField;
                            LOGGER.error(message);
                            throw new RuntimeException(message);
                        }
                    }
                } catch (IOException e) {
                }
            } else {
                String message = "Fail to identify separator wihtin ,;\\t";
                LOGGER.error(message);
                throw new RuntimeException(message);
            }
        }
        CSVFormat csvFormat =
                CSVFormat.DEFAULT
                        .withDelimiter(separator)
                        .withEscape('\\')
                        .withFirstRecordAsHeader();
        CSVParser parse = csvFormat.parse(new StringReader(s));
        Map<String, Integer> headerMap = parse.getHeaderMap();
        for (String s1 : headerMap.keySet()) {
            globTypeBuilder.declareStringField(s1);
        }
        return globTypeBuilder.get();
    }

    private static String getValue(CSVRecord record, int index, boolean trim) {
        String s = record.get(index);
        if (s != null) {
            return s.trim();
        }
//            if (s.length() > 2) {
//                if (s.startsWith("\"") && s.endsWith("\"")) {
//                    return s.substring(1, s.length() - 1);
//                }
//            }
        return s;
    }

    public interface Importer {
        GlobType getType();

        void consume(Consumer<Glob> consumer);
    }

    public interface DataRead {
        Map<String, Integer> getHeader();

        void read(Consumer<Glob> consumer, GlobType globType);
    }

    interface FieldReader {
        void read(MutableGlob mutableGlob, CSVRecord record);
    }

    static class DefaultDataRead implements DataRead {
        private CSVParser parse;
        private boolean trim;

        public DefaultDataRead(CSVParser parse, boolean trim) {
            this.parse = parse;
            this.trim = trim;
        }

        public Map<String, Integer> getHeader() {
            return parse.getHeaderMap();
        }

        GlobType createDefault() {
            GlobTypeBuilder globTypeBuilder = new DefaultGlobTypeBuilder("DefaultCsv");
            Map<String, Integer> headerMap = parse.getHeaderMap();
            for (String s1 : headerMap.keySet()) {
                globTypeBuilder.declareStringField(s1);
            }
            return globTypeBuilder.get();
        }

        public void read(Consumer<Glob> consumer, GlobType globType) {
            ImportReaderBuilder readerBuilder = new ImportReaderBuilder(globType, trim);
            Map<String, Integer> headerMap = parse.getHeaderMap();
            for (Map.Entry<String, Integer> stringIntegerEntry : headerMap.entrySet()) {
                Field field = findField(globType, stringIntegerEntry);
                if (field != null) {
                    readerBuilder.declare(field, stringIntegerEntry.getValue());
                } else {
                    LOGGER.warn(stringIntegerEntry.getKey() + " not used got : " + Arrays.toString(globType.getFields()));
                }
            }

            ImportReader build = readerBuilder.build();
            for (CSVRecord record : parse) {
                consumer.accept(build.read(record));
            }
        }

        private Field findField(GlobType globType, Map.Entry<String, Integer> stringIntegerEntry) {
            Field field = GlobTypeUtils.findNamedField(globType, stringIntegerEntry.getKey());
            if (field == null) {
                LOGGER.warn("Field " + stringIntegerEntry.getKey() + " ignored.");
            }
            return field;
        }
    }

    static class ImportReaderBuilder {
        private final GlobType type;
        private List<FieldReader> fieldReaders = new ArrayList<>();
        private boolean trim;

        ImportReaderBuilder(GlobType type, boolean trim) {
            this.type = type;
            this.trim = trim;
        }

        public void declare(Field field, Integer index) {
            field.safeVisit(new FieldVisitor.AbstractWithErrorVisitor() {
                public void visitInteger(IntegerField field) throws Exception {
                    fieldReaders.add(new IntegerFieldReader(field, index, trim));
                }

                public void visitDouble(DoubleField field) throws Exception {
                    fieldReaders.add(new DoubleFieldReader(field, index, trim));
                }

                public void visitString(StringField field) throws Exception {
                    fieldReaders.add(new StringFieldReader(field.hasAnnotation(ImportEmptyStringHasEmptyStringFormat.KEY), field, index, trim));
                }

                public void visitLong(LongField field) throws Exception {
                    fieldReaders.add(new LongFieldReader(field, index, trim));
                }

                public void visitDate(DateField field) throws Exception {
                    fieldReaders.add(new DateFieldReader(field, index, trim));
                }
            });
        }

        ImportReader build() {
            return new ImportReader(fieldReaders.toArray(new FieldReader[0]), type);
        }
    }

    static class ImportReader {
        private final FieldReader[] fieldReaders;
        private final GlobType type;

        ImportReader(FieldReader[] fieldReaders, GlobType type) {
            this.fieldReaders = fieldReaders;
            this.type = type;
        }

        Glob read(CSVRecord record) {
            MutableGlob instantiate = type.instantiate();
            for (FieldReader fieldReader : fieldReaders) {
                fieldReader.read(instantiate, record);
            }
            return instantiate;
        }
    }

    static class IntegerFieldReader implements FieldReader {
        final IntegerField field;
        final int index;
        private boolean trim;

        IntegerFieldReader(IntegerField field, int index, boolean trim) {
            this.field = field;
            this.index = index;
            this.trim = trim;
        }

        @Override
        public void read(MutableGlob mutableGlob, CSVRecord record) {
            String s = getValue(record, index, trim);
            if (Strings.isNotEmpty(s)) {
                mutableGlob.set(field, Integer.parseInt(s.trim()));
            }
        }
    }

    static class LongFieldReader implements FieldReader {
        final LongField field;
        final int index;
        private boolean trim;

        LongFieldReader(LongField field, int index, boolean trim) {
            this.field = field;
            this.index = index;
            this.trim = trim;
        }

        @Override
        public void read(MutableGlob mutableGlob, CSVRecord record) {
            String s = getValue(record, index, trim);
            if (Strings.isNotEmpty(s)) {
                mutableGlob.set(field, Long.parseLong(s.trim()));
            }
        }
    }

    static class DateFieldReader implements FieldReader {
        final DateField field;
        final int index;
        private boolean trim;
        private DateTimeFormatter dateTimeFormatter;

        DateFieldReader(DateField field, int index, boolean trim) {
            this.field = field;
            this.index = index;
            this.trim = trim;
            Glob dataFormat = field.findAnnotation(ExportDateFormat.KEY);
            if (dataFormat != null) {
                String s = dataFormat.get(ExportDateFormat.FORMAT);
                dateTimeFormatter = DateTimeFormatter.ofPattern(s);
            } else {
                dateTimeFormatter = DateTimeFormatter.ISO_DATE;
            }
        }

        public void read(MutableGlob mutableGlob, CSVRecord record) {
            String s = getValue(record, index, trim);
            if (Strings.isNotEmpty(s)) {
                mutableGlob.set(field, (LocalDate) dateTimeFormatter.parse(s.trim()));
            }
        }
    }

    static class DoubleFieldReader implements FieldReader {
        final DoubleField field;
        final int index;
        private boolean trim;

        DoubleFieldReader(DoubleField field, int index, boolean trim) {
            this.field = field;
            this.index = index;
            this.trim = trim;
        }

        @Override
        public void read(MutableGlob mutableGlob, CSVRecord record) {
            String s = getValue(record, index, trim);
            if (Strings.isNotEmpty(s)) {
                mutableGlob.set(field, Double.parseDouble(s.trim()));
            }
        }
    }

    static class StringFieldReader implements FieldReader {
        final boolean emptyIsNotNull;
        final StringField field;
        final int index;
        private boolean trim;

        StringFieldReader(boolean emptyIsNotNull, StringField field, int index, boolean trim) {
            this.emptyIsNotNull = emptyIsNotNull;
            this.field = field;
            this.index = index;
            this.trim = trim;
        }

        @Override
        public void read(MutableGlob mutableGlob, CSVRecord record) {
            String s = getValue(record, index, trim);
            if (emptyIsNotNull || Strings.isNotEmpty(s)) {
                mutableGlob.set(field, s);
            }
        }
    }

    private static class DefaultImporter implements Importer {
        private final GlobType globType;
        private final DefaultDataRead dataRead;

        public DefaultImporter(GlobType globType, DefaultDataRead dataRead) {
            this.globType = globType;
            this.dataRead = dataRead;
        }

        public GlobType getType() {
            return globType;
        }

        public void consume(Consumer<Glob> consumer) {
            dataRead.read(consumer, globType);
        }
    }
}
