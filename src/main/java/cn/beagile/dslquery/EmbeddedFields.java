package cn.beagile.dslquery;

import javax.persistence.AttributeOverrides;
import java.util.ArrayList;
import java.util.List;

public class EmbeddedFields {
    public EmbeddedFields(Class clz, AttributeOverrides attributeOverrides) {

    }

    public List<ColumnField> fields() {
        return new ArrayList<>();
    }
}
