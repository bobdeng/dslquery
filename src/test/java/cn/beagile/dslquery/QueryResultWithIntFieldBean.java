package cn.beagile.dslquery;
@View("view_query")
public class QueryResultWithIntFieldBean {
    @Column("age")
    private Integer age;

    public Integer getAge() {
        return age;
    }
}
