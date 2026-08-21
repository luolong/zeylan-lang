package org.bourbon.compiler.junit.diff;

import org.jspecify.annotations.Nullable;

public sealed interface FieldChange {

    record ModifiedField(
        String fieldName,
        @Nullable Object expectedValue,
        @Nullable Object actualValue
    ) implements FieldChange {}

    record AddedField(
        String fieldName,
        Object actualValue
    ) implements FieldChange {}

    record RemovedField(
        String fieldName,
        Object expectedValue
    ) implements FieldChange {}
}