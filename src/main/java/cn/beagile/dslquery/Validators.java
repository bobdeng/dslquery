package cn.beagile.dslquery;

import java.util.regex.Pattern;

public interface Validators {
    static void validateField(String field){
        Pattern validFieldPattern = Pattern.compile("[a-zA-Z]{1}[a-zA-Z0-9_-]+$");
        if (!validFieldPattern.matcher(field).matches()) {
            throw new RuntimeException("invalid field:" + field);
        }
    }
}
