# Globs CSV

Import and export [Glob](https://globsframework.org)s as CSV (through `commons-csv`), Excel (through
`apache-poi`) and fixed-width text — the flat-file formats, all driven by a `GlobType` rather than by a
per-file parser.

Three things live here:

- **`ImportFile`** — read a delimited file, an Excel sheet, or a fixed-width one, into Globs
- **`ExportBySize`** — write Globs back out, padded to the declared column sizes
- **`RealReformater`** — map one `GlobType` onto another while the data flows, driven by a Glob description

## Requirements

Java 21, `org.globsframework:globs`, `org.apache.commons:commons-csv`, `org.apache.poi:poi-ooxml` for Excel.

## Installation

```xml
<dependency>
    <groupId>org.globsframework</groupId>
    <artifactId>globs-csv</artifactId>
    <version>5.0.0</version>
</dependency>
```

## Importing a delimited file

```java
ImportFile importFile = new ImportFile();
importFile.withSeparator(',');
importFile.importContent(reader, glob -> process(glob), Type.TYPE);
```

The header line names the columns; each name is matched to a field (`FieldName` / `ReNamedExport` when the
header does not match the Java name). Without a `GlobType`, `create(reader)` builds one from the header, and
`extractHeader(inputStream, separator)` returns just that type.

The builder carries the dialect: `withSeparator`, `withQuoteChar`, `withCharSet` (or
`createReaderWithBomCheck`, which honours a BOM), `trim`, `withHeader` to supply a header the file does not
have, `withHeaderResolver` for a custom name → field mapping, `asExcel` / `createExcel` for a `.xlsx`.

## Fixed-width and multi-type files

A file can also be a sequence of *different* record types, each recognized by its line prefix and cut at
declared column sizes:

```
TYPE_Ava1va2
TYPE_Bvb11vb12
TYPE_Bvb21vb22
TYPE_A a1 a2
TYPE_Bab11ab12
TYPE_Bab21ab22
```

The root type describes the nesting, `CsvHeader` the prefix, `ExportColumnSize` the width:

```java
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
```

```java
ImportFile importFile = new ImportFile();
ImportFile.Importer multi = importFile.createMulti(new StringReader(data), Root.TYPE);
List<Glob> got = new ArrayList<>();
multi.consume(got::add);
Assert.assertEquals(4, got.size());
```

Each `TYPE_A` line opens a new root Glob and the `TYPE_B` lines that follow are collected into its array —
which is how a header/detail file becomes one Glob per record group. `withLeftPadding` /
`withRightPadding` say which side the padding is on, and `filterLineOnFixSizeOnly(match)` drops the lines
that are not records.

## Exporting

```java
ExportBySize exportBySize = new ExportBySize();
exportBySize.withSeparator('|').withLeftPadding();
exportBySize.export(Stream.of(data), writer);
// " some data|   300|  3235.14153|2018/01/02|2019/01/02\n"
```

`exportHeader(headerType, writer)` writes the header line, `exportMulti(rootType, globStream, writer)` the
multi-type shape, `excludeField` and `filterBy(names)` restrict the columns, and `named(name)` selects which
`NamedExport` set of columns to use. Formats default at the exporter level
(`setDefaultDateFormat`, `setDefaultDoubleFormat`, `setBooleanValue(trueValue, falseValue)`) and are
overridden per field by annotation.

## Annotations

| Annotation | Effect |
| --- | --- |
| `CsvHeader(name, firstLineIsHeader)` | the line prefix identifying this record type |
| `ExportColumnSize(size)` | the column width, for both reading and writing fixed-width |
| `ExportDateFormat(format, zoneId)` | the pattern used for a date/time field |
| `ExportDoubleFormat(format, decimalSeparator)` | number formatting |
| `ExportBooleanFormat(true, false)` | what a boolean is written as |
| `CsvSeparator` / `CsvValueSeparator` | the field separator, and the separator inside a multi-value field |
| `NamedExport(names...)` | the named column sets a field belongs to, for `named()`-filtered exports |
| `ReNamedExport` | per-name aliases of a column, plus a default value |
| `ImportEmptyStringHasEmptyStringFormat` | an empty cell reads as `""` rather than as unset |

They are registered in `AllCsvAnnotations`.

## Reformatting on the fly

`RealReformater(fromType, fieldMapping)` builds a new `GlobType` and a `transform(Glob)` from a *Glob*
description of the mapping (`FieldMappingType`). Each entry says where a target field comes from: another
field (`FromType`, with a default value when empty and a chain of formatters), a template over several
fields, a sum, a join, a lookup table, or an override. It is what turns "the file we receive" into "the type
we use" without a conversion class per file — and since the description is itself Globs, it can be read from
JSON rather than compiled in.

## Building

```bash
mvn -o test          # JUnit 4
```

## License

Apache License 2.0 — see <https://www.apache.org/licenses/LICENSE-2.0.txt>.

## Links

- [Globs Framework](https://globsframework.org)
- [GitHub repository](https://github.com/globsframework/globs-csv)
