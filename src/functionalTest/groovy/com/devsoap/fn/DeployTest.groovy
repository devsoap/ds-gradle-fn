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
package com.devsoap.fn

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

import spock.lang.Ignore
import org.gradle.testkit.runner.BuildResult

/**
 * Tests function deployment
 *
 * @author John Ahlroos
 * @since 1.0
 */
class DeployTest extends FunctionalServerTest {

    /**
     * FIXME For some reason Travis cannot connect to the server after it has started
     */
    @Ignore
    void 'deploy default function'() {
        setup:
            run'fnCreateFunction'
        when:
            BuildResult deployResult = run'fnDeploy'
            BuildResult invokeResult = run 'fnInvoke'
        then:
            deployResult.task(':fnDeploy').outcome == SUCCESS
            deployResult.output.contains('Successfully created function')
            deployResult.output.contains('Successfully created trigger')

            invokeResult.task(':fnInvoke').outcome == SUCCESS
            invokeResult.output.contains('Hello, world!')
    }

}
