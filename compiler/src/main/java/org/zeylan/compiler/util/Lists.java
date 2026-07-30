package org.zeylan.compiler.util;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

public class Lists {

    public static <T> @Nullable T head(@Nullable List<T> list) {
        return list == null || list.isEmpty() ? null : list.getFirst();
    }

    public static <T> List<T> tail(@Nullable List<T> list) {
        return list == null || list.size() < 2 ? List.of() : list.subList(1, list.size());
    }

    public static <T> Optional<T> findFirst(@Nullable List<T> list) {
        return list == null || list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    public static <T> Optional<T> findFirst(@Nullable List<T> list, Predicate<T> predicate) {
        return list == null || list.isEmpty() ? Optional.empty() : list.stream().filter(predicate).findFirst();
    }

}
