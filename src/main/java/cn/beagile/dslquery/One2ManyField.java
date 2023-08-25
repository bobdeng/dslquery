package cn.beagile.dslquery;

import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.lang.reflect.Field;
import java.util.Arrays;

import static cn.beagile.dslquery.WhereBuilder.where;

public class One2ManyField {
    private final Field field;

    public One2ManyField(Field field) {

        this.field = field;
    }

    public void fetch(Object master, QueryExecutor queryExecutor) {
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        DSLQuery<Object> dslQuery = new DSLQuery<Object>(queryExecutor, oneToMany.targetEntity());
        String masterFieldValue = Arrays.stream(master.getClass().getDeclaredFields()).filter(f -> f.getName().equals(joinColumn.name()))
                .map(f -> new ReflectField(master, f).get())
                .map(Object::toString)
                .findFirst().orElseThrow(() -> new RuntimeException("can not find master field value"));
        dslQuery.where(where().and().equals(joinColumn.referencedColumnName(), masterFieldValue).build());
        SQLQuery sqlQuery = new SQLBuilder<>(dslQuery)
                .build();
        ReflectField reflectField = new ReflectField(master, field);
        reflectField.set(queryExecutor.list(new DefaultResultSetReader<>(oneToMany.targetEntity(), dslQuery), sqlQuery));
    }
}
