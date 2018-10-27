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

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.options.Option

import java.nio.charset.StandardCharsets

/**
 * Invokes a running function
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnInvokeTask extends Exec {

    static final String NAME = 'fnInvoke'

    private static final String FN_COMMAND = 'fn'

    @Option(option = 'input', description = 'Input to send function')
    String input = ''

    @InputDirectory
    final File dockerImageDir = new File(project.buildDir, 'docker')

    FnInvokeTask() {
        dependsOn FnDeployTask.NAME
        description = 'Invokes the function on the server'
        group = FN_COMMAND
        commandLine FN_COMMAND
        args '--verbose', 'invoke', '--display-call-id', project.name.toLowerCase(), project.name.toLowerCase()
    }

    @Override
    protected void exec() {
        standardInput = new ByteArrayInputStream(input.bytes)
        standardOutput = errorOutput = new ByteArrayOutputStream()
        try {
            super.exec()
        } catch(Exception e){
            def output =  new String(standardOutput.toByteArray(), StandardCharsets.UTF_8)
            String callId = output.readLines().get(0).split(':')[1].trim()
            def logs = new ByteArrayOutputStream()
            try{
                project.exec { logCmd ->
                    commandLine FN_COMMAND
                    standardOutput = logs
                    args 'get', 'logs', project.name.toLowerCase(), project.name.toLowerCase(), callId
                }.assertNormalExitValue()
                logger.error(new String(logs.toByteArray(), StandardCharsets.UTF_8))
            } catch(Exception e2) {
                logger.error('Failed to fetch logs for error from server, client error was:', e)
                logger.debug('Client error', e2)
            }
        }
    }
}
