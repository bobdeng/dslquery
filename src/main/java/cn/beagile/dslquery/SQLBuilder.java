package cn.beagile.dslquery;

public interface SQLBuilder {

    void addParamArray(String paramName, String fieldName, String value);

    void addParam(String paramName, String fieldName, String value);

    String aliasOf(String field);
}
