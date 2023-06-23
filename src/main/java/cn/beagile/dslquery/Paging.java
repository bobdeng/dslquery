package cn.beagile.dslquery;

public class Paging {
    private int skip;
    private int limit;

    public Paging(int skip, int limit) {
        this.skip = skip;
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    public int getSkip() {
        return skip;
    }
}
