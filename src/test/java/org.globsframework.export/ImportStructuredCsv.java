package org.globsframework.export;

import org.globsframework.export.model.FieldMappingType;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.Target;
import org.globsframework.metamodel.fields.GlobArrayField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Glob;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ImportStructuredCsv {

    @Test
    public void name() throws IOException {
        String str = "a;b;c\n" +
                "aa;bb;aa\n" +
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

        ImportFile.Importer importFile = new ImportFile().withSeparator(';')
                .withTransformer(List.of(a, b, c))
                .createComplex(new StringReader(str), L1.TYPE);
        importFile.consume(globConsumer);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("aa", l.get(0).get(L1.aa));
        Assert.assertEquals("bbb", l.get(1).get(L1.aa));
        Assert.assertEquals(2, l.get(0).get(L1.l2).length);
        Assert.assertEquals(1, l.get(0).get(L1.l2)[0].get(L2.l3).length);
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

        public static StringField dd;

        static {
            GlobTypeLoaderFactory.create(L3.class).load();
        }
    }

}
