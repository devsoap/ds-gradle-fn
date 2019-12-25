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
package com.devsoap.fn.extensions

import com.devsoap.fn.tasks.FnPrepareDockerTask
import com.devsoap.fn.util.Versions
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.provider.Property

/**
 * Generic configuration for the plugin
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnExtension {

    static final String NAME = 'fn'
    private static final String GROOVY_VERSION = 'groovy.version'
    private static final String FN_JAVA_FDK_VERSION = 'fn.java.fdk.version'

    private final Property<String> functionClass
    private final Property<String> functionMethod
    private final Property<String> syslogUrl

    private final DependencyHandler dependencyHandler
    private final RepositoryHandler repositoryHandler
    private final Project project

    FnExtension(Project project) {
        this.project = project
        functionClass = project.objects.property(String)
        functionMethod = project.objects.property(String)
        syslogUrl = project.objects.property(String)
        dependencyHandler = project.dependencies
        repositoryHandler = project.repositories
    }

    /**
     * Set the function class name (FQN)
     *
     * @param fc
     *      the function class FQN
     */
    void setFunctionClass(String fc) {
        functionClass.set(fc)
    }

    /**
     * The function method name to call with the request
     */
    void setFunctionMethod(String fm) {
        functionMethod.set(fm)
    }

    /**
     * Get the function class name (FQN)
     */
    String getFunctionClass() {
        functionClass.getOrElse('MyFunction')
    }

    /**
     * Get the function method name
     */
    String getFunctionMethod() {
        functionMethod.getOrElse('handleRequest')
    }

    /**
     * Set the function path
     *
     * @param path
     *      the path that the function listens to
     */
    void setFunctionPaths(List<String> path) {
        FnPrepareDockerTask fnDocker = project.tasks.getByName(FnPrepareDockerTask.NAME)
        fnDocker.triggerPaths = path
    }

    /**
     * Get the function path
     */
    List<String> getFunctionPaths() {
        FnPrepareDockerTask fnDocker = project.tasks.getByName(FnPrepareDockerTask.NAME)
        fnDocker.triggerPaths
    }

    /**
     * Get the syslog server URL
     */
    String getSyslogUrl() {
        syslogUrl.orNull
    }

    /**
     * set the syslog server URL
     */
    void setSyslogUrl(String url) {
        syslogUrl.set(url)
    }

    /**
     * Returns the FN Project private maven repository
     */
    ArtifactRepository fnproject() {
        repository('FN Project Maven Repository', 'https://dl.bintray.com/fnproject/fnproject')
    }

    /**
     * Returns the FN Flow runtime dependency
     */
    Dependency flow() {
        String version = Versions.rawVersion('fn.java.flow.version')
        dependencyHandler.create("com.fnproject.fn:flow-runtime:$version")
    }

    /**
     * Returns the FN Api dependency
     */
    Dependency api() {
        String version = Versions.rawVersion(FN_JAVA_FDK_VERSION)
        dependencyHandler.create("com.fnproject.fn:api:$version")
    }

    /**
     * Returns a compatible Groovy version
     */
    Dependency groovy() {
        String version = Versions.rawVersion(GROOVY_VERSION)
        dependencyHandler.create("org.codehaus.groovy:groovy:$version")
    }

    /**
     * Returns a compatible testing Groovy dependency
     * @return
     */
    Dependency testingGroovy() {
        String version = Versions.rawVersion(GROOVY_VERSION)
        dependencyHandler.create("org.codehaus.groovy:groovy-test:$version")
    }

    /**
     * Returns the core testing dependency
     */
    Dependency testingCore() {
        String version = Versions.rawVersion(FN_JAVA_FDK_VERSION)
        dependencyHandler.create("com.fnproject.fn:testing-core:$version")
    }

    /**
     * Returns the compatibility library for Junit4
     */
    Dependency testingJunit4() {
        String version = Versions.rawVersion(FN_JAVA_FDK_VERSION)
        dependencyHandler.create("com.fnproject.fn:testing-junit4:$version")
    }

    /**
     * Define a Maven repository
     *
     * @param name
     *      the name for the repository
     * @param url
     *      the url of the repository
     */
    private ArtifactRepository repository(String name, String url) {
        repositoryHandler.maven { repository ->
            repository.name = name
            repository.url = url
        }
    }
}
