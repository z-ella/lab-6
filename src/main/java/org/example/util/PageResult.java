package org.example.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PageResult<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalItems;

    public PageResult(List<T> items, int page, int size, long totalItems) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public long getTotalPages() {
        return totalItems == 0 ? 0 : 1 + (totalItems - 1) / size;
    }
}
