package org.globsframework.csv;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeBuilder;
import org.globsframework.core.metamodel.GlobTypeBuilderFactory;
import org.globsframework.core.metamodel.fields.GlobArrayField;
import org.globsframework.core.metamodel.fields.GlobField;
import org.globsframework.core.metamodel.fields.IntegerField;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.csv.annotation.CsvHeader;
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

        public static GlobField<H1> h1;

        public static GlobArrayField<H2> h2;

        public static GlobField<H3> h3;

        static {
            GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("Root");
            h1 = typeBuilder.declareGlobField("h1", () -> H1.TYPE, CsvHeader.create("H1"));
            h2 = typeBuilder.declareGlobArrayField("h2", () -> H2.TYPE, CsvHeader.create("H2", true));
            h3 = typeBuilder.declareGlobField("h3", () -> H3.TYPE, CsvHeader.create("H3"));
            TYPE = typeBuilder.build();
        }
    }

    public static class H1 {
        public static GlobType TYPE;

        public static StringField valA;

        public static StringField valB;

        static {
            GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("H1");
            valA = typeBuilder.declareStringField("valA");
            valB = typeBuilder.declareStringField("valB");
            TYPE = typeBuilder.build();
        }
    }

    public static class H2 {
        public static GlobType TYPE;

        public static StringField D;

        public static StringField E;

        public static StringField F;

        public static StringField C;

        static {
            GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("H2");
            D = typeBuilder.declareStringField("D");
            E = typeBuilder.declareStringField("E");
            F = typeBuilder.declareStringField("F");
            C = typeBuilder.declareStringField("C");
            TYPE = typeBuilder.build();
        }
    }

    public static class H3 {
        public static GlobType TYPE;

        public static StringField valA;

        public static StringField valB;

        static {
            GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("H3");
            valA = typeBuilder.declareStringField("valA");
            valB = typeBuilder.declareStringField("valB");
            TYPE = typeBuilder.build();
        }
    }


    public class CCMOrderType {
        public static GlobType TYPE;

        public static GlobField<CCMLineType> order;

        public static GlobArrayField<CCLLineType> items;

        public static GlobArrayField<ADRLineType> addresses;

        static {
            GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("CCMOrderType");
            order = typeBuilder.declareGlobField("order", () -> CCMLineType.TYPE, CsvHeader.create("CCM"));
            items = typeBuilder.declareGlobArrayField("items", () -> CCLLineType.TYPE, CsvHeader.create("CCL"));
            addresses = typeBuilder.declareGlobArrayField("addresses", () -> ADRLineType.TYPE, CsvHeader.create("ADR"));
            TYPE = typeBuilder.build();
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
                GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("CCMLineType");
                GP_REFINTERNE = typeBuilder.declareStringField("GP_REFINTERNE");
                GP_DATEPIECE = typeBuilder.declareStringField("GP_DATEPIECE");
                GP_TIERS = typeBuilder.declareStringField("GP_TIERS");
                MEJ_CDEECOMENVOI = typeBuilder.declareStringField("MEJ_CDEECOMENVOI");
                MEJ_CDEECOMETAB = typeBuilder.declareStringField("MEJ_CDEECOMETAB");
                MEJ_CDEECOMSUIVI = typeBuilder.declareStringField("MEJ_CDEECOMSUIVI");
                MEJ_CDEECOMREGLT = typeBuilder.declareStringField("MEJ_CDEECOMREGLT");
                MEJ_CDEECOMFACT = typeBuilder.declareStringField("MEJ_CDEECOMFACT");
                MEJ_CDEECOMEXPED = typeBuilder.declareStringField("MEJ_CDEECOMEXPED");
                GP_ETABLISSEMENT = typeBuilder.declareStringField("GP_ETABLISSEMENT");
                GPA_PAYS = typeBuilder.declareStringField("GPA_PAYS");
                TYPE = typeBuilder.build();
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
                GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("CCLLineType");
                GP_REFINTERNE = typeBuilder.declareStringField("GP_REFINTERNE");
                GL_NUMLIGNE = typeBuilder.declareStringField("GL_NUMLIGNE");
                CODEBARRE = typeBuilder.declareStringField("CODEBARRE");
                SKU = typeBuilder.declareStringField("SKU");
                GL_LIBELLE = typeBuilder.declareStringField("GL_LIBELLE");
                GL_QTEFACT = typeBuilder.declareIntegerField("GL_QTEFACT");
                GL_PUTTCNETDEV = typeBuilder.declareStringField("GL_PUTTCNETDEV");
                GL_DEPOT = typeBuilder.declareStringField("GL_DEPOT");
                GL_FAMILLETAXE1 = typeBuilder.declareStringField("GL_FAMILLETAXE1");
                GL_DEVISE = typeBuilder.declareStringField("GL_DEVISE");
                TYPE = typeBuilder.build();
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
                GlobTypeBuilder typeBuilder = GlobTypeBuilderFactory.create("ADRLineType");
                GP_REFINTERNE = typeBuilder.declareStringField("GP_REFINTERNE");
                GPA_TYPEPIECEADR = typeBuilder.declareStringField("GPA_TYPEPIECEADR");
                GPA_JURIDIQUE = typeBuilder.declareStringField("GPA_JURIDIQUE");
                GPA_LIBELLE = typeBuilder.declareStringField("GPA_LIBELLE");
                GPA_LIBELLE2 = typeBuilder.declareStringField("GPA_LIBELLE2");
                GPA_ADRESSE1 = typeBuilder.declareStringField("GPA_ADRESSE1");
                GPA_ADRESSE2 = typeBuilder.declareStringField("GPA_ADRESSE2");
                GPA_ADRESSE3 = typeBuilder.declareStringField("GPA_ADRESSE3");
                GPA_CODEPOSTAL = typeBuilder.declareStringField("GPA_CODEPOSTAL");
                GPA_VILLE = typeBuilder.declareStringField("GPA_VILLE");
                GPA_PAYS = typeBuilder.declareStringField("GPA_PAYS");
                GPA_TELEPHONE = typeBuilder.declareStringField("GPA_TELEPHONE");
                GPA_EMAIL = typeBuilder.declareStringField("GPA_EMAIL");
                TYPE = typeBuilder.build();
            }
        }
    }


}
