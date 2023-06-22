package cn.beagile.dslquery;

public interface CamelToSnake {
    static String camelToSnake(String src) {
        String ret = src.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z])([A-Z])", "$1_$2");
        return ret.toLowerCase();
    }
}
