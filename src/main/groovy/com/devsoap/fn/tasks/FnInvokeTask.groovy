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

import com.devsoap.fn.util.FnUtils
import groovy.json.JsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecException

import java.nio.charset.StandardCharsets

/**
 * Invokes a running function
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnInvokeTask extends DefaultTask {

    static final String NAME = 'fnInvoke'

    private static final String EQUALS = '='
    private static final String LIST = 'list'
    private static final String TAB_CHARACTER = '\\t'
    private static final int VALUE_COLUMN = 4

    @Option(description = 'Input to send function')
    String input = ''

    @Option(description = 'Trigger to invoke')
    String trigger = ''

    @Option(option = 'app', description = 'Application to invoke')
    String application = project.rootProject.name.toLowerCase()

    @Option(description = 'The function context')
    String context = 'default'

    @Option(option = 'headers', description = 'Headers to send in the request')
    List<String> requestHeaders = []

    @Option(description = 'HTTP Method (GET/POST)')
    String method = 'GET'

    @Option(option = 'params', description = 'HTTP Parameters')
    List<String> parameters = []

    @InputDirectory
    final File dockerImageDir = new File(project.buildDir, 'docker')

    FnInvokeTask() {
        dependsOn FnDeployTask.NAME, ':' + FnInstallCliTask.NAME
        description = 'Invokes the function on the server'
        group = 'fn'
    }

    @TaskAction
    void invoke() {
        String functionUrl = findContexts(project)[context]
        logger.info("Function base url is $functionUrl")

        FnPrepareDockerTask fnDocker = project.tasks.findByName(FnPrepareDockerTask.NAME)
        Map triggers = findTriggers(project, application, fnDocker.functionName)
        String  triggerUrl = trigger ? triggers[trigger] : triggers.entrySet().first().value
        logger.info("Function trigger url is $triggerUrl")

        String fullUrl = triggerUrl
        if (!parameters.isEmpty()) {
            fullUrl += '?'
            parameters.each { param ->
                List<String> kv = param.tokenize(EQUALS)
                String key = kv.remove(0)
                String value = URLEncoder.encode(kv.join(EQUALS), StandardCharsets.UTF_8)
                fullUrl += "$key=$value"
            }
        }

        HttpURLConnection conn = null
        try {

            logger.info("Calling function $fullUrl")
            conn = (HttpURLConnection) fullUrl.toURL().openConnection()
            conn.setRequestMethod(method.toUpperCase())

            requestHeaders.each {
                List<String> kv = it.tokenize(EQUALS)
                conn.setRequestProperty(kv[0], kv[1])
            }

            if (method.toUpperCase() == 'POST' &&
                    !input.isEmpty() &&
                    !input.isAllWhitespace()) {
                conn.doOutput = true
                conn.outputStream.withWriter { it << input }
            }

            String response = conn.content.text
            String contentType = conn.getHeaderField('Content-Type')

            println ''
            println ''
            println '================ FUNCTION RESPONSE ===================='
            println ''
            println formatResponse(response, contentType)
            println ''
            println '======================================================='
            println ''
            println ''

        } catch (IOException e) {
            String callId = conn?.getHeaderField('Fn-Call-Id')
            if (callId) {
                String logs = getLogForCallId(project, fnDocker.functionName, callId)
                logger.error(logs)
                throw new GradleException("Invoking $application:$trigger (callId: $callId) failed!")
            } else {
                throw e
            }
        } finally {
            conn?.disconnect()
        }
    }

    private static Map findContexts(Project project) {
        OutputStream output = new ByteArrayOutputStream()
        project.exec { ExecSpec spec ->
            spec.commandLine FnUtils.getFnExecutablePath(project)
            spec.args LIST, 'context'
            spec.standardOutput = output
        }.assertNormalExitValue()

        Map contexts = [:]
        new String(output.toByteArray(), StandardCharsets.UTF_8).readLines().reverse().remove(0).eachLine { line ->
            String[] columns = line.split(TAB_CHARACTER)
            contexts[columns[1]] = columns[VALUE_COLUMN]
        }
        project.logger.info("Found contexts $contexts")
        contexts
    }

    private static Map findTriggers(Project project, String application, String function) {
        OutputStream output = new ByteArrayOutputStream()
        project.exec { ExecSpec spec ->
            spec.commandLine FnUtils.getFnExecutablePath(project)
            spec.args LIST, 'triggers', application, function
            spec.standardOutput = output
        }.assertNormalExitValue()

        Map triggers = [:]
        new String(output.toByteArray(), StandardCharsets.UTF_8).readLines().reverse().dropRight(1).each { line ->
            String[] columns = line.split(TAB_CHARACTER)
            triggers[columns[0]] = columns[VALUE_COLUMN]
        }

        project.logger.info("Found triggers $triggers")
        triggers
    }

    private String getLogForCallId(Project project, String function, String callId) {
        OutputStream logs = new ByteArrayOutputStream()
        try {
            project.exec { ExecSpec spec ->
                spec.commandLine FnUtils.getFnExecutablePath(project)
                spec.standardOutput = logs
                spec.args 'get', 'logs', application, function, callId
            }.assertNormalExitValue()
            new String(logs.toByteArray(), StandardCharsets.UTF_8)
        } catch (ExecException e) {
            throw new GradleException('Failed to get logs from server, is the server running?')
        }
    }

    private static String formatResponse(String response, String contentType) {
        switch (contentType) {
            case 'application/json':
                new JsonBuilder(response).toPrettyString()
                break
            default:
                response
        }
    }
}
