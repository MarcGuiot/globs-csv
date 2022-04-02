package org.globsframework.export;

import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeBuilder;
import org.globsframework.metamodel.fields.GlobArrayField;
import org.globsframework.metamodel.fields.GlobField;
import org.globsframework.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ComplexImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexImporter.class);
    private final GlobType csvType;
    private final GlobType target;

    public ComplexImporter(GlobType sourceType, GlobType target) {
//        GlobTypeBuilder builder = new DefaultGlobTypeBuilder("csv");
//        flat(builder, target);
        csvType = sourceType;
        this.target = target;
    }


    static class ConsumerWithCurrent implements Consumer<Glob>{
        final Consumer<Glob> consumer;
        private final State build;
        private Glob current = null;
        ConsumerWithCurrent(Consumer<Glob> consumer, State build) {
            this.consumer = consumer;
            this.build = build;
        }

        public void accept(Glob glob) {
            Glob newGlob = build.onNewLine(glob);
            if (newGlob != null) {
                if (current != null) {
                    consumer.accept(current);
                }
                current = newGlob;
            }
        }
        void end(){
            if (current != null) {
                consumer.accept(current);
                current = null;
            }
        }
    }

    ConsumerWithCurrent create(Consumer<Glob> consumer) {
        State build = CompositeState.build(target, csvType);
        return new ConsumerWithCurrent(consumer, build);
    }

    public static void flat(GlobTypeBuilder builder, GlobType type) {
        for (Field field : type.getFields()) {
            if (field.getDataType().isPrimive()) {
                builder.declare(field.getName(), field.getDataType(), field.streamAnnotations().collect(Collectors.toList()));
            } else if (field instanceof GlobArrayField) {
                flat(builder, ((GlobArrayField) field).getTargetType());
            } else if (field instanceof GlobField) {
                throw new RuntimeException("Not implemented");
                //flat(builder, ((GlobField) field).getTargetType());
            }
        }
    }

    interface State {
        Glob onNewLine(Glob line);

        void reset();
    }

    record Attr(GlobArrayField array, State state) {
    }

    record LineToTargetField(Field from, Field to) {
    }

    static class FieldMapper {
        List<LineToTargetField> fields = new ArrayList<>();

        void add(Field fromField, Field toField) {
            fields.add(new LineToTargetField(fromField, toField));
        }

        boolean isSame(Glob current, Glob line) {
            for (LineToTargetField field : fields) {
                if (!field.to.valueEqual(current.getValue(field.to), line.getValue(field.from))) {
                    return false;
                }
            }
            return true;
        }

        void copy(MutableGlob to, Glob from) {
            for (LineToTargetField field : fields) {
                to.setValue(field.to, from.getValue(field.from));
            }
        }

    }

    static class CompositeState implements State {
        FieldMapper fieldMapper;
        GlobType type;
        List<Attr> attrs;
        MutableGlob current;

        public CompositeState(FieldMapper fieldMapper, GlobType type, List<Attr> attrs) {
            this.fieldMapper = fieldMapper;
            this.type = type;
            this.attrs = attrs;
        }

        public static State build(GlobType to, GlobType from) {
            List<Attr> attrs = new ArrayList<>();
            FieldMapper fieldMapper = new FieldMapper();
            for (Field toField : to.getFields()) {
                if (toField.getDataType().isPrimive()) {
                    Field fromField = from.findField(toField.getName());
                    if (fromField == null) {
                        LOGGER.info("field " + toField.getName() +" not found in " + to.getName());
                    }
                    else {
                        fieldMapper.add(fromField, toField);
                    }
                } else if (toField instanceof GlobArrayField) {
                    attrs.add(new Attr((GlobArrayField) toField, build(((GlobArrayField) toField).getTargetType(), from)));
                } else {
                    throw new RuntimeException("Not managed");
                }
            }
            return new CompositeState(fieldMapper, to, attrs);
        }

        public Glob onNewLine(Glob line) {
            boolean isNew = current == null || !fieldMapper.isSame(current, line);
            if (isNew) {
                current = type.instantiate();
                for (Attr attr : attrs) {
                    attr.state.reset();
                }
                fieldMapper.copy(current, line);
            }
            for (Attr attr : attrs) {
                Glob glob = attr.state.onNewLine(line);
                if (glob != null) {
                    Glob[] d = current.getOrEmpty(attr.array);
                    d = Arrays.copyOf(d, d.length + 1);
                    d[d.length - 1] = glob;
                    current.set(attr.array, d);
                }
            }

            return isNew ? current : null;
        }

        public void reset() {
            for (Attr attr : attrs) {
                attr.state.reset();
            }
            current = null;
        }
    }

}
