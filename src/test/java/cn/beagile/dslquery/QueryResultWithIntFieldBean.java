package cn.beagile.dslquery;

import javax.persistence.Column;

@View("view_query")
public class QueryResultWithIntFieldBean {
    @Column(name = "age")
    private Integer age;

    public Integer getAge() {
        return age;
    }
}
