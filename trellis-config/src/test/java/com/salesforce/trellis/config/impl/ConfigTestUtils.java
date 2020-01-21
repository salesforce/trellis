package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.rules.Coordinates;
import com.salesforce.trellis.rules.DependencyScope;
import com.salesforce.trellis.rules.OutboundDependency;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Serializes RulesImpls instances for gold file comparisons
 *
 * @author pcal
 * @since 0.0.3
 */
public class ConfigTestUtils {

    // ===================================================================
    // Constants

    private static final Path ORIGINAL_ROOT = Paths.get("src/test/resources");
    private static final Path COPY_ROOT = Paths.get("target/generated-test-resources");

    // ===================================================================
    // Factory & constructor

    /**
     * Given a file under src/test/resources, copies it into the equivalent location under
     * target/generated-test-resources and returns a PathFileAdapter for the copy.
     */
    static PathFileAdapter createTestFile(Path relativePath) throws IOException {
        if (relativePath.isAbsolute()) throw new IllegalArgumentException("Path must be relative: " + relativePath);
        final Path originalPath = relativePath;
        if (!relativePath.startsWith(ORIGINAL_ROOT)) {
            throw new IllegalArgumentException("path must be under " + ORIGINAL_ROOT);
        }
        final Path resourceRelativePath = ORIGINAL_ROOT.relativize(relativePath);
        final Path copyPath = COPY_ROOT.resolve(resourceRelativePath);
        copyPath.getParent().toFile().mkdirs();
        copyPath.toFile().delete();
        if (originalPath.toFile().exists()) {
            Files.copy(originalPath, copyPath);
        }
        return new PathFileAdapter(copyPath);
    }

    /**
     * Wrap a java.lang.String as a SourceLocatableString.
     */
    public static SourceLocatableString string(String string) {
        return SourceLocatableString.of(string);
    }

    /**
     * Wrap a single java.lang.String in a single-item list of SourceLocatableStrings.
     */
    public static List<SourceLocatableString> stringList(String string) {
        return Collections.singletonList(string(string));
    }

    /**
     * Create an OutboundDependency.
     */
    public static OutboundDependency dep(final Coordinates toModule,
                                         final DependencyScope scope,
                                         final boolean isDirect,
                                         final boolean isOptional) {
        return OutboundDependency.create(toModule, scope, isDirect, isOptional);
    }

    /**
     * Serialize a YamlModel into a string.
     */
    public static String model2string(final YamlModel model) throws IOException {
        final StringWriter sw = new StringWriter();
        final FileAdapter fa = FileAdapter.forPath(Paths.get("mock"));
        new YamlParser(fa).writeValue(sw, model);
        return sw.toString();
    }



}
