package uk.gov.moj.cpp.workmanagement.proxy.api.util;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

public class PageUtil {

    private PageUtil() {
    }

    public static <T> List<T> getPaginatedList(List<T> list, int offset, int limit) {

        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        if (offset < 0 || limit < 0 || offset >= list.size()) {
            return Collections.emptyList();
        }

        if (offset + limit > list.size()) {
            return list.subList(offset, list.size());
        }

        if (offset + limit <= list.size()) {
            return list.subList(offset, offset + limit);
        }

        return list;
    }
}
