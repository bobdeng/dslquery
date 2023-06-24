package cn.beagile.dslquery;

interface FilterExpression {
    String toSQL(SQLBuilder sqlQuery);
}
