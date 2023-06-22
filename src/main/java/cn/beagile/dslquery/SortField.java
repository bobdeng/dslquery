package cn.beagile.dslquery;

import java.util.Objects;

public class SortField {
    private String field;
    private String direction;

    public SortField(String sort) {
        String[] tokens = sort.split("\\s+");
        this.field = tokens[0];
        if (tokens.length == 2) {
            this.direction = tokens[1];
        }
    }

    public String toSQL(SQLQuery sqlQuery) {
        return sqlQuery.aliasOf(field) + (Objects.isNull(direction) ? "" : " " + direction);
    }
}
