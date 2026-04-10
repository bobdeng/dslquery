package cn.beagile.dslquery;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import static cn.beagile.dslquery.WhereBuilder.where;

public class One2ManyField {
    private final Field field;
    private String nullsOrder="";

    public One2ManyField(Field field) {
        this.field = field;
    }

    public void fetch(Object master, QueryExecutor queryExecutor) {
        String mappedBy = AnnotationReader.getOneToManyMappedBy(field);
        AnnotationReader.JoinColumnInfo[] joinColumns = AnnotationReader.getJoinColumns(field);

        Class<?> targetEntity = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        @SuppressWarnings("unchecked")
        DSLQuery<Object> dslQuery = new DSLQuery<>(queryExecutor, (Class<Object>) targetEntity);

        String joinColumnName = joinColumns.length > 0 ? joinColumns[0].name : mappedBy;
        String referencedColumnName = joinColumns.length > 0 ? joinColumns[0].referencedColumnName : "id";

        String masterFieldValue = Arrays.stream(master.getClass().getDeclaredFields())
                .filter(f -> f.getName().equals(joinColumnName))
                .map(f -> new ReflectField(master, f).get())
                .map(Object::toString)
                .findFirst().orElseThrow(() -> new RuntimeException("can not find master field value"));

        dslQuery.where(where().and().equals(referencedColumnName, masterFieldValue).build());
        SQLQuery sqlQuery = new DSLSQLBuilder<>(dslQuery)
                .build(nullsOrder);
        ReflectField reflectField = new ReflectField(master, field);
        reflectField.set(queryExecutor.list(new DefaultResultSetReader<>(dslQuery), sqlQuery));
    }
}
