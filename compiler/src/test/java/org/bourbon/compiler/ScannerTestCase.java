package org.bourbon.compiler;

import java.util.List;

public record ScannerTestCase(Source input, List<Token> expectedTokens, List<Diagnostic> expectedDiagnostics) {}
