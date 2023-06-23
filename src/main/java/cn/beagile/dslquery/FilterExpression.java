package cn.beagile.dslquery;

interface FilterExpression {
    String toSQL(SQLQuery sqlQuery);
}
