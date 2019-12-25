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
package com.devsoap.fn

import static junit.framework.TestCase.fail
import static org.gradle.testkit.runner.TaskOutcome.FAILED

import org.gradle.testkit.runner.BuildResult

/**
 * Base class for functional tests that requires a running FN server
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FunctionalServerTest extends FunctionalTest {

    protected void setup() {
        BuildResult result = run('fnStart')
        if (result.task(':fnStart').outcome == FAILED) {
            fail('Failed to start FN server')
        } else {
            println 'Successfully started FN server!'
        }
    }

    protected void cleanup() {
        BuildResult result = run('fnStop')
        if (result.task(':fnStop').outcome == FAILED) {
            fail('Failed to stio FN server')
        } else {
            println 'Successfully stopped FN server!'
        }
    }
}
