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

import com.devsoap.fn.util.DockerUtil
import com.devsoap.fn.util.LogUtils
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.util.logging.Level

/**
 * Starts the FN Flow servers
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log
class FnStartFlowServerTask extends DefaultTask {

    static final String NAME = 'fnFlowStart'

    static final int FN_SERVER_PORT = 8080
    static final int FN_FLOW_SERVER_PORT = 8081
    static final int FN_FLOW_UI_PORT = 3002

    private static final String FLOWSERVER = 'flowserver'
    private static final String ENV = '-e'
    private static final String DOCKER = 'docker'
    private static final String RUN = 'run'
    private static final String DEAMON = '-d'
    private static final String REMOVE_OLD = '--rm'
    private static final String FLOW_UI = 'flowui'
    private static final String PORT = '-p'
    private static final String NAME_PARAMETER = '--name'

    FnStartFlowServerTask() {
        dependsOn FnStartServerTask.NAME
        description = 'Starts the local FN Flow Server'
        group = 'fn-flow'
    }

    @TaskAction
    void start() {
        String fnServerAddress = DockerUtil.resolveContainerAddress(project, 'fnserver')
        if (!fnServerAddress) {
            throw new GradleException('FN Server is not running, aborting starting FN Flow server')
        }

        println "FN server is listening on http://localhost:$FN_SERVER_PORT"
        log.info "FN server listening on internal $fnServerAddress:$FN_SERVER_PORT"

        if (!DockerUtil.isContainerRunning(project, FLOWSERVER)) {
            startFlowServer(fnServerAddress)
        }

        String fnFlowServerAddress = DockerUtil.resolveContainerAddress(project, FLOWSERVER)
        if (!fnFlowServerAddress) {
            throw new GradleException('FN Flow server failed to start, aborting')
        }

        println "FN Flow server is listening on http://localhost:$FN_FLOW_SERVER_PORT"
        log.info "FN Flow server listening on internal $fnFlowServerAddress:$FN_FLOW_SERVER_PORT"

        if (!DockerUtil.isContainerRunning(project, FLOW_UI)) {
            startFlowServerUI(fnServerAddress, fnFlowServerAddress)
        }

        String fnFlowUIServerAddress = DockerUtil.resolveContainerAddress(project, FLOW_UI)

        println "FN Flow UI server is listening on http://localhost:$FN_FLOW_UI_PORT"
        log.info "FN Flow UI server listening on internal $fnFlowUIServerAddress:$FN_FLOW_UI_PORT"
    }

    private void startFlowServer(String fnServerAddress) {
        project.exec {
            commandLine DOCKER
            args    RUN, REMOVE_OLD, DEAMON,
                    PORT, "${FnStartFlowServerTask.FN_FLOW_SERVER_PORT}:8081",
                    ENV, "API_URL=http://$fnServerAddress:${FnStartFlowServerTask.FN_SERVER_PORT}/invoke",
                    ENV, "no_proxy=${fnServerAddress}",
                    NAME_PARAMETER, FLOWSERVER,
                    'fnproject/flow:latest'
            standardOutput = LogUtils.getLogOutputStream(Level.INFO)
            errorOutput = LogUtils.getLogOutputStream(Level.SEVERE)
        }.rethrowFailure()
    }

    private void startFlowServerUI(String fnServerAddress, String fnFlowServerAddress) {
        project.exec {
            commandLine DOCKER
            args    RUN, REMOVE_OLD, DEAMON,
                    PORT, "${FnStartFlowServerTask.FN_FLOW_UI_PORT}:3000",
                    NAME_PARAMETER, FLOW_UI,
                    ENV, "API_URL=http://${fnServerAddress}:${FnStartFlowServerTask.FN_SERVER_PORT}",
                    ENV, "COMPLETER_BASE_URL=http://$fnFlowServerAddress:${FnStartFlowServerTask.FN_FLOW_SERVER_PORT}",
                    'fnproject/flow:ui'
            standardOutput = LogUtils.getLogOutputStream(Level.INFO)
            errorOutput = LogUtils.getLogOutputStream(Level.SEVERE)
        }.rethrowFailure()
    }
}
