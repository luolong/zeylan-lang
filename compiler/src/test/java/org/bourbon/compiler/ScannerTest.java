package org.bourbon.compiler;

import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bourbon.compiler.effects.Effects;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.bourbon.compiler.junit.ListAssert;

@NullMarked
@DisplayName("Scanner test")
class ScannerTest {

    @TestTemplate
    @DisplayName("Scanner recognizes sequence of tokens")
    @ExtendWith(ScannerTestContextProvider.class)
    public void scanTokens(ScannerTestCase testCase) {
        //noinspection ConstantValue
        if (testCase instanceof ScannerTestCase(Source source, List<Token> expectedTokens, List<Diagnostic> expectedDiagnostics)) {
            var actualDiagnostics = new ArrayList<Diagnostic>();
            var reporter = new DiagnosticReporter.Handler() {
                @Override
                public void report(Diagnostic diagnostic) {
                    actualDiagnostics.add(diagnostic);
                }
            };

            var actualTokens = Effects.handle(() -> Scanner.scanTokens(source))
                    .with(DiagnosticReporter.Handler.class, reporter)
                    .get();

            var assertions = new ArrayList<Executable>();
            assertions.add(() -> assertAll("Tokens", assertTokens(expectedTokens, actualTokens)));
            assertions.add(() -> assertAll("Diagnostics", assertDiagnostics(expectedDiagnostics, actualDiagnostics)));

            assertAll(assertions);
        }
    }

    private Stream<Executable> assertDiagnostics(List<Diagnostic> expectedDiagnostics, List<Diagnostic> actualDiagnostics) {
        var normalizedExpected = normalizeDiagnostics(expectedDiagnostics);
        var normalizedActual = normalizeDiagnostics(actualDiagnostics);

        return ListAssert.assertLists(normalizedExpected, normalizedActual)
                .withSemanticEquality(
                        Diagnostic::severity,
                        Diagnostic::code,
                        this::getReferencedFiles
                )
                .stream();
    }

    private Set<String> getReferencedFiles(Diagnostic d) {
        return d.labels().stream()
            .map(l -> l.span().name().name())
            .collect(Collectors.toSet());
    }

    private List<Diagnostic> normalizeDiagnostics(List<Diagnostic> diagnostics) {
        return diagnostics.stream().map(this::normalizeDiagnostic).toList();
    }

    private Diagnostic normalizeDiagnostic(Diagnostic d) {
        var sortedLabels = new ArrayList<>(d.labels());
        sortedLabels.sort((l1, l2) -> {
            SourceSpan s1 = l1.span();
            SourceSpan s2 = l2.span();
            if (s1.line() != s2.line()) {
                return Integer.compare(s1.line(), s2.line());
            }
            if (s1.column() != s2.column()) {
                return Integer.compare(s1.column(), s2.column());
            }
            if (s1.startOffset() != s2.startOffset()) {
                return Integer.compare(s1.startOffset(), s2.startOffset());
            }
            return Boolean.compare(l1.isPrimary(), l2.isPrimary());
        });
        return new Diagnostic(d.code(), d.severity(), d.message(), List.copyOf(sortedLabels), d.suggestions());
    }

    private Stream<Executable> assertTokens(List<Token> expectedTokens, List<Token> actualTokens) {
        return ListAssert.assertLists(expectedTokens, actualTokens)
                .withSemanticEquality(Token::type, Token::lexeme)
                .stream();
    }
}
