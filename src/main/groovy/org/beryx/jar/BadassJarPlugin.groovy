/*
 * Copyright 2021 the original author or authors.
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
package org.beryx.jar


import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.JavaPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion

@CompileStatic
class BadassJarPlugin implements Plugin<Project> {
    private final Logger LOGGER = PluginLogger.of(BadassJarPlugin)

    @Override
    void apply(Project project) {
        if(GradleVersion.current() < GradleVersion.version('7.1')) {
            throw new GradleException("This plugin requires Gradle 7.1 or newer. Update your Gradle or use version 1.2.0 of this plugin.")
        }
        def moduleConfigExtension = project.extensions.create("moduleConfig", ModuleConfigExtension, project)

        project.afterEvaluate {
            Jar jarTask = (Jar)project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)
            if(!jarTask) {
                LOGGER.warn("The badass-jar plugin has been disabled because the jar task cannot be found.")
            } else {
                new JarTaskConfigurer(jarTask, moduleConfigExtension).configure()
            }
        }
    }
}
