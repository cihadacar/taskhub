package io.github.cihadacar.taskhub.common;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public PageResponse {
        content = List.copyOf(content);
    }

    public static <T> PageResponse<T> from(List<T> all, int page, int requestedSize) {
        int size = Math.min(requestedSize, 100);
        int from = (int) Math.min((long) page * size, all.size());
        int to = Math.min(from + size, all.size());
        int totalPages = all.isEmpty() ? 0 : (all.size() + size - 1) / size;
        return new PageResponse<>(all.subList(from, to), page, size, all.size(), totalPages);
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int requestedSize, long totalElements) {
        int size = Math.min(requestedSize, 100);
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
