package cn.beagile.dslquery;
@View("view_query")
public class QueryResultBean {
    @Column("name")
    private String name;

    public QueryResultBean() {
    }

    public String getName() {
        return name;
    }

    public QueryResultBean(String name) {
        this.name = name;
    }
}
