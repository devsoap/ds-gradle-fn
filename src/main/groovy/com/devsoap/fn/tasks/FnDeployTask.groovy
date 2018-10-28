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

import com.devsoap.fn.util.FnUtils
import com.devsoap.fn.util.HashUtils
import com.devsoap.fn.util.LogUtils
import groovy.transform.PackageScope
import groovy.util.logging.Log
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory

import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * Deploys the function to production
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class FnDeployTask extends Exec {

    static final String NAME = 'fnDeploy'

    private static final String HASH_PROPERTY = 'fn-deploy-input-hash'

    /*
     * Should the local docker be used, or are we deploying to a remote instance
     */
    private final Property<Boolean> useLocalDockerInstance = project.objects.property(Boolean)

    /*
     * The directory where the Dockerfile can be found
     */
    @PackageScope
    @InputDirectory
    final File dockerImageDir = new File(project.buildDir, 'docker')

    /**
     * Creates a new deploy task
     */
    FnDeployTask() {
        dependsOn FnPrepareDockerTask.NAME, FnInstallCli.NAME
        description = 'Deploys the function to the server'
        group = 'fn'
        workingDir(dockerImageDir)
        commandLine FnUtils.getFnExecutablePath(project)
        args  '--verbose', 'deploy', '--app', project.name.toLowerCase(), '--no-bump'
        standardOutput = LogUtils.getLogOutputStream(Level.INFO)
        errorOutput = LogUtils.getLogOutputStream(Level.SEVERE)
        useLocalDockerInstance.set(true)
        inputs.property(HASH_PROPERTY) { -> HashUtils.getFileHash(dockerImageDir) }
        outputs.upToDateWhen { inputs.properties.get(HASH_PROPERTY) == HashUtils.getFileHash(dockerImageDir)  }
    }

    @Override
    protected void exec() {
        if (local) {
            args += '--local'
        }

        super.exec()

        logger.info('Waiting for hot functions to terminate...')
        FnPrepareDockerTask fnDocker = project.tasks.getByName(FnPrepareDockerTask.NAME)
        Thread.sleep(TimeUnit.MINUTES.toMillis(fnDocker.idleTimeout))
    }

    /**
     * Is the docker image deployed locally
     */
    @Input
    boolean isLocal() {
        useLocalDockerInstance.getOrElse(true)
    }

    /**
     * Should the docker image be deployed to the local docker instance?
     */
    void setLocal(boolean local) {
        useLocalDockerInstance.set(local)
    }
}
