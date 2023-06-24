package cn.beagile.dslquery;

import java.util.Objects;

class SortField implements FilterExpression {
    private final String field;
    private String direction;

    public SortField(String sort) {
        String[] tokens = sort.split("\\s+");
        this.field = tokens[0];
        Validators.validateFieldName(field);
        if (tokens.length == 2) {
            this.direction = tokens[1];
            validateDirection();
        }
    }

    SortField(String field, String direction) {
        this.field = field;
        this.direction = direction;
    }

    private void validateDirection() {
        if (!this.direction.equalsIgnoreCase("asc") && !this.direction.equalsIgnoreCase("desc")) {
            throw new RuntimeException("invalid direction:" + this.direction);
        }
    }

    public String toSQL(SQLBuilder sqlQuery) {
        return sqlQuery.aliasOf(field) + (Objects.isNull(direction) ? "" : " " + direction);
    }

    @Override
    public String toDSL() {
        return this.field + " " + direction;
    }
}
