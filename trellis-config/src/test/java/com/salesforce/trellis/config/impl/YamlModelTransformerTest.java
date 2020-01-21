package com.salesforce.trellis.config.impl;

import com.salesforce.trellis.common.GoldFileValidator;
import com.salesforce.trellis.config.FileAdapter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.salesforce.trellis.config.impl.ConfigTestUtils.model2string;

/**
 * @author pcal
 * @since 0.0.1
 */
public class YamlModelTransformerTest {

    private static final Path RESOURCES = Paths.get("src/test/resources/com/salesforce/trellis/config/impl");

    @Test
    public void testCanonicalize() throws Exception {
        final Path testResourcesDir = RESOURCES.resolve("YamlModelTransformerTest-testCanonicalize");
        final Path sourceFile = testResourcesDir.resolve("testCanonicalize.yaml");
        final Path goldFile = testResourcesDir.resolve("testCanonicalize.yaml.expected");
        final YamlModel model = parse(sourceFile);
        new YamlModelTransformer(model).canonicalize();
        new GoldFileValidator(goldFile).validate(model2string(model));
    }

    @Test
    public void testConsolidate() throws Exception {
        final Path testResourcesDir = RESOURCES.resolve("YamlModelTransformerTest-testConsolidate");
        final Path sourceFile = testResourcesDir.resolve("testConsolidate.yaml");
        final Path goldFile = testResourcesDir.resolve("testConsolidate.yaml.expected");
        final YamlModel model = parse(sourceFile);
        new YamlModelTransformer(model).consolidate().canonicalize();
        new GoldFileValidator(goldFile).validate(model2string(model));
    }

    private YamlModel parse(final Path path) throws IOException {
        final FileAdapter fa = FileAdapter.forPath(path);
        try (final Reader fr = fa.getReader()) {
            return new YamlParser(fa).readValue(fr, YamlModel.class);
        }
    }
}
