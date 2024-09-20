package org.globsframework.csv;

import junit.framework.TestCase;
import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.fields.StringField;
import org.globsframework.core.model.Glob;
import org.globsframework.csv.model.FieldMappingType;
import org.junit.Assert;

import java.util.List;

public class ReformaterTest extends TestCase {

    public void testName() {
        RealReformater reformater = new RealReformater(L1.TYPE, List.of(FieldMappingType.TYPE.instantiate()
                        .set(FieldMappingType.to, "aa")
                        .set(FieldMappingType.from, FieldMappingType.FromType.TYPE.instantiate()
                                .set(FieldMappingType.FromType.from, "a")
                                .set(FieldMappingType.FromType.formater, new Glob[]{FieldMappingType.FormatType.TYPE.instantiate()
                                        .set(FieldMappingType.FormatType.matcher, "^(.*)-(.*)")
                                        .set(FieldMappingType.FormatType.result, "$1_$2")}
                                )
                        ),
                FieldMappingType.TYPE.instantiate()
                        .set(FieldMappingType.to, "ac")
                        .set(FieldMappingType.from, FieldMappingType.TemplateType.TYPE.instantiate()
                                .set(FieldMappingType.TemplateType.template, "{d}_{b}_{c}")
                                .set(FieldMappingType.TemplateType.from, new Glob[]{
                                        FieldMappingType.RenamedType.TYPE.instantiate()
                                                .set(FieldMappingType.RenamedType.renameTo, "d")
                                                .set(FieldMappingType.RenamedType.from,
                                                FieldMappingType.FromType.TYPE.instantiate()
                                                        .set(FieldMappingType.FromType.from, "b")),
                                        FieldMappingType.RenamedType.TYPE.instantiate()
                                                .set(FieldMappingType.RenamedType.from,
                                                FieldMappingType.FromType.TYPE.instantiate().set(FieldMappingType.FromType.from, "b"))
                                })
                        ),
                FieldMappingType.TYPE.instantiate()
                        .set(FieldMappingType.to, "compute")
                        .set(FieldMappingType.from, FieldMappingType.SumData.TYPE.instantiate()
                                .set(FieldMappingType.SumData.from, new Glob[]{
                                        FieldMappingType.FromType.TYPE.instantiate()
                                                .set(FieldMappingType.FromType.from, "value1"),
                                        FieldMappingType.FromType.TYPE.instantiate()
                                                .set(FieldMappingType.FromType.from, "value2")
                                })
                        ),
                FieldMappingType.TYPE.instantiate()
                        .set(FieldMappingType.to, "maps")
                        .set(FieldMappingType.from, FieldMappingType.MappingData.TYPE.instantiate()
                                .set(FieldMappingType.MappingData.from, FieldMappingType.FromType.TYPE.instantiate()
                                        .set(FieldMappingType.FromType.from, "name"))
                                .set(FieldMappingType.MappingData.mappingName, "traduction")
                                .set(FieldMappingType.MappingData.mapping, new Glob[]{FieldMappingType.KeyValue.create("toto", "titi")})
                        )
        ), true);

        GlobType resultType = reformater.getResultType();
        StringField aa = resultType.getField("aa").asStringField();
        StringField ac = resultType.getField("ac").asStringField();
        StringField a = resultType.getField("a").asStringField();
        StringField compute = resultType.getField("compute").asStringField();
        StringField maps = resultType.getField("maps").asStringField();
        Glob glob = reformater.transform(L1.TYPE.instantiate()
                .set(L1.a, "aa-xx")
                .set(L1.b, "bb")
                .set(L1.name, "toto")
                .set(L1.c, "c")
                .set(L1.value1, "1.1")
                .set(L1.value2, "2.04")
        );
        Assert.assertEquals("aa_xx", glob.get(aa));
        Assert.assertEquals("bb_bb_c", glob.get(ac));
        Assert.assertEquals("bb_bb_c", glob.get(ac));
        Assert.assertEquals("aa-xx", glob.get(a));
        Assert.assertEquals("3.14", glob.get(compute));
        Assert.assertEquals("titi", glob.get(maps));
    }

    public static class L1 {
        public static GlobType TYPE;

        public static StringField a;

        public static StringField b;

        public static StringField c;

        public static StringField value1;

        public static StringField value2;

        public static StringField name;

        static {
            GlobTypeLoaderFactory.create(L1.class).load();
        }
    }
}
