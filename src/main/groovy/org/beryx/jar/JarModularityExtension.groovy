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

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.gradle.api.Project
import org.gradle.api.provider.Property

@CompileStatic
class JarModularityExtension {
    private final Project project
    final Property<String> moduleInfoPath
    final Property<Boolean> multiRelease
    final Property<String> version

    static class JarModularityData {
        final String moduleInfoPath
        final boolean multiRelease
        final String version

        JarModularityData(String moduleInfoPath, boolean multiRelease, String version) {
            this.moduleInfoPath = moduleInfoPath
            this.multiRelease = multiRelease
            this.version = version
        }
    }

    JarModularityExtension(Project project) {
        this.project = project;

        moduleInfoPath = project.objects.property(String)
        moduleInfoPath.set('')

        multiRelease = project.objects.property(Boolean)
        multiRelease.set(true)

        version = project.objects.property(String)
    }

    JarModularityData getData() {
        new JarModularityData(
                moduleInfoPath.get(),
                multiRelease.get(),
                version.getOrElse(project.version as String)
        )
    }
}
