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
import com.devsoap.fn.util.LogUtils
import org.gradle.api.tasks.Exec

import java.util.logging.Level

/**
 * Starts the FN server locally
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnStartServerTask extends Exec {

    static String NAME = 'fnStart'

    FnStartServerTask() {
        dependsOn FnInstallCliTask.NAME
        description = 'Deploys the function to the server'
        group = 'fn'
        commandLine FnUtils.getFnExecutablePath(project)
        args  'start', '-d'
        standardOutput = LogUtils.getLogOutputStream(Level.INFO)
        errorOutput = LogUtils.getLogOutputStream(Level.SEVERE)
    }
}
