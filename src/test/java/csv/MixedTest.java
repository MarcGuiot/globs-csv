package org.globsframework.csv;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.Target;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.GlobField;
import org.globsframework.core.metamodel.fields.IntegerField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.csv.annotation.CsvHeader_;
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
                "H3;a;b\n" +
                "H1;C;D\n" +
                "H2;E;F;G;H";

        ImportFile importFile = new ImportFile();
        ImportFile.Importer multi = importFile.withSeparator(';')
                .createMulti(new StringReader(val), Root.TYPE);
        List<Glob> list = new ArrayList<>();
        multi.consume(list::add);
        Assert.assertEquals(2, list.size());
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


    public class CCMOrderType {
        public static GlobType TYPE;

        @CsvHeader_("CCM")
        @Target(CCMLineType.class)
        public static GlobField order;

        @CsvHeader_("CCL")
        @Target(CCLLineType.class)
        public static GlobArrayField items;

        @CsvHeader_("ADR")
        @Target(ADRLineType.class)
        public static GlobArrayField addresses;

        static {
            GlobTypeLoaderFactory.create(CCMOrderType.class).load();
        }

        public static class CCMLineType {
            public static GlobType TYPE;

            public static StringField GP_REFINTERNE;
            public static StringField GP_DATEPIECE;
            public static StringField GP_TIERS;
            public static StringField MEJ_CDEECOMENVOI;
            public static StringField MEJ_CDEECOMETAB;
            public static StringField MEJ_CDEECOMSUIVI;
            public static StringField MEJ_CDEECOMREGLT;
            public static StringField MEJ_CDEECOMFACT;
            public static StringField MEJ_CDEECOMEXPED;
            public static StringField GP_ETABLISSEMENT; //CFR1, CLU1, CBE1, CCH1, CDE1, GLOE.
            public static StringField GPA_PAYS;

            static {
                GlobTypeLoaderFactory.create(CCMLineType.class).load();
            }
        }

        public static class CCLLineType {
            public static GlobType TYPE;

            public static StringField GP_REFINTERNE;
            public static StringField GL_NUMLIGNE;
            public static StringField CODEBARRE;
            public static StringField SKU;
            public static StringField GL_LIBELLE;
            public static IntegerField GL_QTEFACT;
            public static StringField GL_PUTTCNETDEV;
            public static StringField GL_DEPOT;
            public static StringField GL_FAMILLETAXE1;
            public static StringField GL_DEVISE;

            static {
                GlobTypeLoaderFactory.create(CCLLineType.class).load();
            }
        }

        public static class ADRLineType {
            public static GlobType TYPE;

            public static StringField GP_REFINTERNE;
            public static StringField GPA_TYPEPIECEADR;
            public static StringField GPA_JURIDIQUE;
            public static StringField GPA_LIBELLE;
            public static StringField GPA_LIBELLE2;
            public static StringField GPA_ADRESSE1;
            public static StringField GPA_ADRESSE2;
            public static StringField GPA_ADRESSE3;
            public static StringField GPA_CODEPOSTAL;
            public static StringField GPA_VILLE;
            public static StringField GPA_PAYS;
            public static StringField GPA_TELEPHONE;
            public static StringField GPA_EMAIL;

            static {
                GlobTypeLoaderFactory.create(ADRLineType.class).load();
            }
        }
    }


}
