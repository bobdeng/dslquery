package cn.beagile.dslquery;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@View("view_query")
public class QueryResultBean {
    @Column("name")
    private String name;
    @Column("age")
    private Integer age;
    @Column("json")
    private JsonField[] json;
    @Embedded
    private EmbeddingField embeddingField;

    public EmbeddingField getEmbeddingField() {
        return embeddingField;
    }

    public QueryResultBean() {
    }

    public QueryResultBean(JsonField[] json) {
        this.json = json;
    }

    public JsonField[] getJson() {
        return json;
    }

    public String getName() {
        return name;
    }

    public QueryResultBean(String name) {
        this.name = name;
    }

    public static class JsonField {
        private String name;
        private String code;

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }

    @Embeddable
    public static class EmbeddingField {
        @Column("another_name")
        private String name;

        public String getName() {
            return name;
        }
    }
}
