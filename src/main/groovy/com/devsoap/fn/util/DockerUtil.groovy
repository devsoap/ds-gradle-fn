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
package com.devsoap.fn.util

import groovy.json.StringEscapeUtils
import org.apache.tools.ant.util.StringUtils
import org.codehaus.groovy.util.StringUtil
import org.gradle.api.Project
import org.gradle.process.internal.ExecException

import java.nio.charset.StandardCharsets
import java.util.logging.Level

/**
 * Utilities for executing Docker commands
 *
 * @author John Ahlroos
 * @since 1.0
 */
class DockerUtil {

    static String resolveContainerAddress(Project project, String container) {
        inspectContainerProperty(project, container,  'NetworkSettings.IPAddress')
    }

    static boolean isContainerRunning(Project project, String container) {
        try {
            return inspectContainerProperty(project, container,  'State.Running').toBoolean()
        } catch (ExecException e) {
            return false
        }
    }

    private static String inspectContainerProperty(Project project, String container, String property) {
        ByteArrayOutputStream propertyStream = new ByteArrayOutputStream()
        propertyStream.withStream {
            project.exec {
                commandLine 'docker'
                args  'inspect', '--type', 'container', '-f', "'{{.$property}}'", container
                standardOutput = propertyStream
                errorOutput = LogUtils.getLogOutputStream(Level.FINE)
            }.rethrowFailure()
        }
        String out = new String(propertyStream.toByteArray(), StandardCharsets.UTF_8)
        out.trim().replace("'", '')
    }
}
