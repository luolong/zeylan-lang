package org.bourbon.compiler;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.io.Resource;
import org.bourbon.compiler.Source.Content;

@NullMarked
public class ScannerTestContextProvider implements TestTemplateInvocationContextProvider {

    public record ScannerTestCase(String input, List<Token> expectedTokens, List<Diagnostic> diagnostics) {}

    @Override
    public boolean supportsTestTemplate(ExtensionContext unused) {
        return true;
    }

    @Override
    public Stream<? extends TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return TestCaseDiscovery.of(context).streamResources("/scanner", this::isValidTestCase)
                .map(this::scannerTestContext);
    }

    private TestTemplateInvocationContext scannerTestContext(Resource resource) {
        try (var in = resource.getInputStream()) {
            var source = Source.named(resource.getName()).of(Content.read(in));
            var parser = new ScannerTestCaseParser(source);

            var testCase = parser.parseTestCase();
            var displayName = parser.getDisplayName();

            return scannerTestContext(displayName, testCase);
        }
        catch (IOException e) {
            throw new TestInstantiationException("Failed to load scanner test case", e);
        }
    }

    private boolean isValidTestCase(Resource resource) {
        return resource.getName().endsWith(".txt");
    }

    private TestTemplateInvocationContext scannerTestContext(String displayName, ScannerTestCase testCase) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return "[" + invocationIndex + "] " + displayName;
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext param, ExtensionContext ext) {
                        return param.getParameter().getType().equals(ScannerTestCase.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext ctx, ExtensionContext ext) {
                        return testCase;
                    }
                });
            }
        };
    }
}
