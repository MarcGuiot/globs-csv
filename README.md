This library is used to read csv using apache.commons-csv and excel using apache.poi. It also read text format with
prefix and with fix lenght field like :

```
                        TYPE_Ava1va2
                        TYPE_Bvb11vb12
                        TYPE_Bvb21vb22
                        TYPE_A a1 a2
                        TYPE_Bab11ab12
                        TYPE_Bab21ab22
                        TYPE_A a3 a2
                        TYPE_A a4 a2
```

To read it the globType look like :

```
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
...
```

And the reader code

```
        ImportFile importFile = new ImportFile();
        ImportFile.Importer multi = importFile.createMulti(new StringReader(data), Root.TYPE);
        List<Glob> got = new ArrayList<>();
        multi.consume(new Consumer<Glob>() {
            public void accept(Glob glob) {
                got.add(glob);
            }
        });
        Assert.assertEquals(4, got.size());
```

