package org.bourbon.compiler;

import java.util.List;

public record ScannerTestCase(String input, List<Token> expectedTokens, List<Diagnostic> expectedDiagnostics) {}
