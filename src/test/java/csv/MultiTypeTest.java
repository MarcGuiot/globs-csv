package org.globsframework.csv;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.Target;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.GlobField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.csv.annotation.CsvHeader_;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MultiTypeTest {

    @Test
    public void name() {
        String data =
                "TYPE_A;va1;va2\n" +
                        "TYPE_B;vb11;vb12\n" +
                        "TYPE_B;vb21;vb22\n" +
                        "TYPE_A;a1;a2\n" +
                        "TYPE_B;ab11;ab12\n" +
                        "TYPE_B;ab21;ab22\n" +
                        "TYPE_A;a3;a2\n" +
                        "TYPE_A;a4;a2\n" +
                        "";
        ImportFile importFile = new ImportFile();
        ImportFile.Importer multi = importFile.withSeparator(';')
                .createMulti(new StringReader(data), Root.TYPE);
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
        StringWriter writer = new StringWriter();
        exportBySize.withSeparator(';');
        exportBySize.exportMulti(Root.TYPE, got.stream(), writer);
        Assert.assertEquals(data, writer.toString());
    }

    @Test
    public void nameExcel() {
        String data =
                "TYPE_A;va1;va2\n" +
                        "TYPE_B;vb11;vb12\n" +
                        "TYPE_B;vb21;vb22\n" +
                        "TYPE_A;a1;a2\n" +
                        "TYPE_B;ab11;ab12\n" +
                        "TYPE_B;ab21;ab22\n" +
                        "TYPE_A;a3;a2\n" +
                        "TYPE_A;a4;a2\n" +
                        "";
        ImportFile importFile = new ImportFile();
        ImportFile.Importer multi = importFile.withSeparator(';')
                .createMultiExcel(getClass().getResourceAsStream("/multiType.xlsx"), Root.TYPE, List.of());
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
        StringWriter writer = new StringWriter();
        exportBySize.withSeparator(';');
        exportBySize.exportMulti(Root.TYPE, got.stream(), writer);
        Assert.assertEquals(data, writer.toString());
    }

    public static class Root {
        public static GlobType TYPE;

        @Target(TypeA.class)
        @CsvHeader_("TYPE_A")
        public static GlobField typeA;

        @Target(TypeB.class)
        @CsvHeader_("TYPE_B")
        public static GlobArrayField typeB;

        static {
            GlobTypeLoaderFactory.create(Root.class).load();
        }
    }

    public static class TypeA {
        public static GlobType TYPE;

        public static StringField val1;

        public static StringField val2;

        static {
            GlobTypeLoaderFactory.create(TypeA.class).load();
        }
    }

    public static class TypeB {
        public static GlobType TYPE;

        public static StringField val1;

        public static StringField val2;

        static {
            GlobTypeLoaderFactory.create(TypeB.class).load();
        }
    }
}
