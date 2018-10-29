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
import groovy.json.JsonSlurper
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.channels.Channels

/**
 * Installs the CLI locally
 *
 * @author John Ahlroos
 * @since 1.0
 */
@CacheableTask
class FnInstallCliTask extends DefaultTask {

    static final NAME = 'fnInstall'

    private static final String RELEASES_API = 'https://api.github.com/repos/fnproject/cli/releases/latest'

    private static final String DOWNLOAD_API = 'https://github.com/fnproject/cli/releases/download'

    @OutputFile
    final File fnExecutable = new File(FnUtils.getFnExecutablePath(project))

    FnInstallCliTask() {
        group = 'fn'
        inputs.property('fn.installed', fnExecutable.exists())
    }

    @TaskAction
    void install() {
        logger.info('Fetching latest version tag of FN CLI...')
        Object latest = new JsonSlurper().parseText(RELEASES_API.toURL().text)
        String tag = latest['tag_name']

        URL downloadLink
        if (Os.isFamily(Os.FAMILY_UNIX)) {
            downloadLink = "$DOWNLOAD_API/$tag/fn_linux".toURL()
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            downloadLink = "$DOWNLOAD_API/$tag/fn_mac".toURL()
        } else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            downloadLink = "$DOWNLOAD_API/$tag/fn.exe".toURL()
        } else {
            throw new GradleException('Operating system not supported')
        }

        fnExecutable.parentFile.mkdirs()

        logger.info("Downloading FN CLI from $downloadLink...")
        Channels.newChannel(downloadLink.openStream()).withCloseable { rbc ->
            new FileOutputStream(fnExecutable).withStream { fos ->
                fos.channel.transferFrom(rbc, 0L, Long.MAX_VALUE)
            }
        }

        fnExecutable.setExecutable(true, true)

        logger.info("FN CLI executable available at $fnExecutable")
    }
}
