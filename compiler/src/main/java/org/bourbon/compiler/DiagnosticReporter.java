package org.bourbon.compiler;

import java.util.List;

import org.bourbon.compiler.effects.Effect;
import org.bourbon.compiler.effects.Effects;

@Effect
public final class DiagnosticReporter {
    private DiagnosticReporter() {
        throw new UnsupportedOperationException("Do not instantiate!");
    }

    @Effect.Handler
    public interface Handler {
        void report(Diagnostic diagnostic);
    }

    public static void report(Diagnostic diagnostic) {
        Effects.get(DiagnosticReporter.Handler.class).report(diagnostic);
    }

    public static Diagnostic error(Diagnostic.Code code, String message, List<Label> labels) {
        var error = Diagnostic.error(code, message, labels);
        report(error);
        return error;
    }

    public static Diagnostic error(Diagnostic.Code code, String message, List<Label> label, List<String> suggestions) {
        var error = Diagnostic.error(code, message, label, suggestions);
        report(error);
        return error;
    }

    public static Diagnostic warning(Diagnostic.Code code, String message, List<Label> label) {
        var warning = Diagnostic.warning(code, message, label);
        report(warning);
        return warning;
    }
}
