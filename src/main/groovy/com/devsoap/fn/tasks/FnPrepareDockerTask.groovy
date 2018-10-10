/*
 * Copyright 2018 Devsoap Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.fn.tasks

import com.devsoap.fn.util.TemplateWriter
import com.devsoap.fn.util.Versions
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.util.RelativePathUtil

/**
 * Generates the necessary files to build the function image
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnPrepareDockerTask extends DefaultTask {

    static final String NAME = 'fnDocker'
    public static final String EOL = '\n'
    public static final String DOCKER_APP_PATH = '/function/app/'
    public static final String LIBS_FOLDER = 'libs'

    final Property<String> functionClass = project.objects.property(String)

    final Property<String> functionMethod = project.objects.property(String)

    final Property<File> functionYaml = project.objects.property(File)

    @PackageScope
    @OutputDirectory
    final File dockerDir = new File(project.buildDir, 'docker')

    @PackageScope
    @OutputFile
    final File yaml = new File(dockerDir, 'func.yaml')

    @PackageScope
    @OutputFile
    final File dockerfile = new File(dockerDir, 'Dockerfile')

    FnPrepareDockerTask() {
        group = 'fn'
        description = 'Generates the docker file'
        dependsOn 'build'
    }

    @TaskAction
    void prepareDockerImage() {
        if (!functionClass.isPresent()) {
            throw new GradleException('Function class must be set')
        }

        if (!functionMethod.isPresent()) {
            throw new GradleException('Function method must be set')
        }

        if (!project.name) {
            throw new GradleException('Project name needs to be set')
        }

        if (dockerfile.exists()) {
            dockerfile.text = ''
        } else {
            dockerfile.parentFile.mkdirs()
            dockerfile.createNewFile()
        }

        setBaseImage('fnproject/fn-java-fdk', Versions.rawVersion('fn.java.fdk.version'))

        setWorkdirInDockerFile('/function')

        setCommandInDockerFile(functionClass.get(), functionMethod.get())

        addFilesToDockerFile(copyFilesIntoDockerDir(files), DOCKER_APP_PATH)

        addFileToDockerFile(initYaml(), DOCKER_APP_PATH)
    }

    private File initYaml() {
        if (yaml.exists()) {
            yaml.delete()
        }

        if (functionYaml.isPresent()) {
            yaml.text = functionYaml.get().text
        } else {
            TemplateWriter.builder()
                    .targetDir(dockerDir)
                    .templateFileName(yaml.name)
                    .substitutions([
                    'applicationName' : project.name.toLowerCase(),
                    'version' : project.version == Project.DEFAULT_VERSION ? 'latest' : project.version,
                    'triggerName' : "${project.name.toLowerCase()}-trigger",
                    'triggerPath' : "/${project.name.toLowerCase()}"
            ]).build().write()
        }
        yaml
    }

    @PackageScope
    List<File> getFiles() {
        List<File> files = project.configurations.compile.files.toList()
        File libs = new File(project.buildDir, LIBS_FOLDER)
        if (libs.exists()) {
            files.addAll(libs.listFiles())
        }
        files
    }

    @PackageScope
    List<File> copyFilesIntoDockerDir(List<File> files) {
        File libs = new File(dockerDir, LIBS_FOLDER)
        files.each { File sourceFile ->
            project.copy {
                from sourceFile
                into libs
            }
        }
        files.collect { File file -> new File(libs, file.name) }
    }

    @PackageScope
    void addFileToDockerFile(File from, String to) {
        String path = RelativePathUtil.relativePath(dockerDir, from)
        dockerfile << "ADD $path $to" << EOL
    }

    @PackageScope
    void addFilesToDockerFile(List<File> from, String to) {
        List<String> paths = from.collect { RelativePathUtil.relativePath(dockerDir, it) }
        dockerfile << 'ADD ' << paths.join(' ') << " $to" << EOL
    }

    @PackageScope
    void setCommandInDockerFile(String funcClass, String funcMethod) {
        dockerfile << "CMD [\"${funcClass}::${funcMethod}\"]" << EOL
    }

    @PackageScope
    void setWorkdirInDockerFile(String workdir) {
        dockerfile << "WORKDIR $workdir" << EOL
    }

    @PackageScope
    void setBaseImage(String image, String tag) {
        dockerfile << "FROM $image:$tag" << EOL
    }

    String getFunctionClass() {
        functionClass.orNull
    }

    String getFunctionMethod() {
        functionMethod.orNull
    }

    File getFunctionYaml() {
        functionYaml.orNull
    }

    void setFunctionClass(String fc) {
        functionClass.set(fc)
    }

    void setFunctionMethod(String fm) {
        functionMethod.set(fm)
    }

    void setFunctionYaml(File file) {
        functionYaml.set(file)
    }
}
