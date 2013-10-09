package org.n3r.diamond.server.domain;

import java.util.ArrayList;
import java.util.List;


public class Page<E> {
    private int totalCount;
    private int pageNo;
    private int totalPages;
    private List<E> pageItems = new ArrayList<E>();

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public List<E> getPageItems() {
        return pageItems;
    }
}