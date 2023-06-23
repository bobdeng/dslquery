package cn.beagile.dslquery;

import java.util.Objects;

class SortField {
    private String field;
    private String direction;

    public SortField(String sort) {
        String[] tokens = sort.split("\\s+");
        this.field = tokens[0];
        Validators.validateField(field);
        if (tokens.length == 2) {
            this.direction = tokens[1];
            validateDirection();
        }
    }

    private void validateDirection() {
        if (!this.direction.equalsIgnoreCase("asc") && !this.direction.equalsIgnoreCase("desc")) {
            throw new RuntimeException("invalid direction:" + this.direction);
        }
    }

    public String toSQL(SQLQuery sqlQuery) {
        return sqlQuery.aliasOf(field) + (Objects.isNull(direction) ? "" : " " + direction);
    }
}
