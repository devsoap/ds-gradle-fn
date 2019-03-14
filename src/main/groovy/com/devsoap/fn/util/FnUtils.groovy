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
package com.devsoap.fn.util

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecException

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.logging.Level

/**
 * Utilities for running the FN CLI
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnUtils {

    private static final String FN = 'fn'

    /**
     * Get the path for the CLI executable
     *
     * @param project
     *      the project to get the path for
     */
    static final String getFnExecutablePath(Project project) {
        File fnFolder = Paths.get(project.rootDir.canonicalPath, '.gradle', FN).toFile()
        if (Os.isFamily(Os.FAMILY_MAC)) {
            new File(fnFolder, 'fn_mac').canonicalPath
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            new File(fnFolder, 'fn_linux').canonicalPath
        } else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            new File(fnFolder, 'fn.exe').canonicalPath
        } else {
            FN // Fallback to globally installed FN
        }
    }

    /**
     * Get all active functions for an application
     *
     * @param project
     *      the project with the function
     * @param application
     *      the application name
     */
    static final Map<String, String> getFunctions(Project project, String application) {
        OutputStream output = new ByteArrayOutputStream()
        try {
            project.exec {
                commandLine getFnExecutablePath(project)
                args 'list', 'functions', application
                standardOutput = output
                errorOutput = LogUtils.getLogOutputStream(Level.FINE)
            }
        } catch (ExecException ignored) {
            return [:]
        }

        Map functions = [:]
        new String(output.toByteArray(), StandardCharsets.UTF_8).readLines().reverse().remove(0).eachLine { line ->
            String[] columns = line.split('\\t')
            functions[columns[0]] = columns[2]
        }
        functions
    }

    /**
     * Remove an active function
     *
     * @param project
     *      the project with the function
     * @param application
     *      the application name
     * @param function
     *      the function name
     */
    static final void removeFunction(Project project, String application, String function) {
        if (getFunctions(project, application).keySet().contains(function)) {
            project.exec {
                commandLine getFnExecutablePath(project)
                args 'delete', 'function', application, function
                standardOutput = LogUtils.getLogOutputStream(Level.INFO)
                errorOutput = LogUtils.getLogOutputStream(Level.SEVERE)
            }
        }
    }
}
