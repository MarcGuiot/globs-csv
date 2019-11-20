package org.globsframework.export;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.FieldNameAnnotation;
import org.globsframework.metamodel.fields.DoubleField;
import org.globsframework.metamodel.fields.IntegerField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Glob;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
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
                        ""
        ), new Consumer<Glob>() {
            public void accept(Glob glob) {
                imports.add(glob);
            }
        }, Type.TYPE);

        Assert.assertEquals(4, imports.size());
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

    static public class Type {
        public static GlobType TYPE;

        @FieldNameAnnotation("PRODUCT_ID")
        public static IntegerField ID;

        public static StringField SKU;

        static {
            GlobTypeLoaderFactory.create(Type.class).load();
        }
    }
}