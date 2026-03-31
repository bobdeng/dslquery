package cn.beagile.dslquery;

import jakarta.persistence.Column;

@View("view_query")
public class QueryResultWithIntFieldBean {
    @Column(name = "age")
    private Integer age;

    public Integer getAge() {
        return age;
    }
}
