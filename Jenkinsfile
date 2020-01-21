@Library('sfci-pipeline-sharedlib@v0.13.11') _

import net.sfdc.dci.BuildUtils
import net.sfdc.dci.MavenUtils

/**
 * Define the release branches with their suggested semver segment to update on a release.
 */
env.VERSION_RELEASE_BRANCHES = '{"master": "PATCH"}'

/**
 * Define the parameters for performing a release. Normal CI builds will just use the default, which do not perform a
 * release. To perform a release, go to this build in SFCI and click "Build with Parameters".
 */
def releaseParameters = {
    parameters([
        booleanParam(
            defaultValue: false,
            description: 'Do you want to release? If not checked, version input boxes are not used.',
            name: 'RELEASE'
        ),
        string(
            defaultValue: MavenUtils.getDefaultReleaseVersion(this),
            description: 'The version to be released now.',
            name: 'RELEASE_VERSION'
        ),
        string(
            defaultValue: "${BuildUtils.incrementVersion(MavenUtils.getDefaultReleaseVersion(this), BuildUtils.getVersionTypeToIncrement(this))}-SNAPSHOT",
            description: 'The next development version in the updated pom.xml.',
            name: 'NEXT_RELEASE_VERSION'
        ),
    ])
}

def envDef = [
    emailTo: 'pcal@salesforce.com',
    releaseParameters: releaseParameters,
    conditionalSkipCIBuild: true
]

properties([
    pipelineTriggers([cron('@daily')]),
])


executePipeline(envDef) {
    stage('Init') {
        checkout scm
        def commitSha = BuildUtils.getLatestCommitSha(this)
        echo("Preparing to build ${env.BRANCH_NAME} at ${commitSha}")
        mavenInit()
    }

    if (params.RELEASE) {
        def releaseConfig = [staging_profile_id :  'c581c5ff21e18']
        stage('Prepare Release') {
            echo("===================")
            echo(" PREPARING RELEASE")
            echo("===================")
            mavenReleasePrepare(releaseConfig)
        }
        stage('Release') {
            echo("====================")
            echo(" PERFORMING RELEASE")
            echo("====================")
            mavenReleasePerform(releaseConfig)
        }
    } else {
        stage('Build') {
            mavenBuild()
        }
    }
}
