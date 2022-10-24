package org.globsframework.export;

import org.globsframework.export.annotation.CsvHeader_;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.Target;
import org.globsframework.metamodel.fields.GlobArrayField;
import org.globsframework.metamodel.fields.GlobField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Glob;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class MixedTest {

    @Test
    public void name() {
        String val = "" +
                "H1;A;B\n" +
                "H2;C;D;E;F\n" +
                "H2;vc1;vd1;ve1;vf1\n" +
                "H2;vc2;vd2;ve2;vf2\n" +
                "H3;a;b\n";

        ImportFile importFile = new ImportFile();
        ImportFile.Importer multi = importFile.withSeparator(';')
                .createMulti(new StringReader(val), Root.TYPE);
        List<Glob> list = new ArrayList<>();
        multi.consume(list::add);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(2, list.get(0).get(Root.h2).length);
        Assert.assertEquals("vd2", list.get(0).get(Root.h2)[1].get(H2.D));
    }

    @Test
    public void nameExcel() {
        String val = "" +
                "H1;A;B\n" +
                "H2;C;D;E;F\n" +
                "H2;vc1;vd1;ve1;vf1\n" +
                "H2;vc2;vd2;ve2;vf2\n" +
                "H3;a;b\n";

        ImportFile importFile = new ImportFile();
        ImportFile.Importer multi = importFile.withSeparator(';')
                .createMultiExcel(getClass().getResourceAsStream("/mixed.xlsx"), Root.TYPE, List.of());
        List<Glob> list = new ArrayList<>();
        multi.consume(list::add);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(2, list.get(0).get(Root.h2).length);
        Assert.assertEquals("vd2", list.get(0).get(Root.h2)[1].get(H2.D));
    }

    public static class Root {
        public static GlobType TYPE;

        @CsvHeader_("H1")
        @Target(H1.class)
        public static GlobField h1;

        @CsvHeader_(value = "H2", firstLineIsHeader = true)
        @Target(H2.class)
        public static GlobArrayField h2;

        @CsvHeader_("H3")
        @Target(H3.class)
        public static GlobField h3;

        static {
            GlobTypeLoaderFactory.create(Root.class).load();
        }
    }

    public static class H1 {
        public static GlobType TYPE;

        public static StringField valA;

        public static StringField valB;

        static {
            GlobTypeLoaderFactory.create(H1.class).load();
        }
    }

    public static class H2 {
        public static GlobType TYPE;

        public static StringField D;

        public static StringField E;

        public static StringField F;

        public static StringField C;

        static {
            GlobTypeLoaderFactory.create(H2.class).load();
        }
    }

    public static class H3 {
        public static GlobType TYPE;

        public static StringField valA;

        public static StringField valB;

        static {
            GlobTypeLoaderFactory.create(H3.class).load();
        }
    }



}
