package org.globsframework.csv;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.GlobTypeLoaderFactory;
import org.globsframework.core.metamodel.annotations.IsDate_;
import org.globsframework.core.metamodel.fields.*;
import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;
import org.globsframework.core.utils.Ref;
import org.globsframework.csv.annotation.ExportColumnSize_;
import org.globsframework.csv.annotation.ExportDateFormat_;
import org.globsframework.csv.annotation.NamedExport_;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
            exportBySize.withSeparator('|').withLeftPadding();
            exportBySize.export(Stream.of(data), writer);
            assertEquals(" some data|   300|  3235.14153|2018/01/02|2019/01/02\n", writer.getBuffer().toString());
        }
        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize.withLeftPadding();
            exportBySize.export(Stream.of(data), writer);
            assertEquals(" some data   300  3235.141532018/01/022019/01/02\n", writer.getBuffer().toString());
        }
        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize.withSeparator(';');
            exportBySize.export(Stream.of(data), writer);
            assertEquals("some data;300;3235.14153;2018/01/02;2019/01/02\n", writer.getBuffer().toString());
        }
        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize.withSeparator('|').withRightPadding();
            exportBySize.export(Stream.of(data), writer);
            assertEquals("some data |300   |3235.14153  |2018/01/02|2019/01/02\n", writer.getBuffer().toString());
        }
        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize.withRightPadding();
            exportBySize.export(Stream.of(data), writer);
            assertEquals("some data 300   3235.14153  2018/01/022019/01/02\n", writer.getBuffer().toString());
        }
    }

    @Test
    public void escapeExportWithFirstEscapeChar() throws IOException {

        MutableGlob data = Data.TYPE.instantiate().set(Data.NAME, "\"some\" data")
                .set(Data.COUNT, 300);
        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize.withSeparator(';');
            exportBySize.export(Stream.of(data), writer);
            assertEquals("\"\"\"some\"\" data\";300;;;\n", writer.getBuffer().toString());
            ImportFile importFile = new ImportFile();
            Ref<Glob> actual = new Ref<>();
            importFile.withSeparator(';')
                    .withHeader("name;count;")
                    .create(new StringReader(writer.getBuffer().toString()), Data.TYPE)
                    .consume(actual::set);
            assertNotNull(actual.get());
            assertEquals(actual.get().get(Data.NAME), "\"some\" data");
        }

    }


    @Test
    public void exportWithHeaderAndMultipleLines() {
        Glob data1 = Data.TYPE.instantiate().set(Data.NAME, "some data")
                .set(Data.COUNT, 300)
                .set(Data.VALUE, 3235.14153)
                .set(Data.DATE_AS_INT, (int) LocalDate.of(2018, 01, 02).toEpochDay())
                .set(Data.DATE, LocalDate.of(2019, 01, 02));

        Glob data2 = Data.TYPE.instantiate().set(Data.NAME, "some other data")
                .set(Data.COUNT, 400)
                .set(Data.VALUE, 235.14153)
                .set(Data.DATE_AS_INT, (int) LocalDate.of(2017, 01, 02).toEpochDay())
                .set(Data.DATE, LocalDate.of(2019, 01, 02));

        ExportBySize exportBySize = new ExportBySize().withSeparator(',');
        StringWriter writer = new StringWriter();
        exportBySize.exportHeader(Data.TYPE, writer);
        exportBySize.export(Stream.of(data1, data2), writer);
        String expected = "name,count,value,dateAsInt,date\n" +
                "some data,300,3235.14153,2018/01/02,2019/01/02\n" +
                "some other data,400,235.14153,2017/01/02,2019/01/02\n";
        System.out.println(expected);
        System.out.println(writer.toString());
        Assert.assertEquals(expected, writer.toString());
    }

    @Test
    public void export() throws IOException {
        String value1 = "some \ndata";
        Glob data1 = Data.TYPE.instantiate().set(Data.NAME, value1);

        String value2 = "some \n \"other ,data";
        Glob data2 = Data.TYPE.instantiate().set(Data.NAME, value2);

        String value3 = "some  ,data";
        Glob data3 = Data.TYPE.instantiate().set(Data.NAME, value3);

        ExportBySize exportBySize = new ExportBySize().withSeparator(',');
        StringWriter writer = new StringWriter();
        exportBySize.exportHeader(Data.TYPE, writer);
        exportBySize.export(Stream.of(data1, data2, data3), writer);
        String expected =
                "name,count,value,dateAsInt,date\n" +
                        "some \\ndata,,,,\n" +
                        "\"some \\n \"\"other ,data\",,,,\n" +
                        "\"some  ,data\",,,,\n";
        System.out.println(expected);
        System.out.println(writer.toString());
        Assert.assertEquals(expected, writer.toString());

        ImportFile importFile = new ImportFile().withSeparator(',');

        HashSet<String> data = new HashSet<>();
        importFile.importContent(new StringReader(writer.toString()), new Consumer<Glob>() {
            public void accept(Glob glob) {
                data.add(glob.get(Data.NAME));
            }
        }, Data.TYPE);
        Assert.assertEquals(3, data.size());
        Assert.assertTrue(data.contains(value1));
        Assert.assertTrue(data.contains(value2));
        Assert.assertTrue(data.contains(value3));
    }

    @Test
    public void testWithExcludedField() {
        String value1 = "some \ndata";
        Glob data1 = Data.TYPE.instantiate().set(Data.NAME, value1).set(Data.VALUE, 3);

        String value2 = "some \n \"other ,data";
        Glob data2 = Data.TYPE.instantiate().set(Data.NAME, value2).set(Data.VALUE, 2);

        String value3 = "some  ,data";
        Glob data3 = Data.TYPE.instantiate().set(Data.NAME, value3).set(Data.VALUE, 1);

        ExportBySize exportBySize = new ExportBySize().withSeparator(',')
                .excludeField(Data.VALUE);
        StringWriter writer = new StringWriter();
        exportBySize.exportHeader(Data.TYPE, writer);
        exportBySize.export(Stream.of(data1, data2, data3), writer);
        String expected =
                "name,count,dateAsInt,date\n" +
                        "some \\ndata,,,\n" +
                        "\"some \\n \"\"other ,data\",,,\n" +
                        "\"some  ,data\",,,\n";
        System.out.println(expected);
        System.out.println(writer.toString());
        Assert.assertEquals(expected, writer.toString());
    }


    public static class DataWithArray {
        public static GlobType TYPE;

        public static StringArrayField names;

        static {
            GlobTypeLoaderFactory.create(DataWithArray.class).load();
        }
    }

    @Test
    public void withStringArray() {

        Glob data1 = DataWithArray.TYPE.instantiate()
                .set(DataWithArray.names, new String[]{"A", "B", "C"});

        ExportBySize exportBySize = new ExportBySize().withSeparator(';')
                .withArraySeparator(',');
        StringWriter writer = new StringWriter();
        exportBySize.exportHeader(DataWithArray.TYPE, writer);
        exportBySize.export(Stream.of(data1), writer);
        String expected =
                "names\n" +
                        "A,B,C\n";
        Assert.assertEquals(expected, writer.toString());
    }

    @Test
    public void filterByName() {
        MutableGlob data = Data.TYPE.instantiate().set(Data.NAME, "some data")
                .set(Data.COUNT, 300)
                .set(Data.VALUE, 3235.14153)
                .set(Data.DATE_AS_INT, (int) LocalDate.of(2018, 01, 02).toEpochDay())
                .set(Data.DATE, LocalDate.of(2019, 01, 02));

        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize
                    .withSeparator('|')
                    .filterBy("v1");
            exportBySize.export(Stream.of(data), writer);
            assertEquals("some data\n", writer.getBuffer().toString());
        }
        {
            StringWriter writer = new StringWriter();
            ExportBySize exportBySize = new ExportBySize();
            exportBySize.withSeparator('|')
                    .named("v1")
                    .filterBy("v1", "v2");
            exportBySize.export(Stream.of(data), writer);
            assertEquals("some data|300\n", writer.getBuffer().toString());
        }

    }

    public static class Data {
        public static GlobType TYPE;

        @ExportColumnSize_(10)
        @NamedExport_("v1")
        public static StringField NAME;

        @ExportColumnSize_(6)
        @NamedExport_({"v2"})
        public static IntegerField COUNT;

        @ExportColumnSize_(12)
        public static DoubleField VALUE;

        @IsDate_
        @ExportDateFormat_("YYYY/MM/DD")
        @ExportColumnSize_(10)
        public static IntegerField DATE_AS_INT;

        @ExportDateFormat_("YYYY/MM/DD")
        @ExportColumnSize_(10)
        public static DateField DATE;


        static {
            GlobTypeLoaderFactory.createAndLoad(Data.class, true);
        }
    }
}
