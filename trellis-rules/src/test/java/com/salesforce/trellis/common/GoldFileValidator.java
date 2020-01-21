package com.salesforce.trellis.common;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * @author pcal
 * @since 0.0.1
 */
public class GoldFileValidator {

    public static final String UPDATE_SYSTEM_PROPERTY = "updateGoldFiles";

    private final Path goldFile;
    private final Charset charset;
    private final Logger logger;

    public GoldFileValidator(Path goldFile) {
        this(goldFile, Charset.defaultCharset(), LoggerFactory.getLogger(GoldFileValidator.class));
    }

    public GoldFileValidator(Path goldFile, Charset charset, Logger logger) {
        this.goldFile = requireNonNull(goldFile);
        this.charset = requireNonNull(charset);
        this.logger = requireNonNull(logger);
    }

    public void validate(String actualString) throws IOException {
        requireNonNull(actualString);
        requireNonNull(goldFile);
        if (System.getProperty(UPDATE_SYSTEM_PROPERTY) != null) {
            this.logger.debug("Updating gold file " + goldFile);
            FileUtils.writeStringToFile(goldFile.toFile(), actualString, charset);
        } else {
            final String goldString = FileUtils.readFileToString(goldFile.toFile(), charset);
            Assertions.assertEquals(goldString.trim(), actualString.trim());
        }
    }

    public void validate(Path actualFile) throws IOException {
        requireNonNull(actualFile);
        if (actualFile.equals(goldFile)) {
            throw new IllegalArgumentException("must validate against a different file " + actualFile);
        }

        if (System.getProperty(UPDATE_SYSTEM_PROPERTY) != null) {
            final String actualString = FileUtils.readFileToString(actualFile.toFile(), charset);
            this.logger.debug("Updating gold file " + goldFile);
            FileUtils.writeStringToFile(goldFile.toFile(), actualString, charset);
        } else {
            final String goldString = FileUtils.readFileToString(goldFile.toFile(), charset);
            final String actualString = FileUtils.readFileToString(actualFile.toFile(), charset);
            Assertions.assertEquals(goldString.trim(), actualString.trim());
        }
    }
}
