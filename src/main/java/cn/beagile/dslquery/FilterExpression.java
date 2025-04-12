package cn.beagile.dslquery;

import java.util.List;

interface FilterExpression {
    String toSQL(SQLBuild sqlQuery);
    String toDSL();

    String toSQL(List<SQLField> fields, SQLWhere sqlWhere);
}
