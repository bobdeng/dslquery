package cn.beagile.dslquery;

public class Paging {
    private Integer skip;
    private Integer limit;

    public Paging(Integer skip, Integer limit) {
        this.skip = skip;
        this.limit = limit;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getSkip() {
        return skip;
    }
}
