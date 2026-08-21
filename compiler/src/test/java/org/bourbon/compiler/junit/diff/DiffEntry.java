package org.bourbon.compiler.junit.diff;

import java.util.List;

public sealed interface DiffEntry<T> {

    record Unchanged<T>(
        int expectedIndex,
        int actualIndex,
        T item
    ) implements DiffEntry<T> {}

    record Modified<T>(
        int expectedIndex,
        int actualIndex,
        T expected,
        T actual,
        double similarity,
        List<FieldChange> fieldChanges
    ) implements DiffEntry<T> {}

    record Added<T>(
        int actualIndex,
        T item
    ) implements DiffEntry<T> {}

    record Deleted<T>(
        int expectedIndex,
        T item
    ) implements DiffEntry<T> {}
}