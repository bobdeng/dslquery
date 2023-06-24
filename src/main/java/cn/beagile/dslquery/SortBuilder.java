package cn.beagile.dslquery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SortBuilder {
    private List<SortField> fields = new ArrayList<>();

    public SortBuilder asc(String fieldName) {
        fields.add(new SortField(fieldName, "asc"));
        return this;
    }

    public String build() {
        return fields.stream().map(SortField::toDSL).collect(Collectors.joining(","));
    }

    public SortBuilder desc(String fieldName) {
        fields.add(new SortField(fieldName, "desc"));
        return this;
    }
}
