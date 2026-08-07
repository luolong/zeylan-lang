package org.zeylan.compiler;

import java.net.URISyntaxException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.platform.commons.io.Resource;
import org.junit.platform.commons.io.ResourceFilter;
import org.junit.platform.commons.util.ReflectionUtils;

@NullMarked
public class TestCaseDiscovery {
    private final ExtensionContext context;

    private TestCaseDiscovery(ExtensionContext context) {
        this.context = context;
    }

    public static TestCaseDiscovery of(ExtensionContext context) {
        return new TestCaseDiscovery(context);
    }

    public Stream<Resource> streamResources(String rootPath, Predicate<Resource> resourceFilter) {
        try {
            var rootPathResource = Objects.requireNonNull(context.getRequiredTestClass().getResource(rootPath), rootPath);            rootPathResource.toURI();
            return ReflectionUtils.streamAllResourcesInClasspathRoot(rootPathResource.toURI(), ResourceFilter.of(resourceFilter));
        } catch (NullPointerException npe) {
            throw new TestInstantiationException("could not find test resources folder: " + rootPath, npe);
        } catch (URISyntaxException e) {
            throw new TestInstantiationException("Failed to detect test resources!", e);
        }
    }
}
