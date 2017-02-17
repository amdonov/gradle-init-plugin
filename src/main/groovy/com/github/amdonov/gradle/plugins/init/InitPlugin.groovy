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

import com.netflix.gradle.plugins.packaging.SystemPackagingBasePlugin
import com.netflix.gradle.plugins.packaging.SystemPackagingTask
import com.netflix.gradle.plugins.rpm.Rpm
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectCollection

/*
Heavily copies from ospackage daemon plugin from Netflix.
 */

class InitPlugin implements Plugin<Project> {

    Project project
    InitExtension extension

    Map<String, Object> toContext(InitDefinition definitionDefaults, InitDefinition definition) {
        return [
                daemonName   : definition.daemonName ?: definitionDefaults.daemonName,
                command      : definition.command,
                user         : definition.user ?: definitionDefaults.user,
                group        : definition.group ?: definitionDefaults.group,
                runLevels    : definition.runLevels ?: definitionDefaults.runLevels,
                startSequence: definition.startSequence ?: definitionDefaults.startSequence,
                stopSequence : definition.stopSequence ?: definitionDefaults.stopSequence,
                createUser   : definition.createUser ?: definitionDefaults.createUser,
                userShell    : definition.userShell ?: definitionDefaults.userShell
        ]
    }

    @Override
    void apply(Project project) {
        this.project = project
        project.plugins.apply(SystemPackagingBasePlugin)

        // TODO Use NamedContainerProperOrder so that we can create tasks for each definition as they appear
        DomainObjectCollection<InitDefinition> daemonsList = new DefaultDomainObjectCollection<>(InitDefinition, [])
        extension = project.extensions.create('daemons', InitExtension, daemonsList)

        // Add daemon to project
        project.ext.daemon = { Closure closure ->
            extension.init(closure)
        }

        daemonsList.all { InitDefinition definition ->
            // Check existing name
            def sameName = daemonsList.any { !it.is(definition) && it.daemonName == definition.daemonName }
            if (sameName) {
                if (definition.daemonName) {
                    throw new IllegalArgumentException("A daemon with the name ${definition.daemonName} is already defined")
                } else {
                    throw new IllegalArgumentException("A daemon with no name, and hence the default, is already defined")
                }
            }

            project.tasks.withType(SystemPackagingTask) { SystemPackagingTask task ->
                def isRedhat = task instanceof Rpm
                InitDefinition defaults = getDefaultDaemonDefinition()

                // Calculate daemonName really early, but everything else can be done later.
                // tasks' package name wont' exists if it's a docker task
                def daemonName = definition.daemonName ?: defaults.daemonName ?: task.getPackageName() ?: project.name

                if (!daemonName) {
                    throw new IllegalArgumentException("Unable to find a name on definition ${definition}")
                }
                String cleanedName = daemonName.replaceAll("\\W", "").capitalize()

                def outputDir = new File(project.buildDir, "daemon/${cleanedName}/${task.name}")

                def mapping = [
                        'initd': "/etc/rc.d/init.d/${daemonName}"
                ]



                def templateTask = project.tasks.create("${task.name}${cleanedName}Daemon", InitTemplateTask)
                templateTask.conventionMapping.map('destDir') { outputDir }
                templateTask.conventionMapping.map('context') {
                    Map<String, String> context = toContext(defaults, definition)
                    context.daemonName = daemonName
                    context.isRedhat = true
                    context
                }
                templateTask.conventionMapping.map('templates') { mapping.keySet() }

                task.dependsOn(templateTask)
                mapping.each { String templateName, String destPath ->
                    File rendered = new File(outputDir, templateName) // To be created by task, ok that it's not around yet

                    // Gradle CopySpec can't set the name of a file on the fly, we need to do a rename.
                    def slashIdx = destPath.lastIndexOf('/')
                    def destDir = destPath.substring(0, slashIdx)
                    def destFile = destPath.substring(slashIdx + 1)
                    task.from(rendered) {
                        into(destDir)
                        rename('.*', destFile)
                        fileMode 0555 // Since source files don't have the correct permissions
                        user 'root'
                    }
                }

                task.doFirst {
                    task.postInstall("/sbin/chkconfig ${daemonName} on")
                    task.preUninstall("if [ \"\$1\" = \"0\" ]; then\n" +
                            "    /etc/rc.d/init.d/${daemonName} stop >/dev/null 2>&1\n" +
                            "    /sbin/chkconfig --del ${daemonName}\n" +
                            "fi")

                    if (templateTask.getContext().createUser) {
                        def user = definition.user ?: defaults.user
                        def userShell = definition.userShell ?: defaults.userShell
                        def group = definition.group ?: defaults.group

                        task.preInstall("/usr/sbin/groupadd -r ${group} 2>/dev/null || :\n" +
                                "/usr/sbin/useradd -g ${group} \\ \n" +
                                "    -s ${userShell} -r ${user} 2>/dev/null || :")
                    }

                }
            }
        }

    }

    def getDefaultDaemonDefinition() {
        new InitDefinition(null, null, 'root', 'root', [3, 4, 5], 85, 15, Boolean.FALSE, '/sbin/nologin')
    }
}
