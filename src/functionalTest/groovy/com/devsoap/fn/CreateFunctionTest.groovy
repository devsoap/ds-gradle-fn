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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

import org.gradle.testkit.runner.BuildResult
import java.nio.file.Paths

/**
 * Tests creation of FN projects
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateFunctionTest extends FunctionalTest {

    void 'default project is created and compiles'() {
        setup:
            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'java').toFile()
            File functionFile = Paths.get(javaSourceDir.canonicalPath, 'MyFunction.java').toFile()
        when:
            BuildResult result = run'fnCreateFunction', 'jar'
        then:
            result.task(':fnCreateFunction').outcome == SUCCESS
            result.task(':jar').outcome == SUCCESS
            functionFile.exists()
    }

    void 'default Groovy project is created and compiles'() {
        setup:
            extraPlugins = ['groovy':null]

            buildFile << '''
                dependencies {
                    compile fn.groovy()
                }
            '''.stripIndent()

            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'groovy').toFile()
            File functionFile = Paths.get(javaSourceDir.canonicalPath, 'MyFunction.groovy').toFile()
        when:
            BuildResult result = run'fnCreateFunction', 'jar'
        then:
            result.task(':fnCreateFunction').outcome == SUCCESS
            result.task(':jar').outcome == SUCCESS
            functionFile.exists()
    }

    void 'default Kotlin project is created and compiles'() {
        setup:
            extraPlugins = ['org.jetbrains.kotlin.jvm':'1.3.21']

            buildFile << '''
                dependencies {
                    implementation "org.jetbrains.kotlin:kotlin-stdlib"
                }
            '''.stripIndent()

            File rootDir = testProjectDir.root
            File javaSourceDir = Paths.get(rootDir.canonicalPath, 'src', 'main', 'kotlin').toFile()
            File functionFile = Paths.get(javaSourceDir.canonicalPath, 'MyFunction.kt').toFile()
        when:
            BuildResult result = run'fnCreateFunction', 'jar'
        then:
            result.task(':fnCreateFunction').outcome == SUCCESS
            result.task(':jar').outcome == SUCCESS
            functionFile.exists()
    }
}
