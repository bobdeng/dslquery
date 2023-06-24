package cn.beagile.dslquery;

import java.util.List;

public class Paged<T> {
    private final List<T> result;
    private final int total;
    private final Paging paging;

    public Paged(List<T> result, int total, Paging paging) {

        this.result = result;
        this.total = total;
        this.paging = paging;
    }

    public List<T> getResult() {
        return result;
    }

    public int total() {
        return total;
    }

    public Integer limit() {
        return this.paging.getLimit();
    }

    public Integer skip() {
        return this.paging.getSkip();
    }
}
