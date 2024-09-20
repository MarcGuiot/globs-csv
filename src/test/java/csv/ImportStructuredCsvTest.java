package org.globsframework.csv;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.Target;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.GlobField;
import org.globsframework.core.metamodel.fields.IntegerField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.csv.model.FieldMappingType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ImportStructuredCsvTest {

    @Test
    public void name() throws IOException {
        String str = "a;b;c;dd\n" +
                "aa;bb;aa;d\n" +
                "aa;cc;\n" +
                "bbb;bb;\n";

        List<Glob> l = new ArrayList<>();
        Consumer<Glob> globConsumer = new Consumer<Glob>() {
            public void accept(Glob glob) {
                l.add(glob);
            }
        };

        Glob a = FieldMappingType.TYPE.instantiate()
                .set(FieldMappingType.to, "aa")
                .set(FieldMappingType.from, FieldMappingType.FromType.TYPE.instantiate()
                        .set(FieldMappingType.FromType.from, "a"));
        Glob b = FieldMappingType.TYPE.instantiate()
                .set(FieldMappingType.to, "bb")
                .set(FieldMappingType.from, FieldMappingType.FromType.TYPE.instantiate()
                        .set(FieldMappingType.FromType.from, "b"));
        Glob c = FieldMappingType.TYPE.instantiate()
                .set(FieldMappingType.to, "cc")
                .set(FieldMappingType.from, FieldMappingType.FromType.TYPE.instantiate()
                        .set(FieldMappingType.FromType.from, "c"));
        Glob d = FieldMappingType.TYPE.instantiate()
                .set(FieldMappingType.to, "dd")
                .set(FieldMappingType.from, FieldMappingType.FromType.TYPE.instantiate()
                        .set(FieldMappingType.FromType.from, "dd"));

        ImportFile.Importer importFile = new ImportFile().withSeparator(';')
                .withTransformer(List.of(a, b, c, d), false)
                .createComplex(new StringReader(str), L1.TYPE);
        importFile.consume(globConsumer);
        Assert.assertEquals(2, l.size());
        Glob first = l.get(0);
        Assert.assertEquals("aa", first.get(L1.aa));
        Assert.assertEquals("bbb", l.get(1).get(L1.aa));
        Assert.assertEquals(2, first.get(L1.l2).length);
        Assert.assertEquals(1, first.get(L1.l2)[0].get(L2.l3).length);
        Glob fullFirstLine = first.get(L1.l2)[0].get(L2.l3)[0];
        Assert.assertEquals("aa", fullFirstLine.get(L3.aa));
        Assert.assertEquals("bb", fullFirstLine.get(L3.bb));
        Assert.assertEquals("aa", fullFirstLine.get(L3.cc));
        Assert.assertEquals("d", fullFirstLine.get(L3.dd));
    }

    @Test
    public void nameExcel() throws IOException {

        List<Glob> l = new ArrayList<>();
        Consumer<Glob> globConsumer = new Consumer<Glob>() {
            public void accept(Glob glob) {
                l.add(glob);
            }
        };

        Glob a = FieldMappingType.TYPE.instantiate()
                .set(FieldMappingType.to, "aa")
                .set(FieldMappingType.from, FieldMappingType.FromType.TYPE.instantiate()
                        .set(FieldMappingType.FromType.from, "a"));
        Glob b = FieldMappingType.TYPE.instantiate()
                .set(FieldMappingType.to, "bb")
                .set(FieldMappingType.from, FieldMappingType.FromType.TYPE.instantiate()
                        .set(FieldMappingType.FromType.from, "b"));
        Glob c = FieldMappingType.TYPE.instantiate()
                .set(FieldMappingType.to, "cc")
                .set(FieldMappingType.from, FieldMappingType.FromType.TYPE.instantiate()
                        .set(FieldMappingType.FromType.from, "c"));
        Glob d = FieldMappingType.TYPE.instantiate()
                .set(FieldMappingType.to, "dd")
                .set(FieldMappingType.from, FieldMappingType.FromType.TYPE.instantiate()
                        .set(FieldMappingType.FromType.from, "dd"));

        ImportFile.Importer importFile = new ImportFile().withSeparator(';')
                .withTransformer(List.of(a, b, c, d), false)
                .createComplexExcel(getClass().getResourceAsStream("/struct.xlsx"), L1.TYPE);
        importFile.consume(globConsumer);
        Assert.assertEquals(2, l.size());
        Glob first = l.get(0);
        Assert.assertEquals("aa", first.get(L1.aa));
        Assert.assertEquals("bbb", l.get(1).get(L1.aa));
        Assert.assertEquals(2, first.get(L1.l2).length);
        Assert.assertEquals(1, first.get(L1.l2)[0].get(L2.l3).length);
        Glob fullFirstLine = first.get(L1.l2)[0].get(L2.l3)[0];
        Assert.assertEquals("aa", fullFirstLine.get(L3.aa));
        Assert.assertEquals("bb", fullFirstLine.get(L3.bb));
        Assert.assertEquals("aa", fullFirstLine.get(L3.cc));
        Assert.assertEquals("d", fullFirstLine.get(L3.dd));
    }

    @Test
    public void simple() throws IOException {
        String str = "a;b;c;d;e\n" +
                "aa;bb;aa;d;1\n" +
                "aa;cc;2\n" +
                "bbb;bb;;3\n";

        List<Glob> l = new ArrayList<>();
        Consumer<Glob> globConsumer = new Consumer<Glob>() {
            public void accept(Glob glob) {
                l.add(glob);
            }
        };

        ImportFile.Importer importFile = new ImportFile().withSeparator(';')
                .createComplex(new StringReader(str), SimpleL1.TYPE);
        importFile.consume(globConsumer);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("aa", l.get(0).get(SimpleL1.l4).get(L4.c));
        Assert.assertEquals("d", l.get(0).get(SimpleL1.l4).get(L4.d));
    }

    public static class SimpleL1 {
        public static GlobType TYPE;

        public static StringField a;
        public static StringField b;

        @Target(L4.class)
        public static GlobField l4;

        static {
            GlobTypeLoaderFactory.create(SimpleL1.class).load();
        }
    }

    public static class L4 {
        public static GlobType TYPE;

        public static StringField c;

        public static StringField d;

        public static IntegerField e;

        static {
            GlobTypeLoaderFactory.create(L4.class).load();
        }
    }

    public static class L1 {
        public static GlobType TYPE;

        public static StringField aa;

        @Target(L2.class)
        public static GlobArrayField l2;


        static {
            GlobTypeLoaderFactory.create(L1.class).load();
        }
    }

    public static class L2 {
        public static GlobType TYPE;

        public static StringField bb;

        public static StringField cc;

        @Target(L3.class)
        public static GlobArrayField l3;


        static {
            GlobTypeLoaderFactory.create(L2.class).load();
        }
    }

    public static class L3 {
        public static GlobType TYPE;

        public static StringField aa;
        public static StringField bb;
        public static StringField cc;
        public static StringField dd;

        static {
            GlobTypeLoaderFactory.create(L3.class).load();
        }
    }

}
