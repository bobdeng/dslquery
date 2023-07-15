package cn.beagile.dslquery;

import java.util.regex.Pattern;

interface Validators {
    static void validateFieldName(String field) {
        Pattern validFieldPattern = Pattern.compile("([a-zA-Z]{1}\\.?[a-zA-Z0-9_-]?)*$");
        if (!validFieldPattern.matcher(field).matches()) {
            throw new RuntimeException("invalid field:" + field);
        }
    }
}
