package org.bourbon.compiler;

import java.util.ArrayList;
import java.util.List;

import org.bourbon.compiler.effects.Effects;
import org.bourbon.compiler.junit.CompilerAssertions;
import org.bourbon.compiler.junit.diff.DiffPrinter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@NullMarked
@DisplayName("Scanner test")
class ScannerTest {

    @TestTemplate
    @DisplayName("Scanner recognizes sequence of tokens")
    @ExtendWith(ScannerTestContextProvider.class)
    public void scanTokens(TestInfo testInfo, ScannerTestCase testCase) {
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

            new DiffPrinter(testInfo.getDisplayName()).verbose()
                    .withTokenDiff(expectedTokens, actualTokens)
                    .withDiagnosticDiff(expectedDiagnostics, actualDiagnostics)
                    .printDiff();

            Assertions.assertAll("Compiler Pipeline Verification",
                    CompilerAssertions.tokenSequenceMatch(expectedTokens, actualTokens),
                    CompilerAssertions.diagnosticSequenceMatch(expectedDiagnostics, actualDiagnostics)
            );
        }
    }
}
