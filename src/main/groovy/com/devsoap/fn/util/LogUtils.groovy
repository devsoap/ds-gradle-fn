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
package com.devsoap.fn.util

import groovy.util.logging.Log
import org.gradle.api.Project

import java.nio.charset.StandardCharsets
import java.util.logging.Level

/**
 * Utilities for logging
 *
 * @author John Ahlroos
 * @since 1.0
 */
class LogUtils {

    static final OutputStream getLogOutputStream(Level level) {
        new LogOutputStream(level)
    }

    static final void printIfNotPrintedBefore(Project project, String message) {
        SingletonPrinter.instance.printIfNotPrintedBefore(project, message)
    }

    @Log('LOGGER')
    private static class LogOutputStream extends OutputStream {

        private String mem = ''

        private final Level level

        LogOutputStream(Level level) {
            this.level = level
        }

        @Override
        void write(int i) throws IOException {
            byte[] bytes = new byte[1]
            bytes[0] = (byte) (i & 0xff)
            mem = mem + new String(bytes, StandardCharsets.UTF_8)
            if (mem.endsWith ('\n')) {
                mem = mem[0..(mem.length() - 1)]
                flush()
            }
        }

        @Override
        void flush() throws IOException {
            if (mem.trim() != '') {
                LOGGER.log(level, mem)
                mem = ''
            }
        }
    }

    @Singleton(lazy = false, strict = true)
    private static class SingletonPrinter {

        private final Map<String, Boolean> messageStatus = [:]

        void printIfNotPrintedBefore(Project project, String message) {
            messageStatus.putIfAbsent(message, true)
            if ( messageStatus[message] ) {
                if (project.logger.lifecycleEnabled) {
                    project.logger.lifecycle(message)
                }
                messageStatus[message] = false
            }
        }
    }
}
