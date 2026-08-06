package org.zeylan.compiler;

import org.zeylan.compiler.Source.AnonymousSource;

public final class StringSource extends AnonymousSource {
    private final String content;

    public StringSource(String content) {
        this.content = content;
    }

    public static StringSource of(String content) {
        return new StringSource(content);
    }

    @Override protected CharSequence content() {
        return content;
    }

}
