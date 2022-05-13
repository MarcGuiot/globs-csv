package org.globsframework.export;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.globsframework.export.annotation.*;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ImportFile {
    private static Logger LOGGER = LoggerFactory.getLogger(ImportFile.class);
    private Character quoteChar = '"';
    private boolean withSeparator;
    private char separator;
    private Charset charSet = StandardCharsets.UTF_8;
    private Map<GlobType, ImportReader> importers = new HashMap<>();
    private ExportBySize.PaddingType paddingType;
    private boolean trim;
    private String header;
    private List<Glob> transformer;
    private String reNameFrom;
    private Reformater reformater;

    public static InputStreamReader createReaderWithBomCheck(InputStream inputStream, Charset defaultCharset) throws IOException {
        BOMInputStream in = new BOMInputStream(inputStream, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE,
                ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
        String bomCharsetName = in.getBOMCharsetName();
        InputStreamReader reader = new InputStreamReader(in, bomCharsetName != null ? Charset.forName(bomCharsetName) : defaultCharset);
        return reader;
    }

    static public GlobType extractHeader(InputStream inputStream, Character separator) throws IOException {
        return extractHeader(inputStream, separator, StandardCharsets.UTF_8);
    }

    static public GlobType extractHeader(InputStream inputStream, Character separator, Charset charset) throws IOException {
        GlobTypeBuilder globTypeBuilder = new DefaultGlobTypeBuilder("DEFAULT");
        BufferedReader reader = new BufferedReader(ImportFile.createReaderWithBomCheck(inputStream, charset)); //new BufferedReader(new InputStreamReader(new BOMInputStream(inputStream), charset));
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
            int countPipe = 0;
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
                    case '|':
                        countPipe++;
                }
            }
            if (countComma > countDotComma && countComma > countTab && countComma > countPipe) {
                separator = ',';
                expectedField = countComma;
            }
            if (countDotComma > countComma && countDotComma > countTab && countDotComma > countPipe) {
                separator = ';';
                expectedField = countDotComma;
            }
            if (countTab > countComma && countTab > countDotComma && countTab > countPipe) {
                separator = '\t';
                expectedField = countTab;
            }
            if (countPipe > countComma && countPipe > countDotComma && countPipe > countTab) {
                separator = '|';
                expectedField = countComma;
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
        if (index >= record.size()) {
            return null;
        }
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

    public ImportFile withSeparator(char separator) {
        withSeparator = true;
        this.separator = separator;
        importers = null;
        return this;
    }

    public ImportFile withQuoteChar(Character quoteChar) {
        this.quoteChar = quoteChar;
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

    public ImportFile withNameFrom(String name) {
        this.reNameFrom = name;
        return this;
    }

    public ImportFile withCharSet(Charset charSet) {
        this.charSet = charSet;
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

    public ImportFile withHeader(String headers) {
        this.header = headers;
        return this;
    }

    public Importer create(InputStream inputStream) throws IOException {
        return create(createReaderFromStream(inputStream), null);
    }

    public Importer create(Reader reader) throws IOException {
        return create(reader, null);
    }

    public Importer create(InputStream inputStream, GlobType globType) throws IOException {
        return create(createReaderFromStream(inputStream), globType);
    }

    public Importer create(Reader reader, GlobType globType) throws IOException {
        if (withSeparator) {
            CSVParser parse = load(reader);
            DefaultDataRead dataRead = new DefaultDataRead(parse, trim, reNameFrom);
            if (globType == null) {
                globType = dataRead.createDefault();
            }
            if (transformer != null && !transformer.isEmpty()) {
                reformater = new RealReformater(globType, transformer);
            } else {
                reformater = new NullReformater(globType);
            }
            return new DefaultImporter(globType, dataRead, reformater);
        } else {
            throw new RuntimeException("Not implemented");
        }
    }

    public Importer createComplex(Reader reader, GlobType type) throws IOException {
        Importer importer = create(reader);

        ComplexImporter complexImporter = new ComplexImporter(reformater.getResultType(), type);

        return new Importer() {
            public GlobType getType() {
                return type;
            }

            public <T extends Consumer<Glob>> T consume(T consumer) {
                ComplexImporter.ConsumerWithCurrent globConsumer = complexImporter.create(consumer);
                importer.consume(globConsumer);
                globConsumer.end();
                return consumer;
            }
        };
    }

    public ImportFile withTransformer(List<Glob> transformer) {
        this.transformer = transformer;
        return this;
    }

    public Importer createMulti(Reader reader, GlobType globType) {
        return createMulti(reader, globType, List.of());
    }

    public Importer createMulti(Reader reader, GlobType globType, List<Glob> transformer) {
        if (withSeparator) {
            if (globType == null) {
                throw new RuntimeException("Missing type");
            }
            CSVFormat csvFormat =
                    CSVFormat.DEFAULT
                            .withDelimiter(separator)
                            .withEscape('\\')
                            .withQuote(quoteChar);
            DataRead dataRead = new MultiTypeDataRead(csvFormat, reader);

            Reformater reformater = transformer == null || transformer.isEmpty() ? new NullReformater(globType) : new RealReformater(globType, transformer);
            return new DefaultImporter(globType, dataRead, reformater);
        } else {
            throw new RuntimeException("Not implemented");
        }
    }

    public void importContent(InputStream inputStream, Consumer<Glob> consumer, GlobType globType) throws IOException {
        InputStreamReader reader = createReaderFromStream(inputStream);
        importContent(reader, consumer, globType);
    }

    private InputStreamReader createReaderFromStream(InputStream inputStream) throws IOException {
        return createReaderWithBomCheck(inputStream, charSet);
    }

    public void importContent(Reader reader, Consumer<Glob> consumer, GlobType globType) throws IOException {
        create(reader, globType).consume(consumer);
    }

    public DataRead getDataReader(InputStream inputStream) throws IOException {
        return new DefaultDataRead(load(createReaderFromStream(inputStream)), trim, reNameFrom);
    }

    private CSVParser load(Reader reader) throws IOException {
        CSVFormat csvFormat =
                CSVFormat.DEFAULT
                        .withDelimiter(separator)
                        .withEscape('\\')
                        .withQuote(quoteChar);
        if (header != null) {
            List<String> elements = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (char c : header.toCharArray()) {
                if (c == separator) {
                    elements.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            elements.add(current.toString());
            csvFormat = csvFormat.withHeader(elements.toArray(new String[0]));
        } else {
            csvFormat = csvFormat.withFirstRecordAsHeader();
        }
        return csvFormat.parse(reader);
    }

    interface UpdateLine {
        ImportReader getImporter();

        String getMarkerName();

        boolean add(Glob glob);

        boolean updateAndReset(MutableGlob to);
    }

    public interface Importer {
        GlobType getType();

        <T extends Consumer<Glob>>
        T consume(T consumer);
    }

    public interface DataRead {

        void read(Consumer<Glob> consumer, GlobType globType);
    }

    interface FieldReader {
        void read(MutableGlob mutableGlob, CSVRecord record);
    }

    static class DefaultDataRead implements DataRead {
        private CSVParser parse;
        private boolean trim;
        private final String reNameFrom;
        private int countLine = 0;

        public DefaultDataRead(CSVParser parse, boolean trim, String reNameFrom) {
            this.parse = parse;
            this.trim = trim;
            this.reNameFrom = reNameFrom;
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

        static class RemapName {
            public Map<String, Field> headNameToField = new HashMap<>();

            RemapName(String name, GlobType type) {
                Field[] fields = type.getFields();
                for (Field field : fields) {
                    String headerName = ReNamedExport.getHeaderName(name, field);
                    headNameToField.put(headerName, field);
                }
            }
        }

        public void read(Consumer<Glob> consumer, GlobType globType) {
            ImportReaderBuilder readerBuilder = new ImportReaderBuilder(globType, trim);
            RemapName remapName = new RemapName(reNameFrom, globType);
            Map<String, Integer> headerMap = parse.getHeaderMap();
            for (Map.Entry<String, Integer> stringIntegerEntry : headerMap.entrySet()) {
                Field field = remapName.headNameToField.get(stringIntegerEntry.getKey());
                if (field == null) {
                    field = findField(globType, stringIntegerEntry);
                }
                if (field != null) {
                    readerBuilder.declare(field, stringIntegerEntry.getValue());
                } else {
                    LOGGER.warn(stringIntegerEntry.getKey() + " not used got : " + Arrays.toString(globType.getFields()));
                }
            }
            countLine += 2; // un pour le header et un pour la ligne a lire
            ImportReader build = readerBuilder.build();
            CSVRecord record = null;
            try {
                for (Iterator<CSVRecord> iterator = parse.iterator(); iterator.hasNext(); ) {
                    record = iterator.next();
                    consumer.accept(build.read(record));
                    countLine++;
                }
            } catch (Exception exception) {
                String message = "Fail to read line : " + countLine + " : " + (record != null ? record.toString() : "");
                LOGGER.error(message, exception);
                throw new RuntimeException(message, exception);
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

                public void visitBoolean(BooleanField field) throws Exception {
                    fieldReaders.add(new BooleanFieldReader(field, index));
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

                public void visitDateTime(DateTimeField field) throws Exception {
                    fieldReaders.add(new DateTimeFieldReader(field, index, trim));
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
        private final Pattern removeZero;
        private boolean trim;

        IntegerFieldReader(IntegerField field, int index, boolean trim) {
            this.field = field;
            this.index = index;
            this.trim = trim;
            removeZero = Pattern.compile("\\.0*$");
        }

        @Override
        public void read(MutableGlob mutableGlob, CSVRecord record) {
            String s = getValue(record, index, trim);
            if (Strings.isNotEmpty(s)) {
                s = removeZero.matcher(s.trim()).replaceAll("");
                mutableGlob.set(field, Integer.parseInt(s));
            }
        }
    }

    static class BooleanFieldReader implements FieldReader {
        final BooleanField field;
        final int index;

        public BooleanFieldReader(BooleanField field, int index) {
            this.field = field;
            this.index = index;
        }

        @Override
        public void read(MutableGlob mutableGlob, CSVRecord record) {
            String s = getValue(record, index, true);
            if (Strings.isNotEmpty(s)) {
                mutableGlob.set(field, s.equalsIgnoreCase("true") || s.equalsIgnoreCase("1"));
            }
        }
    }

    static class LongFieldReader implements FieldReader {
        final LongField field;
        final int index;
        private final Pattern removeZero;
        private boolean trim;

        LongFieldReader(LongField field, int index, boolean trim) {
            this.field = field;
            this.index = index;
            this.trim = trim;
            removeZero = Pattern.compile("\\.0*$");
        }

        @Override
        public void read(MutableGlob mutableGlob, CSVRecord record) {
            String s = getValue(record, index, trim);
            if (Strings.isNotEmpty(s)) {
                s = removeZero.matcher(s.trim()).replaceAll("");
                mutableGlob.set(field, Long.parseLong(s));
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
                mutableGlob.set(field, LocalDate.from(dateTimeFormatter.parse(s.trim())));
            }
        }
    }

    static class DateTimeFieldReader implements FieldReader {
        final DateTimeField field;
        final int index;
        private final ZoneId zoneId;
        private boolean trim;
        private DateTimeFormatter dateTimeFormatter;

        DateTimeFieldReader(DateTimeField field, int index, boolean trim) {
            this.field = field;
            this.index = index;
            this.trim = trim;
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

        public void read(MutableGlob mutableGlob, CSVRecord record) {
            String s = getValue(record, index, trim);
            if (Strings.isNotEmpty(s)) {
                TemporalAccessor temporalAccessor = dateTimeFormatter.parseBest(s.trim(), ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
                if (temporalAccessor instanceof ZonedDateTime) {
                    mutableGlob.set(field, (ZonedDateTime) temporalAccessor);
                } else if (temporalAccessor instanceof LocalDateTime) {
                    mutableGlob.set(field, ((LocalDateTime) temporalAccessor).atZone(zoneId));
                } else if (temporalAccessor instanceof LocalDate) {
                    mutableGlob.set(field, ZonedDateTime.of((LocalDate) temporalAccessor, LocalTime.MIDNIGHT, zoneId));
                }
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
        private final DataRead dataRead;
        private final Reformater reformater;

        public DefaultImporter(GlobType globType, DataRead dataRead, Reformater reformater) {
            this.globType = globType;
            this.dataRead = dataRead;
            this.reformater = reformater;
        }

        public GlobType getType() {
            return reformater.getResultType();
        }

        public <T extends Consumer<Glob>> T consume(T consumer) {
            dataRead.read(glob -> {
                consumer.accept(reformater.transform(glob));
            }, globType);
            return consumer;
        }
    }

    private static class NullReformater implements Reformater {
        private final GlobType globType;

        public NullReformater(GlobType globType) {
            this.globType = globType;
        }

        @Override
        public GlobType getResultType() {
            return globType;
        }

        @Override
        public Glob transform(Glob from) {
            return from;
        }
    }

    private class MultiTypeDataRead implements DataRead {
        private final CSVFormat csvFormat;
        private final Reader reader;

        public MultiTypeDataRead(CSVFormat csvFormat, Reader reader) {
            this.csvFormat = csvFormat;
            this.reader = reader;
        }

        public void read(Consumer<Glob> consumer, GlobType globType) {
            try {
                Field[] fields = globType.getFields();
                List<UpdateLine> lines = new ArrayList<>();
                for (Field field : fields) {
                    Glob annotation = field.findAnnotation(CsvHeader.KEY);
                    if (annotation != null) {
                        if (field instanceof GlobField) {
                            lines.add(new SingleUpdateLine(field, annotation));
                        } else if (field instanceof GlobArrayField) {
                            lines.add(new MultiLineUpdateLine(field, annotation));
                        }

                    }
                }

                CSVParser parse = csvFormat.parse(reader);
                Iterator<UpdateLine> first = lines.iterator();
                UpdateLine current = first.next();
                for (CSVRecord csvRecord : parse) {
                    String h = csvRecord.get(0);
                    while (!current.getMarkerName().equals(h)) {
                        if (!first.hasNext()) {
                            pushGlob(consumer, globType, lines);
                            first = lines.iterator();
                            current = first.next();
                        } else {
                            current = first.next();
                        }
                    }
                    if (!current.add(current.getImporter().read(csvRecord))) {
                        if (!first.hasNext()) {
                            pushGlob(consumer, globType, lines);
                            first = lines.iterator();
                            current = first.next();
                        } else {
                            current = first.next();
                        }
                    }
                }
                pushGlob(consumer, globType, lines);
            } catch (Exception e) {
                String msg = "error during parsing";
                LOGGER.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        private void pushGlob(Consumer<Glob> consumer, GlobType globType, List<UpdateLine> lines) {
            MutableGlob res = globType.instantiate();
            boolean hasUpdate = false;
            for (UpdateLine line : lines) {
                hasUpdate |= line.updateAndReset(res);
            }
            if (!hasUpdate) {
                LOGGER.error("Empty loop");
                throw new RuntimeException("Empty loop");
            }
            consumer.accept(res);
        }

        private ImportReader initImportReader(GlobType targetType) {
            ImportReaderBuilder importReaderBuilder = new ImportReaderBuilder(targetType, trim);
            targetType.streamFields().forEach(new Consumer<>() {
                int i = 0;

                public void accept(Field f) {
                    importReaderBuilder.declare(f, ++i);
                }
            });
            ImportReader build = importReaderBuilder.build();
            return build;
        }

        private class SingleUpdateLine implements UpdateLine {
            final ImportReader importReaderBuilder;
            private final Field field;
            private final Glob annotation;
            Glob got;

            public SingleUpdateLine(Field field, Glob annotation) {
                this.field = field;
                this.annotation = annotation;
                GlobType targetType = ((GlobField) field).getTargetType();
                this.importReaderBuilder = initImportReader(targetType);
            }

            public ImportReader getImporter() {
                return importReaderBuilder;
            }

            public String getMarkerName() {
                return annotation.get(CsvHeader.name);
            }

            public boolean add(Glob glob) {
                if (got != null) {
                    String message = "invalide file line : " + got + " already set but receive " + glob;
                    LOGGER.error(message);
                    throw new RuntimeException(message);
                }
                got = glob;
                return false;
            }

            public boolean updateAndReset(MutableGlob to) {
                if (got != null) {
                    to.set(((GlobField) field), got);
                    got = null;
                    return true;
                } else {
                    return false;
                }
            }
        }

        private class MultiLineUpdateLine implements UpdateLine {
            final List<Glob> gots;
            final ImportReader importReaderBuilder;
            private final Field field;
            private final Glob annotation;

            public MultiLineUpdateLine(Field field, Glob annotation) {
                this.field = field;
                this.annotation = annotation;
                gots = new ArrayList<>();
                GlobType targetType = ((GlobArrayField) field).getTargetType();
                this.importReaderBuilder = initImportReader(targetType);
            }

            public ImportReader getImporter() {
                return importReaderBuilder;
            }

            public String getMarkerName() {
                return annotation.get(CsvHeader.name);
            }

            public boolean add(Glob glob) {
                gots.add(glob);
                return true;
            }

            public boolean updateAndReset(MutableGlob to) {
                if (!gots.isEmpty()) {
                    to.set(((GlobArrayField) field), gots.toArray(Glob[]::new));
                    gots.clear();
                    return true;
                } else {
                    return false;
                }
            }
        }

    }
}
