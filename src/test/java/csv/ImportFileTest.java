package org.globsframework.csv;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.FieldName_;
import org.globsframework.core.metamodel.fields.*;
import org.globsframework.core.model.Glob;
import org.globsframework.csv.annotation.ExportDateFormat_;
import org.globsframework.csv.annotation.ImportEmptyStringHasEmptyStringFormat_;
import org.globsframework.csv.annotation.ReNamedExport_;
import org.globsframework.csv.annotation.ReNamedMappingExport_;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.fail;

public class ImportFileTest {


    @Test
    public void name() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',');

        List<Glob> imports = new ArrayList<>();
        importFile.importContent(new StringReader(
                "PRODUCT_ID,sku\n" +
                        "1,\"REF_1\"\n" +
                        " 2 ,\"REF_2\"\n" +
                        "\"3\",\"REF_3\"\n" +
                        "\"4  \",\"REF_4\"\n" +
                        "\"5  \",\"\"\n" +
                        "\"6  \",\\,\n" +
                        "\"7  \",\n" +
                        "\n" +
                        ""
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);

        checkTestName(imports);
    }

    @Test
    public void nameExcel() throws IOException {
        ImportFile importFile = new ImportFile();
        List<Glob> imports = new ArrayList<>();
        importFile.importContentExcel(getClass().getResourceAsStream("/test1.xlsx"), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);

        checkTestName(imports);
    }

    private static void checkTestName(List<Glob> imports) {
        Assert.assertEquals(7, imports.size());
        Glob glob;
        {
            glob = imports.get(0);
            Assert.assertEquals(1, glob.get(Type.ID).intValue());
            Assert.assertEquals("REF_1", glob.get(Type.SKU));
        }
        {
            glob = imports.get(1);
            Assert.assertEquals(2, glob.get(Type.ID).intValue());
            Assert.assertEquals("REF_2", glob.get(Type.SKU));
        }
        {
            glob = imports.get(2);
            Assert.assertEquals(3, glob.get(Type.ID).intValue());
            Assert.assertEquals("REF_3", glob.get(Type.SKU));
        }
        {
            glob = imports.get(3);
            Assert.assertEquals(4, glob.get(Type.ID).intValue());
            Assert.assertEquals("REF_4", glob.get(Type.SKU));
        }
        {
            glob = imports.get(4);
            Assert.assertEquals(5, glob.get(Type.ID).intValue());
            Assert.assertEquals("", glob.get(Type.SKU));
        }
        {
            glob = imports.get(5);
            Assert.assertEquals(6, glob.get(Type.ID).intValue());
            Assert.assertEquals(",", glob.get(Type.SKU));
        }
        {
            glob = imports.get(6);
            Assert.assertEquals(7, glob.get(Type.ID).intValue());
            Assert.assertEquals("", glob.get(Type.SKU));
        }
    }

    @Test
    public void importWithReturnInEscaptedString() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',');

        List<Glob> imports = new ArrayList<>();
        importFile.importContent(new StringReader(
                "PRODUCT_ID,sku\n" +
                        "1,\"REF on \n multiples line_1\"\n" +
                        "\"4  \",\"REF_4\"\n" +
                        ""
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);
        Assert.assertEquals(2, imports.size());
        Glob glob;
        {
            glob = imports.get(0);
            Assert.assertEquals(1, glob.get(Type.ID).intValue());
            Assert.assertEquals("REF on \n multiples line_1", glob.get(Type.SKU));
        }
    }

    @Test
    public void withDoubleQuoteInString() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',');

        List<Glob> imports = new ArrayList<>();
        importFile.importContent(new StringReader(
                "PRODUCT_ID,sku\n" +
                        "1,\"REF on \\\"a quoted text\\\"\"\n" +
                        "\"4  \",\"REF_4\"\n" +
                        ""
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);
        Assert.assertEquals(2, imports.size());
        Glob glob;
        {
            glob = imports.get(0);
            Assert.assertEquals(1, glob.get(Type.ID).intValue());
            Assert.assertEquals("REF on \"a quoted text\"", glob.get(Type.SKU));
        }
    }

    @Test
    public void checkErrorReportLineOfBadLine() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',');

        List<Glob> imports = new ArrayList<>();
        try {
            importFile.importContent(new StringReader(
                    "PRODUCT_ID,sku\n" +
                            "1,\"REF on \"a badly quoted text\"\"\n" +
                            "\"4  \",\"REF_4\"\n" +
                            ""
            ), new Consumer<Glob>() {
                public void accept(Glob glob) {
                    imports.add(glob);
                }
            }, Type.TYPE);
            fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getMessage().contains("line 2"));
        }
    }

    @Test
    public void withoutType() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',');

        List<Glob> imports = new ArrayList<>();
        ImportFile.Importer importer = importFile.create(new StringReader(
                "PRODUCT_ID,sku\n" +
                        "1,\"REF_1\"\n" +
                        ""
        ));

        GlobType type = importer.getType();
        Assert.assertEquals("DefaultCsv", type.getName());
        importer.consume(new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        });

        Assert.assertEquals(1, imports.size());
        Glob glob;
        {
            glob = imports.get(0);
            Assert.assertEquals("1", glob.get(type.getField("PRODUCT_ID").asStringField()));
            Assert.assertEquals("REF_1", glob.get(type.getField("sku").asStringField()));
        }

    }

    @Test
    public void withDefaultType() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',').withDefaultGlobTypeName("Test");

        List<Glob> imports = new ArrayList<>();
        ImportFile.Importer importer = importFile.create(new StringReader(
                "PRODUCT_ID,sku\n" +
                        "1,\"REF_1\"\n" +
                        ""
        ));

        GlobType type = importer.getType();
        Assert.assertEquals("Test", type.getName());
        importer.consume(new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        });

        Assert.assertEquals(1, imports.size());
        Glob glob;
        {
            glob = imports.get(0);
            Assert.assertEquals("1", glob.get(type.getField("PRODUCT_ID").asStringField()));
            Assert.assertEquals("REF_1", glob.get(type.getField("sku").asStringField()));
        }
    }

    @Test
    public void extractHeader() throws IOException {
        GlobType globType = ImportFile.extractHeader(new ByteArrayInputStream("PR,TA,A".getBytes()), null);
        Assert.assertNotNull(globType);

        globType = ImportFile.extractHeader(new ByteArrayInputStream("PR,TA,A\ne,z,\"sdf,sd\"".getBytes()), null);
        Assert.assertNotNull(globType);

        globType = ImportFile.extractHeader(new ByteArrayInputStream("PR\tTA\tA".getBytes()), null);
        Assert.assertNotNull(globType);

        // fail =>
//        globType = ImportFile.extractHeader(new ByteArrayInputStream("PR,TA,A,B\ne,z,\"sdf\n,sd\",ee".getBytes()), null);
//        Assert.assertNotNull(globType);

    }

    @Test
    public void testWithQuoteChar() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',').withQuoteChar(null);

        List<Glob> imports = new ArrayList<>();
        importFile.importContent(new StringReader(
                "PRODUCT_ID,sku\n" +
                        "1,\"REF_1\"\n" +
                        ""
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);

        Assert.assertEquals(1, imports.size());
        Assert.assertEquals("\"REF_1\"", imports.get(0).get(Type.SKU));

    }

    @Test
    public void testWithoutHeader() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',').withQuoteChar(null);

        List<Glob> imports = new ArrayList<>();
        importFile.withHeader("PRODUCT_ID,sku");
        importFile.importContent(new StringReader(
                "1,\"REF_1\"\n" +
                        ""
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);

        Assert.assertEquals(1, imports.size());
        Assert.assertEquals("\"REF_1\"", imports.get(0).get(Type.SKU));
    }

    @Test
    public void testDateTimeAndPartialDateTime() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',');

        List<Glob> imports = new ArrayList<>();
        importFile.importContent(new StringReader(
                "PRODUCT_ID,date,dateTime,dateTimeWithoutTime\n" +
                        "1,20201130,20201130 223200,20201130\n"
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);

        Assert.assertEquals(1, imports.size());
        Assert.assertEquals("2020-11-29", imports.get(0).get(Type.dateTimeWithoutTime).format(DateTimeFormatter.ISO_LOCAL_DATE
                .withZone(ZoneId.of("America/Los_Angeles"))));
        Assert.assertEquals("2020-11-30", imports.get(0).get(Type.dateTimeWithoutTime).format(DateTimeFormatter.ISO_LOCAL_DATE
                .withZone(ZoneId.of("Europe/Paris"))));
    }

    @Test
    public void testDateTimeAndPartialDateTimeFromExcel() throws IOException {
        ImportFile importFile = new ImportFile();

        List<Glob> imports = new ArrayList<>();
        importFile.importContentExcel(getClass().getResourceAsStream("/date.xlsx"), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);

        Assert.assertEquals(1, imports.size());
        Assert.assertEquals("2020-11-29", imports.get(0).get(Type.dateTimeWithoutTime).format(DateTimeFormatter.ISO_LOCAL_DATE
                .withZone(ZoneId.of("America/Los_Angeles"))));
        Assert.assertEquals("2020-11-30", imports.get(0).get(Type.dateTimeWithoutTime).format(DateTimeFormatter.ISO_LOCAL_DATE
                .withZone(ZoneId.of("Europe/Paris"))));
    }

    @Test
    public void testDisableQuote() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',')
                .withQuoteChar(null);

        List<Glob> imports = new ArrayList<>();
        importFile.importContent(new StringReader(
                """
                        PRODUCT_ID,sku
                        120201130," ""XXX" "Y"""
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);

    }

    @Test
    public void renameHeader() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(',');
        importFile.withNameFrom("fi");

        List<Glob> imports = new ArrayList<>();
        importFile.importContent(new StringReader(
                "aa,cc,dd\n" +
                        "AZE,EZA,QDS\n"
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, RenameTestType.TYPE);
        Assert.assertEquals(1, imports.size());
        Glob glob;
        {
            glob = imports.get(0);
            Assert.assertEquals("AZE", glob.get(RenameTestType.a));
            Assert.assertEquals("EZA", glob.get(RenameTestType.b));
            Assert.assertEquals("QDS", glob.get(RenameTestType.d));
        }
    }

    @Test
    public void renameExcelHeader() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withNameFrom("fi");

        List<Glob> imports = new ArrayList<>();
        importFile.importContentExcel(getClass().getResourceAsStream("/test3.xlsx"), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, RenameTestType.TYPE);
        Assert.assertEquals(1, imports.size());
        Glob glob;
        {
            glob = imports.get(0);
            Assert.assertEquals("AZE", glob.get(RenameTestType.a));
            Assert.assertEquals("EZA", glob.get(RenameTestType.b));
            Assert.assertEquals("QDS", glob.get(RenameTestType.d));
        }
    }

    @Test
    public void readBigNumberAndFormula() throws IOException {
        ImportFile importFile = new ImportFile();
        List<Glob> imports = new ArrayList<>();
        importFile.importContentExcel(getClass().getResourceAsStream("/testBigNumberAndFormula.xlsx"), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, BigLine.TYPE);
        Assert.assertEquals("82100335101", imports.get(0).get(BigLine.CODE_ART));
        Assert.assertEquals("3700358217446", imports.get(0).get(BigLine.EAN));
        Assert.assertEquals("400", imports.get(0).get(BigLine.QTE_ATTENDUE));
        Assert.assertEquals("20221121", imports.get(0).get(BigLine.DATE_PREVUE));
        Assert.assertEquals("1", imports.get(0).get(BigLine.NUM_BL));
        Assert.assertEquals("1", imports.get(0).get(BigLine.NUM_COLIS));
        Assert.assertEquals("3700358090360", imports.get(1).get(BigLine.CODE_ART));
        Assert.assertEquals("3423222084141", imports.get(1).get(BigLine.EAN));
        Assert.assertEquals("12", imports.get(1).get(BigLine.QTE_ATTENDUE));
        Assert.assertEquals("20221121", imports.get(1).get(BigLine.DATE_PREVUE));
        Assert.assertEquals("1", imports.get(1).get(BigLine.NUM_BL));
        Assert.assertEquals("2", imports.get(1).get(BigLine.NUM_COLIS));

    }

    public static class BigLine {
        public static GlobType TYPE;

        public static StringField CODE_ART;
        public static StringField EAN;
        public static StringField QTE_ATTENDUE;
        public static StringField DATE_PREVUE;
        public static StringField NUM_BL;
        public static StringField NUM_COLIS;

        static {
            GlobTypeLoaderFactory.create(BigLine.class).load();
        }
    }


    static public class RenameTestType {
        public static GlobType TYPE;

        @ReNamedExport_(multi = @ReNamedMappingExport_(name = "fi", to = "aa"))
        public static StringField a;

        @ReNamedExport_(multi = @ReNamedMappingExport_(name = "fi", to = "cc"))
        public static StringField b;

        @ReNamedExport_("dd")
        public static StringField d;

        static {
            GlobTypeLoaderFactory.create(RenameTestType.class).load();
        }
    }

    static public class Type {
        public static GlobType TYPE;

        @FieldName_("PRODUCT_ID")
        public static IntegerField ID;

        @ImportEmptyStringHasEmptyStringFormat_(true)
        public static StringField SKU;

        @ExportDateFormat_("yyyyMMdd")
        public static DateField date;

        @ExportDateFormat_("yyyyMMdd HHmmss")
        public static DateTimeField dateTime;

        @ExportDateFormat_(value = "yyyyMMdd", zoneId = "Europe/Paris")
        public static DateTimeField dateTimeWithoutTime;

        static {
            GlobTypeLoaderFactory.create(Type.class, true).load();
        }
    }


    @Test
    public void testReadStringArray() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(';');
        List<Glob> data = new ArrayList<>();
        importFile.importContent(new StringReader("f1\na,b,c"), data::add, ObjectWithArray.TYPE);
        Assert.assertFalse(data.isEmpty());
        final String[] values = data.get(0).get(ObjectWithArray.f1);
        Assert.assertEquals(3, values.length);
        Assert.assertEquals("a", values[0]);
        Assert.assertEquals("b", values[1]);
        Assert.assertEquals("c", values[2]);
    }


    public static class ObjectWithArray {
        public static GlobType TYPE;

        public static StringArrayField f1;

        static {
            GlobTypeLoaderFactory.create(ObjectWithArray.class).load();
        }
    }

    // test done because the string contain \u0000 and it is forbidden in postgresql
    @Test
    public void importBinaryFile() throws IOException {
        ImportFile importFile = new ImportFile();
        importFile.withSeparator(';');
        final ImportFile.Importer importer = importFile.create(getClass().getResourceAsStream("/blankImage.jpg"));
        final GlobType type = importer.getType();
        Assert.assertNotNull(type.describe());
        final byte[] bytes = type.describe().getBytes(StandardCharsets.UTF_8);
        final String s = new String(bytes, StandardCharsets.UTF_8);
        Assert.assertEquals(type.describe(), s);
        Assert.assertTrue(type.describe().contains("\u0000"));
        final String replace = type.describe().replace('\u0000', ' ');
        Assert.assertFalse(replace.contains("\u0000"));
    }

    @Test
    public void readExcel() {

    }
}
