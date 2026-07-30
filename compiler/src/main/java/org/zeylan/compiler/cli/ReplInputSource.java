package org.zeylan.compiler.cli;

import org.zeylan.compiler.Source;

public class ReplInputSource implements Source.AnonymousSource, Appendable {
    private final StringBuilder content = new StringBuilder();

    @Override
    public CharSequence content() {
        return content;
    }

    @Override
    public String toString() {
        return content.toString();
    }

    @Override
    public ReplInputSource append(CharSequence csq) {
        content.append(csq);
        return this;
    }

    @Override
    public ReplInputSource append(CharSequence csq, int start, int end) {
        content.append(csq, start, end);
        return this;
    }

    @Override
    public ReplInputSource append(char c) {
        content.append(c);
        return this;
    }

}
