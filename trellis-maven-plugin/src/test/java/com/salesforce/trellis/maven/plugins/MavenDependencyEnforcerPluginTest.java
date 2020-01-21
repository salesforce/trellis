package com.salesforce.trellis.maven.plugins;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.takari.maven.testing.TestProperties;
import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.Assert.assertEquals;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.6.0"})
public class MavenDependencyEnforcerPluginTest {

    @Rule
    public final TestResources resources =
                    new TestResources("src/test/resources/projects", "target/test/test-projects");

    private final MavenRuntime maven;

    TestProperties testProperties = new TestProperties();

    public MavenDependencyEnforcerPluginTest(MavenRuntimeBuilder mavenRuntimeBuilder) throws Exception {
        this.maven = mavenRuntimeBuilder.withCliOptions("-Dmaven.logging=legacy", "-B", "-U", "-e").build();
    }

    @Test
    public void basic() throws Exception {
        basicTestDir("basic-test", "whitelist.yaml", "");
    }

    @Test
    public void basicTestSkip() throws Exception {
        File basedir = resources.getBasedir("basic-test-skip");

        // Run the configured build plugin goal trellis:enforce.
        MavenExecutionResult mavenExecutionResult = maven.forProject(basedir).execute("install").assertErrorFreeLog();

        // Expect a validation error for module2 having a denied dependency on module1.
        mavenExecutionResult.assertLogText("[INFO] Skipping trellis goal!");

        // Run trellis:update-whitelists plugin goal to update the maven dependency rules whitelist.
        mavenExecutionResult =
                        maven.forProject(basedir).execute("trellis:update-whitelists").assertErrorFreeLog();

        // Make sure we are not skipping the update.
        mavenExecutionResult
                        .assertLogText("[INFO] Successfully validated and updated maven dependency enforcement rules.");
    }

    @Test
    public void basicTestWithAdditionalInfoOnError() throws Exception {
        File basedir = resources.getBasedir("basic-test");

        Files.copy(basedir.toPath().resolve("pom.xml.additional.info.on.error"), basedir.toPath().resolve("pom.xml"),
                   StandardCopyOption.REPLACE_EXISTING);

        // Run the configured build plugin goal trellis:enforce.
        MavenExecutionResult mavenExecutionResult = maven.forProject(basedir).execute("install");

        // Expect a validation error for module2 having a denied dependency on module1 with additional information.
        mavenExecutionResult.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module2: ");
        mavenExecutionResult.assertLogText("[ERROR] com.salesforce.trellis.tests:module1:jar:1.0.0-SNAPSHOT:compile");
        mavenExecutionResult.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult.assertLogText("[ERROR]  For more information see https://trellis/faq.md");

    }

    @Test
    public void basicTestWithMultipleConfigurationFiles() throws Exception {
        File basedir = resources.getBasedir("basic-test");

        Files.copy(basedir.toPath().resolve("pom.xml.additional.rules.config"), basedir.toPath().resolve("pom.xml"),
                   StandardCopyOption.REPLACE_EXISTING);

        // Run the configured build plugin goal trellis:enforce.
        MavenExecutionResult mavenExecutionResult = maven.forProject(basedir).execute("install");

        // Expect a validation error for module1 having a denied dependency on junit
        // from additional junit-rule.yaml file.
        mavenExecutionResult.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module1: ");
        mavenExecutionResult.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult.assertLogText("[ERROR] junit:junit:jar:4.9:test");

        // Run the configured build plugin goal trellis:enforce on module2.
        mavenExecutionResult = maven.forProject(basedir).withCliOptions("--projects", "module2").execute("install");

        // Expect a validation error for module2 having a denied dependency on module1
        // from the original dependency-rules.yaml file.
        mavenExecutionResult.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module2: ");
        mavenExecutionResult.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult.assertLogText("[ERROR] com.salesforce.trellis.tests:module1:jar:1.0.0-SNAPSHOT:compile");
    }

    @Test
    public void basicReasons() throws Exception {

        // Test 1 reason
        File basedir = resources.getBasedir("basic-test");

        // Run the configured build plugin goal trellis:enforce.
        MavenExecutionResult mavenExecutionResult = maven.forProject(basedir).execute("install");

        mavenExecutionResult.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module2: ");
        mavenExecutionResult.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult.assertLogText("[ERROR] com.salesforce.trellis.tests:module1:jar:1.0.0-SNAPSHOT:compile");
        mavenExecutionResult.assertLogText("[ERROR] com.salesforce.trellis.tests:module1:jar:1.0.0-SNAPSHOT:compile:");
        mavenExecutionResult.assertLogText("[ERROR] Implementation modules are not allowed to be used in API modules.");


        // Test multiple reasons
        basedir = resources.getBasedir("basic-reasons");

        mavenExecutionResult = maven.forProject(basedir).execute("install");

        mavenExecutionResult.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module1: ");
        mavenExecutionResult.assertLogText("[ERROR] The following dependencies are not allowed for this project:");
        mavenExecutionResult.assertLogText("[ERROR] javax.inject:javax.inject:jar:1:compile:");
        mavenExecutionResult.assertLogText("[ERROR] javax.inject is not allowed.");
        mavenExecutionResult.assertLogText("[ERROR] junit:junit:jar:4.9:test:");
        mavenExecutionResult.assertLogText("[ERROR] junit is not allowed.");

        // This dependency doesn't have a reason after, so no colon
        mavenExecutionResult.assertLogText("[ERROR] com.google.guava:guava-jdk5:jar:17.0:compile");
        mavenExecutionResult.assertNoLogText("[ERROR] com.google.guava:guava-jdk5:jar:17.0:compile:");

    }

    @Test
    public void basicDirectories() throws Exception {
        basicTestDir("basic-directories", "whitelists/whitelist.yaml", "pom.xml.basic");
    }

    @Test
    public void basicWhitelists() throws Exception {
        File basedir = resources.getBasedir("basic-directories");
        Files.copy(basedir.toPath().resolve("pom.xml.basic.whitelists"), basedir.toPath().resolve("pom.xml"),
                StandardCopyOption.REPLACE_EXISTING);

        // Run the configured build plugin goal trellis:enforce.
        MavenExecutionResult mavenExecutionResult = maven.forProject(basedir).execute("install");

        //test that it found .mvn/trellis/service/security/whiteslists/whitelist.yaml
        //through relative declaration of whitelist/whitelist.yaml
        mavenExecutionResult.assertLogText("[WARNING] The dependency com.salesforce.trellis.tests:module1"
                + ":jar:1.0.0-SNAPSHOT:compile is discouraged for this project! Whitelist 1 "
                + "Warning - Implementation modules are not allowed to be used in API modules.");

        Files.copy(basedir.toPath().resolve(".mvn/trellis/service/security/new-diffrules.yaml.basic.whitelists"),
                basedir.toPath().resolve(".mvn/trellis/service/security/new-diffrules.yaml"),
                StandardCopyOption.REPLACE_EXISTING);

        // Run the configured build plugin goal trellis:enforce.
        mavenExecutionResult = maven.forProject(basedir).execute("install");

        //test that it found .mvn/trellis/service/security/whiteslists/whitelist.yaml
        //through config root declaration of $config_root/whitelist/whitelist.yaml
        mavenExecutionResult.assertLogText("[WARNING] The dependency com.salesforce.trellis.tests:module1:"
                + "jar:1.0.0-SNAPSHOT:compile is discouraged for this project! "
                + "Whitelist 2 Warning - Implementation modules are not allowed to be used in API modules.");

    }
    
    @Test
    public void basicWhitelistMapping() throws Exception {
        File basedir = resources.getBasedir("basic-directories");
        Files.copy(basedir.toPath().resolve("pom.xml.basic"), basedir.toPath().resolve("pom.xml"),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(basedir.toPath().resolve(".mvn/trellis/general/dependency-rules.yaml.whitelist"), basedir.toPath().resolve(".mvn/trellis/general/dependency-rules.yaml"),
                StandardCopyOption.REPLACE_EXISTING);

        MavenExecutionResult mavenExecutionResult = maven.forProject(basedir).execute("install");

        // Expect a validation error for module2 having a denied dependency on module1.
        mavenExecutionResult.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module2: ");
        mavenExecutionResult.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult.assertLogText("[ERROR] com.salesforce.trellis.tests:module1:jar:1.0.0-SNAPSHOT:compile");

        
    }


    @Test
    public void basicBadConfigRoot() throws Exception {
        File basedir = resources.getBasedir("basic-directories");
        Files.copy(basedir.toPath().resolve("pom.xml.basic.whitelists"), basedir.toPath().resolve("pom.xml"),
                StandardCopyOption.REPLACE_EXISTING);

        Files.copy(basedir.toPath().resolve(".mvn/trellis/service/security/new-diffrules.yaml.basic.testWrongRoot"),
                basedir.toPath().resolve(".mvn/trellis/service/security/new-diffrules.yaml"),
                StandardCopyOption.REPLACE_EXISTING);

        // Run the configured build plugin goal trellis:enforce.
        MavenExecutionResult mavenExecutionResult = maven.forProject(basedir).execute("install");

        // This tests that it found the new-diffules.yaml but no whitelist file
        // Expect a validation error for module2 having a denied dependency on module1.
        mavenExecutionResult.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module2: ");
        mavenExecutionResult.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult.assertLogText("[ERROR] com.salesforce.trellis.tests:module1:jar:1.0.0-SNAPSHOT:compile");

        // Assert that it could not find whitelist, assertion includes $wrong_root
        mavenExecutionResult.assertLogText("[WARNING] whitelist file does not exist: "
                + basedir.getAbsolutePath() + "/.mvn/trellis/service/security/$wrong_root/whitelists/whitelist.yaml");


        Files.copy(basedir.toPath().resolve(".mvn/trellis/service/security/new-diffrules.yaml.basic.testConfigRoot"),
                basedir.toPath().resolve(".mvn/trellis/service/security/new-diffrules.yaml"),
                StandardCopyOption.REPLACE_EXISTING);

        MavenExecutionResult mavenExecutionResult2 = maven.forProject(basedir).execute("install");

        // Expect a validation error for module2 having a denied dependency on module1.
        mavenExecutionResult2.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module2: ");
        mavenExecutionResult2.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult2.assertLogText("[ERROR] com.salesforce.trellis.tests:module1:jar:1.0.0-SNAPSHOT:compile");

        // Assert that it could not find whitelist, assertion includes bad_directory
        mavenExecutionResult2.assertLogText("[WARNING] whitelist file does not exist: "
                + basedir.getAbsolutePath() + "/.mvn/trellis/bad_directory/whitelists/whitelist.yaml");

    }

    @Test
    public void basicConfigurationFilesWithGlob() throws Exception {
        File basedir = resources.getBasedir("basic-directories");
        Files.copy(basedir.toPath().resolve("pom.xml.basic.configFiles"), basedir.toPath().resolve("pom.xml"),
                StandardCopyOption.REPLACE_EXISTING);

        // Run the configured build plugin goal trellis:enforce.
        MavenExecutionResult mavenExecutionResult = maven.forProject(basedir).execute("install");

        // Expect a validation error for module1 having a denied dependency on junit
        mavenExecutionResult.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module1: ");
        mavenExecutionResult.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult.assertLogText("[ERROR] junit:junit:jar:4.9:test");

        // Run trellis:update-whitelists plugin goal to update the maven dependency rules whitelist.
        maven.forProject(basedir).execute("trellis:update-whitelists").assertErrorFreeLog();

        // Run validate lifecycle phase to execute the configured build plugin goal trellis:enforce again.
        // This time this should run without any dependency enforcer errors.
        maven.forProject(basedir).execute("validate").assertErrorFreeLog();

        // We are going to stop using dependency-rules whitelist
        Files.copy(basedir.toPath().resolve(".mvn/trellis/general/dependency-rules.yaml.no-whitelist"),
                basedir.toPath().resolve(".mvn/trellis/general/dependency-rules.yaml"),
                StandardCopyOption.REPLACE_EXISTING);


        MavenExecutionResult mavenExecutionResult2 = maven.forProject(basedir).execute("validate");

        // Expect a validation error for module2 having a denied dependency on module1.
        mavenExecutionResult2.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module2: ");
        mavenExecutionResult2.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult2.assertLogText("[ERROR] com.salesforce.trellis.tests:module1:jar:1.0.0-SNAPSHOT:compile");

    }

    private void basicTestDir(String dir, String whitelistLocation, String pom) throws Exception {
        File basedir = resources.getBasedir(dir);

        if (pom.length() != 0) {
            Files.copy(basedir.toPath().resolve(pom), basedir.toPath().resolve("pom.xml"),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        // Run the configured build plugin goal trellis:enforce.
        MavenExecutionResult mavenExecutionResult = maven.forProject(basedir).execute("install");

        // Expect a validation error for module2 having a denied dependency on module1.
        mavenExecutionResult.assertLogText("[ERROR] Failed to execute goal com.salesforce.trellis:trellis-maven-plugin:"
                        + testProperties.get("project.version")
                        + ":enforce (enforce-dependency-rules) on project module2: ");
        mavenExecutionResult.assertLogText("[ERROR] The following dependency is not allowed for this project:");
        mavenExecutionResult.assertLogText("[ERROR] com.salesforce.trellis.tests:module1:jar:1.0.0-SNAPSHOT:compile");

        // Run trellis:update-whitelists plugin goal to update the maven dependency rules whitelist.
        maven.forProject(basedir).execute("trellis:update-whitelists").assertErrorFreeLog();

        // assert that the generated whitelist is correct
        final File whitelistDir = new File(basedir, ".mvn/trellis");
        final File expectedWhitelist = new File(whitelistDir, whitelistLocation + ".expected");
        final File actualWhitelist = new File(whitelistDir, whitelistLocation);
        assertEquals("generated whitelist doesn't match expectation",
            readFileToString(expectedWhitelist, "UTF-8").trim(),
            readFileToString(actualWhitelist, "UTF-8").trim());

        // Run validate lifecycle phase to execute the configured build plugin goal trellis:enforce again.
        // This time this should run without any dependency enforcer errors.
        maven.forProject(basedir).execute("validate").assertErrorFreeLog();
    }
}
