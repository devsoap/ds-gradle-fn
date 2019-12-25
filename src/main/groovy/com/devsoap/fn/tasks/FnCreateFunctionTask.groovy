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
package com.devsoap.fn.tasks

import com.devsoap.fn.extensions.FnExtension
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
    private static final String SRC = 'src'
    private static final String MAIN = 'main'
    private static final String DOT_PATTERN = '\\.'

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

    @Input
    @Option(option = 'module', description = 'Create function as a submodule')
    boolean asSubModule = false

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

        boolean hasFnExtension = project.extensions.findByName(FnExtension.NAME)

        if (asSubModule || !hasFnExtension) {
            initFunctionAsSubmodule()
        } else {
            initFunctionInCurrentProject()
        }
    }

    private void initFunctionInCurrentProject() {
        FnExtension fn = project.extensions.getByType(FnExtension)

        functionMethod = functionMethod ?: fn.functionMethod

        if (!functionClass) {
            List<String> tokens = fn.functionClass.tokenize(DOT)
            functionClass = tokens.last()
            functionPackage = tokens.dropRight(1).join(DOT)
        }

        File root = project.projectDir
        File sourceMain = Paths.get(root.canonicalPath, SRC, MAIN).toFile()

        ProjectType projectType = ProjectType.get(project)
        File languageSourceDir = new File(sourceMain, projectType.sourceDir)
        String sourceFileExtension = projectType.extension

        File pkgDir = Paths.get(languageSourceDir.canonicalPath, functionPackage.split(DOT_PATTERN)).toFile()
        String funcClassName = TemplateWriter.makeStringJavaCompatible(functionClass)
        String funcMethodName = TemplateWriter.makeStringJavaCompatible(functionMethod).uncapitalize()

        initGradleFile(root, funcClassName, funcMethodName)

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

    private void initFunctionAsSubmodule() {

        functionMethod = functionMethod ?: 'handleRequest'
        functionClass =  functionClass ?: 'MyFunction'

        File parentRoot = project.projectDir

        File gradleSettings = new File(project.rootDir, 'settings.gradle')
        if (gradleSettings.exists()) {
            gradleSettings << "\ninclude '${functionClass.toLowerCase()}'"
        } else {
            gradleSettings.text = "include '${functionClass.toLowerCase()}'"
        }

        File root = new File(parentRoot, functionClass.toLowerCase())
        root.mkdir()

        File sourceMain = Paths.get(root.canonicalPath, SRC, MAIN).toFile()

        ProjectType projectType = ProjectType.get(project)
        File languageSourceDir = new File(sourceMain, projectType.sourceDir)
        String sourceFileExtension = projectType.extension

        File pkgDir = Paths.get(languageSourceDir.canonicalPath, functionPackage.split(DOT_PATTERN)).toFile()
        String funcClassName = TemplateWriter.makeStringJavaCompatible(functionClass)
        String funcMethodName = TemplateWriter.makeStringJavaCompatible(functionMethod).uncapitalize()

        initGradleFile(root, funcClassName, funcMethodName)

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

    private void initGradleFile(File root, String funcClassName, String funcMethodName) {
        File gradleBuild = new File(root, 'build.gradle')
        gradleBuild << """
        fn {
            functionClass = '${ functionPackage ? "${functionPackage}.${funcClassName}" : funcClassName }'
            functionMethod = '$funcMethodName'
        }
        """.stripIndent()
    }
}
