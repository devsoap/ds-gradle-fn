package com.devsoap.fn.tasks

import com.devsoap.fn.util.DockerUtil
import com.devsoap.fn.util.LogUtils
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.util.logging.Level

@Log
class FnStartFlowServerTask extends DefaultTask {

    static final String NAME = 'fnFlowStart'

    static final int FN_SERVER_PORT = 8080
    static final int FN_FLOW_SERVER_PORT = 8081
    static final int FN_FLOW_UI_PORT = 3002


    FnStartFlowServerTask() {
        dependsOn FnStartServerTask.NAME
        description = 'Starts the local FN Flow Server'
        group = 'fn-flow'
    }

    @TaskAction
    void execute() {
        String fnServerAddress = DockerUtil.resolveContainerAddress(project,'fnserver')
        if(!fnServerAddress) {
            throw new GradleException('FN Server is not running, aborting starting FN Flow server')
        }

        println "FN server is listening on http://localhost:$FN_SERVER_PORT"
        log.info "FN server listening on internal $fnServerAddress:$FN_SERVER_PORT"

        if(!DockerUtil.isContainerRunning(project, 'flowserver')) {
            startFlowServer(fnServerAddress)
        }

        String fnFlowServerAddress = DockerUtil.resolveContainerAddress(project, 'flowserver')
        if(!fnFlowServerAddress) {
            throw new GradleException('FN Flow server failed to start, aborting')
        }

        println "FN Flow server is listening on http://localhost:$FN_FLOW_SERVER_PORT"
        log.info "FN Flow server listening on internal $fnFlowServerAddress:$FN_FLOW_SERVER_PORT"

        if(!DockerUtil.isContainerRunning(project, 'flowui')) {
            startFlowServerUI(fnServerAddress, fnFlowServerAddress)
        }

        String fnFlowUIServerAddress = DockerUtil.resolveContainerAddress(project, 'flowui')

        println "FN Flow UI server is listening on http://localhost:$FN_FLOW_UI_PORT"
        log.info "FN Flow UI server listening on internal $fnFlowUIServerAddress:$FN_FLOW_UI_PORT"
    }

    private void startFlowServer(String fnServerAddress) {
        project.exec {
            commandLine 'docker'
            args    'run', '--rm', '-d',
                    '-p', "${FnStartFlowServerTask.FN_FLOW_SERVER_PORT}:8081",
                    '-e', "API_URL=http://$fnServerAddress:${FnStartFlowServerTask.FN_SERVER_PORT}/invoke",
                    '-e', "no_proxy=${fnServerAddress}",
                    '--name', 'flowserver',
                    'fnproject/flow:latest'
            standardOutput = LogUtils.getLogOutputStream(Level.INFO)
            errorOutput = LogUtils.getLogOutputStream(Level.SEVERE)
        }.rethrowFailure()
    }

    private void startFlowServerUI(String fnServerAddress, String fnFlowServerAddress) {
        project.exec {
            commandLine 'docker'
            args    'run', '--rm', '-d',
                    '-p', "${FnStartFlowServerTask.FN_FLOW_UI_PORT}:3000",
                    '--name', 'flowui',
                    '-e', "API_URL=http://${fnServerAddress}:${FnStartFlowServerTask.FN_SERVER_PORT}",
                    '-e', "COMPLETER_BASE_URL=http://$fnFlowServerAddress:${FnStartFlowServerTask.FN_FLOW_SERVER_PORT}",
                    'fnproject/flow:ui'
            standardOutput = LogUtils.getLogOutputStream(Level.INFO)
            errorOutput = LogUtils.getLogOutputStream(Level.SEVERE)
        }.rethrowFailure()
    }
}
