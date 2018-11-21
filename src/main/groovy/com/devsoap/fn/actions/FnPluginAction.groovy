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
package com.devsoap.fn.actions

import com.devsoap.fn.FnPlugin
import com.devsoap.fn.util.Versions
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 * FN Plugin action
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnPluginAction extends PluginAction {

    final String pluginId = FnPlugin.PLUGIN_ID

    @Override
    void apply(Project project) {
        super.apply(project)
        project.plugins.apply('java')
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)

        project.with {
            String pluginDependency =
                    "com.devsoap:fn-gradle-plugin:${Versions.rawVersion('fn.plugin.version')}"
            Dependency vaadin = dependencies.create(pluginDependency) {
                description = 'Fn Plugin'
            }
            configurations['compileOnly'].dependencies.add(vaadin)
        }
    }
}
