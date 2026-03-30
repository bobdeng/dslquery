package cn.beagile.dslquery;

import java.util.regex.Pattern;

interface Validators {
    static void validateFieldName(String field) {
        Pattern validFieldPattern = Pattern.compile("[a-zA-Z][a-zA-Z0-9_\\.-]*$");
        if (!validFieldPattern.matcher(field).matches()) {
            throw new RuntimeException("invalid field:" + field);
        }
    }

    static void validateJoinPath(String path) {
        Pattern validJoinPathPattern = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$");
        if (!validJoinPathPattern.matcher(path).matches()) {
            throw new RuntimeException("invalid join path:" + path);
        }
    }
}
