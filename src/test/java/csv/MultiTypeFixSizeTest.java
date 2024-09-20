package org.globsframework.csv;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.Target;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.GlobField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.csv.annotation.CsvHeader_;
import org.globsframework.csv.annotation.ExportColumnSize_;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MultiTypeFixSizeTest {

    @Test
    public void name() {
        String data =
                """
                        TYPE_Ava1va2
                        TYPE_Bvb11vb12
                        TYPE_Bvb21vb22
                        TYPE_A a1 a2
                        TYPE_Bab11ab12
                        TYPE_Bab21ab22
                        TYPE_A a3 a2
                        TYPE_A a4 a2
                        """;
        ImportFile importFile = new ImportFile();
        ImportFile.Importer multi = importFile.createMulti(new StringReader(data), Root.TYPE);
        List<Glob> got = new ArrayList<>();
        multi.consume(new Consumer<Glob>() {
            public void accept(Glob glob) {
                got.add(glob);
            }
        });
        Assert.assertEquals(4, got.size());
        Glob glob = got.get(0);
        Assert.assertNotNull(glob.get(Root.typeA));
        Assert.assertEquals(2, glob.getOrEmpty(Root.typeB).length);
        Assert.assertEquals("vb11", glob.getOrEmpty(Root.typeB)[0].get(TypeB.val1));
        Assert.assertEquals("vb12", glob.getOrEmpty(Root.typeB)[0].get(TypeB.val2));
        Assert.assertEquals("vb21", glob.getOrEmpty(Root.typeB)[1].get(TypeB.val1));
        Assert.assertEquals("vb22", glob.getOrEmpty(Root.typeB)[1].get(TypeB.val2));
        Glob glob3 = got.get(2);
        Assert.assertNotNull(glob3.get(Root.typeA));
        Assert.assertEquals(0, glob3.getOrEmpty(Root.typeB).length);

        ExportBySize exportBySize = new ExportBySize();
        exportBySize.withLeftPadding();
        StringWriter writer = new StringWriter();
        exportBySize.exportMulti(Root.TYPE, got.stream(), writer);
        Assert.assertEquals(data, writer.toString());
    }


    public static class Root {
        public static GlobType TYPE;

        @Target(TypeA.class)
        @CsvHeader_("TYPE_A")
        @ExportColumnSize_(6)
        public static GlobField typeA;

        @Target(TypeB.class)
        @CsvHeader_("TYPE_B")
        @ExportColumnSize_(6)
        public static GlobArrayField typeB;

        static {
            GlobTypeLoaderFactory.create(Root.class).load();
        }
    }

    public static class TypeA {
        public static GlobType TYPE;

        @ExportColumnSize_(3)
        public static StringField val1;

        @ExportColumnSize_(3)
        public static StringField val2;

        static {
            GlobTypeLoaderFactory.create(TypeA.class).load();
        }
    }

    public static class TypeB {
        public static GlobType TYPE;

        @ExportColumnSize_(4)
        public static StringField val1;

        @ExportColumnSize_(4)
        public static StringField val2;

        static {
            GlobTypeLoaderFactory.create(TypeB.class).load();
        }
    }
}
