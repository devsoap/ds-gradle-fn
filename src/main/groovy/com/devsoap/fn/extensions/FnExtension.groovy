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
package com.devsoap.fn.extensions

import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * Generic configuration for the plugin
 *
 * @author John Ahlroos
 * @since 1.0
 */
class FnExtension {

    static final String NAME = 'fn'

    private final Property<String> functionClass
    private final Property<String> functionMethod

    FnExtension(Project project) {
        functionClass = project.objects.property(String)
        functionMethod = project.objects.property(String)
    }

    /**
     * Set the function class name (FQN)
     *
     * @param fc
     *      the function class FQN
     */
    void setFunctionClass(String fc) {
        functionClass.set(fc)
    }

    /**
     * The function method name to call with the request
     */
    void setFunctionMethod(String fm) {
        functionMethod.set(fm)
    }

    /**
     * Get the function class name (FQN)
     */
    String getFunctionClass() {
        functionClass.getOrElse('MyFunction')
    }

    /**
     * Get the function method name
     */
    String getFunctionMethod() {
        functionMethod.getOrElse('handleRequest')
    }
}
