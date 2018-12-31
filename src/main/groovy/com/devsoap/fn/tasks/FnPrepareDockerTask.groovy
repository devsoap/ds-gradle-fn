/*
 * Copyright 2018-2019 Devsoap Inc.
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

import com.devsoap.fn.extensions.FnExtension
import com.devsoap.fn.util.HashUtils
import com.devsoap.fn.util.TemplateWriter
import com.devsoap.fn.util.Versions
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
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

    private static final String EOL = '\n'
    private static final String DOCKER_APP_PATH = '/function/app/'
    private static final String LIBS_FOLDER = 'libs'

    /*
     * Use a custom function.yaml file
     */
    @Optional
    @Input
    final Property<File> functionYaml = project.objects.property(File)

    /*
     * If not using a custom func.yaml, then the name of the trigger
     */
    @Input
    final Property<String> triggerName = project.objects.property(String)

    /*
    * If not using a custom func.yaml, then the path of the trigger
    */
    @Input
    final Property<String> triggerPath = project.objects.property(String)

    /*
    * If not using a custom func.yaml, then the type of the trigger
    */
    @Input
    final Property<String> triggerType = project.objects.property(String)

    /*
    * If not using a custom func.yaml, then the idle functionTimeout in seconds
    */
    @Input
    final Property<Integer> idleTimeout = project.objects.property(Integer)

    /*
    * If not using a custom func.yaml, then the functionTimeout in seconds
    */
    @Input
    final Property<Integer> functionTimeout = project.objects.property(Integer)

    /*
     * The directory where the Dockerfile will be generated
     */
    @OutputDirectory
    final File dockerDir = new File(project.buildDir, 'docker')

    /*
    * If not using a custom func.yaml, then the generated func.yaml
    */
    @OutputFile
    final File yaml = new File(dockerDir, 'func.yaml')

    /*
    * The Dockerfile in use
    */
    @OutputFile
    final File dockerfile = new File(dockerDir, 'Dockerfile')

    /*
    * Where all dependencies which will be included in the Docker image will be assembled
    */
    @InputDirectory
    final File libs = new File(project.buildDir, LIBS_FOLDER)

    /**
     * Prepares the docker image for the function
     */
    FnPrepareDockerTask() {
        group = 'fn'
        description = 'Generates the docker file'
        dependsOn 'jar'
    }

    /**
     * Prepares the docker image by generated files and copying dependendencies
     */
    @TaskAction
    void prepareDockerImage() {
        FnExtension fn = project.extensions.getByType(FnExtension)

        if (!project.name) {
            throw new GradleException('project.name needs to be set')
        }

        if (dockerfile.exists()) {
            dockerfile.text = ''
        } else {
            dockerfile.parentFile.mkdirs()
            dockerfile.createNewFile()
        }

        setBaseImage('fnproject/fn-java-fdk', Versions.rawVersion('fn.java.fdk.version'))

        setWorkdirInDockerFile('/function')

        setCommandInDockerFile(fn.functionClass, fn.functionMethod)

        setFileHash() // Must be before files are added to force no-cache if a file is changed

        addFilesToDockerFile(copyFilesIntoDockerDir(files), DOCKER_APP_PATH)

        addFileToDockerFile(initYaml(), DOCKER_APP_PATH)
    }

    private File initYaml() {
        if (yaml.exists()) {
            yaml.delete()
        }

        if (functionYaml.isPresent()) {
            yaml.text = getFunctionYaml().text
        } else {
            TemplateWriter.builder()
                    .targetDir(dockerDir)
                    .templateFileName(yaml.name)
                    .substitutions([
                    'functionName' : project.name.toLowerCase(),
                    'version' : (project.version == Project.DEFAULT_VERSION ? '' : project.version)
                            + new Date().format('yyyyMMddssmm'),
                    'triggerName' : getTriggerName(),
                    'triggerPath' : getTriggerPath(),
                    'triggerType' : getTriggerType(),
                    'functionTimeout' : getFunctionTimeout(),
                    'idleTimeout' : getIdleTimeout(),

            ]).build().write()
        }
        yaml
    }

    /**
     * Return the dependencies to package into the docker image
     */
    @PackageScope
    List<File> getFiles() {
        List<File> files = project.configurations.compile.files.toList()
        File libs = new File(project.buildDir, LIBS_FOLDER)
        if (libs.exists()) {
            files.addAll(libs.listFiles())
        }
        files
    }

    /**
     * Copies the given files into the libs directory
     *
     * @param files
     *      the files to copy
     * @return
     *      list of files pointing to the copies in the libs direcotry
     */
    @PackageScope
    List<File> copyFilesIntoDockerDir(List<File> files) {
        File libs = new File(dockerDir, LIBS_FOLDER)
        if (libs.exists()) {
            project.delete(libs)
        }
        files.each { File sourceFile ->
            project.copy {
                from sourceFile
                into libs
            }
        }
        files.collect { File file -> new File(libs, file.name) }
    }

    /**
     * Add instruction to add a file to the Dockerfile
     *
     * @param from
     *      path from where file is copied
     * @param to
     *      target path in docker image
     */
    @PackageScope
    void addFileToDockerFile(File from, String to) {
        String path = RelativePathUtil.relativePath(dockerDir, from)
        dockerfile << "ADD $path $to" << EOL
    }

    /**
     * Add instruction to add multiple files ot the Dockerfile
     *
     * @param from
     *      list of paths of files to be copied
     * @param to
     *      target path in docker image
     */
    @PackageScope
    void addFilesToDockerFile(List<File> from, String to) {
        List<String> paths = from.collect { RelativePathUtil.relativePath(dockerDir, it) }
        dockerfile << 'ADD ' << paths.join(' ') << " $to" << EOL
    }

    /**
     * Add instruction to set the command in the Dockerfile
     *
     * @param funcClass
     *      the Fn function class (FQN)
     *
     * @param funcMethod
     *      the Fn function method
     */
    @PackageScope
    void setCommandInDockerFile(String funcClass, String funcMethod) {
        dockerfile << "CMD [\"${funcClass}::${funcMethod}\"]" << EOL
    }

    /**
     * Add instruction to set the workdir in the Dockerfile
     *
     * @param workdir
     *      the workdir path
     */
    @PackageScope
    void setWorkdirInDockerFile(String workdir) {
        dockerfile << "WORKDIR $workdir" << EOL
    }

    /**
     * Add instruction to set the base image in the Dockerfile
     *
     * @param image
     *      the image to use
     * @param tag
     *      the tag to use
     */
    @PackageScope
    void setBaseImage(String image, String tag) {
        dockerfile << "FROM $image:$tag" << EOL
    }

    /**
     * Generates a label instruction to the Dockerfile with the current unique hash
     */
    @PackageScope
    void setFileHash() {
        dockerfile << "LABEL build.id=${HashUtils.getFileHash(dockerDir)}" << EOL
    }

    /**
     * Get the custom function.yaml file if set
     */
    File getFunctionYaml() {
        this.functionYaml.orNull
    }

    /**
     * Get the trigger name
     */
    String getTriggerName() {
        this.triggerName.getOrElse("${project.name.toLowerCase()}-trigger")
    }

    /**
     * Get the trigger path
     */
    String getTriggerPath() {
        this.triggerPath.getOrElse("/${project.name.toLowerCase()}")
    }

    /**
     * Get the trigger type
     */
    String getTriggerType() {
        this.triggerType.getOrElse('http')
    }

    /**
     * Get the idle functionTimeout of the function
     */
    Integer getIdleTimeout() {
        this.idleTimeout.getOrElse(1)
    }

    /**
     * Get the functionTimeout of the function
     */
    Integer getFunctionTimeout() {
        this.functionTimeout.getOrElse(30)
    }

    /**
     * Set the custom func.yaml file to use
     *
     * @param file
     *      the file pointing to a valid func.yaml
     */
    void setFunctionYaml(File file) {
        this.functionYaml.set(file)
    }

    /**
     * Set the trigger name
     *
     * @param triggerName
     *      trigger name to use
     */
    void setTriggerName(String triggerName) {
        this.triggerName.set(triggerName)
    }

    /**
     * Set the path the trigger listens to
     *
     * @param triggerPath
     *      the path of the trigger
     */
    void setTriggerPath(String triggerPath) {
        this.triggerPath.set(triggerPath)
    }

    /**
     * Set the trigger type
     *
     * @param triggerType
     *      the type of trigger to create
     *
     */
    void setTriggerType(String triggerType) {
        this.triggerType.set(triggerType)
    }

    /**
     * Set the idle functionTimeout of the function
     *
     * @param idleTimeout
     *      the idle-functionTimeout, in seconds, between 1-1000
     */
    void setIdleTimeout(int idleTimeout) {
        this.idleTimeout.set(idleTimeout)
    }

    /**
     * Set the functionTimeout of the function
     *
     * @param timeout
     *      the functionTimeout, in seconds, between 1-1000
     */
    void setFunctionTimeout(int timeout) {
        this.functionTimeout.set(timeout)
    }

}
