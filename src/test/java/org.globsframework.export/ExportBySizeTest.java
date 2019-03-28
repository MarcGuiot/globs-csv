package org.globsframework.export;

import org.globsframework.export.annotation.ExportColumnSize_;
import org.globsframework.export.annotation.ExportDateFormat_;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.fields.DateField;
import org.globsframework.metamodel.fields.DoubleField;
import org.globsframework.metamodel.fields.IntegerField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.MutableGlob;
import org.globsframework.sqlstreams.annotations.typed.TypedIsDate;
import org.junit.Test;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ExportBySizeTest {

    @Test
    public void basicExport() {

        MutableGlob data = Data.TYPE.instantiate().set(Data.NAME, "some data")
                .set(Data.COUNT, 300)
                .set(Data.VALUE, 3235.14153)
                .set(Data.DATE_AS_INT, (int) LocalDate.of(2018, 01, 02).toEpochDay())
                .set(Data.DATE, LocalDate.of(2019, 01, 02));

        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize.withSeparator('|').withPadding();
            exportBySize.export(Stream.of(data), writer);
            assertEquals(" some data|   300|  3235.14153|2018/01/02|2019/01/02", writer.getBuffer().toString());
        }
        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize.withPadding();
            exportBySize.export(Stream.of(data), writer);
            assertEquals(" some data   300  3235.141532018/01/022019/01/02", writer.getBuffer().toString());
        }
        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize.withSeparator(';');
            exportBySize.export(Stream.of(data), writer);
            assertEquals("some data;300;3235.14153;2018/01/02;2019/01/02", writer.getBuffer().toString());
        }
    }

    public static class Data {
        public static GlobType TYPE;

        @ExportColumnSize_(10)
        public static StringField NAME;

        @ExportColumnSize_(6)
        public static IntegerField COUNT;

        @ExportColumnSize_(12)
        public static DoubleField VALUE;

        @TypedIsDate
        @ExportDateFormat_("YYYY/MM/DD")
        @ExportColumnSize_(10)
        public static IntegerField DATE_AS_INT;

        @ExportDateFormat_("YYYY/MM/DD")
        @ExportColumnSize_(10)
        public static DateField DATE;

        static {
            GlobTypeLoaderFactory.createAndLoad(Data.class);
        }
    }
}