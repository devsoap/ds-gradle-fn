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
package com.devsoap.fn

import com.devsoap.fn.tasks.FnCreateFunctionTask
import com.devsoap.fn.tasks.FnDeployTask
import com.devsoap.fn.tasks.FnInstallCliTask
import com.devsoap.fn.tasks.FnInvokeTask
import com.devsoap.fn.tasks.FnPrepareDockerTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Builder for creating writers for writing a template to the file system
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnPlugin implements Plugin<Project> {

    static final String PLUGIN_ID = 'com.devsoap.fn'

    @Override
    void apply(Project project) {
        project.tasks.with {
            register(FnInstallCliTask.NAME, FnInstallCliTask)
            register(FnPrepareDockerTask.NAME, FnPrepareDockerTask)
            register(FnCreateFunctionTask.NAME, FnCreateFunctionTask)
            register(FnDeployTask.NAME, FnDeployTask)
            register(FnInvokeTask.NAME, FnInvokeTask)
        }
    }
}
