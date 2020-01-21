package com.salesforce.trellis.maven.components;

import com.salesforce.trellis.config.Config;
import com.salesforce.trellis.config.ConfigException;
import com.salesforce.trellis.config.FileAdapter;
import com.salesforce.trellis.config.ParserListener;
import com.salesforce.trellis.config.YamlConfigBuilder;
import com.salesforce.trellis.rules.builder.RuleSetBuilder;
import com.salesforce.trellis.whitelist.builder.WhitelisterBuilder;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulate the trellis configuration that applies in a given MavenSession.
 *
 * @author s.srinivasan
 * @since 0.0.1
 */
class ReactorDependencyRulesConfig {

    private final Logger logger = LoggerFactory.getLogger(ReactorDependencyRulesConfig.class);
    private final MavenSession mavenSession;
    private Properties pluginPropertiesOrNull;

    // Relative file path(s) to dependency enforcer rules configuration file from the reactor root.
    // Or any absolute file path(s).
    // Only file name could be pattern matched.
    private String[] dependencyRulesConfigFiles;

    /**
     * Construct an instance for the given session.
     */
    ReactorDependencyRulesConfig(final MavenSession mavenSession) {
        this.mavenSession = requireNonNull(mavenSession);
    }

    void setDependencyRulesConfigFiles(final String[] dependencyRulesConfigFiles) {
        this.dependencyRulesConfigFiles = dependencyRulesConfigFiles;
    }

    void setPluginProperties(final Properties pluginProperties) {
        this.pluginPropertiesOrNull = requireNonNull(pluginProperties);
    }

    /**
     * Apply the configuration to the given rules builder.
     */
    void applyTo(RuleSetBuilder reactorRulesBuilder) throws IOException, ConfigException {
        getReactorRulesConfig().applyTo(reactorRulesBuilder, getConfigYamlParserListener());
    }

    /**
     * Apply the configuration to the given whitelister builder.
     */
    void applyTo(WhitelisterBuilder whitelisterBuilder) throws IOException, ConfigException {
        getReactorRulesConfig().applyTo(whitelisterBuilder, getConfigYamlParserListener());
    }

    /**
     * Get reactor scoped rules configuration.
     *
     * @return the reactor scoped rules configuration instance, ready to be used rules or whitelist builders.
     * @throws FileNotFoundException
     *             If the configuration root does not exist.
     * @throws ConfigException
     *             If the configuration builder runs into errors.
     */
    private Config getReactorRulesConfig() throws IOException, ConfigException {
        final YamlConfigBuilder configBuilder = YamlConfigBuilder.create();
        configBuilder.mavenHelper(new MavenHelperImpl(
                this.mavenSession.getSystemProperties(),
                this.mavenSession.getUserProperties(),
                this.pluginPropertiesOrNull));

        for (String configFile : dependencyRulesConfigFiles) {
          Path dependencyRulesConfigFile = getReactorRoot().resolve(configFile);

          // Supports only the file name containing a pattern match - ex: *.yaml
          if (dependencyRulesConfigFile.getFileName().toString().contains("*")) {
            // Search the rules configuration parent directory for the matching pattern
            // and add them to the configBuilder.

             Path configDirectoryPath = dependencyRulesConfigFile;
             while(configDirectoryPath.getFileName().toString().contains("*"))
                 configDirectoryPath = configDirectoryPath.getParent();

             Path reactorRoot = getReactorRoot();
             PathMatcher matcherRules = FileSystems.getDefault().getPathMatcher("glob:" + configFile);

             Files.walkFileTree(configDirectoryPath, new SimpleFileVisitor<Path>() {
                 @Override
                 public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                         throws IOException
                 {
                     if (matcherRules.matches(reactorRoot.relativize(file))) {
                         configBuilder.addFile(FileAdapter.forPath(file));
                     }
                     return FileVisitResult.CONTINUE;
                 }
             });

          } else {
            logger.debug("Parsing reactor scoped maven dependency enforcer configuration at "
                + dependencyRulesConfigFile);
            configBuilder.addFile(FileAdapter.forPath(dependencyRulesConfigFile));
          }
        }

        configBuilder.logger(this.logger);
        // create an instance of the Config which could later be used to create RuleSet instances
        return configBuilder.build();
    }

    /**
     * Get yaml parser listener.
     *
     * @return an instance of yaml parser listener with proper logger implementation.
     */
    private ParserListener getConfigYamlParserListener() {
        return event -> {
            if (event.getType() == ParserListener.ParserEventType.ERROR) {
                logger.error(event.getMessage(), logger.isDebugEnabled() ? event.getCause() : null);
            } else if (event.getType() == ParserListener.ParserEventType.WARNING) {
                logger.warn(event.getMessage());
            } else {
                logger.info(event.getMessage());
            }
        };
    }

    /**
     * Get Maven reactor root directory based on MultiModuleProjectDirectory from a maven session.
     * Uses debug loggers to provide more details into the state of the maven session.
     *
     * @return the Maven reactor root directory or null.
     */

    private Path getReactorRoot() {
        logger.debug("Retrieving MultiModuleProjectDirectory from maven session");
        logger.debug("is Maven session null? - " + (mavenSession == null));
        logger.debug("is Maven request null? - " + (mavenSession == null || mavenSession.getRequest() == null));
        if (mavenSession != null && mavenSession.getRequest() != null
                        && mavenSession.getRequest().getMultiModuleProjectDirectory() != null) {
            logger.debug("MultiModuleProjectDirectory is "
                            + mavenSession.getRequest().getMultiModuleProjectDirectory());
            return mavenSession.getRequest().getMultiModuleProjectDirectory().toPath();
        } else {
            logger.debug("Unable to determine the maven MultiModuleProjectDirectory, using null.");
            return null;
        }
    }
}
