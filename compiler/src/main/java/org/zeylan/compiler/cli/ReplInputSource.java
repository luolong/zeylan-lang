package org.zeylan.compiler.cli;

import org.zeylan.compiler.Source;

public class ReplInputSource extends Source.AnonymousSource implements Appendable {
    private final StringBuilder content = new StringBuilder();

    @Override
    protected CharSequence content() {
        return content;
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
