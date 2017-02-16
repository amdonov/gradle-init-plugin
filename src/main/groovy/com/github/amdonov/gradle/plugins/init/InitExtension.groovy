/*
 * Copyright 2014-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.amdonov.gradle.plugins.init

import groovy.transform.Canonical
import org.gradle.api.DomainObjectCollection
import org.gradle.util.ConfigureUtil

@Canonical
class InitExtension {
    final DomainObjectCollection<InitDefinition> daemons

    // TBD Add defaults, like user name for all daemons

    def init(Closure configure) {
        def definition = new InitDefinition()
        ConfigureUtil.configure(configure, definition)
        daemons.add(definition)
        return definition
    }
}
