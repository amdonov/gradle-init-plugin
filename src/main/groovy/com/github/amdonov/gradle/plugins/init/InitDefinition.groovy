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

/**
 * Template variables. Fields start off empty an only at
 */
@Canonical
class InitDefinition {

    String daemonName // defaults to packageName
    String command // Required
    String user // defaults to "root"
    String group // defaults to "root"
    List<Integer> runLevels = new LinkedList<>() // rpm default == [3,4,5]
    Boolean autoStart // default true
    Integer startSequence // default 85
    Integer stopSequence // default 15
    Boolean createUser // default true
}
