package org.globsframework.export;

import junit.framework.TestCase;
import org.globsframework.export.model.FieldMappingType;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Glob;
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
                                .set(FieldMappingType.TemplateType.from, new Glob[]{FieldMappingType.RenamedType.TYPE.instantiate()
                                        .set(FieldMappingType.RenamedType.renameTo, "d")
                                        .set(FieldMappingType.RenamedType.from,
                                        FieldMappingType.FromType.TYPE.instantiate().set(FieldMappingType.FromType.from, "b")),
                                        FieldMappingType.RenamedType.TYPE.instantiate()
                                                .set(FieldMappingType.RenamedType.from,
                                                FieldMappingType.FromType.TYPE.instantiate().set(FieldMappingType.FromType.from, "b")),
                                        FieldMappingType.RenamedType.TYPE.instantiate()
                                                .set(FieldMappingType.RenamedType.from,
                                                FieldMappingType.FromType.TYPE.instantiate().set(FieldMappingType.FromType.from, "c"))
                                })
                        )

        ));

        GlobType resultType = reformater.getResultType();
        StringField aa = resultType.getField("aa").asStringField();
        StringField ac = resultType.getField("ac").asStringField();
        Glob glob = reformater.transform(L1.TYPE.instantiate()
                .set(L1.a, "aa-xx")
                .set(L1.b, "bb")
                .set(L1.c, "c")
        );
        Assert.assertEquals("aa_xx", glob.get(aa));
        Assert.assertEquals("bb_bb_c", glob.get(ac));
    }

    public static class L1 {
        public static GlobType TYPE;

        public static StringField a;

        public static StringField b;

        public static StringField c;

        static {
            GlobTypeLoaderFactory.create(L1.class).load();
        }
    }
}