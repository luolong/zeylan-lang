package org.zeylan.compiler;

public final class StringSource implements Source.AnonymousSource {
    private final String content;

    public static StringSource of(String content) {
        return new StringSource(content);
    }

    private StringSource(String content) {
        this.content = content;
    }


    @Override
    public CharSequence content() {
        return content;
    }

    @Override public String toString() {
        return content;
    }

}
