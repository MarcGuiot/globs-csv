package org.globsframework.export;

import org.globsframework.export.annotation.ExportDateFormat_;
import org.globsframework.export.annotation.ImportEmptyStringHasEmptyStringFormat_;
import org.globsframework.export.annotation.ReNamedExport_;
import org.globsframework.export.annotation.ReNamedMappingExport_;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.FieldNameAnnotation;
import org.globsframework.metamodel.fields.DateField;
import org.globsframework.metamodel.fields.DateTimeField;
import org.globsframework.metamodel.fields.IntegerField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Glob;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
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
                        "\"6  \",\n" +
                        ""
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);

        Assert.assertEquals(6, imports.size());
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

        @FieldNameAnnotation("PRODUCT_ID")
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
}