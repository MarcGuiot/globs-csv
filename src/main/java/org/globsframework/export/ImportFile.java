package org.globsframework.export;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.SheetUtil;
import org.globsframework.export.annotation.CsvHeader;
import org.globsframework.export.annotation.ExportDateFormat;
import org.globsframework.export.annotation.ImportEmptyStringHasEmptyStringFormat;
import org.globsframework.export.annotation.ReNamedExport;
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
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
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
    private boolean propagateInFields;
    private Map<String, RealReformater.DataAccess> externalVariables;
    private Reformater.CustomDataAccessFactory dataAccessFactory;
    private String reNameFrom;
    private Reformater reformater;
    private boolean isExcel;

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

    private static String getValue(CsvLine record, int index, boolean trim) {
        if (index >= record.size()) {
            return null;
        }
        String s = record.getAt(index);
        if (s != null) {
            return s.trim();
        }
        return s;
    }

    private static Field findField(GlobType globType, String key) {
        Field field = GlobTypeUtils.findNamedField(globType, key);
        if (field == null) {
            LOGGER.warn("Field " + key + " ignored.");
        }
        return field;
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

    public ImportFile asExcel() {
        isExcel = true;
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


    interface CsvLine {
        Date getAsDate(int index);

        String getAt(int index);

        int size();
    }

    interface CsvDocument {
        Map<String, Integer> getHeader();

        void read(Consumer<CsvLine> line, int maxFieldCount);
    }


    public Importer createExcel(InputStream inputStream, GlobType globType) throws IOException {
        final DefaultDataRead dataRead = new DefaultDataRead(loadExcel(inputStream), trim, reNameFrom);
        if (globType == null) {
            globType = dataRead.createDefault();
        }
        if (transformer != null && !transformer.isEmpty()) {
            reformater = new RealReformater(globType, transformer, propagateInFields, externalVariables, dataAccessFactory);
        } else {
            reformater = new NullReformater(globType);
        }
        return new DefaultImporter(globType, dataRead, reformater);
    }

    public Importer create(Reader reader, GlobType globType) throws IOException {
        if (withSeparator) {
            CsvDocument parse = load(reader);
            DefaultDataRead dataRead = new DefaultDataRead(parse, trim, reNameFrom);
            if (globType == null) {
                globType = dataRead.createDefault();
            }
            if (transformer != null && !transformer.isEmpty()) {
                reformater = new RealReformater(globType, transformer, propagateInFields, externalVariables, dataAccessFactory);
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

    public Importer createComplexExcel(InputStream reader, GlobType type) throws IOException {
        Importer importer = createExcel(reader, null);

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

    public ImportFile withTransformer(List<Glob> transformer, boolean propagateInFields) {
        this.transformer = transformer;
        this.propagateInFields = propagateInFields;
        return this;
    }

    public ImportFile withTransformer(List<Glob> transformer, boolean propagateInFields,
                                      Map<String, RealReformater.DataAccess> externalVariables,
                                      Reformater.CustomDataAccessFactory dataAccessFactory) {
        this.transformer = transformer;
        this.propagateInFields = propagateInFields;
        this.externalVariables = externalVariables;
        this.dataAccessFactory = dataAccessFactory;
        return this;
    }

    public Importer createMultiExcel(InputStream inputStream, GlobType globType, List<Glob> transformer) {
        if (globType == null) {
            throw new RuntimeException("Missing type");
        }
        final Workbook sheets;
        try {
            sheets = WorkbookFactory.create(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final ExcelDocument excelDocument = new ExcelDocument(sheets, sheets.getSheetAt(0), Map.of());
        excelDocument.skipFirstLine(false);
        DataRead dataRead = new MultiTypeDataRead(excelDocument);

        Reformater reformater = transformer == null || transformer.isEmpty() ? new NullReformater(globType) : new RealReformater(globType, transformer);
        return new DefaultImporter(globType, dataRead, reformater);
    }


    public Importer createMulti(InputStream inputStream, GlobType globType) throws IOException {
        return createMulti(createReaderFromStream(inputStream), globType, List.of());
    }

    public Importer createMulti(Reader reader, GlobType globType) {
        return createMulti(reader, globType, List.of());
    }

    public Importer createMulti(Reader reader, GlobType globType, List<Glob> transformer) {
        if (withSeparator) {
            if (globType == null) {
                throw new RuntimeException("Missing type");
            }
            CSVFormat.Builder csvFormatBuilder =
                    CSVFormat.Builder.create(CSVFormat.DEFAULT)
                            .setDelimiter(separator)
                            .setEscape('\\')
                            .setQuote(quoteChar);
            try {
                CsvDocument csvDocument = new CsvDocumentFromCSVParse(csvFormatBuilder.build().parse(reader));
                DataRead dataRead = new MultiTypeDataRead(csvDocument);

                Reformater reformater = transformer == null || transformer.isEmpty() ? new NullReformater(globType) : new RealReformater(globType, transformer);
                return new DefaultImporter(globType, dataRead, reformater);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

    public void importContentExcel(InputStream inputStream, Consumer<Glob> consumer, GlobType globType) throws IOException {
        createExcel(inputStream, globType).consume(consumer);
    }

    public DataRead getDataReader(InputStream inputStream) throws IOException {
        return new DefaultDataRead(load(createReaderFromStream(inputStream)), trim, reNameFrom);
    }

    private CsvDocument loadExcel(InputStream inputStream) {
        final Workbook sheets;
        try {
            sheets = WorkbookFactory.create(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        boolean skipFirstLine;
        Sheet sheet = sheets.getSheetAt(0);
        Map<String, Integer> headers = new LinkedHashMap<>();
        if (header == null) {
            final Row row = sheet.getRow(0);
            if (row == null) {
                throw new RuntimeException("Fail to extract header");
            }
            int i = 0;
            while (true) {
                final Cell cell = row.getCell(i);
                if (cell != null) {
                    headers.put(cell.getStringCellValue(), i);
                } else {
                    break;
                }
                i++;
            }
            skipFirstLine = true;
        }
        else {
            skipFirstLine = false;
            StringBuilder current = new StringBuilder();
            for (char c : header.toCharArray()) {
                if (c == separator) {
                    headers.put(current.toString(), headers.size());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        final ExcelDocument excelDocument = new ExcelDocument(sheets, sheet, headers);
        excelDocument.skipFirstLine(skipFirstLine);
        return excelDocument;
    }

    private CsvDocument load(Reader reader) throws IOException {
        CSVFormat.Builder csvFormatBuilder =
                CSVFormat.Builder.create(CSVFormat.DEFAULT)
                        .setDelimiter(separator)
                        .setEscape('\\')
                        .setQuote(quoteChar);
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
            csvFormatBuilder.setSkipHeaderRecord(false);
            csvFormatBuilder.setAllowMissingColumnNames(true);
            csvFormatBuilder.setHeader(elements.toArray(new String[0]));
        } else {
            csvFormatBuilder.setHeader(); //to force read header from file
        }
        final CSVParser parse = csvFormatBuilder.build().parse(reader);
        return new CsvDocumentFromCSVParse(parse);

    }

    interface UpdateLine {
//        ImportReader getImporter();

        Optional<Glob> read(CsvLine record);

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
        void read(MutableGlob mutableGlob, CsvLine record);
    }

    static class DefaultDataRead implements DataRead {
        private final String reNameFrom;
        private CsvDocument parse;
        private boolean trim;
        private int countLine = 0;

        public DefaultDataRead(CsvDocument parse, boolean trim, String reNameFrom) {
            this.parse = parse;
            this.trim = trim;
            this.reNameFrom = reNameFrom;
        }

        GlobType createDefault() {
            GlobTypeBuilder globTypeBuilder = new DefaultGlobTypeBuilder("DefaultCsv");
            Map<String, Integer> headerMap = parse.getHeader();
            for (String s1 : headerMap.keySet()) {
                globTypeBuilder.declareStringField(s1);
            }
            return globTypeBuilder.get();
        }

        public void read(Consumer<Glob> consumer, GlobType globType) {
            ImportReaderBuilder readerBuilder = new ImportReaderBuilder(globType, trim, parse);
            RemapName remapName = new RemapName(globType, reNameFrom);
            Map<String, Integer> headerMap = parse.getHeader();
            for (Map.Entry<String, Integer> stringIntegerEntry : headerMap.entrySet()) {
                Field field = remapName.headNameToField.get(stringIntegerEntry.getKey());
                if (field == null) {
                    field = findField(globType, stringIntegerEntry.getKey());
                }
                if (field != null) {
                    readerBuilder.declare(field, stringIntegerEntry.getValue());
                } else {
                    LOGGER.warn(stringIntegerEntry.getKey() + " not used got : " + Arrays.toString(globType.getFields()));
                }
            }
            countLine += 2; // un pour le header et un pour la ligne a lire
            ImportReader build = readerBuilder.build();

            parse.read(record -> {
                try {
                    consumer.accept(build.read(record));
                    countLine++;
                } catch (Exception exception) {
                    String message = "Fail to read line : " + countLine + " : " + (record != null ? record.toString() : "");
                    LOGGER.error(message, exception);
                    throw new RuntimeException(message, exception);
                }
            }, 0);

        }

        static class RemapName {
            public Map<String, Field> headNameToField = new HashMap<>();

            RemapName(GlobType type, String name) {
                Field[] fields = type.getFields();
                for (Field field : fields) {
                    String headerName = ReNamedExport.getHeaderName(name, field);
                    headNameToField.put(headerName, field);
                }
            }
        }

    }

    static class ImportReaderBuilder {
        private final GlobType type;
        private List<FieldReader> fieldReaders = new ArrayList<>();
        private boolean trim;
        private CsvDocument csvDocument;

        ImportReaderBuilder(GlobType type, boolean trim, CsvDocument csvDocument) {
            this.type = type;
            this.trim = trim;
            this.csvDocument = csvDocument;
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

                public void visitStringArray(StringArrayField field) throws Exception {
                    fieldReaders.add(new StringArrayFieldReader(field, index, trim));
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

        Glob read(CsvLine record) {
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

        public void read(MutableGlob mutableGlob, CsvLine record) {
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

        public void read(MutableGlob mutableGlob, CsvLine record) {
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

        public void read(MutableGlob mutableGlob, CsvLine record) {
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

        public void read(MutableGlob mutableGlob, CsvLine record) {
            final Date date = record.getAsDate(index);
            if (date != null) {
                mutableGlob.set(field, LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()));
            }
            else {
                String s = getValue(record, index, trim);
                if (Strings.isNotEmpty(s)) {
                    mutableGlob.set(field, LocalDate.from(dateTimeFormatter.parse(s.trim())));
                }
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

        public void read(MutableGlob mutableGlob, CsvLine record) {
            final Date date = record.getAsDate(index);
            if (date != null) {
                mutableGlob.set(field, ZonedDateTime.ofInstant(date.toInstant(), zoneId));
            }
            else {
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
        public void read(MutableGlob mutableGlob, CsvLine record) {
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

        public void read(MutableGlob mutableGlob, CsvLine record) {
            String s = getValue(record, index, trim);
            if (emptyIsNotNull || Strings.isNotEmpty(s)) {
                mutableGlob.set(field, s == null ? "" : s);
            }
        }
    }

    static class StringArrayFieldReader implements FieldReader {
        final StringArrayField field;
        final int index;
        private boolean trim;

        StringArrayFieldReader(StringArrayField field, int index, boolean trim) {
            this.field = field;
            this.index = index;
            this.trim = trim;
        }

        public void read(MutableGlob mutableGlob, CsvLine record) {
            String s = getValue(record, index, trim);
            if (Strings.isNotEmpty(s)) {
                String[] split = s.split(",");
                mutableGlob.set(field, split);
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

    private static class CsvDocumentFromCSVParse implements CsvDocument {
        private final CSVParser parse;

        public CsvDocumentFromCSVParse(CSVParser parse) {
            this.parse = parse;
        }

        public Map<String, Integer> getHeader() {
            return parse.getHeaderMap();
        }

        public void read(Consumer<CsvLine> line, int maxFieldCount) {
            for (CSVRecord strings : parse) {
                boolean isValide = false;
                for (String string : strings) {
                    isValide |= Strings.isNotEmpty(string);
                }
                if (isValide) {
                    line.accept(new CsvLine() {
                        public Date getAsDate(int index) {
                            return null;
                        }

                        public String getAt(int index) {
                            return strings.get(index);
                        }

                        public int size() {
                            return strings.size();
                        }

                    });
                }
                else {
                    LOGGER.info("Ignore empty line");
                }
            }
        }
    }

    private static class ExcelDocument implements CsvDocument {
        private Workbook workbook;
        private Sheet sheet;
        private final Map<String, Integer> headers;
        private boolean skipFirstLine;
        private FormulaEvaluator formulaEvaluator;

        public ExcelDocument(Workbook workbook, Sheet sheet, Map<String, Integer> headers) {
            this.workbook = workbook;
            this.sheet = sheet;
            this.headers = headers;

        }

        public Map<String, Integer> getHeader() {
            return headers;
        }

        void skipFirstLine(boolean skipFirstLine){
            this.skipFirstLine = skipFirstLine;
        }

        public void read(Consumer<CsvLine> line, int maxFieldCount) {
            int currentPos = skipFirstLine ? 0 : -1;
            final int maxSize = Math.max(maxFieldCount, headers.size());
            Map<Integer, Cell> realLine = new LinkedHashMap<>();
            while (true) {
                currentPos++;
                realLine.clear();
                boolean hasAValue = false;
                final Row row = sheet.getRow(currentPos);
                if (row == null) {
                    return;
                }
                for (int i = 0; i < maxSize; i++) {
                    final Cell cell = row.getCell(i);
                    realLine.put(i, cell);
                    hasAValue |= cell != null;
                }
                if (!hasAValue) {
                    return;
                }
                line.accept(new CsvLine() {
                    public Date getAsDate(int index) {
                        final Cell cell = realLine.get(index);
                        if (cell != null) {
                            return switch (cell.getCellType()) {
                                case _NONE, BLANK, STRING, FORMULA, BOOLEAN, ERROR -> null;
                                case NUMERIC -> cell.getDateCellValue();
                            };
                        }
                        return null;
                    }

                    public String getAt(int index) {
                        final Cell cell = realLine.get(index);
                        if (cell != null) {
                            return getCellValueAsString(cell);
                        }
                        return null;
                    }

                    public int size() {
                        return maxSize;
                    }
                });
            }
        }

        private String getCellValueAsString(Cell cell) {
            final String str = switch (cell.getCellType()) {
                case _NONE, BLANK -> null;
                case NUMERIC -> {
                    final double numericCellValue = cell.getNumericCellValue();
                    if (numericCellValue == Math.rint(numericCellValue)) {
                        yield Long.toString(Double.valueOf(numericCellValue).longValue());
                    }
                    yield Double.toString(numericCellValue);
                }
                case STRING -> cell.getStringCellValue();
                case FORMULA -> getValueFromFormula(cell);
                case BOOLEAN -> cell.getBooleanCellValue() ? "true" : "false";
                case ERROR -> throw new RuntimeException("Formula not allowed");
            };
            return str;
        }

        private String getValueFromFormula(Cell cell) {
            if (formulaEvaluator == null) {
                formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            }
            final CellValue evaluate = formulaEvaluator.evaluate(cell);
            return switch (evaluate.getCellType()) {
                case _NONE, BLANK -> null;
                case NUMERIC -> {
                    final String s = BigDecimal.valueOf(evaluate.getNumberValue()).toPlainString();
                    if (s.endsWith(".0")) {
                        yield s.substring(0, s.length() - 2);
                    }
                    yield s;
                }
                case STRING -> evaluate.getStringValue();
                case BOOLEAN -> evaluate.getBooleanValue() ? "true" : "false";
                case FORMULA, ERROR -> throw new RuntimeException("Error " + evaluate.getErrorValue());
            };
        }
    }

    private class MultiTypeDataRead implements DataRead {

        private CsvDocument csvDocument;

        public MultiTypeDataRead(CsvDocument csvDocument) {
            this.csvDocument = csvDocument;
        }

        public void read(Consumer<Glob> consumer, GlobType globType) {
            int maxFieldCount = 0;
            try {
                Field[] fields = globType.getFields();
                List<UpdateLine> lines = new ArrayList<>();
                for (Field field : fields) {
                    Glob annotation = field.findAnnotation(CsvHeader.KEY);
                    if (annotation != null) {
                        if (field instanceof GlobField) {
                            lines.add(new SingleUpdateLine(field, annotation));
                            maxFieldCount = Math.max(maxFieldCount, ((GlobField) field).getTargetType().getFieldCount() + 1);
                        } else if (field instanceof GlobArrayField) {
                            lines.add(new MultiLineUpdateLine(field, annotation));
                            maxFieldCount = Math.max(maxFieldCount, ((GlobArrayField) field).getTargetType().getFieldCount() + 1);
                        }
                    }
                }

                Iterator<UpdateLine> first = lines.iterator();
                final CsvLineConsumer line = new CsvLineConsumer(first, consumer, globType, lines);
                csvDocument.read(line, maxFieldCount);
                line.complete();

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

        private ImportReader initImportReader(GlobType targetType, Glob csvHeader, CsvLine record) {
            if (csvHeader.isTrue(CsvHeader.firstLineIsHeader)) {
                ImportReaderBuilder readerBuilder = new ImportReaderBuilder(targetType, trim, csvDocument);
                DefaultDataRead.RemapName remapName = new DefaultDataRead.RemapName(targetType, reNameFrom);
                for (int i = 1; i < record.size(); i++) {
                    String key = record.getAt(i);
                    Field field = remapName.headNameToField.get(key);
                    if (field == null) {
                        field = findField(targetType, key);
                    }
                    if (field != null) {
                        readerBuilder.declare(field, i);
                    } else {
                        LOGGER.warn(key + " not used got : " + Arrays.toString(targetType.getFields()));
                    }
                }
                return readerBuilder.build();
            } else {
                ImportReaderBuilder importReaderBuilder = new ImportReaderBuilder(targetType, trim, csvDocument);
                targetType.streamFields().forEach(new Consumer<>() {
                    int i = 0;

                    public void accept(Field f) {
                        importReaderBuilder.declare(f, ++i);
                    }
                });
                ImportReader build = importReaderBuilder.build();
                return build;
            }
        }

        private class SingleUpdateLine implements UpdateLine {
            private final Field field;
            private final Glob csvHeader;
            private final GlobType targetType;
            private final boolean csvHeaderTrue;
            private boolean isFirst = true;
            ImportReader importReaderBuilder;
            Glob got;

            public SingleUpdateLine(Field field, Glob csvHeader) {
                this.field = field;
                this.csvHeader = csvHeader;
                targetType = ((GlobField) field).getTargetType();
                csvHeaderTrue = csvHeader.isTrue(CsvHeader.firstLineIsHeader);
            }

            public Optional<Glob> read(CsvLine record) {
                if (importReaderBuilder == null) {
                    this.importReaderBuilder = initImportReader(targetType, csvHeader, record);
                }
                if (csvHeaderTrue && isFirst) {
                    isFirst = false;
                    return Optional.empty();
                }
                return Optional.of(importReaderBuilder.read(record));
            }

            public String getMarkerName() {
                return csvHeader.get(CsvHeader.name);
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
                isFirst = true;
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
            private final Field field;
            private final Glob csvHeader;
            private final GlobType targetType;
            private final boolean csvHeaderTrue;
            private boolean isFirst = true;
            ImportReader importReaderBuilder;

            public MultiLineUpdateLine(Field field, Glob csvHeader) {
                this.field = field;
                this.csvHeader = csvHeader;
                gots = new ArrayList<>();
                targetType = ((GlobArrayField) field).getTargetType();
                csvHeaderTrue = csvHeader.isTrue(CsvHeader.firstLineIsHeader);
            }

            public Optional<Glob> read(CsvLine record) {
                if (this.importReaderBuilder == null) {
                    this.importReaderBuilder = initImportReader(targetType, csvHeader, record);
                }
                if (csvHeaderTrue && isFirst) {
                    isFirst = false;
                    return Optional.empty();
                }
                return Optional.of(importReaderBuilder.read(record));
            }

            public String getMarkerName() {
                return csvHeader.get(CsvHeader.name);
            }

            public boolean add(Glob glob) {
                gots.add(glob);
                return true;
            }

            public boolean updateAndReset(MutableGlob to) {
                isFirst = true;
                if (!gots.isEmpty()) {
                    to.set(((GlobArrayField) field), gots.toArray(Glob[]::new));
                    gots.clear();
                    return true;
                } else {
                    return false;
                }
            }
        }

        private class CsvLineConsumer implements Consumer<CsvLine> {
            private Iterator<UpdateLine> first;
            private final Consumer<Glob> consumer;
            private final GlobType globType;
            private final List<UpdateLine> lines;
            UpdateLine current;
            boolean push;

            public CsvLineConsumer(Iterator<UpdateLine> first, Consumer<Glob> consumer, GlobType globType, List<UpdateLine> lines) {
                this.first = first;
                this.consumer = consumer;
                this.globType = globType;
                this.lines = lines;
                current = first.next();
                push = false;
            }

            public void accept(CsvLine csvLine) {
                String h = csvLine.getAt(0);
                while (!current.getMarkerName().equals(h)) {
                    if (!first.hasNext()) {
                        push = true;
                        pushGlob(consumer, globType, lines);
                        first = lines.iterator();
                        current = first.next();
                    } else {
                        current = first.next();
                    }
                }
                Optional<Glob> readed = current.read(csvLine);
                push &= readed.isEmpty();
                if (readed.isPresent() && !current.add(readed.get())) {
                    if (!first.hasNext()) {
                        push = true;
                        pushGlob(consumer, globType, lines);
                        first = lines.iterator();
                        current = first.next();
                    } else {
                        current = first.next();
                    }
                }
            }
            public void complete(){
                if (!push) {
                    pushGlob(consumer, globType, lines);
                }
            }
        }

    }

    record Column(Row row) {
    }
}
