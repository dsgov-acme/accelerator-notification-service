package io.nuvalence.platform.notification.service.model;

import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Locale;

/**
 * Base class for all filters.
 */
@SuperBuilder
public abstract class BaseFilter {
    private static final String DEFAULT_SORT_BY = "lastUpdatedTimestamp";
    private static final int DEFAULT_SIZE = 50;
    private static final int DEFAULT_PAGE = 0;

    protected String sortBy;
    protected String sortOrder;
    protected Integer pageNumber;
    protected Integer pageSize;

    /**
     * Returns a PageRequest object based on the filter's page, size, sort by, and sort order.
     *
     * @return PageRequest object
     */
    public PageRequest getPageRequest() {
        String resolvedSortBy = sortBy == null ? DEFAULT_SORT_BY : sortBy;

        return PageRequest.of(
                pageNumber == null ? DEFAULT_PAGE : pageNumber,
                pageSize == null ? DEFAULT_SIZE : pageSize,
                resolveSort(resolvedSortBy));
    }

    private Sort resolveSort(String sortBy) {
        Sort sort = Sort.unsorted();
        switch (sortBy.toLowerCase(Locale.ENGLISH)) {
            case "key":
            case "name":
            case "version":
            case "createdtimestamp":
            case "lastupdatedtimestamp":
            case "status":
                if ("asc".equalsIgnoreCase(sortOrder)) {
                    sort = Sort.by(Sort.Direction.ASC, sortBy);
                } else {
                    sort = Sort.by(Sort.Direction.DESC, sortBy);
                }
                break;
            default:
                break;
        }
        return sort;
    }
}
