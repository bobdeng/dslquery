package cn.beagile.dslquery;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Sort {
    private List<SortField> fields;
    public Sort(String sort) {
        this.fields = Stream.of(sort.split(","))
                .map(SortField::new).collect(Collectors.toList());
    }

    public String toSQL(SQLQuery sqlQuery) {
        return fields.stream().map(sortField -> sortField.toSQL(sqlQuery)).collect(Collectors.joining(","));
    }
}
