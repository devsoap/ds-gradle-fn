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

import com.devsoap.fn.util.ProjectType
import com.devsoap.fn.util.TemplateWriter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.nio.file.Paths

/**
 * Creates a new FN function
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnCreateFunctionTask extends DefaultTask {

    static final String NAME = 'fnCreateFunction'

    private static final String DOT = '.'

    /*
     * Function class name
     */
    @Input
    @Optional
    @Option(option = 'name', description = 'The name of the function class')
    String functionClass

    /*
     * Function method name
     */
    @Input
    @Optional
    @Option(option = 'method', description = 'The name of the function method entrypoint')
    String functionMethod

    /*
     * FQN of the package where the function class will be generated
     */
    @Input
    @Option(option = 'package', description = 'Function package')
    String functionPackage = 'com.example'

    /**
     * Creates a new FN Creation task
     */
    FnCreateFunctionTask() {
        description = 'Creates a Fn Function project'
        group = 'fn'
    }

    /**
     * Creates a new function project
     */
    @TaskAction
    void run() {
        FnPrepareDockerTask fnDocker = project.tasks.getByName(FnPrepareDockerTask.NAME)

        if (!functionMethod && fnDocker.functionMethod) {
            functionMethod = fnDocker.functionMethod
        } else {
            functionMethod = 'handleRequest'
        }

        if (!functionClass && fnDocker.functionClass) {
            List<String> tokens = fnDocker.functionClass.tokenize(DOT)
            functionClass = tokens.last()
            functionPackage = tokens.dropRight(1).join(DOT)
        } else {
            functionClass = 'MyFunction'
        }

        File root = project.projectDir
        File sourceMain = Paths.get(root.canonicalPath, 'src', 'main').toFile()

        ProjectType projectType = ProjectType.get(project)
        File languageSourceDir = new File(sourceMain, projectType.sourceDir)
        String sourceFileExtension = projectType.extension

        File pkgDir = Paths.get(languageSourceDir.canonicalPath, functionPackage.split('\\.')).toFile()
        String funcClassName = TemplateWriter.makeStringJavaCompatible(functionClass)
        String funcMethodName = TemplateWriter.makeStringJavaCompatible(functionMethod).uncapitalize()

        TemplateWriter.builder()
                .templateFileName("Function.$sourceFileExtension")
                .targetDir(pkgDir)
                .targetFileName("${funcClassName}.$sourceFileExtension")
                .substitutions([
                    'functionPackage' : functionPackage,
                    'functionClass' : funcClassName,
                    'functionMethod' : funcMethodName
        ]).build().write()
    }
}
