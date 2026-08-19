package org.bourbon.compiler;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.bourbon.compiler.effects.Effects;
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

    @Override
    public boolean supportsTestTemplate(ExtensionContext unused) {
        return true;
    }

    @Override
    public Stream<? extends TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        var testCaseFilter = System.getProperty("scanner.test.case");

        var testResources = TestCaseDiscovery.of(context)
                .streamResources("/scanner", this::isValidTestCase);

        if (testCaseFilter != null && !testCaseFilter.isBlank()) {
            testResources = testResources.filter(resource -> resource.getName().endsWith(testCaseFilter));
        }

        return testResources.map(resource -> scannerTestContext(context, resource));
    }

    private boolean isValidTestCase(Resource resource) {
        return resource.getName().endsWith(".bourbon.txt");
    }

    private TestTemplateInvocationContext scannerTestContext(ExtensionContext context, Resource resource) {
        try (var in = resource.getInputStream()) {
            var source = Source.named(resource.getName()).of(Content.read(in));
            var parser = new ScannerTestCaseParser(source);

            var testCase = Effects.handle(parser::parseTestCase).with(
                    DiagnosticReporter.Handler.class, diagnostic -> DiagnosticFormatter.format(source, diagnostic, System.err::print))
                    .get();

            return scannerTestContext(parser.getDisplayName(), testCase);
        }
        catch (IOException e) {
            throw new TestInstantiationException("Failed to load scanner test case", e);
        }
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
