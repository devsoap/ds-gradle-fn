/*
 * Copyright 2019 Devsoap Inc.
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0
 * International Public License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *         https://creativecommons.org/licenses/by-nd/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.devsoap.fn

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Base class for functional tests
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FunctionalTest extends Specification {

    static final String PLUGIN_ID = 'com.devsoap.fn'

    @Rule
    protected TemporaryFolder testProjectDir

    protected File buildFile

    protected File settingsFile

    private long testStart

    protected Map<String, String> extraPlugins

    /**
     * Sets up the test
     */
    protected void setup() {
        extraPlugins = [:]
        initBuildFile()
        initSettingsFile()
        testStart = System.currentTimeMillis()
        println "Running test in ${testProjectDir.root}"
    }

    /**
     * Set additional plugins for the test.
     *
     * @param plugins
     *      the plugins to add besides the Vaadin plugin
     */
    protected void setExtraPlugins(Map<String, String> plugins) {
        extraPlugins = plugins
        buildFile.delete()
        initBuildFile()
    }

    /**
     * Cleans up the test
     */
    protected void cleanup() {
        long ms = System.currentTimeMillis() - testStart
        println "Test took ${TimeUnit.MILLISECONDS.toSeconds(ms)} seconds."
    }

    /**
     * Runs the project
     *
     * @param args
     *      the command line arguments to pass to gradle
     * @return
     *      the result of the build
     */
    protected BuildResult run(ConfigureRunner config = { }, String... args) {
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(['--stacktrace', '--info'] + (args as List))
                .withPluginClasspath()
        config.run(runner)
        println "Running gradle ${runner.arguments.join(' ')}"
        runner.build()
    }

    /**
     * Runs the project and is expected to fail
     *
     * @param args
     *      the command line arguments to pass to gradle
     * @return
     *       the result of the build
     */
    protected BuildResult runAndFail(ConfigureRunner config = { }, String... args) {
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(['--stacktrace', '--info'] + (args as List))
                .withPluginClasspath()
        config.run(runner)
        println "Running gradle ${runner.arguments.join(' ')}"
        runner.buildAndFail()
    }

    private void initBuildFile() {
        String gradlePluginDirectory =  Paths.get('.', 'build', 'libs').toFile().canonicalPath
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id '$PLUGIN_ID'
                ${extraPlugins.collect { it.value ? "id '$it.key' version '$it.value'" : "id '$it.key'" }.join('\n')}
            }

            repositories {
                flatDir dirs: "$gradlePluginDirectory"
            }

        """.stripIndent()
    }

    private void initSettingsFile() {
        settingsFile = testProjectDir.newFile('settings.gradle')
    }

    /**
     * Interface representing a GradleRunner configuration closure
     */
    @FunctionalInterface
    interface ConfigureRunner {
        void run(GradleRunner runner)
    }

}
