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
package com.devsoap.fn.actions

import com.devsoap.fn.FnPlugin
import com.devsoap.fn.util.LogUtils
import com.devsoap.fn.util.Versions
import com.devsoap.license.DevsoapLicenseExtension
import com.devsoap.license.DevsoapLicensePlugin
import com.devsoap.license.Validator
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 * FN Plugin action
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnPluginAction extends PluginAction {

    private static final String PLUGIN_VERSION_KEY = 'fn.plugin.version'
    private static final String COMPILE_ONLY = 'compileOnly'

    final String pluginId = FnPlugin.PLUGIN_ID

    @Override
    void apply(Project project) {
        super.apply(project)
        project.plugins.apply('java')

        project.pluginManager.apply(DevsoapLicensePlugin)

        DevsoapLicenseExtension devsoap = project.extensions.getByType(DevsoapLicenseExtension)
        devsoap.credential(FnPlugin.PRODUCT_NAME,
                System.getProperty("devsoap.${FnPlugin.PRODUCT_NAME}.license.email"),
                System.getProperty("devsoap.${FnPlugin.PRODUCT_NAME}.license.key")
        ) { DevsoapLicenseExtension.Credential c ->
            c.signature =  Versions.rawVersion('fn.plugin.signature')
        }

        String licenseDependency =
                "com.devsoap:devsoap-license-plugin:${ Versions.rawVersion('devsoap.license.version') }"
        Dependency license = project.dependencies.create(licenseDependency)
        project.configurations[COMPILE_ONLY].dependencies.add(license)
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)

        project.with {

            repositories.maven { repository ->
                repository.name = 'Gradle Plugin Portal'
                repository.url = 'https://plugins.gradle.org/m2/'
            }

            String pluginDependency =
                    "com.devsoap:fn-gradle-plugin:${Versions.rawVersion(PLUGIN_VERSION_KEY)}"
            Dependency vaadin = dependencies.create(pluginDependency) {
                description = 'Fn Plugin'
            }
            configurations[COMPILE_ONLY].dependencies.add(vaadin)
        }
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)

        String pluginVersion = Versions.version(PLUGIN_VERSION_KEY)
        if (Validator.isValidLicense(project, FnPlugin.PRODUCT_NAME)) {
            DevsoapLicenseExtension devsoap = project.extensions[DevsoapLicenseExtension.NAME]
            DevsoapLicenseExtension.Credential credential = devsoap.getCredential(FnPlugin.PRODUCT_NAME)
            LogUtils.printIfNotPrintedBefore( project,
                    "Using DS FN Plugin $pluginVersion (Licensed to ${credential.email})",
                    true
            )
        } else {
            LogUtils.printIfNotPrintedBefore( project,
                    "Using DS FN Plugin $pluginVersion (UNLICENSED). Hide this message using --quiet " +
                            'with PRO subscription.',
                    false
            )
        }
    }
}
